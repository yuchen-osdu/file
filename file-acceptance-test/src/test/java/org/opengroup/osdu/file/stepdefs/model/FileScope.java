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

package org.opengroup.osdu.file.stepdefs.model;

import io.cucumber.guice.ScenarioScoped;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.opengroup.osdu.core.common.dms.model.RetrievalInstructionsResponse;
import org.opengroup.osdu.core.test.client.ClientException;
import org.opengroup.osdu.core.common.dms.model.StorageInstructionsResponse;
import org.opengroup.osdu.core.common.model.file.FileListResponse;
import org.opengroup.osdu.core.common.model.file.FileLocationResponse;
import org.opengroup.osdu.core.common.model.file.LocationResponse;
import org.opengroup.osdu.core.test.client.FileClient;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.core.test.client.StorageClient;
import org.opengroup.osdu.core.test.client.model.file.DownloadUrlResponse;
import org.opengroup.osdu.core.test.client.model.file.FileMetadataRecord;
import org.opengroup.osdu.core.test.client.model.file.FileMetadataResponse;
import org.opengroup.osdu.core.test.client.model.storage.StorageRecord;
@ScenarioScoped
@Getter
@Setter
public class FileScope {

  private Map<String, String> authHeaders;
  private Map<String, String> queryParams;
  private String id;
  private String fileSource;
  private String signedUrl;
  private String version;
  private String jobId;
  private String status;
  private String responseCode;

  private FileClient fileClient;
  private StorageClient storageClient;

  private HttpResponse<LocationResponse> locationResponse;
  private HttpResponse<DownloadUrlResponse> downloadUrlResponse;
  private HttpResponse<FileMetadataRecord> metadataRecordResponse;
  private HttpResponse<FileMetadataResponse> metadataPostResponse;
  private HttpResponse<FileLocationResponse> fileLocationResponse;
  private HttpResponse<FileListResponse> fileListResponse;
  private HttpResponse<Void> deleteResponse;
  private HttpResponse<StorageRecord> storageResponse;
  private HttpResponse<StorageInstructionsResponse> storageInstructionsResponse;
  private HttpResponse<RetrievalInstructionsResponse> retrievalInstructionsResponse;

  private ClientException clientException;
  private HttpResponse<?> lastResponse;

  public int getLastStatusCode() {
    if (lastResponse != null) {
      return lastResponse.statusCode();
    }
    return clientException == null ? 0 : clientException.getStatusCode();
  }

  public void recordClientException(ClientException exception) {
    this.clientException = exception;
    this.lastResponse = null;
  }

  public void setLocationResponse(HttpResponse<LocationResponse> response) {
    this.locationResponse = response;
    this.lastResponse = response;
  }

  public void setDownloadUrlResponse(HttpResponse<DownloadUrlResponse> response) {
    this.downloadUrlResponse = response;
    this.lastResponse = response;
  }

  public void setMetadataRecordResponse(HttpResponse<FileMetadataRecord> response) {
    this.metadataRecordResponse = response;
    this.lastResponse = response;
  }

  public void setMetadataPostResponse(HttpResponse<FileMetadataResponse> response) {
    this.metadataPostResponse = response;
    this.lastResponse = response;
  }

  public void setFileLocationResponse(HttpResponse<FileLocationResponse> response) {
    this.fileLocationResponse = response;
    this.lastResponse = response;
  }

  public void setFileListResponse(HttpResponse<FileListResponse> response) {
    this.fileListResponse = response;
    this.lastResponse = response;
  }

  public void setDeleteResponse(HttpResponse<Void> response) {
    this.deleteResponse = response;
    this.lastResponse = response;
  }

  public void setStorageResponse(HttpResponse<StorageRecord> response) {
    this.storageResponse = response;
    this.lastResponse = response;
  }

  public void setStorageInstructionsResponse(HttpResponse<StorageInstructionsResponse> response) {
    this.storageInstructionsResponse = response;
    this.lastResponse = response;
  }

  public void setRetrievalInstructionsResponse(HttpResponse<RetrievalInstructionsResponse> response) {
    this.retrievalInstructionsResponse = response;
    this.lastResponse = response;
  }

  public void setLastResponse(HttpResponse<?> response) {
    this.lastResponse = response;
  }

  public void setupClients(FileClient fileClient, StorageClient storageClient) {
    this.fileClient = fileClient;
    this.storageClient = storageClient;
  }

  public void teardownClients() {
    if (fileClient != null) {
      fileClient.teardown();
      fileClient = null;
    }
    if (storageClient != null) {
      storageClient.teardown();
      storageClient = null;
    }
  }
}
