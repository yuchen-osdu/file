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
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;
import org.apache.hc.core5.http.HttpStatus;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.net.URL;
import org.opengroup.osdu.core.common.dms.model.RetrievalInstructionsRequest;
import org.opengroup.osdu.core.common.dms.model.RetrievalInstructionsResponse;
import org.opengroup.osdu.core.common.dms.model.StorageInstructionsResponse;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.core.test.client.model.file.FileMetadataResponse;
import org.opengroup.osdu.core.test.client.model.file.FileMetadataRequest;
import org.opengroup.osdu.file.stepdefs.model.FileScope;
import org.opengroup.osdu.file.util.FileUtils;
import org.opengroup.osdu.file.util.PayloadFormatUtil;

@SuppressWarnings("unused")
public class FileDMSStepDefs {

  @Inject
  private FileScope context;

  @When("I send request for storage instructions i should receive valid response")
  public void iSendRequestForStorageInstructionsIShouldReceiveValidResponse() {
    HttpResponse<StorageInstructionsResponse> coreResp =
        context.getFileClient().getStorageInstructions();
    assertEquals(HttpStatus.SC_OK, coreResp.statusCode());
    this.context.setStorageInstructionsResponse(coreResp);
    StorageInstructionsResponse storageInstructions = coreResp.body();
    this.context.setFileSource(
        storageInstructions.getStorageLocation().get("fileSource").toString());
    this.context.setSignedUrl(storageInstructions.getStorageLocation().get("signedUrl").toString());
  }

  @Then("I should be able to upload file from {string} with provided instruction")
  public void iShouldBeAbleToUploadFileWithProvidedInstruction(String inputFilePath) throws IOException {
    int responseCode = FileUtils.uploadFileBySignedUrl(this.context.getSignedUrl(),
        inputFilePath);
    assertTrue(
        responseCode == HttpStatus.SC_CREATED || responseCode == HttpStatus.SC_OK);
  }

  @Then("I should be able to register metadata with {string} and uploaded file")
  public void iShouldBeAbleToRegisterMetadataWithUploadedFile(String metadataInputPayload)
      throws IOException {
    String body = FileUtils.readFromLocalFilePath(metadataInputPayload);
    FileMetadataRequest metadata = PayloadFormatUtil.readMetadataPayload(body);
    PayloadFormatUtil.updateFilePath(metadata, this.context.getFileSource());

    HttpResponse<FileMetadataResponse> coreResp =
        context.getFileClient().createMetadata(PayloadFormatUtil.toRequestJson(metadata));
    assertEquals(HttpStatus.SC_CREATED, coreResp.statusCode());
    this.context.setMetadataPostResponse(coreResp);
    FileMetadataResponse metadataResponse = coreResp.body();
    this.context.setId(metadataResponse.id());
  }

  @Then("I should be able request for retrieval instructions for uploaded file by {string}")
  public void iShouldBeAbleRequestForRetrievalInstructionsForUploadedFileByMetadataId(
      String datasetRegistryInputPayload)
      throws IOException {
    String recordId = this.context.getId();
    String retrievalInstructionsJson =
        FileUtils.readFromLocalFilePath(datasetRegistryInputPayload);
    RetrievalInstructionsRequest request =
        PayloadFormatUtil.readRetrievalInstructionsRequest(retrievalInstructionsJson, recordId);

    HttpResponse<RetrievalInstructionsResponse> coreResp =
        context.getFileClient().getRetrievalInstructions(request);
    assertEquals(HttpStatus.SC_OK, coreResp.statusCode());
    this.context.setRetrievalInstructionsResponse(coreResp);

    var retrievalProperties = coreResp.body().getDatasets().stream()
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Get retrieval instructions empty datasets"));
    this.context.setSignedUrl(
        retrievalProperties.getRetrievalProperties().get("signedUrl").toString());
  }

  @Then("I should be able retrieve file by provided instructions, and downloaded files is same as {string}")
  public void i_should_be_able_retrieve_file_by_provided_instructions_and_downloaded_files_is_same_as(
      String inputFilePath)
      throws IOException {
    String expectedFileContent = FileUtils.readFromLocalFilePath(inputFilePath);
    URL downloadUrl = new URL(this.context.getSignedUrl());
    String actualFileContent = FileUtils.readFileBySignedUrl(downloadUrl);
    assertEquals(expectedFileContent, actualFileContent);
  }
}
