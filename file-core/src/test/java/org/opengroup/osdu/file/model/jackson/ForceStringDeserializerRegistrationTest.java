package org.opengroup.osdu.file.model.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

class ForceStringDeserializerRegistrationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
      .withUserConfiguration(ForceStringDeserializer.class);

  record Probe(String fileId) {
  }

  @Test
  void bootJsonMapperShouldRejectNumbersForStringFields() {
    contextRunner.run(context -> {
      JsonMapper mapper = context.getBean(JsonMapper.class);

      assertThat(mapper.readValue("{\"fileId\":\"f1\"}", Probe.class).fileId()).isEqualTo("f1");
      assertThatThrownBy(() -> mapper.readValue("{\"fileId\":1}", Probe.class))
          .isInstanceOf(MismatchedInputException.class)
          .hasMessageContaining("Attempted to parse int to string");
    });
  }
}
