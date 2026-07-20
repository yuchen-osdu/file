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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.file.exception.OsduBadRequestException;
import org.opengroup.osdu.file.provider.aws.helper.ExpirationDateHelper;
import org.opengroup.osdu.file.provider.aws.helper.S3Helper;
import org.opengroup.osdu.file.util.ExpiryTimeUtil;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.opengroup.osdu.file.provider.aws.auth.TemporaryCredentials;
import org.opengroup.osdu.file.provider.aws.impl.StorageUtilServiceImpl;
import org.opengroup.osdu.file.provider.aws.config.ProviderConfigurationBag;
import org.opengroup.osdu.file.provider.aws.helper.StsCredentialsHelper;
import org.opengroup.osdu.file.provider.aws.helper.StsRoleHelper;
import org.opengroup.osdu.file.util.ExpiryTimeUtil.RelativeTimeValue;

@ExtendWith(MockitoExtension.class)
class StorageUtilServiceImplTest {

    @Mock
    StsCredentialsHelper stsCredentialsHelper;

    @Mock
    StsRoleHelper stsRoleHelper;

    @Mock
    DpsHeaders headers;

    @Mock
    ProviderConfigurationBag providerConfigurationBag;

    @Mock
    ExpiryTimeUtil expiryTimeUtil;



    @InjectMocks
    private StorageUtilServiceImpl storageUtilService;



    @Test
    void testGetChecksum() throws IOException {
        String uri = "s3://bucket/path/key";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600L * 1000L);
        TemporaryCredentials credentials = mock(TemporaryCredentials.class);
        
        try (MockedStatic<S3Helper> s3HelperMock = mockStatic(S3Helper.class);
             MockedStatic<ExpirationDateHelper> expMockedStatic = mockStatic(ExpirationDateHelper.class)) {
            
            when(stsCredentialsHelper.getRetrievalCredentials(any(), any(), any())).thenReturn(credentials);
            when(stsRoleHelper.getRoleArnForPartition(any(DpsHeaders.class), any())).thenReturn("testRole");
            
            ExpiryTimeUtil expTimeUtil = new ExpiryTimeUtil();
            RelativeTimeValue relativeTimeValue = expTimeUtil.getExpiryTimeValueInTimeUnit(null);
            when(expiryTimeUtil.getExpiryTimeValueInTimeUnit(any())).thenReturn(relativeTimeValue);
            
            expMockedStatic.when(() -> ExpirationDateHelper.getExpiration(any(Instant.class), any(Duration.class)))
                .thenReturn(expirationDate);
            
            s3HelperMock.when(() -> S3Helper.doesObjectExist(any(), any())).thenReturn(true);
            
            // Mock S3Client creation
            S3Client mockS3Client = mock(S3Client.class);
            s3HelperMock.when(() -> S3Helper.createS3Client(any(), any())).thenReturn(mockS3Client);
            
            ResponseInputStream<GetObjectResponse> responseStream = mock(ResponseInputStream.class);
            GetObjectResponse response = GetObjectResponse.builder().contentLength(10L).build();
            when(responseStream.response()).thenReturn(response);
            when(responseStream.read(any(byte[].class))).thenReturn(4, -1);
            when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);
            
            String actual = storageUtilService.getChecksum(uri);
            assertNotNull(actual);
        }
    }

    @Test
    void getChecksum_InvalidS3Location_ThrowsOsduBadRequestException() {
        String invalidS3Path = "invalid-s3-path";

        OsduBadRequestException exception = assertThrows(
            OsduBadRequestException.class,
            () -> storageUtilService.getChecksum(invalidS3Path)
        );

        assertTrue(exception.getMessage().contains("Invalid source file path to copy from " + invalidS3Path));
    }


}
