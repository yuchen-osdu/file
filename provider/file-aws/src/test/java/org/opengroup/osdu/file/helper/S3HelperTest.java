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

package org.opengroup.osdu.file.helper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.file.provider.aws.auth.TemporaryCredentials;
import org.opengroup.osdu.file.provider.aws.helper.S3Helper;
import org.opengroup.osdu.file.provider.aws.model.S3Location;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class S3HelperTest {

    private final TemporaryCredentials credentials = new TemporaryCredentials("accessKey", "secretKey", "sessionToken",
            new Date(System.currentTimeMillis() + 3600L * 1000L));
    private final String uri = "s3://my-bucket/my-key";
    private final S3Location location = S3Location.of(uri);

    @Test
    void testGeneratePresignedUrlForGet() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        S3ClientBuilder s3ClientBuilder = mock(S3ClientBuilder.class);
        S3Presigner presigner = mock(S3Presigner.class);
        S3Presigner.Builder presignerBuilder = mock(S3Presigner.Builder.class);
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        URL expectedUrl = new URL("http://localhost");

        try (MockedStatic<S3Client> s3ClientMock = mockStatic(S3Client.class);
             MockedStatic<S3Presigner> presignerMock = mockStatic(S3Presigner.class)) {
            
            s3ClientMock.when(S3Client::builder).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.credentialsProvider(any())).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.build()).thenReturn(s3Client);
            when(s3Client.getBucketLocation(any(GetBucketLocationRequest.class)))
                .thenReturn(GetBucketLocationResponse.builder().locationConstraint("us-east-1").build());
            
            presignerMock.when(S3Presigner::builder).thenReturn(presignerBuilder);
            when(presignerBuilder.region(any(Region.class))).thenReturn(presignerBuilder);
            when(presignerBuilder.credentialsProvider(any())).thenReturn(presignerBuilder);
            when(presignerBuilder.build()).thenReturn(presigner);
            when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);
            when(presignedRequest.url()).thenReturn(expectedUrl);
            
            URL actual = S3Helper.generatePresignedUrl(location, SdkHttpMethod.GET, new Date(), credentials);
            assertEquals(expectedUrl, actual);
        }
    }

    @Test
    void testGeneratePresignedUrlForPut() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        S3ClientBuilder s3ClientBuilder = mock(S3ClientBuilder.class);
        S3Presigner presigner = mock(S3Presigner.class);
        S3Presigner.Builder presignerBuilder = mock(S3Presigner.Builder.class);
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        URL expectedUrl = new URL("http://localhost");

        try (MockedStatic<S3Client> s3ClientMock = mockStatic(S3Client.class);
             MockedStatic<S3Presigner> presignerMock = mockStatic(S3Presigner.class)) {
            
            s3ClientMock.when(S3Client::builder).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.credentialsProvider(any())).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.build()).thenReturn(s3Client);
            when(s3Client.getBucketLocation(any(GetBucketLocationRequest.class)))
                .thenReturn(GetBucketLocationResponse.builder().locationConstraint("us-east-1").build());
            
            presignerMock.when(S3Presigner::builder).thenReturn(presignerBuilder);
            when(presignerBuilder.region(any(Region.class))).thenReturn(presignerBuilder);
            when(presignerBuilder.credentialsProvider(any())).thenReturn(presignerBuilder);
            when(presignerBuilder.build()).thenReturn(presigner);
            when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);
            when(presignedRequest.url()).thenReturn(expectedUrl);
            
            URL actual = S3Helper.generatePresignedUrl(location, SdkHttpMethod.PUT, new Date(), credentials);
            assertEquals(expectedUrl, actual);
        }
    }

    @Test
    void testGeneratePresignedUrlResponseHeaders() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        S3ClientBuilder s3ClientBuilder = mock(S3ClientBuilder.class);
        S3Presigner presigner = mock(S3Presigner.class);
        S3Presigner.Builder presignerBuilder = mock(S3Presigner.Builder.class);
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        URL expectedUrl = new URL("http://localhost");
        AwsRequestOverrideConfiguration overrideConfig = mock(AwsRequestOverrideConfiguration.class);

        try (MockedStatic<S3Client> s3ClientMock = mockStatic(S3Client.class);
             MockedStatic<S3Presigner> presignerMock = mockStatic(S3Presigner.class)) {
            
            s3ClientMock.when(S3Client::builder).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.credentialsProvider(any())).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.build()).thenReturn(s3Client);
            when(s3Client.getBucketLocation(any(GetBucketLocationRequest.class)))
                .thenReturn(GetBucketLocationResponse.builder().locationConstraint("us-east-1").build());
            
            presignerMock.when(S3Presigner::builder).thenReturn(presignerBuilder);
            when(presignerBuilder.region(any(Region.class))).thenReturn(presignerBuilder);
            when(presignerBuilder.credentialsProvider(any())).thenReturn(presignerBuilder);
            when(presignerBuilder.build()).thenReturn(presigner);
            when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);
            when(presignedRequest.url()).thenReturn(expectedUrl);
            
            URL actual = S3Helper.generatePresignedUrl(location, SdkHttpMethod.GET, new Date(), credentials, overrideConfig);
            assertEquals(expectedUrl, actual);
        }
    }

    @Test
    void testDoesObjectExist() {
        S3Client s3Client = mock(S3Client.class);
        S3ClientBuilder s3ClientBuilder = mock(S3ClientBuilder.class);

        try (MockedStatic<S3Client> s3ClientMock = mockStatic(S3Client.class)) {
            s3ClientMock.when(S3Client::builder).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.credentialsProvider(any())).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.region(any(Region.class))).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.build()).thenReturn(s3Client);
            when(s3Client.getBucketLocation(any(GetBucketLocationRequest.class)))
                .thenReturn(GetBucketLocationResponse.builder().locationConstraint("us-east-1").build());
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());
            
            boolean actual = S3Helper.doesObjectExist(location, credentials);
            assertTrue(actual);
            
            when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(SdkException.class);
            actual = S3Helper.doesObjectExist(location, credentials);
            assertFalse(actual);
        }
    }

    @Test
    void testDoesObjectCollectionExist() {
        S3Client s3Client = mock(S3Client.class);
        S3ClientBuilder s3ClientBuilder = mock(S3ClientBuilder.class);

        try (MockedStatic<S3Client> s3ClientMock = mockStatic(S3Client.class)) {
            s3ClientMock.when(S3Client::builder).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.credentialsProvider(any())).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.region(any(Region.class))).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.build()).thenReturn(s3Client);
            when(s3Client.getBucketLocation(any(GetBucketLocationRequest.class)))
                .thenReturn(GetBucketLocationResponse.builder().locationConstraint("us-east-1").build());
            when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(Collections.emptyList()).build());
            
            boolean actual = S3Helper.doesObjectCollectionExist(location, credentials);
            assertFalse(actual);
            
            when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(S3Object.builder().key("test").build()).build());
            actual = S3Helper.doesObjectCollectionExist(location, credentials);
            assertTrue(actual);
            
            when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenThrow(SdkException.class);
            actual = S3Helper.doesObjectCollectionExist(location, credentials);
            assertFalse(actual);
        }
    }

    @Test
    void testGetBucketRegion() {
        S3Client s3Client = mock(S3Client.class);
        S3ClientBuilder s3ClientBuilder = mock(S3ClientBuilder.class);
        String expectedRegion = "us-east-1";
        String bucketName = "my-bucket";

        try (MockedStatic<S3Client> s3ClientMock = mockStatic(S3Client.class)) {
            s3ClientMock.when(S3Client::builder).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.credentialsProvider(any())).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.build()).thenReturn(s3Client);
            when(s3Client.getBucketLocation(any(GetBucketLocationRequest.class)))
                .thenReturn(GetBucketLocationResponse.builder().locationConstraint(expectedRegion).build());
            
            String actual = S3Helper.getBucketRegion(bucketName, credentials);
            assertEquals(expectedRegion, actual);
        }
    }

    @Test
    void testGetBucketRegionNullConstraint() {
        S3Client s3Client = mock(S3Client.class);
        S3ClientBuilder s3ClientBuilder = mock(S3ClientBuilder.class);

        try (MockedStatic<S3Client> s3ClientMock = mockStatic(S3Client.class)) {
            s3ClientMock.when(S3Client::builder).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.credentialsProvider(any())).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.build()).thenReturn(s3Client);
            when(s3Client.getBucketLocation(any(GetBucketLocationRequest.class)))
                .thenReturn(GetBucketLocationResponse.builder().build());
            
            String actual = S3Helper.getBucketRegion("my-bucket", credentials);
            assertEquals("us-east-1", actual);
        }
    }

    @Test
    void testGetObject() {
        S3Client s3Client = mock(S3Client.class);
        S3ClientBuilder s3ClientBuilder = mock(S3ClientBuilder.class);
        ResponseInputStream<GetObjectResponse> responseStream = mock(ResponseInputStream.class);

        try (MockedStatic<S3Client> s3ClientMock = mockStatic(S3Client.class)) {
            s3ClientMock.when(S3Client::builder).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.credentialsProvider(any())).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.region(any(Region.class))).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.build()).thenReturn(s3Client);
            when(s3Client.getBucketLocation(any(GetBucketLocationRequest.class)))
                .thenReturn(GetBucketLocationResponse.builder().locationConstraint("us-east-1").build());
            when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);
            
            ResponseInputStream<GetObjectResponse> actual = S3Helper.getObject(location, credentials);
            assertEquals(responseStream, actual);
            assertNotNull(actual);
        }
    }

    @Test
    void testCreateS3Client() {
        S3Client s3Client = mock(S3Client.class);
        S3ClientBuilder s3ClientBuilder = mock(S3ClientBuilder.class);

        try (MockedStatic<S3Client> s3ClientMock = mockStatic(S3Client.class)) {
            s3ClientMock.when(S3Client::builder).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.credentialsProvider(any())).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.region(any(Region.class))).thenReturn(s3ClientBuilder);
            when(s3ClientBuilder.build()).thenReturn(s3Client);
            when(s3Client.getBucketLocation(any(GetBucketLocationRequest.class)))
                .thenReturn(GetBucketLocationResponse.builder().locationConstraint("us-east-1").build());
            
            S3Client actual = S3Helper.createS3Client("my-bucket", credentials);
            assertNotNull(actual);
        }
    }

}