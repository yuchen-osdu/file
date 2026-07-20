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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import com.google.inject.Inject;
import io.cucumber.java8.En;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.file.LocationResponse;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.core.test.client.model.file.DownloadUrlResponse;
import org.opengroup.osdu.core.test.client.model.file.FileMetadataRecord;
import org.opengroup.osdu.file.stepdefs.model.FileScope;
import org.opengroup.osdu.file.util.ErrorResponseAssertions;
import org.opengroup.osdu.file.util.FileClientExceptionSupport;
import org.opengroup.osdu.file.util.FileUtils;

@Slf4j
@SuppressWarnings("unused")
public class FileStepDef_GET implements En {

  @Inject
  private FileScope context;

  public FileStepDef_GET() {

    Given("I generate user token and set request headers with {string}", (String tenant) -> {
      // no-op: StringHttpClient handles auth and headers automatically
    });

    Given("I hit File service GET API with missing or invalid {string} and {string}",
        (String header, String headerValue) -> {
          Map<String, String> customHeaders = new HashMap<>();
          customHeaders.put(header, headerValue);
          FileClientExceptionSupport.invokeExpectingFailure(
              () -> context.getFileClient().getUploadUrl(null, customHeaders), context);
          log.info("resp - {}", context.getLastStatusCode());
        });

    Given("I hit File service GET uploadURL API", () -> {
      HttpResponse<LocationResponse> coreResp = context.getFileClient().getUploadUrl();
      this.context.setLocationResponse(coreResp);
      setNewFileSourceValue(coreResp.body());
      setUploadSignedUrl(coreResp.body());
      assertEquals("200", String.valueOf(coreResp.statusCode()));
      log.info("resp - {}", coreResp.statusCode());
    });

    When("I try to use signed url after expiration period {string} and file path {string}",
        (String expiredURL, String inputFilePath) -> {
          int code = FileUtils.uploadFileBySignedUrl(expiredURL, inputFilePath);
          this.context.setResponseCode(Integer.toString(code));
        });

    When("I try to use signed url within expiration period and file path {string}", (String inputFilePath) -> {
      LocationResponse signedUrlResponse = context.getLocationResponse().body();
      assertNotNull(signedUrlResponse.getLocation().get("SignedURL"));
      int code = FileUtils.uploadFileBySignedUrl(
          signedUrlResponse.getLocation().get("SignedURL"), inputFilePath);
      this.context.setResponseCode(Integer.toString(code));
    });

    Then("service should respond back with a valid {string} and upload input file from {string}",
        (String respCode, String inputFilePath) -> verifySuccessfulGetSignedURLResponse(respCode, inputFilePath));

    Then("download service should respond back with a valid {string}", (String respCode) -> {
      validateResponseCode(respCode);
      assertNotNull(context.getSignedUrl());
    });

    Then("metadata service should respond back with a valid {string}", (String respCode) -> {
      validateResponseCode(respCode);
      assertNotNull(context.getMetadataRecordResponse().body());
    });

    Then("Service should respond metadata with not null checksum {string}", (String respCode) -> {
      validateResponseCode(respCode);
      FileMetadataRecord metadata = context.getMetadataRecordResponse().body();
      assertNotNull("Metadata response body is null", metadata);
      assertNotNull("Metadata data is null", metadata.data());
      assertNotNull("DatasetProperties is null", metadata.data().datasetProperties());
      assertNotNull("FileSourceInfo is null", metadata.data().datasetProperties().fileSourceInfo());
      String checksum = metadata.data().datasetProperties().fileSourceInfo().checksum();
      assertNotNull("Checksum is null in metadata response", checksum);
    });

    Then("service should respond back with error {string} and {string}", (String errorCode, String errorMsg) ->
        verifyFailedResponse(errorCode, errorMsg));

    Then("service should respond back with error {string} or {string} and {string}",
        (String errorCode, String alternateErrorCode, String errorMsg) ->
            validateResponseCode(errorCode, alternateErrorCode));

    Then("service should respond back with error code {string}", (String errorCode) ->
        assertEquals(errorCode, this.context.getResponseCode()));

    Given("I hit File service GET download signed API with a valid Id", () -> {
      String id = this.context.getId();
      HttpResponse<DownloadUrlResponse> coreResp = context.getFileClient().getDownloadUrl(id);
      this.context.setDownloadUrlResponse(coreResp);
      log.info("resp - {}", coreResp.statusCode());
    });

    Given("I hit File service GET metadata signed API with a valid Id", () -> {
      String id = this.context.getId();
      HttpResponse<FileMetadataRecord> coreResp = context.getFileClient().getMetadata(id);
      this.context.setMetadataRecordResponse(coreResp);
      log.info("resp - {}", coreResp.statusCode());
    });

    Given("I hit File service GET metadata signed API with an {string}", (String invalidId) -> {
      FileClientExceptionSupport.invokeExpectingFailure(
          () -> context.getFileClient().getMetadata(invalidId), context);
      log.info("resp - {}", context.getLastStatusCode());
    });

    When("I hit signed url to download a file within expiration period at {string}", (String outputFilePath) -> {
      String downLoadUrl = this.context.getSignedUrl();
      FileUtils.readFileBySignedUrlAndWriteToLocalFile(downLoadUrl, outputFilePath);
    });

    When("content of the file uploaded {string} and downloaded {string} files is same",
        (String outputFilePath, String inputFilePath) -> compareFileContent(outputFilePath, inputFilePath));

    When("I hit File service GET download signed API with a valid Id and expiry", () -> {
      String id = this.context.getId();
      String expiryTimeInMinutes = CommonUtility.getSignedURLExpiryTime();
      log.info("Configured Expiry Time for Signed URL is {}", expiryTimeInMinutes);
      HttpResponse<DownloadUrlResponse> coreResp =
          context.getFileClient().getDownloadUrl(id, expiryTimeInMinutes + "M", Map.of());
      this.context.setDownloadUrlResponse(coreResp);
    });

    Then("I should be able to download the file within expiry period", () -> {
      DownloadUrlResponse signedUrlResponse = context.getDownloadUrlResponse().body();
      assertNotNull("No download url returned by service.", signedUrlResponse.signedUrl());
      URL url = new URL(signedUrlResponse.signedUrl());
      URLConnection conn = url.openConnection();
      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        assertNotNull("No content present in the file downloaded using download url.",
            readDownloadedFileContent(br));
      } catch (IOException ex) {
        log.info("Exception accessing download url - {}", ex.getMessage());
        fail("Failed to download the file within expiry time");
      }
    });

    And("I should not be able to download the file after expiry period", () -> {
      CommonUtility.customStaticWait_Timeout_Minutes();
      DownloadUrlResponse signedUrlResponse = context.getDownloadUrlResponse().body();
      URL url = new URL(signedUrlResponse.signedUrl());
      URLConnection conn = url.openConnection();
      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        log.info("Value of line read from file - {}", readDownloadedFileContent(br));
        fail("File could be downloaded even after expiry time");
      } catch (IOException ex) {
        log.info("As expected, Exception occured accessing download url post expiry period - {}",
            ex.getMessage());
      }
    });
  }

  private String readDownloadedFileContent(BufferedReader br) throws IOException {
    String inputLine;
    StringBuilder downloadedFile = new StringBuilder();
    while ((inputLine = br.readLine()) != null) {
      downloadedFile.append(inputLine);
    }
    return downloadedFile.toString();
  }

  private void compareFileContent(String outputFilePath, String inputFilePath) throws IOException {
    String outputContent = FileUtils.readFromLocalFilePath(outputFilePath);
    String inputContent = FileUtils.readFromLocalFilePath(inputFilePath);
    assertTrue(outputContent.contentEquals(inputContent));
  }

  private void verifySuccessfulGetSignedURLResponse(String responseCode, String inputFilePath) {
    validateResponseCode(responseCode);
    LocationResponse signedUrlResponse = context.getLocationResponse().body();
    assertNotNull(signedUrlResponse);
    assertNotNull(signedUrlResponse.getLocation().get("SignedURL"));
    assertNotNull(signedUrlResponse.getLocation().get("FileSource"));

    int code;
    try {
      code = FileUtils.uploadFileBySignedUrl(
          signedUrlResponse.getLocation().get("SignedURL"), inputFilePath);
    } catch (IOException e) {
      fail("Fail to call signed URL because of message=" + e.getMessage());
      return;
    }
    assertTrue(code == 200 || code == 201);
  }

  private void validateResponseCode(String responseCode) {
    assertEquals(responseCode, String.valueOf(this.context.getLastStatusCode()));
  }

  private void validateResponseCode(String responseCode, String alternateResponseCode) {
    assertTrue(responseCode.equals(String.valueOf(this.context.getLastStatusCode()))
        || alternateResponseCode.equals(String.valueOf(this.context.getLastStatusCode())));
  }

  private void setNewFileSourceValue(LocationResponse response) {
    log.info("[DEBUG] GET uploadURL body: {}", response);
    this.context.setFileSource(response.getLocation().get("FileSource"));
  }

  private void setUploadSignedUrl(LocationResponse response) {
    this.context.setSignedUrl(response.getLocation().get("SignedURL"));
  }

  private void verifyFailedResponse(String statusCode, String respMsg) {
    validateResponseCode(statusCode);
    ErrorResponseAssertions.assertApiErrorResponse(respMsg, this.context.getClientException());
  }
}
