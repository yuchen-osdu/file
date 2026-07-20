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

package org.opengroup.osdu.file.provider.aws.auth;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.util.Date;

public class TemporaryCredentialsProvider implements AwsCredentialsProvider {

    private final TemporaryCredentials temporaryCredentials;

    public TemporaryCredentialsProvider(TemporaryCredentials temporaryCredentials) {
        this.temporaryCredentials = temporaryCredentials;
    }

    @Override
    public AwsCredentials resolveCredentials() {
            // Check if credentials are expired
            if (temporaryCredentials.getExpiration() != null &&
                temporaryCredentials.getExpiration().before(new Date())) {
                throw SdkClientException.create("Credentials have expired");
            }

            return AwsSessionCredentials.create(
                temporaryCredentials.getAccessKeyId(),
                temporaryCredentials.getSecretAccessKey(),
                temporaryCredentials.getSessionToken()
            );
    }
}
