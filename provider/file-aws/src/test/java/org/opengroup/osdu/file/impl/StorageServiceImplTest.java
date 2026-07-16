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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.dms.model.RetrievalInstructionsResponse;
import org.opengroup.osdu.core.common.dms.model.StorageInstructionsResponse;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.file.model.FileRetrievalData;
import org.opengroup.osdu.file.provider.aws.helper.S3Helper;
import org.opengroup.osdu.file.provider.aws.model.ProviderLocation;
import org.opengroup.osdu.file.provider.aws.service.FileLocationProvider;
import org.opengroup.osdu.file.util.ExpiryTimeUtil;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.opengroup.osdu.file.model.SignedUrl;
import org.opengroup.osdu.file.model.SignedUrlParameters;
import org.opengroup.osdu.file.provider.aws.auth.TemporaryCredentials;
import org.opengroup.osdu.file.provider.aws.impl.StorageServiceImpl;

@ExtendWith(MockitoExtension.class)
class StorageServiceImplTest {

    private final String fileID = "fileID";
	private final String authorizationToken = "authorizationToken";
	private final String partitionID = "partitionID";
	private final String datasetId = "datasetId";
	private final String localhost = "http://localhost";
	private final String malform = "hp://localhost,malform";
	private final String s3Uri = "s3://somebucket/something/";

    @Mock
    ObjectMapper objectMapper;

    @Mock
    DpsHeaders headers;

    @Mock
    FileLocationProvider fileLocationProvider;

    private ExpiryTimeUtil expiryTimeUtil = new ExpiryTimeUtil();

    private StorageServiceImpl storageService;

    @BeforeEach
    void setup() {
        storageService = new StorageServiceImpl(fileLocationProvider, headers, objectMapper, expiryTimeUtil);
    }

    @Test
    void testCreateSignedUrlWithExpiryTime() throws Exception {
        // Arrange
        SignedUrlParameters params = new SignedUrlParameters();
        params.setExpiryTime("12H");

        ProviderLocation mockLocation = ProviderLocation.builder()
            .signedUrl(new URI("https://test-bucket.s3.amazonaws.com/test-file?signature=test"))
            .locationSource("s3://test-bucket/test-file")
            .build();

        when(fileLocationProvider.getUploadFileLocation(eq(fileID), eq(partitionID), any(Duration.class)))
            .thenReturn(mockLocation);
        when(headers.getUserEmail()).thenReturn("test@example.com");

        // Act
        SignedUrl result = storageService.createSignedUrl(fileID, authorizationToken, partitionID, params);

        // Assert
        assertNotNull(result);
        verify(fileLocationProvider).getUploadFileLocation(fileID, partitionID, Duration.ofHours(12));
    }

    @Test
    void testCreateStorageInstructionsWithExpiryTime() throws Exception {
        // Arrange
        SignedUrlParameters params = new SignedUrlParameters();
        params.setExpiryTime("7D");

        TemporaryCredentials mockCredentials = TemporaryCredentials.builder()
            .accessKeyId("test-key")
            .secretAccessKey("test-secret")
            .sessionToken("test-token")
            .expiration(new Date())
            .build();

        ProviderLocation mockLocation = ProviderLocation.builder()
            .unsignedUrl("s3://test-bucket/test-file")
            .signedUrl(new URI("https://test-bucket.s3.amazonaws.com/test-file?signature=test"))
            .locationSource("s3://test-bucket/test-file")
            .connectionString("test-connection")
            .credentials(mockCredentials)
            .createdAt(Instant.now())
            .build();

        when(fileLocationProvider.getUploadFileLocation(eq(datasetId), eq(partitionID), any(Duration.class)))
            .thenReturn(mockLocation);
        when(headers.getUserEmail()).thenReturn("test@example.com");

        try (MockedStatic<S3Helper> s3HelperMock = mockStatic(S3Helper.class)) {
            s3HelperMock.when(() -> S3Helper.getBucketRegion(anyString(), any())).thenReturn("us-east-1");

            // Act
            StorageInstructionsResponse result = storageService.createStorageInstructions(datasetId, partitionID, params);

            // Assert
            assertNotNull(result);
            verify(fileLocationProvider).getUploadFileLocation(datasetId, partitionID, Duration.ofDays(7));
        }
    }

    @Test
    void testGetDurationFromExpiryTime() {
        // Arrange
        SignedUrlParameters params = new SignedUrlParameters();
        params.setExpiryTime("24H");

        ProviderLocation mockLocation = ProviderLocation.builder()
            .signedUrl(URI.create("https://test.com"))
            .locationSource("test")
            .build();

        when(fileLocationProvider.getUploadFileLocation(anyString(), anyString(), any(Duration.class)))
            .thenReturn(mockLocation);
        when(headers.getUserEmail()).thenReturn("test@example.com");

        // Act
        storageService.createSignedUrl("test", "token", "partition", params);

        // Assert - verify 24 hours duration was passed
        verify(fileLocationProvider).getUploadFileLocation(anyString(), anyString(), eq(Duration.ofHours(24)));
    }

    @Test
    void testCreateSignedUrl() throws Exception {
		ProviderLocation fileLocation = mock(ProviderLocation.class);
		when(fileLocation.getSignedUrl()).thenReturn(new URI(localhost));
		when(fileLocationProvider.getUploadFileLocation(fileID, partitionID)).thenReturn(fileLocation);

        SignedUrl url = storageService.createSignedUrl(fileID, authorizationToken, partitionID);
		assertEquals(localhost, url.getUri().toString());
    }

	@Test
    void testCreateSignedUrl_malform() throws Exception {
		ProviderLocation fileLocation = mock(ProviderLocation.class);
		when(fileLocation.getSignedUrl()).thenReturn(new URI(malform));
		when(fileLocationProvider.getUploadFileLocation(fileID, partitionID)).thenReturn(fileLocation);

        assertThrows(AppException.class, () -> {
            storageService.createSignedUrl(fileID, authorizationToken, partitionID);
        });
    }

	@Test
	void testCreateStorageInstructions() throws Exception {
		ProviderLocation fileLocation = mock(ProviderLocation.class);
		when(fileLocation.getUnsignedUrl()).thenReturn(localhost);
		when(fileLocation.getSignedUrl()).thenReturn(new URI(localhost));
		when(fileLocationProvider.getUploadFileLocation(datasetId, partitionID)).thenReturn(fileLocation);

		try (MockedStatic<S3Helper> s3MockedStatic = mockStatic(S3Helper.class)) {
            s3MockedStatic
                    .when(() -> S3Helper.getBucketRegion(anyString(), any()))
                    .thenReturn("us-east-1");

			StorageInstructionsResponse response = storageService.createStorageInstructions(datasetId, partitionID);

			assertNotNull(response);
        }
	}

	@Test
	void testCreateRetrievalInstructions_invalidLocation() throws Exception {
		List<FileRetrievalData> fileRetrievalDatas = new ArrayList<FileRetrievalData>();
		FileRetrievalData fileRetrievalData = mock(FileRetrievalData.class);
		when(fileRetrievalData.getUnsignedUrl()).thenReturn(malform);
		fileRetrievalDatas.add(fileRetrievalData);

        assertThrows(AppException.class, () -> {
            storageService.createRetrievalInstructions(fileRetrievalDatas);
        });
	}

	@Test
	void testCreateRetrievalInstructions() throws Exception {
		List<FileRetrievalData> fileRetrievalDatas = new ArrayList<FileRetrievalData>();
		FileRetrievalData fileRetrievalData = mock(FileRetrievalData.class);
		when(fileRetrievalData.getUnsignedUrl()).thenReturn(s3Uri);

		TemporaryCredentials credentials = new TemporaryCredentials("accessKey", "secretKey", "sessionToken",
                new Date(System.currentTimeMillis() + 3600L * 1000L));
        String expectedRegion = "us-east-1";

        try (MockedStatic<S3Helper> s3MockedStatic = mockStatic(S3Helper.class)) {
            s3MockedStatic.when(() -> S3Helper.getBucketRegion(anyString(), any()))
                .thenReturn(expectedRegion);

			fileRetrievalDatas.add(fileRetrievalData);

			ProviderLocation fileLocation = mock(ProviderLocation.class);
			when(fileLocation.getUnsignedUrl()).thenReturn(localhost);
			when(fileLocation.getSignedUrl()).thenReturn(new URI(localhost));
			when(fileLocation.getLocationSource()).thenReturn(localhost);
			when(fileLocation.getCredentials()).thenReturn(credentials);
			when(fileLocationProvider.getRetrievalFileLocation(any(), any())).thenReturn(fileLocation);

			RetrievalInstructionsResponse response = storageService.createRetrievalInstructions(fileRetrievalDatas);

			assertNotNull(response);
        }
	}

	@Test
	void testCreateSignedUrlFileLocation_invalidLocation() {
		SignedUrlParameters signedUrlParameters = mock(SignedUrlParameters.class);

        assertThrows(AppException.class, () -> {
            storageService.createSignedUrlFileLocation(malform, "authorizationToken", signedUrlParameters);
        });
	}

	@Test
	void testCreateSignedUrlFileLocation() throws Exception {
		ExpiryTimeUtil expTimeUtil = new ExpiryTimeUtil();
		ProviderLocation fileLocation = mock(ProviderLocation.class);
		when(fileLocation.getSignedUrl()).thenReturn(new URI(localhost));
		when(fileLocationProvider.getRetrievalFileLocation(any(), any(), any())).thenReturn(fileLocation);

		SignedUrlParameters signedUrlParameters = mock(SignedUrlParameters.class);

		SignedUrl url = storageService.createSignedUrlFileLocation(s3Uri, "authorizationToken", signedUrlParameters);

		assertEquals(localhost, url.getUri().toString());
	}

	@Test
	void testCreateSignedUrlWithCustomExpiryTime() throws Exception {
		// Arrange
		SignedUrlParameters params = new SignedUrlParameters();
		params.setExpiryTime("12H");

		ProviderLocation mockLocation = ProviderLocation.builder()
			.signedUrl(new URI("https://test-bucket.s3.amazonaws.com/test-file"))
			.locationSource("s3://test-bucket/test-file")
			.build();

		when(fileLocationProvider.getUploadFileLocation(eq(fileID), eq(partitionID), any(Duration.class)))
			.thenReturn(mockLocation);
		when(headers.getUserEmail()).thenReturn("test@example.com");

		// Act
		SignedUrl result = storageService.createSignedUrl(fileID, authorizationToken, partitionID, params);

		// Assert
		assertNotNull(result);
		verify(fileLocationProvider).getUploadFileLocation(fileID, partitionID, Duration.ofHours(12));
	}

	@Test
	void testCreateStorageInstructionsWithCustomExpiryTime() throws Exception {
		// Arrange
		SignedUrlParameters params = new SignedUrlParameters();
		params.setExpiryTime("7D");

		TemporaryCredentials mockCredentials = TemporaryCredentials.builder()
			.accessKeyId("test-key")
			.secretAccessKey("test-secret")
			.sessionToken("test-token")
			.expiration(new Date())
			.build();

		ProviderLocation mockLocation = ProviderLocation.builder()
			.unsignedUrl("s3://test-bucket/test-file")
			.signedUrl(new URI("https://test-bucket.s3.amazonaws.com/test-file"))
			.locationSource("s3://test-bucket/test-file")
			.connectionString("test-connection")
			.credentials(mockCredentials)
			.createdAt(Instant.now())
			.build();

		when(fileLocationProvider.getUploadFileLocation(eq(datasetId), eq(partitionID), any(Duration.class)))
			.thenReturn(mockLocation);
		when(headers.getUserEmail()).thenReturn("test@example.com");

		try (MockedStatic<S3Helper> s3HelperMock = mockStatic(S3Helper.class)) {
			s3HelperMock.when(() -> S3Helper.getBucketRegion(anyString(), any())).thenReturn("us-east-1");

			// Act
			StorageInstructionsResponse result = storageService.createStorageInstructions(datasetId, partitionID, params);

			// Assert
			assertNotNull(result);
			verify(fileLocationProvider).getUploadFileLocation(datasetId, partitionID, Duration.ofDays(7));
		}
	}

}
