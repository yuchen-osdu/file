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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.file.model.FileRetrievalData;
import org.opengroup.osdu.file.model.SignedUrlParameters;
import org.opengroup.osdu.file.provider.aws.helper.S3Helper;
import org.opengroup.osdu.file.provider.aws.impl.FileCollectionStorageServiceImpl;
import org.opengroup.osdu.file.provider.aws.model.ProviderLocation;
import org.opengroup.osdu.file.provider.aws.service.FileLocationProvider;
import org.opengroup.osdu.file.util.ExpiryTimeUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class FileCollectionStorageServiceImplTest {

    private final String datasetID = "datasetID";
    private final String partitionID = "partitionID";
    private final String s3Url = "s3://somebucket/something/";

    @Mock
    FileLocationProvider fileLocationProvider;

    @Mock
    DpsHeaders headers;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    ExpiryTimeUtil expiryTimeUtil;

    @InjectMocks
    FileCollectionStorageServiceImpl service;

    MockedStatic<S3Helper> mockS3Helper;

    @BeforeEach
	void setUp() {
        mockS3Helper = mockStatic(S3Helper.class);
	}

	@AfterEach
	void after() {
        mockS3Helper.close();
	}	

    @Test
    void testCreateStorageInstructions() {

        when(fileLocationProvider.getFileCollectionUploadLocation(anyString(), anyString())).thenReturn(new ProviderLocation());

        mockS3Helper.when(() -> S3Helper.getBucketRegion(any(), any())).thenReturn("us-east-1");

        assertNotNull(service.createStorageInstructions(datasetID, partitionID));
    }

    @Test
    void testCreateRetrievalInstructions_badRequest() {

        List<FileRetrievalData> fileRetrievalDatas = new ArrayList<FileRetrievalData>();
        FileRetrievalData fileRetrievalData = mock(FileRetrievalData.class);
        fileRetrievalDatas.add(fileRetrievalData);

        assertThrows(AppException.class, () -> {
            service.createRetrievalInstructions(fileRetrievalDatas);
        });
    }

    @Test
    void testCreateRetrievalInstructions() {

        ExpiryTimeUtil realExpiryTimeUtil = new ExpiryTimeUtil();
        SignedUrlParameters signedUrlParameters = new SignedUrlParameters();
        List<FileRetrievalData> fileRetrievalDatas = new ArrayList<FileRetrievalData>();
        FileRetrievalData fileRetrievalData = mock(FileRetrievalData.class);
        when(fileRetrievalData.getUnsignedUrl()).thenReturn(s3Url);
        when(expiryTimeUtil.getExpiryTimeValueInTimeUnit(any())).thenReturn(realExpiryTimeUtil.getExpiryTimeValueInTimeUnit(signedUrlParameters.getExpiryTime()));
        when(fileLocationProvider.getFileCollectionRetrievalLocation(any(), any())).thenReturn(new ProviderLocation());

        mockS3Helper.when(() -> S3Helper.getBucketRegion(any(), any())).thenReturn("us-east-1");

        fileRetrievalDatas.add(fileRetrievalData);

        assertNotNull(service.createRetrievalInstructions(fileRetrievalDatas));
    }
}
