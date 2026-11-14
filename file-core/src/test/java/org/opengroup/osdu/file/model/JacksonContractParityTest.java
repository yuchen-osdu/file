package org.opengroup.osdu.file.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opengroup.osdu.core.common.http.HttpConfiguration;
import org.opengroup.osdu.file.exception.handler.ApiError;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serializes and reads every file API model with the HTTP mapper the service ran with on Jackson 2
 * (os-core-common 9.x {@code HttpConfiguration#jsonObjectMapper}, which replaces Boot's mapper) and
 * with its Jackson 3 replacement, and requires the same JSON. Property order is not compared: Jackson 3
 * sorts properties alphabetically and JSON objects are unordered (same rule as os-core-common's
 * Jackson3ModelParityTest).
 */
class JacksonContractParityTest {

  private static final com.fasterxml.jackson.databind.ObjectMapper JACKSON2 =
      new com.fasterxml.jackson.databind.ObjectMapper()
          .registerModule(new JavaTimeModule())
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
          .setTimeZone(TimeZone.getDefault());

  private static final JsonMapper JACKSON3 = new HttpConfiguration().jsonObjectMapper();

  static Stream<Class<?>> apiModels() throws ClassNotFoundException {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false) {
          @Override
          protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
            return definition.getMetadata().isConcrete()
                && !definition.getMetadata().isAnnotation();
          }
        };
    scanner.addIncludeFilter((reader, factory) -> true);
    List<Class<?>> classes = new ArrayList<>();
    for (String pkg : List.of("org.opengroup.osdu.file.model", "org.opengroup.osdu.file.errors")) {
      for (BeanDefinition definition : scanner.findCandidateComponents(pkg)) {
        Class<?> type = Class.forName(definition.getBeanClassName());
        if (!type.isEnum() && !type.isAnonymousClass() && !type.isLocalClass()
            && !type.getPackageName().endsWith(".jackson")
            && !type.getPackageName().endsWith(".property")
            && !type.getSimpleName().endsWith("Builder")
            && !type.getName().contains("Test")) {
          classes.add(type);
        }
      }
    }
    classes.add(ApiError.class);
    return classes.stream();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("apiModels")
  void populatedModelSerializesIdenticallyAndRoundTrips(Class<?> type) throws Exception {
    Object model = populate(type, type.getSimpleName(), 0);
    String expected = JACKSON2.writeValueAsString(model);

    assertSameJson(JACKSON3.writeValueAsString(model), expected);
    if (type == ApiError.class) {
      return;
    }
    Object jackson2Read;
    try {
      jackson2Read = JACKSON2.readValue(expected, type);
    } catch (com.fasterxml.jackson.core.JacksonException e) {
      // Response-only types without a usable creator; Jackson 3 may detect a constructor here, which only
      // accepts more input.
      return;
    }
    assertSameJson(JACKSON3.writeValueAsString(JACKSON3.readValue(expected, type)),
        JACKSON2.writeValueAsString(jackson2Read));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("apiModels")
  void emptyModelSerializesIdentically(Class<?> type) throws Exception {
    Object model = instantiate(type, type.getSimpleName());
    assertSameJson(JACKSON3.writeValueAsString(model), JACKSON2.writeValueAsString(model));
  }

  private static void assertSameJson(String actual, String expected) throws Exception {
    assertThat(JACKSON2.readTree(actual)).as(actual).isEqualTo(JACKSON2.readTree(expected));
  }

  @Test
  void httpStatusIsWrittenByName() {
    ApiError error = ApiError.builder().status(HttpStatus.BAD_REQUEST).message("m").build();
    assertThat(JACKSON3.writeValueAsString(error)).contains("\"status\":\"BAD_REQUEST\"");
  }

  private static Object instantiate(Class<?> type, String name) throws Exception {
    if (type == ApiError.class) {
      return ApiError.builder().build();
    }
    Constructor<?> constructor = Arrays.stream(type.getDeclaredConstructors())
        .min(Comparator.comparingInt(Constructor::getParameterCount))
        .orElseThrow();
    constructor.setAccessible(true);
    Type[] parameters = constructor.getGenericParameterTypes();
    Object[] args = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      args[i] = sample(parameters[i], name + ".arg" + i, 4);
    }
    return constructor.newInstance(args);
  }

  private static Object populate(Class<?> type, String name, int depth) throws Exception {
    Object model = instantiate(type, name);
    for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field field : c.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
          continue;
        }
        Object value = sample(field.getGenericType(), name + "." + field.getName(), depth);
        if (value != null) {
          field.setAccessible(true);
          field.set(model, value);
        }
      }
    }
    return model;
  }

  private static Object sample(Type type, String name, int depth) throws Exception {
    if (type instanceof ParameterizedType parameterized) {
      Class<?> raw = (Class<?>) parameterized.getRawType();
      Type[] args = parameterized.getActualTypeArguments();
      if (List.class.isAssignableFrom(raw)) {
        List<Object> list = new ArrayList<>();
        list.add(sample(args[0], name + "[0]", depth));
        return list;
      }
      if (Set.class.isAssignableFrom(raw)) {
        Set<Object> set = new LinkedHashSet<>();
        set.add(sample(args[0], name + "[0]", depth));
        return set;
      }
      if (Map.class.isAssignableFrom(raw)) {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("zKey", sample(args[1], name + ".zKey", depth));
        map.put("aKey", sample(args[1], name + ".aKey", depth));
        map.put("nullKey", null);
        return map;
      }
      return null;
    }
    if (type instanceof GenericArrayType arrayType) {
      ParameterizedType component = (ParameterizedType) arrayType.getGenericComponentType();
      Object array = Array.newInstance((Class<?>) component.getRawType(), 1);
      Array.set(array, 0, sample(component, name + "[0]", depth));
      return array;
    }
    if (!(type instanceof Class<?> raw)) {
      return name;
    }
    if (raw == Map.class) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("zKey", name + ".zKey");
      map.put("aKey", name + ".aKey");
      map.put("nullKey", null);
      return map;
    }
    if (raw == List.class) {
      return new ArrayList<>(List.of(name + "[0]"));
    }
    if (raw == String.class || raw == Object.class) {
      return name;
    }
    if (raw == Integer.class || raw == int.class) {
      return 7;
    }
    if (raw == Long.class || raw == long.class) {
      return 9L;
    }
    if (raw == Boolean.class || raw == boolean.class) {
      return true;
    }
    if (raw == Double.class || raw == double.class) {
      return 1.5d;
    }
    if (raw.isArray()) {
      Object array = Array.newInstance(raw.getComponentType(), 1);
      Array.set(array, 0, sample(raw.getComponentType(), name + "[0]", depth));
      return array;
    }
    if (raw.isEnum()) {
      return raw == HttpStatus.class ? HttpStatus.BAD_REQUEST : raw.getEnumConstants()[0];
    }
    if (raw.getName().startsWith("org.opengroup.osdu") && !raw.isInterface()
        && !Modifier.isAbstract(raw.getModifiers()) && depth < 4) {
      return populate(raw, name, depth + 1);
    }
    return null;
  }
}
