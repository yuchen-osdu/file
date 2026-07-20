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

package org.opengroup.osdu.file.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.aws.v2.s3.util.S3ClientConnectionInfo;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.file.provider.aws.auth.TemporaryCredentials;
import org.opengroup.osdu.file.provider.aws.config.ProviderConfigurationBag;
import org.opengroup.osdu.file.provider.aws.helper.ExpirationDateHelper;
import org.opengroup.osdu.file.provider.aws.helper.S3ConnectionInfoHelper;
import org.opengroup.osdu.file.provider.aws.helper.S3Helper;
import org.opengroup.osdu.file.provider.aws.helper.StsCredentialsHelper;
import org.opengroup.osdu.file.provider.aws.helper.StsRoleHelper;
import org.opengroup.osdu.file.provider.aws.model.S3Location;
import org.opengroup.osdu.file.provider.aws.service.impl.FileLocationProviderImpl;

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;


@ExtendWith(MockitoExtension.class)
class FileLocationProviderImplTest  {

    private final String fileID = "fileID";
    private final String datasetID = "datasetID";
    private final String partitionID = "partitionID";
    private final String bucketName = "bucketName";
    private final String keyName = "keyName";
    private final String region = "us-east-1";
    private final String endpoint = "endpoint";
    private final String bucketParameterRelativePath = "bucketParameterRelativePath";
    private final String stsRoleIamParameterRelativePath = "stsRoleIamParameterRelativePath";
    private final String stsRoleArn = "stsRoleArn";
    private final String localhost = "http://localhost";
    private final String badURL = "http://localhost^bad";

    @Mock
    ProviderConfigurationBag providerConfigurationBag;

    @Mock
    DpsHeaders headers;

    @Mock
    StsRoleHelper stsRoleHelper;

    @Mock
    StsCredentialsHelper stsCredentialsHelper;

    @Mock
    S3ConnectionInfoHelper s3ConnectionInfoHelper;

    @Mock
    ExpirationDateHelper expirationDateHelper;

	@Mock
    S3Helper s3Helper;

    MockedStatic<S3Helper> mockS3Helper;

    private FileLocationProviderImpl provider;
    private final Duration duration = Duration.ofSeconds(1800);

    @BeforeEach
	void setUp() {
        mockS3Helper = mockStatic(S3Helper.class);
        provider = new FileLocationProviderImpl(providerConfigurationBag, stsCredentialsHelper, stsRoleHelper, s3ConnectionInfoHelper, headers);
	}

	@AfterEach
	void after() {
        mockS3Helper.close();
	}	

    @Test
    void testGetUploadFileLocation_nullS3ConnectionInfo() {

        assertThrows(AppException.class, () -> {
            provider.getUploadFileLocation(fileID, partitionID);
        });

    }

    @Test
    void testGetUploadFileLocation_nullStsRoleArn() {

        providerConfigurationBag.bucketParameterRelativePath = bucketParameterRelativePath;
        when(s3ConnectionInfoHelper.getS3ConnectionInfoForPartition(any(DpsHeaders.class), anyString())).thenReturn(new S3ClientConnectionInfo(bucketName, region, endpoint));

        assertThrows(AppException.class, () -> {
            provider.getUploadFileLocation(fileID, partitionID);
        });

    }

    

    @Test
    void testGetUploadLocation() throws MalformedURLException {

        TemporaryCredentials credentials = new TemporaryCredentials("accessKey", "secretKey", "sessionToken",
                new Date(System.currentTimeMillis() + 3600L * 1000L));

        providerConfigurationBag.bucketParameterRelativePath = bucketParameterRelativePath;
        when(s3ConnectionInfoHelper.getS3ConnectionInfoForPartition(any(DpsHeaders.class), anyString())).thenReturn(new S3ClientConnectionInfo(bucketName, region, endpoint));
        providerConfigurationBag.stsRoleIamParameterRelativePath = stsRoleIamParameterRelativePath;
        when(stsRoleHelper.getRoleArnForPartition(any(DpsHeaders.class), anyString())).thenReturn(stsRoleArn);
        when(stsCredentialsHelper.getUploadCredentials(any(), anyString(), any())).thenReturn(credentials);

        mockS3Helper.when(() -> S3Helper.generatePresignedUrl(any(), any(), any(), any())).thenReturn(new URL(localhost));

        assertNotNull(provider.getUploadFileLocation(fileID, partitionID));
        assertNotNull(provider.getFileCollectionUploadLocation(datasetID, partitionID));

    }

    @Test
    void testGetUploadFileLocation_badURI() throws MalformedURLException {

        TemporaryCredentials credentials = new TemporaryCredentials("accessKey", "secretKey", "sessionToken",
                new Date(System.currentTimeMillis() + 3600L * 1000L));

        providerConfigurationBag.bucketParameterRelativePath = bucketParameterRelativePath;
        when(s3ConnectionInfoHelper.getS3ConnectionInfoForPartition(any(DpsHeaders.class), anyString())).thenReturn(new S3ClientConnectionInfo(bucketName, region, endpoint));
        providerConfigurationBag.stsRoleIamParameterRelativePath = stsRoleIamParameterRelativePath;
        when(stsRoleHelper.getRoleArnForPartition(any(DpsHeaders.class), anyString())).thenReturn(stsRoleArn);
        when(stsCredentialsHelper.getUploadCredentials(any(), anyString(), any())).thenReturn(credentials);

        mockS3Helper.when(() -> S3Helper.generatePresignedUrl(any(), any(), any(), any())).thenReturn(new URL(badURL));

        assertThrows(AppException.class, () -> {
            provider.getUploadFileLocation(fileID, partitionID);
        });

    }

    @Test
    void testGetRetrievalFileLocation_nullStsRoleArn() {
        S3Location s3Location = new S3Location(bucketName, keyName);
        assertThrows(AppException.class, () -> {
            provider.getRetrievalFileLocation(s3Location, duration);
        });
    }

    @Test
    void testGetRetrievalLocation() throws MalformedURLException {

        TemporaryCredentials credentials = new TemporaryCredentials("accessKey", "secretKey", "sessionToken",
                new Date(System.currentTimeMillis() + 3600L * 1000L));

        providerConfigurationBag.stsRoleIamParameterRelativePath = stsRoleIamParameterRelativePath;
        when(stsRoleHelper.getRoleArnForPartition(any(DpsHeaders.class), anyString())).thenReturn(stsRoleArn);
        when(stsCredentialsHelper.getRetrievalCredentials(any(), anyString(), any())).thenReturn(credentials);

        mockS3Helper.when(() -> S3Helper.doesObjectExist(any(), any())).thenReturn(true);
        mockS3Helper.when(() -> S3Helper.doesObjectCollectionExist(any(), any())).thenReturn(true);
        mockS3Helper.when(() -> S3Helper.generatePresignedUrl(any(), any(), any(), any())).thenReturn(new URL(localhost));

        assertNotNull(provider.getRetrievalFileLocation(new S3Location(bucketName, keyName), duration));
        assertNotNull(provider.getFileCollectionRetrievalLocation(new S3Location(bucketName, keyName + "/"), duration));
    }

    @Test
    void testGetRetrievalFileLocation_responseHeaderOverrides() throws MalformedURLException {

        TemporaryCredentials credentials = new TemporaryCredentials("accessKey", "secretKey", "sessionToken",
                new Date(System.currentTimeMillis() + 3600L * 1000L));

        providerConfigurationBag.stsRoleIamParameterRelativePath = stsRoleIamParameterRelativePath;
        when(stsRoleHelper.getRoleArnForPartition(any(DpsHeaders.class), anyString())).thenReturn(stsRoleArn);
        when(stsCredentialsHelper.getRetrievalCredentials(any(), anyString(), any())).thenReturn(credentials);

        mockS3Helper.when(() -> S3Helper.doesObjectExist(any(), any())).thenReturn(true);
        mockS3Helper.when(() -> S3Helper.generatePresignedUrl(any(), any(), any(), any(), any())).thenReturn(new URL(localhost));
        AwsRequestOverrideConfiguration awsRequestOverrideConfiguration = mock(AwsRequestOverrideConfiguration.class);

        assertNotNull(provider.getRetrievalFileLocation(new S3Location(bucketName, keyName), duration, awsRequestOverrideConfiguration));
    }

    @Test
    void testGetRetrievalFileLocation_badURI() throws MalformedURLException {

        TemporaryCredentials credentials = new TemporaryCredentials("accessKey", "secretKey", "sessionToken",
                new Date(System.currentTimeMillis() + 3600L * 1000L));

        providerConfigurationBag.stsRoleIamParameterRelativePath = stsRoleIamParameterRelativePath;
        when(stsRoleHelper.getRoleArnForPartition(any(DpsHeaders.class), anyString())).thenReturn(stsRoleArn);
        when(stsCredentialsHelper.getRetrievalCredentials(any(), anyString(), any())).thenReturn(credentials);

        mockS3Helper.when(() -> S3Helper.doesObjectExist(any(), any())).thenReturn(true);
        mockS3Helper.when(() -> S3Helper.generatePresignedUrl(any(), any(), any(), any())).thenReturn(new URL(badURL));
        S3Location s3Location = new S3Location(bucketName, keyName);

        assertThrows(AppException.class, () -> {
            provider.getRetrievalFileLocation(s3Location, duration);
        });
    }

    @Test
    void validateInput_invalidObjectKey() {

        S3Location unsignedLocation = mock(S3Location.class);

        when(stsRoleHelper.getRoleArnForPartition(any(DpsHeaders.class), any())).thenReturn(stsRoleArn);
        when(unsignedLocation.isValid()).thenReturn(true);
        when(unsignedLocation.isFolder()).thenReturn(true);

        assertThrows(AppException.class, () -> {
            provider.getRetrievalFileLocation(unsignedLocation, duration);
        });
    }

    @Test
    void validateInput_FileCollection_invalidObjectKey() {

        S3Location unsignedLocation = mock(S3Location.class);

        when(stsRoleHelper.getRoleArnForPartition(any(DpsHeaders.class), any())).thenReturn(stsRoleArn);
        when(unsignedLocation.isValid()).thenReturn(true);
        when(unsignedLocation.isFile()).thenReturn(true);

        assertThrows(AppException.class, () -> {
            provider.getFileCollectionRetrievalLocation(unsignedLocation, duration);
        });
    }

    @Test
    void validateInput_fileNotExist() {

        S3Location unsignedLocation = mock(S3Location.class);

        when(stsRoleHelper.getRoleArnForPartition(any(DpsHeaders.class), any())).thenReturn(stsRoleArn);
        when(unsignedLocation.isValid()).thenReturn(true);
        when(unsignedLocation.isFolder()).thenReturn(false);
        mockS3Helper.when(() -> S3Helper.doesObjectExist(any(), any())).thenReturn(false);

        assertThrows(AppException.class, () -> {
            provider.getRetrievalFileLocation(unsignedLocation, duration);
        });
    }

    @Test
    void validateInput_folderNotExist() {

        S3Location unsignedLocation = mock(S3Location.class);

        when(stsRoleHelper.getRoleArnForPartition(any(DpsHeaders.class), any())).thenReturn(stsRoleArn);
        when(unsignedLocation.isValid()).thenReturn(true);
        mockS3Helper.when(() -> S3Helper.doesObjectCollectionExist(any(), any())).thenReturn(false);

        assertThrows(AppException.class, () -> {
            provider.getFileCollectionRetrievalLocation(unsignedLocation, duration);
        });
    }
    
}
