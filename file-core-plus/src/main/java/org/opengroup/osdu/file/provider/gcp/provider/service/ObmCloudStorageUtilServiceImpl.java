/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
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

package org.opengroup.osdu.file.provider.gcp.provider.service;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.partition.PartitionPropertyResolver;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;

import org.opengroup.osdu.core.obm.core.Driver;
import org.opengroup.osdu.core.obm.core.EnvironmentResolver;
import org.opengroup.osdu.core.obm.core.ObmPathProvider;
import org.opengroup.osdu.core.obm.core.model.ObmBlob;
import org.opengroup.osdu.core.obm.core.persistence.ObmDestination;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.file.api.FileDmsApi;
import org.opengroup.osdu.file.constant.ChecksumAlgorithm;
import org.opengroup.osdu.file.exception.OsduBadRequestException;
import org.opengroup.osdu.file.provider.gcp.config.PartitionPropertyNames;
import org.opengroup.osdu.file.provider.gcp.config.PropertiesConfiguration;
import org.opengroup.osdu.file.provider.gcp.provider.util.ObmStorageUrlBuilder;
import org.opengroup.osdu.file.provider.interfaces.IStorageUtilService;
import org.springframework.stereotype.Component;

/**
 * Get cloud storage staging and persistent location directory path.
 * Used for getRetrievalInstructions and copyDatasetsToPersistentLocation operations {@link FileDmsApi}.
 * Directory paths are generated based on `gcp.storage.stagingArea` and `gcp.storage.persistentArea properties`.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ObmCloudStorageUtilServiceImpl implements IStorageUtilService {

  private final ITenantFactory tenantFactory;
  private final ObmPathProvider pathProvider;
  private final EnvironmentResolver environmentResolver;
  private final Driver obmStorageDriver;
  private final DpsHeaders dpsHeaders;
  private final PropertiesConfiguration properties;
  private final PartitionPropertyNames partitionPropertyNames;
  private final PartitionPropertyResolver partitionPropertyResolver;

  @Override
  public String getPersistentLocation(String relativePath, String partitionId) {
    return resolveLocation(relativePath, partitionId,
        partitionPropertyNames.getPersistentLocationName(), properties.getPersistentArea());
  }

  @Override
  public String getStagingLocation(String relativePath, String partitionId) {
    return resolveLocation(relativePath,
        partitionId,
        partitionPropertyNames.getStagingLocationName(),
        properties.getStagingArea());
  }

  /**
   * Builds a full unsigned URL for a blob location by combining the transfer protocol,
   * bucket name, and relative path.
   *
   * <p>The bucket name is resolved from the partition property (if configured) or computed
   * from tenant info as a fallback.
   *
   * <p>Example output (partition property configured, bucket = "refi-osdu-staging-area"):
   * <pre>
   *   https://seaweedfs.dev1.osdu-cimpl.opengroup.org/refi-osdu-staging-area/uuid/fileId
   * </pre>
   *
   * <p>Example output (fallback, projectId = "refi", name = "osdu", areaSuffix = "staging-area"):
   * <pre>
   *   https://seaweedfs.dev1.osdu-cimpl.opengroup.org/refi-osdu-staging-area/uuid/fileId
   * </pre>
   *
   * <p>The result must be a full unsigned S3 URL because downstream consumers
   * ({@link ObmPathProvider#extractBucketInfoFromUnsignedUrl}) parse the bucket and path from it.
   *
   * @param relativePath          the relative blob path, e.g. "/uuid/fileId"
   * @param partitionId           the data partition identifier
   * @param partitionPropertyName the partition property key for the bucket name
   * @param fallbackAreaSuffix    the area suffix used when no partition property is set
   * @return full unsigned URL suitable for ObmPathProvider parsing
   */
  private String resolveLocation(String relativePath,
                                 String partitionId,
                                 String partitionPropertyName,
                                 String fallbackAreaSuffix) {
    String transferProtocol = environmentResolver.getTransferProtocol(partitionId);
    // Resolve bucket name: partition property takes priority, otherwise derive from tenant info
    String bucket = partitionPropertyResolver
        .getOptionalPropertyValue(partitionPropertyName, partitionId)
        .orElseGet(() -> defaultBucketName(partitionId, fallbackAreaSuffix));
    String result = ObmStorageUrlBuilder.buildUnsignedUrl(transferProtocol, bucket, relativePath);
    log.debug("Resolved storage location: partition={}, partitionProperty={}, bucket={}, relativePath={}",
        partitionId, partitionPropertyName, bucket, relativePath);
    return result;
  }

  // e.g. "refi" + "-" + "osdu" + "-" + "staging-area" → "refi-osdu-staging-area"
  private String defaultBucketName(String partitionId, String areaSuffix) {
    TenantInfo tenantInfo = tenantFactory.getTenantInfo(partitionId);
    return tenantInfo.getProjectId() + "-" + tenantInfo.getName() + "-" + areaSuffix;
  }

  @Override
  public String getChecksum(String filePath) {
    if (Strings.isBlank(filePath)) {
      throw new OsduBadRequestException(String.format("Illegal file path argument - { %s }", filePath));
    }

    String partitionId = dpsHeaders.getPartitionId();
    String fromBucket = pathProvider.extractBucketInfoFromUnsignedUrl(filePath, partitionId).getBucketName();
    String fromPath = pathProvider.getDirectoryPath(filePath, partitionId);
    log.debug("Computing checksum: bucket={}, key={}, partition={}", fromBucket, fromPath, partitionId);
    ObmDestination obmDestination = ObmDestination.builder().partitionId(partitionId).build();
    ObmBlob sourceBlob = obmStorageDriver.getBlob(fromBucket, fromPath, obmDestination);
    String checksum = sourceBlob.getChecksum();
    log.debug("Computed checksum: bucket={}, key={}, checksumPresent={}",
        fromBucket, fromPath, Strings.isNotBlank(checksum));
    return checksum;
  }

  @Override
  public ChecksumAlgorithm getChecksumAlgorithm() {
    return ChecksumAlgorithm.valueOf(environmentResolver.getChecksumAlgorithm());
  }
}
