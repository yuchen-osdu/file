package org.opengroup.osdu.file.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class WebConfigurationTest {

  private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
      .addFilters(new WebConfiguration().trailingSlashFilter().getFilter())
      .build();

  @ParameterizedTest
  @ValueSource(strings = {"/v2/files/uploadURL", "/v2/files/uploadURL/"})
  void shouldPreserveBodyAndQueryWithEitherTrailingSlash(String path) throws Exception {
    mvc.perform(post(path).queryParam("expiryTime", "1H")
            .contentType(MediaType.APPLICATION_JSON).content("{\"FileID\":\"f1\"}"))
        .andExpect(status().isOk())
        .andExpect(content().string("1H:{\"FileID\":\"f1\"}"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/v2/files/opendes:dataset--File.Generic:1/metadata",
      "/v2/files/opendes:dataset--File.Generic:1/metadata/"})
  void shouldMatchPathVariableEndpointWithEitherTrailingSlash(String path) throws Exception {
    mvc.perform(get(path))
        .andExpect(status().isOk())
        .andExpect(content().string("opendes:dataset--File.Generic:1"));
  }

  @RestController
  static class ProbeController {
    @PostMapping("/v2/files/uploadURL")
    public String upload(@RequestParam("expiryTime") String expiryTime, @RequestBody String body) {
      return expiryTime + ":" + body;
    }

    @GetMapping("/v2/files/{id}/metadata")
    public String metadata(@PathVariable("id") String id) {
      return id;
    }
  }
}
