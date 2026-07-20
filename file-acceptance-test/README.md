### Running E2E Tests

You will need the following environment variables.

| name                             | value                              | description                   | sensitive? | source | required |
|----------------------------------|------------------------------------|-------------------------------|------------|--------|----------|
| `FILE_SERVICE_HOST`              | ex `http://localhost:8080/api/file` | Endpoint of File service host                                  | no         | -      | yes      |
| `DATA_PARTITION_ID`              | ex `opendes`                        | Default data partition id used by `os-core-test` clients       | no         | -      | yes      |
| `PRIVATE_TENANT1`                | ex `opendes`                        | Partition used by explicit File API header override scenarios   | no         | -      | yes      |
| `ACL_OWNERS`                     | ex `data.default.owners`            | ACL owner group placeholder replacement                         | no         | -      | no       |
| `ACL_VIEWERS`                    | ex `data.default.viewers`           | ACL viewer group placeholder replacement                        | no         | -      | no       |
| `ENTITLEMENTS_DOMAIN`            | ex `group`                          | Domain used to build ACL principal placeholders                 | no         | -      | no       |
| `LEGAL_TAG`                      | ex `opendes`                        | Legal tag placeholder replacement                               | no         | -      | no       |
| `SIGNED_URL_EXPIRY_TIME_MINUTES` | ex `15`                             | Wait time in signed URL expiration scenarios                    | no         | -      | no       |

Notes:
- `PRIVATE_TENANT2`, `SHARED_TENANT`, and `TENANT_NAME` are no longer used by the current acceptance test code.
- Tenant columns in some legacy feature tables are retained for compatibility, but partition selection is now driven by `DATA_PARTITION_ID` and explicit header overrides.

Authentication can be provided as OIDC config:

| name                                            | value                                   | description                                 | sensitive? | source |
|-------------------------------------------------|-----------------------------------------|---------------------------------------------|------------|--------|
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_ID`     | `********`                              | PRIVILEGED_USER Client Id                   | yes        | -      |
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_SECRET` | `********`                              | PRIVILEGED_USER Client secret               | yes        | -      |
| `TEST_OPENID_PROVIDER_URL`                      | `https://keycloak.com/auth/realms/osdu` | OpenID provider url                         | yes        | -      |
| `PRIVILEGED_USER_OPENID_PROVIDER_SCOPE`         | ex `api://my-app/.default`              | OAuth2 scope (optional, defaults to openid) | no         | -      |

Or tokens can be used directly from env variables:

| name                    | value      | description           | sensitive? | source |
|-------------------------|------------|-----------------------|------------|--------|
| `PRIVILEGED_USER_TOKEN` | `********` | PRIVILEGED_USER Token | yes        | -      |


Execute following command to build code and run all the integration tests:

 ```bash
 # Note: this assumes that the environment variables for integration tests as outlined
 #       above are already exported in your environment.
 # build + install integration test core
 $ (cd file-acceptance-test && mvn clean verify)
 ```

Integration tests run in three phases via `maven-failsafe-plugin`:

1. **Pre-integration** (`PreIntegrationTestsRunner`, `@Startup`) — validates the File service `/info` endpoint before main scenarios
2. **Integration** (`FileTestsRunner`, `FileDmsTestsRunner`, `@File` / `@FileDMS`) — main acceptance scenarios; per-scenario cleanup runs via `FileScenarioHooks`
3. **Post-integration** (`TearDownTestsRunner`, `@TearDown`) — final tear-down phase after all scenarios complete

## License

Copyright © Google LLC

Copyright © EPAM Systems

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
