/**
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

package org.opengroup.osdu.file.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.aws.v2.dynamodb.interfaces.IDynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.v2.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.aws.v2.dynamodb.model.QueryPageResult;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import org.opengroup.osdu.core.common.model.file.DriverType;
import org.opengroup.osdu.core.common.model.file.FileListRequest;
import org.opengroup.osdu.core.common.model.file.FileLocation;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.file.exception.FileLocationNotFoundException;
import org.opengroup.osdu.file.exception.OsduException;
import org.opengroup.osdu.file.provider.aws.config.ProviderConfigurationBag;
import org.opengroup.osdu.file.provider.aws.datamodel.entity.FileLocationDoc;
import org.opengroup.osdu.file.provider.aws.impl.FileLocationRepositoryImpl;

import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;


@ExtendWith(MockitoExtension.class)
class FileLocationRepositoryImplTest {

    private final String fileID = "fileID";
    private final String partitionID = "partitionID";
    private final DriverType driver = DriverType.GCS;
    private final String location = "location";
    private final Date createdAt = Date.from(Instant.now());
    private final String createdBy = "createdBy";
    private final String cursor = "cursor";
    private final String TABLE_PARAMETER_PATH="whatevertablepath";

    @Mock
    DpsHeaders headers;

    @Mock
    IDynamoDBQueryHelperFactory queryHelperFactory;

    @Mock
    ProviderConfigurationBag providerConfigurationBag;

    @Mock
    DynamoDBQueryHelper<FileLocationDoc> queryHelper;

    private FileLocationRepositoryImpl repository;

    @BeforeEach
    void setup() {
        lenient().when(queryHelperFactory.createQueryHelper(headers, providerConfigurationBag.fileLocationTableParameterRelativePath, FileLocationDoc.class)).thenReturn(queryHelper);
        repository = new FileLocationRepositoryImpl(headers, queryHelperFactory, providerConfigurationBag);
    }

    @Test
    void testFindByFileID_null() {
        assertNull(repository.findByFileID(null));
    }

    @Test
    void testFindByFileID_resourceNotFound() {
        doThrow(ResourceNotFoundException.class).when(queryHelper).getItem(anyString(), any());

        AppException exception = assertThrows(AppException.class, () -> {
            repository.findByFileID(fileID);
        });

        assertEquals(500, exception.getError().getCode());
    }

    @Test
    void testFindByFileID() {
        FileLocationDoc doc = FileLocationDoc.builder().driver("GCS").build();
        when(queryHelper.getItem(anyString(), any())).thenReturn(Optional.of(doc));
        assertNotNull(repository.findByFileID(fileID));
    }

    @Test
    void testSave() {
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn(partitionID);
        FileLocation fileLocation = new FileLocation(fileID, driver, location, createdAt, createdBy);
        assertEquals(fileLocation, repository.save(fileLocation));
    }

    @Test
    void testFindAll_Exception() {

        short num = 1000;

        FileListRequest request = new FileListRequest(LocalDateTime.now(), LocalDateTime.of(2040, 1, 1, 1, 0, 0), 0, num, "userID");
        when(queryHelper.scanPage(any(ScanEnhancedRequest.class))).thenThrow(new RuntimeException());
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn(partitionID);

        assertThrows(OsduException.class, () -> {
            repository.findAll(request);
        });
    }

    @Test
    void testFindAll() {

        short num = 1000;

        FileListRequest request = new FileListRequest(LocalDateTime.now(), LocalDateTime.of(2040, 1, 1, 1, 0, 0), 0, num, "userID");
        List<FileLocationDoc> results = new ArrayList<FileLocationDoc>();
        FileLocationDoc doc = mock(FileLocationDoc.class);
        QueryPageResult<FileLocationDoc> docs = new QueryPageResult<FileLocationDoc>(results, null, null);

        results.add(doc);
        when(doc.createFileLocationFromDoc()).thenReturn(new FileLocation());
        doReturn(docs).when(queryHelper).scanPage(any(ScanEnhancedRequest.class));
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn(partitionID);

        assertNotNull(repository.findAll(request));
    }

    @Test
    void testFindAll_empty() {

        short num = 1000;

        FileListRequest request = new FileListRequest(LocalDateTime.of(2024, 11, 21, 15, 21, 4), LocalDateTime.of(2040, 1, 1, 1, 0, 0), 0, num, "userID");
        List<FileLocationDoc> results = new ArrayList<FileLocationDoc>();
        QueryPageResult<FileLocationDoc> docs = new QueryPageResult<FileLocationDoc>(results, null, null);

        doReturn(docs).when(queryHelper).scanPage(any(ScanEnhancedRequest.class));
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn(partitionID);

        FileLocationNotFoundException exception = assertThrows(FileLocationNotFoundException.class, () -> {
            repository.findAll(request);
        });

        assertEquals("No file locations found for user userID and time range 2024-11-21T15:21:04 to 2040-01-01T01:00", exception.getMessage());

    }
}
