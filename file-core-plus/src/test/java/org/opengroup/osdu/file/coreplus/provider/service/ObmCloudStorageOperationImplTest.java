package org.opengroup.osdu.file.coreplus.provider.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.obm.core.Driver;
import org.opengroup.osdu.core.obm.core.EnvironmentResolver;
import org.opengroup.osdu.core.obm.core.ObmDriverRuntimeException;
import org.opengroup.osdu.core.obm.core.ObmPathProvider;
import org.opengroup.osdu.core.obm.core.model.DirectoryInfo;
import org.opengroup.osdu.core.obm.core.model.ObmBlob;
import org.opengroup.osdu.core.obm.core.persistence.ObmDestination;
import org.opengroup.osdu.file.exception.OsduBadRequestException;
import org.opengroup.osdu.file.model.file.FileCopyOperation;
import org.opengroup.osdu.file.model.file.FileCopyOperationResponse;
import org.opengroup.osdu.file.model.filecollection.DatasetCopyOperation;

@ExtendWith(MockitoExtension.class)
class ObmCloudStorageOperationImplTest {

  private static final String PARTITION = "opendes";
  private static final ObmDestination DESTINATION =
      ObmDestination.builder().partitionId(PARTITION).build();

  @Mock private ObmPathProvider paths;
  @Mock private Driver driver;
  @Mock private DpsHeaders headers;
  @Mock private EnvironmentResolver environment;
  private ObmCloudStorageOperationImpl operations;

  @BeforeEach
  void setUp() {
    operations = new ObmCloudStorageOperationImpl(paths, driver, headers, environment);
    when(headers.getPartitionId()).thenReturn(PARTITION);
  }

  @Test
  void copiesExistingFileWithinTheRequestPartition() {
    resolve("source", "staging", "folder/file");
    resolve("target", "persistent", "dataset/file");
    when(driver.getBlob("staging", "folder/file", DESTINATION)).thenReturn(blob());
    when(driver.copyBlob(DESTINATION, "staging", "folder/file", "persistent", "dataset/file"))
        .thenReturn("persistent/dataset/file");
    when(environment.getTransferProtocol(PARTITION)).thenReturn("s3://");

    assertEquals("s3://persistent/dataset/file", operations.copyFile("source", "target"));
    verify(driver).copyBlob(DESTINATION, "staging", "folder/file", "persistent", "dataset/file");
  }

  @ParameterizedTest
  @CsvSource(value = {
      "'',file,persistent,file",
      "staging,'',persistent,file",
      "staging,file,'',file",
      "staging,file,persistent,''"
  })
  void rejectsIncompletePathsBeforeCopyingFilesOrDirectories(
      String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
    resolve("source", sourceBucket, sourceKey);
    resolve("target", targetBucket, targetKey);

    OsduBadRequestException error =
        assertThrows(OsduBadRequestException.class, () -> operations.copyFile("source", "target"));
    assertEquals("Storage record does not have a valid file url", error.getMessage());
    assertEquals(List.of(false), operations.copyDirectories(List.of(
        new FileCopyOperation("source", "target"))).stream().map(DatasetCopyOperation::isSuccess).toList());
    verifyNoInteractions(driver);
  }

  @Test
  void reportsMissingFileWithoutTryingToCopy() {
    resolve("source", "staging", "missing");
    resolve("target", "persistent", "file");

    assertThrows(OsduBadRequestException.class, () -> operations.copyFile("source", "target"));
    verify(driver, never()).copyBlob(any(), anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void fileBatchKeepsInputOrderAndContinuesAfterMissingSource() {
    resolve("missing", "staging", "missing");
    resolve("source", "staging", "file");
    resolve("target", "persistent", "file");
    when(driver.getBlob("staging", "missing", DESTINATION)).thenReturn(null);
    when(driver.getBlob("staging", "file", DESTINATION)).thenReturn(blob());
    when(driver.copyBlob(DESTINATION, "staging", "file", "persistent", "file"))
        .thenReturn("persistent/file");
    when(environment.getTransferProtocol(PARTITION)).thenReturn("s3://");
    List<FileCopyOperation> batch = List.of(
        new FileCopyOperation("missing", "target"), new FileCopyOperation("source", "target"));

    List<FileCopyOperationResponse> result = operations.copyFiles(batch);

    assertEquals(List.of(false, true), result.stream().map(FileCopyOperationResponse::isSuccess).toList());
    assertEquals(batch, result.stream().map(FileCopyOperationResponse::getCopyOperation).toList());
  }

  @Test
  void directoryBatchContinuesAfterMissingSourceAndDriverFailure() {
    resolve("missing", "staging", "missing/");
    resolve("broken", "staging", "broken/");
    resolve("source", "staging", "folder/");
    resolve("target", "persistent", "dataset/");
    when(driver.listBlobsByPrefix("staging", DESTINATION, "missing/")).thenReturn(List.of());
    when(driver.listBlobsByPrefix("staging", DESTINATION, "broken/"))
        .thenThrow(new ObmDriverRuntimeException(new IllegalStateException("storage unavailable")));
    when(driver.listBlobsByPrefix("staging", DESTINATION, "folder/")).thenReturn(List.of(blob()));
    when(driver.copyBlobs(DESTINATION, "staging", "folder/", "persistent", "dataset/"))
        .thenReturn(List.of("persistent/dataset/a", "persistent/dataset/b"));
    when(environment.getTransferProtocol(PARTITION)).thenReturn("s3://");
    List<FileCopyOperation> batch = List.of(
        new FileCopyOperation("missing", "target"),
        new FileCopyOperation("broken", "target"),
        new FileCopyOperation("source", "target"));

    List<DatasetCopyOperation> result = operations.copyDirectories(batch);

    assertEquals(List.of(false, false, true), result.stream().map(DatasetCopyOperation::isSuccess).toList());
    assertEquals(batch, result.stream().map(DatasetCopyOperation::getFileCopyOperation).toList());
    verify(driver).copyBlobs(DESTINATION, "staging", "folder/", "persistent", "dataset/");
    verify(driver, never()).copyBlobs(any(), anyString(), eq("missing/"), anyString(), anyString());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void returnsDeletionOutcomeForResolvedObject(boolean deleted) {
    resolve("source", "staging", "folder/file");
    when(driver.deleteBlob("staging", "folder/file", DESTINATION)).thenReturn(deleted);

    assertEquals(deleted, operations.deleteFile("source"));
    verify(driver).deleteBlob("staging", "folder/file", DESTINATION);
  }

  private void resolve(String url, String bucket, String key) {
    when(paths.extractBucketInfoFromUnsignedUrl(url, PARTITION))
        .thenReturn(DirectoryInfo.builder().bucketName(bucket).directoryId(key).build());
    when(paths.getDirectoryPath(url, PARTITION)).thenReturn(key);
  }

  private ObmBlob blob() {
    return new ObmBlob("file", "staging", null, "checksum", 1L, null, null);
  }
}
