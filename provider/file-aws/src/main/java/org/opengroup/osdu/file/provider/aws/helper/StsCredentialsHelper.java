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

package org.opengroup.osdu.file.provider.aws.helper;
import java.util.UUID;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.StsException;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamStatement;
import software.amazon.awssdk.policybuilder.iam.IamEffect;
import org.opengroup.osdu.core.aws.v2.sts.STSConfig;
import org.opengroup.osdu.file.exception.OsduBadRequestException;
import org.opengroup.osdu.file.provider.aws.auth.TemporaryCredentials;
import org.opengroup.osdu.file.provider.aws.config.ProviderConfigurationBag;
import org.opengroup.osdu.file.provider.aws.model.S3Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class StsCredentialsHelper {

    private final StsClient securityTokenService;

    @Autowired
    public StsCredentialsHelper(ProviderConfigurationBag providerConfigurationBag) {
        final STSConfig config = new STSConfig(providerConfigurationBag.amazonRegion);
        this.securityTokenService = config.amazonSTS();
    }

    public TemporaryCredentials getUploadCredentials(S3Location fileLocation, String roleArn, Date expiration) {
        IamPolicy policy = createUploadPolicy(fileLocation);

        return getCredentials(policy, roleArn, expiration);
    }

    public TemporaryCredentials getRetrievalCredentials(S3Location fileLocation, String roleArn, Date expiration) {
        IamPolicy policy = createRetrievalPolicy(fileLocation);

        return getCredentials(policy, roleArn, expiration);
    }

    public TemporaryCredentials getCredentials(IamPolicy policy, String roleArn, Date expiration) {
        Instant now = Instant.now();
        UUID uuid = UUID.randomUUID();
        String roleSessionName = uuid.toString();
        long duration = Math.round(((expiration.getTime() - now.toEpochMilli()) / 1_000.0) / 60.0) * 60;

        try {
            AssumeRoleRequest roleRequest = AssumeRoleRequest.builder()
                .roleArn(roleArn)
                .roleSessionName(roleSessionName)
                .durationSeconds((int) duration)
                .policy(policy.toJson())
                .build();

            AssumeRoleResponse response = securityTokenService.assumeRole(roleRequest);
            Credentials sessionCredentials = response.credentials();

            return TemporaryCredentials.builder()
                .accessKeyId(sessionCredentials.accessKeyId())
                .expiration(Date.from(sessionCredentials.expiration()))
                .secretAccessKey(sessionCredentials.secretAccessKey())
                .sessionToken(sessionCredentials.sessionToken())
                .build();

        } catch (StsException e) {
            throw new OsduBadRequestException("Failed to assume role: " + e.getMessage(), e);
        }
    }

    private IamPolicy createUploadPolicy(S3Location fileLocation) {
        String fileLocationKeyWithoutTrailingSlash = fileLocation.getKey().replaceFirst("/$", "");

        return IamPolicy.builder()
            .addStatement(allowUserToSeeBucketList())
            .addStatement(allowRootAndLocationListing(fileLocation, fileLocationKeyWithoutTrailingSlash))
            .addStatement(allowSubLocationListing(fileLocation, fileLocationKeyWithoutTrailingSlash))
            .addStatement(allowSubLocationGet(fileLocation, fileLocationKeyWithoutTrailingSlash))
            .addStatement(allowLocationGet(fileLocation, fileLocationKeyWithoutTrailingSlash))
            .addStatement(allowLocationPut(fileLocation, fileLocationKeyWithoutTrailingSlash))
            .addStatement(allowSubLocationPut(fileLocation, fileLocationKeyWithoutTrailingSlash))
            .build();
    }

    private IamPolicy createRetrievalPolicy(S3Location fileLocation) {
        String fileLocationKeyWithoutTrailingSlash = fileLocation.getKey().replaceFirst("/$", "");

        return IamPolicy.builder()
            .addStatement(allowUserToSeeBucketList())
            .addStatement(allowRootAndLocationListing(fileLocation, fileLocationKeyWithoutTrailingSlash))
            .addStatement(allowSubLocationListing(fileLocation, fileLocationKeyWithoutTrailingSlash))
            .addStatement(allowSubLocationGet(fileLocation, fileLocationKeyWithoutTrailingSlash))
            .addStatement(allowLocationGet(fileLocation, fileLocationKeyWithoutTrailingSlash))
            .build();

    }

    private IamStatement allowUserToSeeBucketList() {
        return IamStatement.builder()
            .effect(IamEffect.ALLOW)
            .addAction("s3:ListAllMyBuckets")
            .addAction("s3:GetBucketLocation")
            .addResource("arn:aws:s3:::*")
            .build();
    }

    private IamStatement allowRootAndLocationListing(S3Location fileLocation, String fileLocationKeyWithoutTrailingSlash) {
        return IamStatement.builder()
            .effect(IamEffect.ALLOW)
            .addAction("s3:ListBucket")
            .addAction("s3:ListBucketVersions")
            .addResource(String.format("arn:aws:s3:::%s", fileLocation.getBucket()))
            .addCondition(condition -> condition
                .operator("StringEquals")
                .key("s3:prefix")
                .value("")
                .value(fileLocationKeyWithoutTrailingSlash))
            .build();
    }

    private IamStatement allowSubLocationListing(S3Location fileLocation, String fileLocationKeyWithoutTrailingSlash) {
        return IamStatement.builder()
            .effect(IamEffect.ALLOW)
            .addAction("s3:ListBucket")
            .addAction("s3:ListBucketVersions")
            .addResource(String.format("arn:aws:s3:::%s", fileLocation.getBucket()))
            .addCondition(condition -> condition
                .operator("StringLike")
                .key("s3:prefix")
                .value(String.format("%s/*", fileLocationKeyWithoutTrailingSlash)))
            .build();
    }

    private IamStatement allowLocationGet(S3Location fileLocation, String fileLocationKeyWithoutTrailingSlash) {
        return IamStatement.builder()
            .effect(IamEffect.ALLOW)
            .addAction("s3:GetObject")
            .addAction("s3:GetObjectVersion")
            .addAction("s3:GetObjectAcl")
            .addResource(String.format("arn:aws:s3:::%s/%s", fileLocation.getBucket(), fileLocationKeyWithoutTrailingSlash))
            .build();
    }

    private IamStatement allowSubLocationGet(S3Location fileLocation, String fileLocationKeyWithoutTrailingSlash) {
        return IamStatement.builder()
            .effect(IamEffect.ALLOW)
            .addAction("s3:GetObject")
            .addAction("s3:GetObjectVersion")
            .addAction("s3:GetObjectAcl")
            .addResource(String.format("arn:aws:s3:::%s/%s/*", fileLocation.getBucket(), fileLocationKeyWithoutTrailingSlash))
            .build();
    }

    private IamStatement allowLocationPut(S3Location fileLocation, String fileLocationKeyWithoutTrailingSlash) {
        return IamStatement.builder()
            .effect(IamEffect.ALLOW)
            .addAction("s3:PutObject")
            .addAction("s3:ListBucketMultipartUploads")
            .addAction("s3:ListMultipartUploadParts")
            .addAction("s3:AbortMultipartUpload")
            .addResource(String.format("arn:aws:s3:::%s/%s", fileLocation.getBucket(), fileLocationKeyWithoutTrailingSlash))
            .build();
    }

    private IamStatement allowSubLocationPut(S3Location fileLocation, String fileLocationKeyWithoutTrailingSlash) {
        return IamStatement.builder()
            .effect(IamEffect.ALLOW)
            .addAction("s3:PutObject")
            .addAction("s3:ListBucketMultipartUploads")
            .addAction("s3:ListMultipartUploadParts")
            .addAction("s3:AbortMultipartUpload")
            .addResource(String.format("arn:aws:s3:::%s/%s/*", fileLocation.getBucket(), fileLocationKeyWithoutTrailingSlash))
            .build();
    }
}
