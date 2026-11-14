/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.file.exception.handler;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.StdSerializer;

@Data
@Builder
public class ApiError {

  // Jackson 3 writes enums with toString() ("400 BAD_REQUEST"); keep the Jackson 2 wire value "BAD_REQUEST".
  @JsonSerialize(using = HttpStatusNameSerializer.class)
  private HttpStatus status;
  private String message;
  private List<String> errors;

  static class HttpStatusNameSerializer extends StdSerializer<HttpStatus> {

    HttpStatusNameSerializer() {
      super(HttpStatus.class);
    }

    @Override
    public void serialize(HttpStatus value, JsonGenerator generator, SerializationContext context) {
      generator.writeString(value.name());
    }
  }
}
