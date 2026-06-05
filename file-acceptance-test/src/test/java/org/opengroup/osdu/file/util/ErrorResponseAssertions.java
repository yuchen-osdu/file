/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.file.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import lombok.experimental.UtilityClass;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.test.client.ClientException;

@UtilityClass
public class ErrorResponseAssertions {

  public static void assertApiErrorResponse(String expectedPayloadPath, ClientException exception) {
    assertNotNull("ClientException is null", exception);
    AppError error = requireError(exception);
    String expectedJson = readPayload(expectedPayloadPath);
    String actualJson = requireErrorBody(error);
    assertEquals(parseJson(expectedJson), parseJson(actualJson));
  }

  public static void assertFileServiceErrorMessage(String expectedMessage, ClientException exception) {
    assertErrorMessage(expectedMessage, exception);
  }

  public static void assertApiErrorMessage(String expectedMessage, ClientException exception) {
    assertErrorMessage(expectedMessage, exception);
  }

  private static void assertErrorMessage(String expectedMessage, ClientException exception) {
    assertNotNull("ClientException is null", exception);
    AppError error = requireError(exception);
    String actualMessage = resolveErrorMessage(error);
    assertTrue("Expected message - " + expectedMessage + " ; Actual message - " + actualMessage,
        expectedMessage.equalsIgnoreCase(actualMessage));
  }

  private static AppError requireError(ClientException exception) {
    assertNotNull("AppError is null", exception.getError());
    return exception.getError();
  }

  private static String requireErrorBody(AppError error) {
    String errorBody = resolveErrorBody(error);
    assertNotNull("Error body is null", errorBody);
    return errorBody;
  }

  private static String resolveErrorBody(AppError error) {
    String reason = error.getReason();
    if (reason != null && reason.trim().startsWith("{")) {
      return reason;
    }
    String message = error.getMessage();
    if (message != null && message.trim().startsWith("{")) {
      return message;
    }
    return reason != null ? reason : message;
  }

  private static String resolveErrorMessage(AppError error) {
    String message = error.getMessage();
    if (message != null && !message.isBlank() && !message.trim().startsWith("{")) {
      return message;
    }
    return extractMessageFromJson(resolveErrorBody(error));
  }

  private static String extractMessageFromJson(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
    if (root.has("error") && root.get("error").isJsonObject()) {
      JsonObject nested = root.getAsJsonObject("error");
      if (nested.has("message")) {
        return nested.get("message").getAsString();
      }
    }
    if (root.has("message")) {
      return root.get("message").getAsString();
    }
    return json;
  }

  private static JsonElement parseJson(String json) {
    return JsonParser.parseString(json);
  }

  private static String readPayload(String payloadPath) {
    try {
      return FileUtils.readFromLocalFilePath(payloadPath);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read expected payload: " + payloadPath, exception);
    }
  }
}
