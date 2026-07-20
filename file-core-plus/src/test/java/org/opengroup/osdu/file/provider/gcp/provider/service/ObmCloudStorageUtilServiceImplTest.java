package org.opengroup.osdu.file.provider.gcp.provider.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.partition.PartitionPropertyResolver;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.obm.core.Driver;
import org.opengroup.osdu.core.obm.core.EnvironmentResolver;
import org.opengroup.osdu.core.obm.core.ObmPathProvider;
import org.opengroup.osdu.core.obm.core.model.ObmBlob;
import org.opengroup.osdu.file.provider.gcp.config.PartitionPropertyNames;
import org.opengroup.osdu.file.provider.gcp.config.PropertiesConfiguration;

@ExtendWith(MockitoExtension.class)
class ObmCloudStorageUtilServiceImplTest {

  private static final String PARTITION_ID = "opendes";
  private static final String TRANSFER_PROTOCOL = "https://seaweedfs.example.com";
  private static final String STAGING_PROPERTY = "stagingLocation";
  private static final String PERSISTENT_PROPERTY = "persistentLocation";
  private static final String STAGING_BUCKET = "custom-staging-bucket";
  private static final String RELATIVE_PATH = "/uuid/fileId";

  @Mock
  private ITenantFactory tenantFactory;
  @Mock
  private Driver obmStorageDriver;
  @Mock
  private DpsHeaders dpsHeaders;
  @Mock
  private EnvironmentResolver environmentResolver;
  @Mock
  private PartitionPropertyResolver partitionPropertyResolver;
  @Mock
  private TenantInfo tenantInfo;

  private ObmCloudStorageUtilServiceImpl storageUtilService;

  @BeforeEach
  void setUp() {
    PropertiesConfiguration properties = new PropertiesConfiguration();
    properties.setStagingArea("staging-area");
    properties.setPersistentArea("persistent-area");

    PartitionPropertyNames partitionPropertyNames = new PartitionPropertyNames();
    partitionPropertyNames.setStagingLocationName(STAGING_PROPERTY);
    partitionPropertyNames.setPersistentLocationName(PERSISTENT_PROPERTY);

    storageUtilService = new ObmCloudStorageUtilServiceImpl(
        tenantFactory,
        new ObmPathProvider(environmentResolver),
        environmentResolver,
        obmStorageDriver,
        dpsHeaders,
        properties,
        partitionPropertyNames,
        partitionPropertyResolver);

    given(environmentResolver.getTransferProtocol(PARTITION_ID)).willReturn(TRANSFER_PROTOCOL);
  }

  @Test
  void getStagingLocationShouldBuildFullUnsignedUrlFromConfiguredBucket() {
    given(partitionPropertyResolver.getOptionalPropertyValue(STAGING_PROPERTY, PARTITION_ID))
        .willReturn(Optional.of(STAGING_BUCKET));

    String location = storageUtilService.getStagingLocation(RELATIVE_PATH, PARTITION_ID);

    assertEquals(TRANSFER_PROTOCOL + "/" + STAGING_BUCKET + RELATIVE_PATH, location);
    verifyNoInteractions(tenantFactory);
  }

  @Test
  void getPersistentLocationShouldBuildFullUnsignedUrlFromTenantFallback() {
    given(partitionPropertyResolver.getOptionalPropertyValue(PERSISTENT_PROPERTY, PARTITION_ID))
        .willReturn(Optional.empty());
    given(tenantFactory.getTenantInfo(PARTITION_ID)).willReturn(tenantInfo);
    given(tenantInfo.getProjectId()).willReturn("refi");
    given(tenantInfo.getName()).willReturn("osdu");

    String location = storageUtilService.getPersistentLocation(RELATIVE_PATH, PARTITION_ID);

    assertEquals(
        TRANSFER_PROTOCOL + "/refi-osdu-persistent-area" + RELATIVE_PATH,
        location);
  }

  @Test
  void getChecksumShouldResolveBucketAndKeyFromFullUnsignedUrlWithLeadingSlash() {
    given(partitionPropertyResolver.getOptionalPropertyValue(STAGING_PROPERTY, PARTITION_ID))
        .willReturn(Optional.of(STAGING_BUCKET));
    given(dpsHeaders.getPartitionId()).willReturn(PARTITION_ID);
    given(obmStorageDriver.getBlob(eq(STAGING_BUCKET), eq("uuid/fileId"), any()))
        .willReturn(new ObmBlob("uuid/fileId", STAGING_BUCKET, null, "abc123", 0L, null, null));

    String location = storageUtilService.getStagingLocation(RELATIVE_PATH, PARTITION_ID);
    String checksum = storageUtilService.getChecksum(location);

    assertEquals("abc123", checksum);
    verify(obmStorageDriver).getBlob(eq(STAGING_BUCKET), eq("uuid/fileId"), any());
  }

  @Test
  void getStagingLocationShouldNotCreateDoubleSlashWhenProtocolAlreadyEndsWithSlash() {
    given(partitionPropertyResolver.getOptionalPropertyValue(STAGING_PROPERTY, PARTITION_ID))
        .willReturn(Optional.of(STAGING_BUCKET));
    given(environmentResolver.getTransferProtocol(PARTITION_ID)).willReturn(TRANSFER_PROTOCOL + "/");

    String location = storageUtilService.getStagingLocation(RELATIVE_PATH, PARTITION_ID);

    assertEquals(TRANSFER_PROTOCOL + "/" + STAGING_BUCKET + RELATIVE_PATH, location);
  }
}
