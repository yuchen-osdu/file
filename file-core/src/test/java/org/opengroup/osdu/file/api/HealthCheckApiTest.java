package org.opengroup.osdu.file.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(SpringExtension.class)
public class HealthCheckApiTest {

  @InjectMocks
  private HealthCheckApi healthCheckApi;

  @Test
  public void test_livenessCheck() {
    ResponseEntity<Void> response = healthCheckApi.livenessCheck();
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  public void test_readinessCheck() {
    ResponseEntity<Void> response = healthCheckApi.readinessCheck();
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(response.getBody());
  }
}
