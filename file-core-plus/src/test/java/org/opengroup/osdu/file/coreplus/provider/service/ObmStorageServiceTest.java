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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.PartitionPropertyResolver;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.obm.core.EnvironmentResolver;
import org.opengroup.osdu.core.obm.core.ObmPathProvider;
import org.opengroup.osdu.core.obm.core.model.DirectoryInfo;
import org.opengroup.osdu.file.coreplus.config.PartitionPropertyNames;
import org.opengroup.osdu.file.coreplus.config.PropertiesConfiguration;
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

    assertThrows(
        AppException.class,
        () -> storageService.createSignedUrlFileLocation(
            UNSIGNED_URL, "Bearer token", signedUrlParameters));

    verify(storageRepository).getSignedObjectBasedOnParams(
        eq("bucket"), eq("path"), any(SignedUrlParameters.class));
  }
}
