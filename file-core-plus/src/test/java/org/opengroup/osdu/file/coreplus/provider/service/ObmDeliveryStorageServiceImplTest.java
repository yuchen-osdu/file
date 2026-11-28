package org.opengroup.osdu.file.coreplus.provider.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.obm.core.Driver;
import org.opengroup.osdu.core.obm.core.ObmPathProvider;
import org.opengroup.osdu.core.obm.core.model.DirectoryInfo;
import org.opengroup.osdu.core.obm.core.persistence.ObmDestination;
import org.opengroup.osdu.file.coreplus.config.CorePlusConfigurationProperties;
import org.opengroup.osdu.file.model.delivery.SignedUrl;

@ExtendWith(MockitoExtension.class)
class ObmDeliveryStorageServiceImplTest {

  @Mock private DpsHeaders headers;
  @Mock private Driver driver;
  @Mock private ObmPathProvider paths;

  @Test
  void signsResolvedObjectWithConfiguredDaysAndRequestPartition() throws Exception {
    CorePlusConfigurationProperties properties = new CorePlusConfigurationProperties();
    properties.getSignedUrl().setExpirationDays(3);
    ObmDeliveryStorageServiceImpl service =
        new ObmDeliveryStorageServiceImpl(headers, driver, properties, paths);
    when(headers.getPartitionId()).thenReturn("opendes");
    when(paths.extractBucketInfoFromUnsignedUrl("s3://bucket/folder/file", "opendes"))
        .thenReturn(DirectoryInfo.builder().bucketName("bucket").directoryId("folder/file").build());
    ObmDestination destination = ObmDestination.builder().partitionId("opendes").build();
    URL url = URI.create("https://storage.example/bucket/folder/file?signature=value").toURL();
    when(driver.getSignedUrlForDownload("bucket", destination, "folder/file", 3, TimeUnit.DAYS))
        .thenReturn(url);
    Instant before = Instant.now();

    SignedUrl result = service.createSignedUrl("s3://bucket/folder/file", "Bearer token");

    assertEquals(url, result.getUrl());
    assertFalse(result.getCreatedAt().isBefore(before));
    assertFalse(result.getCreatedAt().isAfter(Instant.now()));
    verify(driver).getSignedUrlForDownload("bucket", destination, "folder/file", 3, TimeUnit.DAYS);
  }
}
