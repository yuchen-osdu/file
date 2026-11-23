package org.opengroup.osdu.file.coreplus.provider.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.partition.PartitionPropertyResolver;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.obm.core.EnvironmentResolver;
import org.opengroup.osdu.file.coreplus.config.PartitionPropertyNames;
import org.opengroup.osdu.file.coreplus.config.PropertiesConfiguration;

@ExtendWith(MockitoExtension.class)
class ObmCollectionStorageUtilServiceTest {

  @Mock private ITenantFactory tenants;
  @Mock private EnvironmentResolver environment;
  @Mock private PartitionPropertyResolver partitions;
  private ObmCollectionStorageUtilService service;

  @BeforeEach
  void setUp() {
    PropertiesConfiguration properties = new PropertiesConfiguration();
    properties.setStagingArea("staging");
    properties.setPersistentArea("persistent");
    PartitionPropertyNames names = new PartitionPropertyNames();
    names.setStagingLocationName("stagingLocation");
    names.setPersistentLocationName("persistentLocation");
    service = new ObmCollectionStorageUtilService(tenants, environment, properties, names, partitions);
    when(environment.getTransferProtocol("opendes")).thenReturn("s3://");
  }

  @ParameterizedTest
  @CsvSource({"true,true", "true,false", "false,true", "false,false"})
  void resolvesStagingAndPersistentCollectionsWithoutLosingNestedPath(
      boolean staging, boolean configured) {
    String area = staging ? "staging" : "persistent";
    when(partitions.getOptionalPropertyValue(area + "Location", "opendes"))
        .thenReturn(configured ? Optional.of("custom-" + area) : Optional.empty());
    if (!configured) {
      TenantInfo tenant = new TenantInfo();
      tenant.setProjectId("project");
      tenant.setName("tenant");
      when(tenants.getTenantInfo("opendes")).thenReturn(tenant);
    }

    String result = staging ? service.getStagingLocation("/dataset/nested/", "opendes")
        : service.getPersistentLocation("/dataset/nested/", "opendes");

    assertEquals("s3://" + (configured ? "custom-" : "project-tenant-") + area + "/dataset/nested/", result);
    if (configured) {
      verifyNoInteractions(tenants);
    }
  }
}
