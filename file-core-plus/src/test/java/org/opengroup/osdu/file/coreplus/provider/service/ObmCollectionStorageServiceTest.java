package org.opengroup.osdu.file.coreplus.provider.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.dms.model.DatasetRetrievalProperties;
import org.opengroup.osdu.core.common.dms.model.StorageInstructionsResponse;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.partition.PartitionPropertyResolver;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.obm.core.Driver;
import org.opengroup.osdu.core.obm.core.EnvironmentResolver;
import org.opengroup.osdu.core.obm.core.ObmDriverRuntimeException;
import org.opengroup.osdu.core.obm.core.SignedDirectoryPropertiesResolver;
import org.opengroup.osdu.core.obm.core.persistence.ObmDestination;
import org.opengroup.osdu.file.coreplus.config.PartitionPropertyNames;
import org.opengroup.osdu.file.coreplus.config.PropertiesConfiguration;
import org.opengroup.osdu.file.model.FileRetrievalData;
import org.opengroup.osdu.file.model.SignedObject;
import org.opengroup.osdu.file.provider.interfaces.IStorageRepository;
import org.opengroup.osdu.file.util.ExpiryTimeUtil;

@ExtendWith(MockitoExtension.class)
class ObmCollectionStorageServiceTest {

  @Mock private ITenantFactory tenants;
  @Mock private DpsHeaders headers;
  @Mock private EnvironmentResolver environment;
  @Mock private IStorageRepository repository;
  @Mock private Driver driver;
  @Mock private SignedDirectoryPropertiesResolver directories;
  @Mock private PartitionPropertyResolver partitions;
  private ObmCollectionStorageService service;

  @BeforeEach
  void setUp() {
    PropertiesConfiguration properties = new PropertiesConfiguration();
    properties.setStagingArea("staging");
    PartitionPropertyNames names = new PartitionPropertyNames();
    names.setStagingLocationName("stagingLocation");
    service = new ObmCollectionStorageService(tenants, headers, environment, repository,
        properties, driver, new ExpiryTimeUtil(), directories, names, partitions);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void uploadInstructionsSignOnlyTheRequestedDirectoryInResolvedBucket(boolean configured)
      throws Exception {
    String bucket = configured ? "custom-staging" : "project-tenant-staging";
    when(partitions.getOptionalPropertyValue("stagingLocation", "opendes"))
        .thenReturn(configured ? Optional.of(bucket) : Optional.empty());
    if (!configured) {
      TenantInfo tenant = new TenantInfo();
      tenant.setProjectId("project");
      tenant.setName("tenant");
      when(tenants.getTenantInfo("opendes")).thenReturn(tenant);
    }
    when(headers.getPartitionId()).thenReturn("opendes");
    when(headers.getUserEmail()).thenReturn("user@example.com");
    when(environment.getProviderKey()).thenReturn("s3");
    SignedObject object = SignedObject.builder().uri(URI.create("s3://" + bucket))
        .url(URI.create("https://storage.example/" + bucket).toURL()).build();
    when(repository.createSignedObject(bucket, "dataset")).thenReturn(object);
    Map<String, String> options = Map.of("policy", "directory-scoped-policy");
    ObmDestination destination = ObmDestination.builder().partitionId("opendes").build();
    when(driver.getSigningOptions(bucket, "dataset/", destination, 1L, TimeUnit.HOURS))
        .thenReturn(options);

    StorageInstructionsResponse result = service.createStorageInstructions("dataset", "opendes");

    assertEquals("s3", result.getProviderKey());
    assertEquals(Map.of("url", object.getUrl().toString(), "createdBy", "user@example.com",
        "fileCollectionSource", "dataset", "signingOptions", options), result.getStorageLocation());
    verify(driver).getSigningOptions(bucket, "dataset/", destination, 1L, TimeUnit.HOURS);
    if (configured) {
      verifyNoInteractions(tenants);
    }
  }

  @Test
  void retrievalPreservesEachDatasetAndItsOwnDirectoryCredentials() {
    when(headers.getPartitionId()).thenReturn("opendes");
    when(environment.getProviderKey()).thenReturn("s3");
    Map<String, Object> first = Map.of("url", "https://storage.example/first", "token", "first-token");
    Map<String, Object> second = Map.of("url", "https://storage.example/second", "token", "second-token");
    when(directories.getSignedDirectoryProperties("s3://bucket/first", "opendes")).thenReturn(first);
    when(directories.getSignedDirectoryProperties("s3://bucket/second", "opendes")).thenReturn(second);

    List<DatasetRetrievalProperties> result = service.createRetrievalInstructions(List.of(
        retrieval("first"), retrieval("second"))).getDatasets();

    assertEquals(List.of("first", "second"),
        result.stream().map(DatasetRetrievalProperties::getDatasetRegistryId).toList());
    assertEquals(List.of(first, second),
        result.stream().map(DatasetRetrievalProperties::getRetrievalProperties).toList());
    assertTrue(result.stream().allMatch(item -> "s3".equals(item.getProviderKey())));
    verifyNoInteractions(repository, driver);
  }

  @Test
  void failedCredentialResolutionDoesNotProduceSuccessfulInstructions() {
    when(headers.getPartitionId()).thenReturn("opendes");
    when(directories.getSignedDirectoryProperties("s3://bucket/first", "opendes"))
        .thenThrow(new ObmDriverRuntimeException(new IllegalStateException("unavailable")));

    assertThrows(ObmDriverRuntimeException.class,
        () -> service.createRetrievalInstructions(List.of(retrieval("first"))));
  }

  private FileRetrievalData retrieval(String id) {
    return FileRetrievalData.builder().recordId(id).unsignedUrl("s3://bucket/" + id).build();
  }
}
