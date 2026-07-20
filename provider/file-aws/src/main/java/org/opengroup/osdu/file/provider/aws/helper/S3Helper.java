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

import static java.time.Instant.now;

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import org.opengroup.osdu.file.provider.aws.auth.TemporaryCredentials;
import org.opengroup.osdu.file.provider.aws.auth.TemporaryCredentialsProvider;
import org.opengroup.osdu.file.provider.aws.model.S3Location;


import java.net.URL;
import java.util.Date;
import java.util.Map;

public class S3Helper {

    private S3Helper() {
        //private constructor
    }

    /**
     * Generates a presigned URL for the S3 location
     */
    public static URL generatePresignedUrl(S3Location location,
                                           SdkHttpMethod httpMethod,
                                           Date expiration,
                                           TemporaryCredentials credentials) throws SdkException {
        return generatePresignedUrl(location, httpMethod, expiration, credentials, null);
    }
    
    /**
     * Generates a presigned URL for the S3 location with head overrides
     */
    public static URL generatePresignedUrl(S3Location location,
                                           SdkHttpMethod httpMethod,
                                           Date expiration,
                                           TemporaryCredentials credentials,
                                           AwsRequestOverrideConfiguration requestOverrideConfiguration) throws SdkException {
        try (S3Presigner presigner = generatePresignerWithCredentials(location.getBucket(), credentials)) {

            if (httpMethod == SdkHttpMethod.GET) {
                GetObjectRequest.Builder requestBuilder = GetObjectRequest.builder()
                    .bucket(location.getBucket())
                    .key(location.getKey());
                
                if (requestOverrideConfiguration != null) {
                    requestBuilder.overrideConfiguration(requestOverrideConfiguration);
                }
                
                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(requestBuilder.build())
                    .signatureDuration(java.time.Duration.between(now(), expiration.toInstant()))
                    .build();
                return presigner.presignGetObject(presignRequest).url();
            } else {
                PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(location.getBucket())
                    .key(location.getKey());
                
                if (requestOverrideConfiguration != null) {
                    requestBuilder.overrideConfiguration(requestOverrideConfiguration);
                }
                
                PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .putObjectRequest(requestBuilder.build())
                    .signatureDuration(java.time.Duration.between(now(), expiration.toInstant()))
                    .build();
                return presigner.presignPutObject(presignRequest).url();
            }
        }
    }

    public static boolean doesObjectExist(S3Location location, TemporaryCredentials credentials) {
        try (S3Client s3 = generateClientWithCredentials(location.getBucket(), credentials)) {
            try {
                s3.headObject(HeadObjectRequest.builder()
                                               .bucket(location.getBucket())
                                               .key(location.getKey())
                                               .build());
                return true;
            } catch (SdkException exception) {
                return false;
            }
        }
    }

    public static boolean doesObjectCollectionExist(S3Location location, TemporaryCredentials credentials) {
        try (S3Client s3 = generateClientWithCredentials(location.getBucket(), credentials)) {
            try {
                final ListObjectsV2Response response = s3.listObjectsV2(ListObjectsV2Request.builder()
                                                                                            .bucket(location.getBucket())
                                                                                            .prefix(location.getKey())
                                                                                            .build());
                return !response.contents().isEmpty();
            } catch (SdkException exception) {
                return false;
            }
        }
    }

    public static String getBucketRegion(String bucket, TemporaryCredentials credentials) {
        final S3Client s3Client = S3Client.builder()
            .credentialsProvider(new TemporaryCredentialsProvider(credentials))
            .build();
        final GetBucketLocationResponse response = s3Client.getBucketLocation(
            GetBucketLocationRequest.builder().bucket(bucket).build());
        String regionStr = response.locationConstraintAsString();

        // Handle special case for us-east-1
        if (regionStr == null || regionStr.isEmpty()) {
            regionStr = "us-east-1"; // Default region when constraint is null
        }
        final Region region = Region.of(regionStr);
        return region.id();
    }

    public static S3Client createS3Client(String bucket, TemporaryCredentials credentials) {
        return generateClientWithCredentials(bucket, credentials);
    }

    public static ResponseInputStream<GetObjectResponse> getObject(S3Location location, TemporaryCredentials credentials) {
        try (S3Client s3Client = createS3Client(location.getBucket(), credentials)) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                                .bucket(location.getBucket())
                                                                .key(location.getKey())
                                                                .build();
            return s3Client.getObject(getObjectRequest);
        }
    }

    private static S3Client generateClientWithCredentials(String bucket, TemporaryCredentials credentials) {
        final String region = getBucketRegion(bucket, credentials);

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(new TemporaryCredentialsProvider(credentials))
                .build();
    }
    
    private static S3Presigner generatePresignerWithCredentials(String bucket, TemporaryCredentials credentials) {
        final String region = getBucketRegion(bucket, credentials);

        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(new TemporaryCredentialsProvider(credentials))
                .build();
    }
}
