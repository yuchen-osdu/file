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

package org.opengroup.osdu.file.stepdefs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import com.google.inject.Inject;
import io.cucumber.java8.En;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.file.FileListRequest;
import org.opengroup.osdu.core.common.model.file.FileLocationRequest;
import org.opengroup.osdu.core.common.model.file.FileLocationResponse;
import org.opengroup.osdu.core.common.model.file.LocationRequest;
import org.opengroup.osdu.core.common.model.file.LocationResponse;
import org.opengroup.osdu.core.test.client.ClientException;
import org.opengroup.osdu.core.test.client.FileClient;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.file.constants.TestConstants;
import org.opengroup.osdu.file.stepdefs.model.FileScope;
import org.opengroup.osdu.file.util.ErrorResponseAssertions;
import org.opengroup.osdu.file.util.FileApiJson;
import org.opengroup.osdu.file.util.FileClientExceptionSupport;
import org.opengroup.osdu.file.util.FileUtils;

@Slf4j
@SuppressWarnings("unused")
public class FileStepDef_GetLocation_FileLocation_FileList implements En {

  private static final String HEADER_AUTHORIZATION = "Authorization";
  private static final String HEADER_DATA_PARTITION_ID = "data-partition-id";

  @Inject
  private FileScope context;

  public FileStepDef_GetLocation_FileLocation_FileList() {

    Given("I hit File service GetFileLocation API with non-existing file id", () -> {
      FileClientExceptionSupport.invokeExpectingFailure(() -> postFileLocationRequest(
          fileLocationRequest(CommonUtility.generateUniqueFileID()),
          partitionHeaders(TestConstants.PRIVATE_TENANT1)), context);
    });

    Then("service should respond back with {string}", (String expectedReponseStatusCode) -> {
      String actualStatusCode = String.valueOf(this.context.getLastStatusCode());
      assertTrue("Expected status - " + expectedReponseStatusCode + " ; Actual status code - " + actualStatusCode,
          expectedReponseStatusCode.equalsIgnoreCase(actualStatusCode));
    });

    Then("service should respond back with {string} or {string}",
        (String expectedResponseStatusCode, String alternateResponseCode) -> {
          String actualStatusCode = String.valueOf(this.context.getLastStatusCode());
          assertTrue(expectedResponseStatusCode.equalsIgnoreCase(actualStatusCode)
              || alternateResponseCode.equalsIgnoreCase(actualStatusCode));
        });

    Given("I hit File service {string} with invalid partition id", (String apiName) -> {
      FileClientExceptionSupport.invokeExpectingFailure(
          () -> postEmptyBodyRequest(apiName, invalidPartitionHeaders()), context);
    });

    Given("I hit File service {string} without partition id", (String apiName) -> {
      FileClientExceptionSupport.invokeExpectingFailure(
          () -> postEmptyBodyRequest(apiName, withoutPartitionHeaders()), context);
    });

    Given("I hit File service {string} without auth token", (String apiName) -> {
      FileClientExceptionSupport.invokeExpectingFailure(
          () -> postEmptyBodyRequest(apiName, withoutAuthHeaders()), context);
    });

    Given("I hit File service {string} with invalid auth token", (String apiName) -> {
      FileClientExceptionSupport.invokeExpectingFailure(
          () -> postEmptyBodyRequest(apiName, invalidAuthHeaders()), context);
    });

    Given("I hit File service GetFileLocation API with {string}", (String bodyContent) -> {
      if (bodyContent.equalsIgnoreCase("emptyReqBody")) {
        FileClientExceptionSupport.invokeExpectingFailure(
            () -> postFileLocationValidationErrorRequest(partitionHeaders(TestConstants.PRIVATE_TENANT1)),
            context);
      } else if (bodyContent.equalsIgnoreCase("invalidFileId")) {
        FileClientExceptionSupport.invokeExpectingFailure(
            () -> postFileLocationErrorRequest(fileLocationRequest("test"),
                partitionHeaders(TestConstants.PRIVATE_TENANT1)),
            context);
      }
    });

    Then("service should respond back with {string} and {string}",
        (String expectedReponseStatusCode, String expectedReponseMessage) -> {
          String actualStatusCode = String.valueOf(this.context.getLastStatusCode());
          assertTrue("Expected status - " + expectedReponseStatusCode + " ; Actual status code - "
              + actualStatusCode, expectedReponseStatusCode.equalsIgnoreCase(actualStatusCode));
          ErrorResponseAssertions.assertFileServiceErrorMessage(expectedReponseMessage,
              context.getClientException());
        });

    Then("service should respond back with {string} and error message {string}",
        (String expectedReponseStatusCode, String expectedReponseMessage) -> {
          String actualStatusCode = String.valueOf(this.context.getLastStatusCode());
          assertTrue("Expected status - " + expectedReponseStatusCode + " ; Actual status code - "
              + actualStatusCode, expectedReponseStatusCode.equalsIgnoreCase(actualStatusCode));
          ErrorResponseAssertions.assertApiErrorMessage(expectedReponseMessage,
              context.getClientException());
        });

    Given("I hit File service GetLocation API with {string}", (String bodyContent) -> {
      LocationRequest request;
      if (bodyContent.equalsIgnoreCase("invalid file location")) {
        request = locationRequest("/" + CommonUtility.generateUniqueFileID());
      } else if (bodyContent.equalsIgnoreCase("fileId legth exceeding limit")) {
        request = locationRequest(CommonUtility.generateFileIDExceedingLegthLimit());
      } else {
        return;
      }
      FileClientExceptionSupport.invokeExpectingFailure(
          () -> postLocationRequest(request, partitionHeaders(TestConstants.PRIVATE_TENANT1)), context);
    });

    Given("I hit File service GetLocation API with existing file id", () -> {
      LocationRequest request = locationRequest(CommonUtility.generateUniqueFileID());
      Map<String, String> headers = partitionHeaders(TestConstants.PRIVATE_TENANT1);
      HttpResponse<LocationResponse> response = postLocationRequest(request, headers);
      String actualStatusCode = String.valueOf(response.statusCode());
      assertTrue("Expected status - 200; Actual status code - " + actualStatusCode,
          "200".equalsIgnoreCase(actualStatusCode));
      FileClientExceptionSupport.invokeExpectingFailure(
          () -> postLocationRequest(request, headers), context);
    });

    Given("I hit File service GetFileList API with {string}", (String inputPayload) -> {
      FileListRequest request = FileApiJson.readFileListRequest(inputPayload);
      runExpectingClientException(() -> context.getFileClient().getFileList(request,
          partitionHeaders(TestConstants.PRIVATE_TENANT1)));
    });

    Given("I hit File service GetLocation API without File Id", () -> {
      HttpResponse<LocationResponse> response = postLocationRequest(
          LocationRequest.builder().build(),
          partitionHeaders(TestConstants.PRIVATE_TENANT1));
      this.context.setLocationResponse(response);
    });

    Then("service should respond back with {string} , File Id and Signed URL",
        (String expectedReponseStatusCode) -> {
          String actualStatusCode = String.valueOf(this.context.getLastStatusCode());
          assertTrue("Expected status - " + expectedReponseStatusCode + " ; Actual status code - "
              + actualStatusCode, expectedReponseStatusCode.equalsIgnoreCase(actualStatusCode));
          LocationResponse locationResponse = context.getLocationResponse().body();
          assertFalse(locationResponse.getFileID().isEmpty());
          assertFalse(locationResponse.getLocation().get("FileSource").isEmpty());
          assertFalse(locationResponse.getLocation().get("SignedURL").isEmpty());
        });

    Given("I hit File service GetLocation API with a File Id", () -> {
      HttpResponse<LocationResponse> response = postLocationRequest(locationRequest(CommonUtility.generateUniqueFileID()),
          partitionHeaders(TestConstants.PRIVATE_TENANT1));
      this.context.setLocationResponse(response);
    });

    Given("I hit File service GetFileLocation API with same File Id", () -> {
      LocationRequest request = locationRequest(CommonUtility.generateUniqueFileID());
      HttpResponse<LocationResponse> locationResult =
          postLocationRequest(request, partitionHeaders(TestConstants.PRIVATE_TENANT1));
      String responseFileId = locationResult.body().getFileID();
      log.info("File Id generated by getLocation - {}", responseFileId);
      HttpResponse<FileLocationResponse> fileLocationResult = postFileLocationRequest(
          fileLocationRequest(responseFileId), partitionHeaders(TestConstants.PRIVATE_TENANT1));
      this.context.setFileLocationResponse(fileLocationResult);
    });

    Then("service should respond back with {string} and UnSigned URL", (String expectedReponseStatusCode) -> {
      String actualStatusCode = String.valueOf(this.context.getLastStatusCode());
      assertTrue("Expected status - " + expectedReponseStatusCode + " ; Actual status code - " + actualStatusCode,
          expectedReponseStatusCode.equalsIgnoreCase(actualStatusCode));
      FileLocationResponse fileLocationResponse = context.getFileLocationResponse().body();
      assertFalse(fileLocationResponse.getLocation().isEmpty());
    });
  }

  private static Map<String, String> partitionHeaders(String partition) {
    return Map.of(HEADER_DATA_PARTITION_ID, partition);
  }


  private static Map<String, String> invalidPartitionHeaders() {
    return Map.of(HEADER_DATA_PARTITION_ID, "invalidPartitionName");
  }

  private static Map<String, String> withoutPartitionHeaders() {
    return Map.of(HEADER_DATA_PARTITION_ID, "");
  }

  private static Map<String, String> withoutAuthHeaders() {
    return Map.of(HEADER_AUTHORIZATION, "");
  }

  private Map<String, String> invalidAuthHeaders() {
    return Map.of(HEADER_AUTHORIZATION,
        "Bearer invalid");
  }

  private void postEmptyBodyRequest(String apiName, Map<String, String> headerOverrides) {
    switch (apiName) {
      case "GetFileLocation" -> context.getFileClient().getFileLocation(
          fileLocationRequest(null), headerOverrides);
      case "GetFileList" -> context.getFileClient().getFileList(
          FileListRequest.builder().build(), headerOverrides);
      default -> throw new IllegalArgumentException("Unknown API: " + apiName);
    }
  }

  private static LocationRequest locationRequest(String fileId) {
    return LocationRequest.builder().fileID(fileId).build();
  }

  private static FileLocationRequest fileLocationRequest(String fileId) {
    return FileLocationRequest.builder().fileID(fileId).build();
  }

  private HttpResponse<LocationResponse> postLocationRequest(LocationRequest request, Map<String, String> headerMap) {
    return context.getFileClient().getLocation(request, headerMap);
  }

  private HttpResponse<FileLocationResponse> postFileLocationRequest(FileLocationRequest request,
      Map<String, String> headerMap) {
    return context.getFileClient().getFileLocation(request, headerMap);
  }

  private void postFileLocationValidationErrorRequest(Map<String, String> headerMap) {
    context.getFileClient().getFileLocation("{}", headerMap,
        FileLocationResponse.class);
  }

  private void postFileLocationErrorRequest(FileLocationRequest request, Map<String, String> headerMap) {
    context.getFileClient().getFileLocation(request, headerMap);
  }

  private void runExpectingClientException(Runnable action) {
    try {
      action.run();
      fail("Expected ClientException");
    } catch (ClientException exception) {
      context.recordClientException(exception);
    }
  }
}
