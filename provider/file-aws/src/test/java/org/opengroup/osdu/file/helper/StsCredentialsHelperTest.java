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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.aws.v2.sts.STSConfig;
import org.opengroup.osdu.file.exception.OsduBadRequestException;
import org.opengroup.osdu.file.provider.aws.config.ProviderConfigurationBag;
import org.opengroup.osdu.file.provider.aws.helper.StsCredentialsHelper;
import org.opengroup.osdu.file.provider.aws.model.S3Location;

import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.StsException;

@ExtendWith(MockitoExtension.class)
class StsCredentialsHelperTest {

    private static final String TEST_REGION = "us-east-1";
    private static final String TEST_BUCKET = "XXXXXX";
    private static final String TEST_KEY = "key";
    private static final String TEST_ROLE_ARN = "roleArn";
    private static final Date TEST_EXPIRATION = new Date(2040, Calendar.JANUARY, 1, 1, 0, 0);

    @Mock
    private StsClient securityTokenService;

    @Mock
    private AssumeRoleResponse assumeRoleResponse;

    private Credentials credentials;

    private ProviderConfigurationBag providerConfigurationBag;
    private StsCredentialsHelper stsCredentialsHelper;

    @BeforeEach
    void setUp() {
        providerConfigurationBag = new ProviderConfigurationBag();
        providerConfigurationBag.amazonRegion = TEST_REGION;

        // Setup default mock behavior
        credentials = Credentials.builder()
            .accessKeyId("accessKeyId")
            .secretAccessKey("secretAccessKey")
            .sessionToken("sessionToken")
            .expiration(TEST_EXPIRATION.toInstant())
            .build();
        lenient().when(assumeRoleResponse.credentials()).thenReturn(credentials);
        lenient().when(securityTokenService.assumeRole(any(AssumeRoleRequest.class))).thenReturn(assumeRoleResponse);
    }

    @Test
    void shouldGetUploadCredentials_WhenValidParametersProvided() {
        // Arrange
        try (MockedConstruction<STSConfig> config = Mockito.mockConstruction(STSConfig.class,
            (mock, context) -> when(mock.amazonSTS()).thenReturn(securityTokenService))) {

            stsCredentialsHelper = new StsCredentialsHelper(providerConfigurationBag);
            S3Location s3Location = new S3Location(TEST_BUCKET, TEST_KEY);

            // Act
            Object result = stsCredentialsHelper.getUploadCredentials(s3Location, TEST_ROLE_ARN, TEST_EXPIRATION);

            // Assert
            assertNotNull(result, "Upload credentials should not be null");
            verify(securityTokenService).assumeRole(any(AssumeRoleRequest.class));
            verify(assumeRoleResponse).credentials();
        }
    }

    @Test
    void shouldGetRetrievalCredentials_WhenValidParametersProvided() {
        // Arrange
        try (MockedConstruction<STSConfig> config = Mockito.mockConstruction(STSConfig.class,
                                                                             (mock, context) -> when(mock.amazonSTS()).thenReturn(securityTokenService))) {

            stsCredentialsHelper = new StsCredentialsHelper(providerConfigurationBag);
            S3Location s3Location = new S3Location(TEST_BUCKET, TEST_KEY);

            // Act
            Object result = stsCredentialsHelper.getRetrievalCredentials(s3Location, TEST_ROLE_ARN, TEST_EXPIRATION);

            // Assert
            assertNotNull(result, "Retrieval credentials should not be null");
            verify(securityTokenService).assumeRole(any(AssumeRoleRequest.class));
            verify(assumeRoleResponse).credentials();
        }
    }

    @Test
    void shouldThrowRuntimeException_WhenSecurityTokenServiceFails() {
        // Arrange
        when(securityTokenService.assumeRole(any(AssumeRoleRequest.class)))
            .thenThrow(StsException.builder().message("Failed to assume role").build());

        try (MockedConstruction<STSConfig> config = Mockito.mockConstruction(STSConfig.class,
            (mock, context) -> when(mock.amazonSTS()).thenReturn(securityTokenService))) {

            stsCredentialsHelper = new StsCredentialsHelper(providerConfigurationBag);
            S3Location s3Location = new S3Location(TEST_BUCKET, TEST_KEY);

            // Act & Assert
            assertThrows(OsduBadRequestException.class, () -> {
                stsCredentialsHelper.getUploadCredentials(s3Location, TEST_ROLE_ARN, TEST_EXPIRATION);
            });
        }
    }

    @Test
    void shouldRoundUpToOneMinute_WhenDurationIsLessThanOneMinute() {
        // Arrange
        try (MockedConstruction<STSConfig> config = Mockito.mockConstruction(STSConfig.class,
            (mock, context) -> when(mock.amazonSTS()).thenReturn(securityTokenService))) {

            stsCredentialsHelper = new StsCredentialsHelper(providerConfigurationBag);
            S3Location s3Location = new S3Location(TEST_BUCKET, TEST_KEY);

            Date now = new Date();
            // Set expiration to 45 seconds
            Date expiration = new Date(now.getTime() + 45_000);

            // Act
            stsCredentialsHelper.getUploadCredentials(s3Location, TEST_ROLE_ARN, expiration);

            // Assert
            verify(securityTokenService).assumeRole(argThat((AssumeRoleRequest request) ->
                request.durationSeconds() == 60));
        }
    }
}

