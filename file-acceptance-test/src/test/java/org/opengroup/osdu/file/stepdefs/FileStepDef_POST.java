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

import java.io.IOException;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.core.test.client.model.file.FileMetadataResponse;
import org.opengroup.osdu.core.test.client.model.storage.StorageRecord;
import org.opengroup.osdu.core.test.client.model.file.FileMetadataRequest;
import org.opengroup.osdu.file.util.ErrorResponseAssertions;
import org.opengroup.osdu.file.util.FileClientExceptionSupport;
import org.opengroup.osdu.file.util.FileUtils;
import com.google.inject.Inject;
import io.cucumber.java8.En;
import org.opengroup.osdu.file.stepdefs.model.FileScope;
import org.opengroup.osdu.file.util.PayloadFormatUtil;

public class FileStepDef_POST implements En {

  @Inject
  private FileScope context;

  public FileStepDef_POST() {

    Given("I hit File service metadata service POST API with {string} and data-partition-id as {string}",
        (String inputPayload, String tenant) -> {
          FileMetadataRequest metadata = readMetadataPayload(inputPayload);
          PayloadFormatUtil.updateFilePath(metadata, this.context.getFileSource());
          HttpResponse<FileMetadataResponse> response = createMetadataRequest(metadata);
          this.context.setMetadataPostResponse(response);
          setId(response);
        });

    Given("I hit File metadata service POST API with {string} and data-partition-id as {string} for validations",
        (String inputPayload, String tenant) -> {
          FileMetadataRequest metadata = readMetadataPayload(inputPayload);
          postMetadataValidationRequest(metadata);
        });

    Then("Service should respond back with {string}", (String reponseStatusCode) -> {
      assertEquals(reponseStatusCode, String.valueOf(this.context.getLastStatusCode()));
      commonAssertion(context.getMetadataPostResponse());
    });

    Then("Service should respond back with error {string} and {string}",
        (String reponseStatusCode, String responseToBeVerified) -> {
          assertEquals(reponseStatusCode, String.valueOf(this.context.getLastStatusCode()));
          ErrorResponseAssertions.assertApiErrorResponse(responseToBeVerified,
              this.context.getClientException());
        });

    Then("I update ancestry value with {string} and data-partition-id as {string}",
        (String inputPayload, String tenant) -> {
          FileMetadataRequest metadata = readMetadataPayload(inputPayload);
          PayloadFormatUtil.removeAncestry(metadata);
          PayloadFormatUtil.updateFilePath(metadata, this.context.getFileSource());
          HttpResponse<FileMetadataResponse> response = createMetadataRequest(metadata);
          this.context.setMetadataPostResponse(response);
          setId(response);
          String id = this.context.getId();
          HttpResponse<StorageRecord> storageResponse = context.getStorageClient().getRecord(id);
          this.context.setStorageResponse(storageResponse);
          setVersion(storageResponse);
          String version = this.context.getVersion();
          String ancestryVal = id + ":" + version;
          PayloadFormatUtil.replaceAncestryWithNewValue(metadata, ancestryVal);
        });
  }

  private FileMetadataRequest readMetadataPayload(String inputPayload) throws IOException {
    String body = FileUtils.readFromLocalFilePath(inputPayload);
    return PayloadFormatUtil.readMetadataPayload(body);
  }

  private HttpResponse<FileMetadataResponse> createMetadataRequest(FileMetadataRequest metadata) {
    return context.getFileClient().createMetadata(PayloadFormatUtil.toRequestJson(metadata));
  }

  private HttpResponse<FileMetadataResponse> postMetadataRequest(FileMetadataRequest metadata) {
    return context.getFileClient().postMetadata(PayloadFormatUtil.toRequestJson(metadata));
  }

  private void postMetadataValidationRequest(FileMetadataRequest metadata) {
    FileClientExceptionSupport.invokeExpectingFailure(
        () -> context.getFileClient().postMetadata(PayloadFormatUtil.toRequestJson(metadata)),
        context);
  }

  private void commonAssertion(HttpResponse<FileMetadataResponse> response) {
    assertNotNull(response.body());
    assertNotNull(response.body().id());
  }

  private void setId(HttpResponse<FileMetadataResponse> response) {
    this.context.setId(response.body().id());
  }

  private void setVersion(HttpResponse<StorageRecord> response) {
    this.context.setVersion(response.body().version());
  }
}
