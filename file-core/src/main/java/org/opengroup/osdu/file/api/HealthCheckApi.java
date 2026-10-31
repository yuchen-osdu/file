/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.file.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Unauthenticated probes. Empty {@code @SecurityRequirements} clears the global bearer
 * scheme so the spec publishes {@code security: []}. Mappings omit {@code produces}
 * because probes often send {@code Accept: text/plain}.
 */
@RestController
@RequestScope
@Tag(name = "health-check-api", description = "Health Check API")
public class HealthCheckApi {

  @Operation(summary = "${healthChecksApi.livenessCheck.summary}",
      description = "${healthChecksApi.livenessCheck.description}", tags = { "health-check-api" })
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "502", description = "Bad Gateway",
          content = {@Content(schema = @Schema(implementation = AppError.class))}),
      @ApiResponse(responseCode = "503", description = "Service Unavailable",
          content = {@Content(schema = @Schema(implementation = AppError.class))})
  })
  @SecurityRequirements
  @PermitAll
  @GetMapping("/v2/liveness_check")
  public ResponseEntity<Void> livenessCheck() {
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "${healthChecksApi.readinessCheck.summary}",
      description = "${healthChecksApi.readinessCheck.description}", tags = { "health-check-api" })
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "502", description = "Bad Gateway",
          content = {@Content(schema = @Schema(implementation = AppError.class))}),
      @ApiResponse(responseCode = "503", description = "Service Unavailable",
          content = {@Content(schema = @Schema(implementation = AppError.class))})
  })
  @SecurityRequirements
  @PermitAll
  @GetMapping("/v2/readiness_check")
  public ResponseEntity<Void> readinessCheck() {
    return ResponseEntity.ok().build();
  }

}
