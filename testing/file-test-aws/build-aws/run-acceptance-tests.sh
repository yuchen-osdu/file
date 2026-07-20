#!/bin/bash
# Copyright © Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

# Store current directory
CUR_DIR=$(pwd)
SCRIPT_SOURCE_DIR=$(dirname "$0")
cd "$SCRIPT_SOURCE_DIR"

# Required variables for the tests
export INTEGRATION_TESTER_EMAIL="${ADMIN_USER}"

export STORAGE_HOST="${AWS_BASE_URL}/api/storage/v2"
export FILE_SERVICE_HOST="${AWS_BASE_URL}/api/file"
export PRIVATE_TENANT1=osdu
export PRIVATE_TENANT2=common
export SHARED_TENANT=shared
export TENANT_NAME="osdu"
export ENTITLEMENTS_DOMAIN="example.com"
export INTEGRATION_TESTER_EMAIL=${ADMIN_USER}
export LEGAL_TAG="osdu-public-usa-dataset"
export PRIVILEGED_USER_TOKEN=$(aws cognito-idp initiate-auth --region ${AWS_REGION} --auth-flow ${AWS_COGNITO_AUTH_FLOW} --client-id ${AWS_IDP_CLIENT_ID} --auth-parameters USERNAME=${ADMIN_USER},PASSWORD=${ADMIN_PASSWORD} --query AuthenticationResult.AccessToken --output text)

# Run the tests
mvn clean verify
TEST_EXIT_CODE=$?

# Return to original directory
cd "$CUR_DIR"

# Copy test reports if output directory is specified
if [ -n "$1" ]; then
  mkdir -p "$1/file-acceptance-test"
  cp -R "$SCRIPT_SOURCE_DIR/target/surefire-reports/"* "$1/file-acceptance-test"
fi

exit $TEST_EXIT_CODE
