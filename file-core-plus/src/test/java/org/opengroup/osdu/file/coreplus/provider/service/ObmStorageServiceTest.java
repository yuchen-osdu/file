/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
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

package org.opengroup.osdu.file.coreplus.provider.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.dms.model.DatasetRetrievalProperties;
import org.opengroup.osdu.core.common.dms.model.StorageInstructionsResponse;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.partition.PartitionPropertyResolver;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.obm.core.EnvironmentResolver;
import org.opengroup.osdu.core.obm.core.ObmPathProvider;
import org.opengroup.osdu.core.obm.core.model.DirectoryInfo;
import org.opengroup.osdu.file.coreplus.config.PartitionPropertyNames;
import org.opengroup.osdu.file.coreplus.config.PropertiesConfiguration;
import org.opengroup.osdu.file.model.FileRetrievalData;
import org.opengroup.osdu.file.model.SignedObject;
import org.opengroup.osdu.file.model.SignedUrl;
import org.opengroup.osdu.file.model.SignedUrlParameters;
import org.opengroup.osdu.file.provider.interfaces.IStorageRepository;

@ExtendWith(MockitoExtension.class)
class ObmStorageServiceTest {

  private static final String PARTITION_ID = "opendes";
  private static final String UNSIGNED_URL = "https://storage.example.com/bucket/path";

  @Mock
  private IStorageRepository storageRepository;
  @Mock
  private ObmPathProvider pathProvider;
  @Mock
  private ITenantFactory tenantFactory;
  @Mock
  private DpsHeaders dpsHeaders;
  @Mock
  private EnvironmentResolver environmentResolver;
  @Mock
  private PropertiesConfiguration properties;
  @Mock
  private PartitionPropertyNames partitionPropertyNames;
  @Mock
  private PartitionPropertyResolver partitionPropertyResolver;
  @Mock
  private DirectoryInfo directoryInfo;

  private ObmStorageService storageService;

  @BeforeEach
  void setUp() {
    storageService = new ObmStorageService(
        storageRepository,
        pathProvider,
        tenantFactory,
        dpsHeaders,
        environmentResolver,
        properties,
        partitionPropertyNames,
        partitionPropertyResolver);
  }

  @Test
  void createSignedUrlFileLocation_throwsAppException_whenRepositoryReturnsNull() {
    given(dpsHeaders.getPartitionId()).willReturn(PARTITION_ID);
    given(pathProvider.extractBucketInfoFromUnsignedUrl(UNSIGNED_URL, PARTITION_ID))
        .willReturn(directoryInfo);
    given(directoryInfo.getBucketName()).willReturn("bucket");
    given(directoryInfo.getDirectoryId()).willReturn("path");
    SignedUrlParameters signedUrlParameters = new SignedUrlParameters();

    AppException error = assertThrows(
        AppException.class,
        () -> storageService.createSignedUrlFileLocation(
            UNSIGNED_URL, "Bearer token", signedUrlParameters));

    assertEquals(500, error.getError().getCode());
    assertEquals("Storage repository returned no signed object", error.getError().getMessage());
    verify(storageRepository).getSignedObjectBasedOnParams(
        eq("bucket"), eq("path"), any(SignedUrlParameters.class));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void createsUploadInConfiguredOrTenantFallbackBucket(boolean configured) throws Exception {
    String bucket = configureStaging(configured);
    SignedObject object = signedObject();
    given(storageRepository.createSignedObject(eq(bucket), any())).willReturn(object);
    given(dpsHeaders.getUserEmail()).willReturn("user@example.com");
    Instant before = Instant.now();

    SignedUrl result = storageService.createSignedUrl("well.csv", "Bearer token", PARTITION_ID);

    ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
    verify(storageRepository).createSignedObject(eq(bucket), path.capture());
    String[] segments = path.getValue().split("/");
    assertEquals(2, segments.length);
    assertEquals(segments[0], UUID.fromString(segments[0]).toString());
    assertEquals("well.csv", segments[1]);
    assertEquals("/" + path.getValue(), result.getFileSource());
    assertEquals(object.getUrl(), result.getUrl());
    assertEquals(object.getUri(), result.getUri());
    assertEquals("user@example.com", result.getCreatedBy());
    assertFalse(result.getCreatedAt().isBefore(before));
    assertFalse(result.getCreatedAt().isAfter(Instant.now()));
    if (configured) {
      verifyNoInteractions(tenantFactory);
    }
  }

  @Test
  void rejectsUploadWhenGeneratedPathExceedsLimitBeforeUsingStorage() {
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> storageService.createSignedUrl("a".repeat(988), "Bearer token", PARTITION_ID));

    assertTrue(error.getMessage().contains("1025"));
    verifyNoInteractions(storageRepository, partitionPropertyResolver, tenantFactory);
  }

  @Test
  void acceptsUploadAtMaximumGeneratedPathLength() throws Exception {
    String bucket = configureStaging(true);
    given(storageRepository.createSignedObject(eq(bucket), any())).willReturn(signedObject());

    SignedUrl result = storageService.createSignedUrl("a".repeat(987), "Bearer token", PARTITION_ID);

    assertEquals(1024, result.getFileSource().substring(1).length());
  }

  @Test
  void storageInstructionsExposeUploadUrlAndProviderWithoutInternalUri() throws Exception {
    String bucket = configureStaging(true);
    SignedObject object = signedObject();
    given(storageRepository.createSignedObject(eq(bucket), any())).willReturn(object);
    given(dpsHeaders.getUserEmail()).willReturn("user@example.com");
    given(environmentResolver.getProviderKey()).willReturn("s3");

    StorageInstructionsResponse result = storageService.createStorageInstructions("dataset", PARTITION_ID);

    assertEquals("s3", result.getProviderKey());
    assertEquals(3, result.getStorageLocation().size());
    assertEquals(object.getUrl().toString(), result.getStorageLocation().get("signedUrl"));
    assertEquals("user@example.com", result.getStorageLocation().get("createdBy"));
    assertTrue(result.getStorageLocation().get("fileSource").toString().endsWith("/dataset"));
  }

  @Test
  void retrievalInstructionsPreserveDatasetOrderAndResolveEachObject() throws Exception {
    given(dpsHeaders.getPartitionId()).willReturn(PARTITION_ID);
    given(dpsHeaders.getUserEmail()).willReturn("user@example.com");
    given(environmentResolver.getProviderKey()).willReturn("s3");
    given(pathProvider.extractBucketInfoFromUnsignedUrl(UNSIGNED_URL, PARTITION_ID))
        .willReturn(DirectoryInfo.builder().bucketName("bucket").directoryId("path").build());
    given(pathProvider.extractBucketInfoFromUnsignedUrl("s3://other/file", PARTITION_ID))
        .willReturn(DirectoryInfo.builder().bucketName("other").directoryId("file").build());
    SignedObject first = signedObject();
    SignedObject second = SignedObject.builder().uri(URI.create("s3://other/file"))
        .url(URI.create("https://storage.example/other/file?signature=second").toURL()).build();
    given(storageRepository.getSignedObjectBasedOnParams(eq("bucket"), eq("path"), any()))
        .willReturn(first);
    given(storageRepository.getSignedObjectBasedOnParams(eq("other"), eq("file"), any()))
        .willReturn(second);

    List<DatasetRetrievalProperties> result = storageService.createRetrievalInstructions(List.of(
        FileRetrievalData.builder().recordId("dataset-1").unsignedUrl(UNSIGNED_URL).build(),
        FileRetrievalData.builder().recordId("dataset-2").unsignedUrl("s3://other/file").build()))
        .getDatasets();

    assertEquals(List.of("dataset-1", "dataset-2"),
        result.stream().map(DatasetRetrievalProperties::getDatasetRegistryId).toList());
    assertEquals(List.of(first.getUrl().toString(), second.getUrl().toString()),
        result.stream().map(item -> item.getRetrievalProperties().get("signedUrl")).toList());
    assertTrue(result.stream().allMatch(item -> "s3".equals(item.getProviderKey())));
    assertTrue(result.stream().allMatch(item ->
        "user@example.com".equals(item.getRetrievalProperties().get("createdBy"))));
  }

  private String configureStaging(boolean configured) {
    given(partitionPropertyNames.getStagingLocationName()).willReturn("stagingLocation");
    given(partitionPropertyResolver.getOptionalPropertyValue("stagingLocation", PARTITION_ID))
        .willReturn(configured ? Optional.of("custom-staging") : Optional.empty());
    if (!configured) {
      TenantInfo tenant = new TenantInfo();
      tenant.setProjectId("project");
      tenant.setName("tenant");
      given(tenantFactory.getTenantInfo(PARTITION_ID)).willReturn(tenant);
      given(properties.getStagingArea()).willReturn("staging");
    }
    return configured ? "custom-staging" : "project-tenant-staging";
  }

  private SignedObject signedObject() throws Exception {
    return SignedObject.builder().uri(URI.create("s3://bucket/path"))
        .url(URI.create("https://storage.example/bucket/path?signature=value").toURL()).build();
  }
}
