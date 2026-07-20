/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.file.service;

import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.file.constant.FileMetadataConstant;
import org.opengroup.osdu.file.exception.ApplicationException;
import org.opengroup.osdu.file.exception.KindValidationException;
import org.opengroup.osdu.file.exception.NotFoundException;
import org.opengroup.osdu.file.exception.OsduBadRequestException;
import org.opengroup.osdu.file.mapper.FileMetadataRecordMapper;
import org.opengroup.osdu.file.model.filemetadata.FileMetadata;
import org.opengroup.osdu.file.model.filemetadata.FileMetadataResponse;
import org.opengroup.osdu.file.model.filemetadata.RecordVersion;
import org.opengroup.osdu.file.model.filemetadata.filedetails.FileSourceInfo;
import org.opengroup.osdu.file.model.storage.Record;
import org.opengroup.osdu.file.model.storage.UpsertRecords;
import org.opengroup.osdu.file.provider.interfaces.ICloudStorageOperation;
import org.opengroup.osdu.file.provider.interfaces.IStorageUtilService;
import org.opengroup.osdu.file.service.status.FileDatasetDetailsPublisher;
import org.opengroup.osdu.file.service.status.FileStatusPublisher;
import org.opengroup.osdu.file.service.storage.DataLakeStorageFactory;
import org.opengroup.osdu.file.service.storage.DataLakeStorageService;
import org.opengroup.osdu.file.service.storage.StorageException;
import org.opengroup.osdu.file.util.FileMetadataUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileMetadataService {

    final JaxRsDpsLog log;
    final DataLakeStorageFactory dataLakeStorageFactory;
    final IStorageUtilService storageUtilService;
    final ICloudStorageOperation cloudStorageOperation;
    final DpsHeaders dpsHeaders;
    final FileMetadataUtil fileMetadataUtil;
    final FileMetadataRecordMapper fileMetadataRecordMapper;
    final FileStatusPublisher fileStatusPublisher;
    final FileDatasetDetailsPublisher fileDatasetDetailsPublisher;

    public FileMetadataResponse saveMetadata(FileMetadata fileMetadata)
            throws OsduBadRequestException, StorageException, ApplicationException {

        log.info(FileMetadataConstant.METADATA_SAVE_STARTED);
        fileStatusPublisher.publishInProgressStatus();
        FileMetadataResponse fileMetadataResponse = new FileMetadataResponse();
        String recordId = null;
        String filePath = null;
        String stagingLocation = null;
        String persistentLocation = null;
        String checksumAlgorithm = null;
        try {
            validateKind(fileMetadata.getKind());

            DataLakeStorageService dataLakeStorage = this.dataLakeStorageFactory.create(dpsHeaders);
            filePath = fileMetadata.getData().getDatasetProperties().getFileSourceInfo().getFileSource();
            log.info("Saving file metadata: partition=" + dpsHeaders.getPartitionId()
                    + ", kind=" + fileMetadata.getKind() + ", fileSource=" + filePath);
            fileMetadata.setId(fileMetadataUtil.generateRecordId(dpsHeaders.getPartitionId(),
                    fetchEntityFromKind(fileMetadata.getKind())));
            recordId = fileMetadata.getId();

            stagingLocation = storageUtilService.getStagingLocation(filePath, dpsHeaders.getPartitionId());
            persistentLocation = storageUtilService.getPersistentLocation(filePath, dpsHeaders.getPartitionId());
            log.debug("Prepared metadata save: recordId=" + recordId + ", stagingLocation=" + stagingLocation
                    + ", persistentLocation=" + persistentLocation);

            cloudStorageOperation.copyFile(stagingLocation, persistentLocation);
            String checksum = storageUtilService.getChecksum(persistentLocation);
            if (!StringUtils.isBlank(checksum)) {
              FileSourceInfo fileSourceInfo = fileMetadata.getData().getDatasetProperties().getFileSourceInfo();
              checksumAlgorithm = storageUtilService.getChecksumAlgorithm().toString();
              fileSourceInfo.setChecksum(checksum);
              fileSourceInfo.setChecksumAlgorithm(checksumAlgorithm);
            }
            log.debug("Prepared persistent file for metadata save: recordId=" + recordId
                    + ", checksumPresent=" + StringUtils.isNotBlank(checksum)
                    + ", checksumAlgorithm=" + checksumAlgorithm);
            Record fileMetadataRecord = fileMetadataRecordMapper.fileMetadataToRecord(fileMetadata);

            UpsertRecords upsertRecords = dataLakeStorage.upsertRecord(fileMetadataRecord);
            fileMetadataResponse.setId(upsertRecords.getRecordIds().get(0));
            fileStatusPublisher.publishSuccessStatus(upsertRecords.getRecordIds().get(0),
                    upsertRecords.getRecordIdVersions().get(0));
            fileDatasetDetailsPublisher.publishDatasetDetails(upsertRecords.getRecordIds().get(0),
                    upsertRecords.getRecordIdVersions().get(0));

            /**
             * Issue: https://community.opengroup.org/osdu/platform/system/file/-/issues/76
             * Resolution:
             * 1. Check the staging file exists
            * 2. Catch deletion failures and ignore them as deletion failure should not
            *    invalidate the call to save metadata
            * 3. Delete should be the last step of metadata save process
            * */
            cleanupStagingLocation(stagingLocation, dataLakeStorage, fileMetadataRecord);
            log.info("Saved file metadata: recordId=" + upsertRecords.getRecordIds().get(0)
                    + ", recordVersion=" + upsertRecords.getRecordIdVersions().get(0)
                    + ", checksumPresent=" + StringUtils.isNotBlank(checksum));
        } catch (StorageException e) {
            log.error("Storage failure while saving metadata: recordId=" + recordId + ", fileSource=" + filePath
                    + ", persistentLocation=" + persistentLocation + ", responseCode="
                    + (e.getHttpResponse() != null ? e.getHttpResponse().getResponseCode() : null), e);
            cloudStorageOperation.deleteFile(persistentLocation);
            fileStatusPublisher.publishFailureStatus(e.getHttpResponse());
            throw e;
        } catch (OsduBadRequestException e) {
            log.error("Bad request while saving metadata: recordId=" + recordId + ", kind="
                    + fileMetadata.getKind() + ", fileSource=" + filePath, e);
            fileStatusPublisher.publishFailureStatus(e.getMessage(), HttpStatus.BAD_REQUEST.value());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected failure while saving metadata: recordId=" + recordId + ", fileSource=" + filePath
                    + ", persistentLocation=" + persistentLocation, e);
            cloudStorageOperation.deleteFile(persistentLocation);
            fileStatusPublisher.publishFailureStatus(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
            throw new ApplicationException("Error occurred while creating file metadata", e);
        }
        return fileMetadataResponse;
    }

    private void cleanupStagingLocation(String stagingLocation, DataLakeStorageService dataLakeStorage, Record fileMetadataRecord) {
      try{
        if(dataLakeStorage.getRecord(fileMetadataRecord.getId()) != null) {
          boolean deleted = cloudStorageOperation.deleteFile(stagingLocation);
          log.debug("Staging cleanup completed: recordId=" + fileMetadataRecord.getId()
                  + ", stagingLocation=" + stagingLocation + ", deleted=" + deleted);
        } else {
          log.debug("Skipping staging cleanup because record was not found after upsert: recordId="
                  + fileMetadataRecord.getId());
        }
      }
      catch (Exception e){
        log.warning("Staging cleanup failed: recordId=" + fileMetadataRecord.getId()
                + ", stagingLocation=" + stagingLocation, e);
      }
    }

    public RecordVersion getMetadataById(String id)
            throws OsduBadRequestException, NotFoundException, ApplicationException, StorageException {
        log.debug("Fetching metadata: id=" + id);
        DataLakeStorageService dataLakeStorage = this.dataLakeStorageFactory.create(dpsHeaders);
        Record rec = null;
        log.info("Fetching Record Id");
        try {
            rec = dataLakeStorage.getRecord(id);
            log.debug("Fetched record: id=" + id + ", found=" + (rec != null));

        } catch (StorageException storageExc) {
            log.error("Storage error fetching record: id=" + id + ", responseCode="
                + (storageExc.getHttpResponse() != null ? storageExc.getHttpResponse().getResponseCode() : "null"));

            HttpResponse response = storageExc.getHttpResponse();
            if (FileMetadataConstant.HTTP_CODE_400 == response.getResponseCode()) {
                log.error("Invalid file id", storageExc);
            } else {
                log.error("Failed to find record for the given file id.");
            }
            throw storageExc;
        }

        if (null == rec) {
            log.info("Record not found: id=" + id);
            throw new NotFoundException("Record Not Found");
        }

        RecordVersion result = fileMetadataRecordMapper.recordToRecordVersion(rec);
        FileSourceInfo fileSourceInfo = result.getData() != null
                && result.getData().getDatasetProperties() != null
                ? result.getData().getDatasetProperties().getFileSourceInfo()
                : null;
        String fileSource = fileSourceInfo != null ? fileSourceInfo.getFileSource() : "null";
        log.debug("Fetched metadata: id=" + id + ", fileSource=" + fileSource);
        return result;
    }

    private void validateKind(String kind) {
        String[] kindArr = kind.split(FileMetadataConstant.KIND_SEPRATOR);

        if (kindArr.length != 4) {
            throw new KindValidationException("Invalid kind");
        } else if (!kindArr[1].equalsIgnoreCase(FileMetadataConstant.FILE_KIND_SOURCE)) {
            throw new KindValidationException("Invalid source in kind");
        } else if (!kindArr[2].equalsIgnoreCase(FileMetadataConstant.FILE_KIND_ENTITY)) {
            throw new KindValidationException("Invalid entity in kind");
        }
    }

    private String fetchEntityFromKind(String kind) {
        String[] kindArr = kind.split(FileMetadataConstant.KIND_SEPRATOR);

        return kindArr[2];
    }

    public void deleteMetadataRecord(String recordId)
            throws OsduBadRequestException, StorageException, NotFoundException, ApplicationException {
        log.info(FileMetadataConstant.METADATA_DELETE_STARTED);
        log.info("Deleting file metadata: recordId=" + recordId);
        RecordVersion metaRecord = this.getMetadataById(recordId);
        deleteMetadataRecordFromStorage(recordId);
        deleteFileFromPersistentLocation(metaRecord);
        log.info("Deleted file metadata: recordId=" + recordId);
    }

    private void deleteFileFromPersistentLocation(RecordVersion metaRecord) {
        String filePath = metaRecord.getData().getDatasetProperties().getFileSourceInfo().getFileSource();
        String persistentLocation = storageUtilService.getPersistentLocation(filePath, dpsHeaders.getPartitionId());
        boolean result = cloudStorageOperation.deleteFile(persistentLocation);
        log.debug("Deleted persistent blob: recordId=" + metaRecord.getId()
                + ", persistentLocation=" + persistentLocation + ", deleted=" + result);
    }

    private void deleteMetadataRecordFromStorage(String recordId) throws StorageException {
        DataLakeStorageService dataLakeStorage = this.dataLakeStorageFactory.create(dpsHeaders);
        HttpResponse response = dataLakeStorage.deleteRecord(recordId);
        if (FileMetadataConstant.HTTP_CODE_204 != response.getResponseCode()) {
            log.error("Failed to delete storage record: id=" + recordId + ", responseCode="
                    + response.getResponseCode());
            throw new StorageException(
                    "Unable to delete metadata record from storage. Check the inner HttpResponse for more info.",
                    response);
        }
        log.debug("Deleted storage record: id=" + recordId + ", responseCode=" + response.getResponseCode());
    }

}
