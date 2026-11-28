package org.opengroup.osdu.file.coreplus.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.cfg.GenericConstraintDef;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opengroup.osdu.core.common.model.file.FileLocationRequest;
import org.opengroup.osdu.file.validation.FileIdValidator;
import org.opengroup.osdu.file.validation.FileLocationRequestValidatorWrapper;
import org.opengroup.osdu.file.validation.annotation.ValidFileLocationRequest;

class FileLocationRequestValidatorTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    HibernateValidatorConfiguration configuration = Validation.byProvider(HibernateValidator.class).configure();
    ConstraintMapping mapping = configuration.createConstraintMapping();
    mapping.type(FileLocationRequest.class)
        .constraint(new GenericConstraintDef<>(ValidFileLocationRequest.class));
    ConstraintValidatorFactory validators = mock(ConstraintValidatorFactory.class);
    when(validators.getInstance(FileLocationRequestValidatorWrapper.class))
        .thenReturn(new FileLocationRequestValidatorWrapper(
            new FileLocationRequestValidator(new FileIdValidator())));
    factory = configuration.addMapping(mapping).constraintValidatorFactory(validators).buildValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void close() {
    factory.close();
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 1024})
  void acceptsFileIdsUpToTheStoragePathLimit(int length) {
    assertTrue(violations("a".repeat(length)).isEmpty());
  }

  @Test
  void overlongFileIdProducesAFieldSpecificLengthViolation() {
    Set<ConstraintViolation<FileLocationRequest>> violations = violations("a".repeat(1025));

    assertEquals(1, violations.size());
    ConstraintViolation<FileLocationRequest> violation = violations.iterator().next();
    assertEquals("FileID", violation.getPropertyPath().toString());
    assertEquals("length should be less than resulted GCS filepath. Max: ${max_length}",
        violation.getMessageTemplate());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "invalid/path"})
  void retainsCommonFileIdValidation(String id) {
    Set<ConstraintViolation<FileLocationRequest>> violations = violations(id);

    assertEquals(1, violations.size());
    ConstraintViolation<FileLocationRequest> violation = violations.iterator().next();
    assertEquals("FileID", violation.getPropertyPath().toString());
    assertEquals("invalid/path".equals(id) ? "Invalid FileID"
        : "{jakarta.validation.constraints.NotBlank.message}", violation.getMessageTemplate());
  }

  private Set<ConstraintViolation<FileLocationRequest>> violations(String id) {
    return validator.validate(FileLocationRequest.builder().fileID(id).build());
  }
}
