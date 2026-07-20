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

package org.opengroup.osdu.file.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.file.provider.aws.auth.TemporaryCredentials;
import org.opengroup.osdu.file.provider.aws.config.ProviderConfigurationBag;
import org.opengroup.osdu.file.provider.aws.helper.StsCredentialsHelper;
import org.opengroup.osdu.file.provider.aws.model.S3Location;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;

@Slf4j
@ExtendWith(MockitoExtension.class)
class StsHelperTest {

    private final String roleArn = "arn:partition:service:region:account-id:resource-id";
    private final S3Location fileLocation = S3Location.of("s3://bucket/path/key");

    private StsCredentialsHelper stsCredentialsHelper;

    @Mock
    private ProviderConfigurationBag providerConfigurationBag;
    @Mock
    private StsClient securityTokenService;

    @BeforeEach
    void setUp() {
        providerConfigurationBag.amazonRegion = "us-east-1";
        stsCredentialsHelper = new StsCredentialsHelper(providerConfigurationBag);

        ReflectionTestUtils.setField(stsCredentialsHelper, "securityTokenService", securityTokenService);
    }

    @Test
    void shouldGetFolderCredentialsForSingleFileTypes() throws JSONException {
        String expectedArn = "arn:aws:s3:::bucket";
        Instant now = Instant.now();
        Date expirationDate = Date.from(now.plusSeconds(3600));

        AssumeRoleResponse mockAssumeRoleResponse = Mockito.mock(AssumeRoleResponse.class);
        Credentials mockCredentials = Credentials.builder()
                                                .accessKeyId("AccessKeyId")
                                                .sessionToken("SessionToken")
                                                .secretAccessKey("SecretAccessKey")
                                                .expiration(expirationDate.toInstant())
                                                .build();
        Mockito.when(mockAssumeRoleResponse.credentials()).thenReturn(mockCredentials);
        Mockito.when(securityTokenService.assumeRole(Mockito.any(AssumeRoleRequest.class))).thenReturn(mockAssumeRoleResponse);

        TemporaryCredentials credentials = stsCredentialsHelper.getRetrievalCredentials(fileLocation, roleArn, expirationDate);

        ArgumentCaptor<AssumeRoleRequest> requestArgumentCaptor = ArgumentCaptor.forClass(AssumeRoleRequest.class);
        Mockito.verify(securityTokenService, Mockito.times(1)).assumeRole(requestArgumentCaptor.capture());
        AssumeRoleRequest assumeRoleRequest = requestArgumentCaptor.getValue();

        assertEquals(roleArn, assumeRoleRequest.roleArn());

        String policyJson = assumeRoleRequest.policy();
        log.info("Policy: {}", policyJson);

        String resource = getBucketResourceArn(policyJson, expectedArn);
        assertEquals(expectedArn, resource);
        assertEquals(mockCredentials.accessKeyId(), credentials.getAccessKeyId());
        assertEquals(mockCredentials.secretAccessKey(), credentials.getSecretAccessKey());
        assertEquals(mockCredentials.sessionToken(), credentials.getSessionToken());
    }

    /* Duplicate test
    @Test
    void shouldGetFolderCredentialsForOvdsTypes() throws JSONException {
        String expectedArn = "arn:aws:s3:::bucket";
        Instant now = Instant.now();
        Date expirationDate = Date.from(now.plusSeconds(3600));

        AssumeRoleResponse mockAssumeRoleResponse = Mockito.mock(AssumeRoleResponse.class);
        Credentials mockCredentials = Credentials.builder()
                                                .accessKeyId("AccessKeyId")
                                                .sessionToken("SessionToken")
                                                .secretAccessKey("SecretAccessKey")
                                                .expiration(expirationDate.toInstant())
                                                .build();
        Mockito.when(mockAssumeRoleResponse.credentials()).thenReturn(mockCredentials);
        Mockito.when(securityTokenService.assumeRole(Mockito.any(AssumeRoleRequest.class))).thenReturn(mockAssumeRoleResponse);

        TemporaryCredentials credentials = stsCredentialsHelper.getRetrievalCredentials(fileLocation, roleArn, expirationDate);
        ArgumentCaptor<AssumeRoleRequest> requestArgumentCaptor = ArgumentCaptor.forClass(AssumeRoleRequest.class);
        Mockito.verify(securityTokenService, Mockito.times(1)).assumeRole(requestArgumentCaptor.capture());
        AssumeRoleRequest assumeRoleRequest = requestArgumentCaptor.getValue();

        assertEquals(roleArn, assumeRoleRequest.roleArn());

        String policyJson = assumeRoleRequest.policy();
        log.info("Policy: {}", policyJson);

        String resource = getBucketResourceArn(policyJson, expectedArn);
        assertEquals(expectedArn, resource);
        assertEquals(mockCredentials.accessKeyId(), credentials.getAccessKeyId());
        assertEquals(mockCredentials.secretAccessKey(), credentials.getSecretAccessKey());
        assertEquals(mockCredentials.sessionToken(), credentials.getSessionToken());
    } */

    private String getBucketResourceArn(String policyJson, String arnToLookFor) throws JSONException {
        JSONObject policyObject = new JSONObject(policyJson);
        JSONArray statements = policyObject.getJSONArray("Statement");
        for (int i = 0; i < statements.length(); i++) {
            if ( (statements.getJSONObject(i).get("Resource") instanceof String) && statements.getJSONObject(i).getString("Resource").equals(arnToLookFor)) {
                return statements.getJSONObject(i).getString("Resource");
            }
        }
        return null;
    }
}
