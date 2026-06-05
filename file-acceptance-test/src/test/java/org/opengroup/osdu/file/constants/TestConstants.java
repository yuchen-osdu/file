/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.file.constants;

import lombok.experimental.UtilityClass;
import org.opengroup.osdu.core.test.config.EnvLoader;

/**
 * Environment-backed partition ids and payload placeholder tokens used in test JSON files.
 */
@UtilityClass
public class TestConstants {

  /** Placeholders substituted by {@link TestPayloadValues} when reading request payloads. */
  public static final String TENANT_NAME_PLACEHOLDER = "<tenant_name>";
  public static final String ACL_VIEWERS_GROUP = "<acl_viewers>";
  public static final String ACL_OWNERS_GROUP = "<acl_owners>";
  public static final String LEGAL_TAGS = "<legal_tags>";
  public static final String REGISTRY_ID = "<registry_id>";

  public static final String PRIVATE_TENANT1 = EnvLoader.get("PRIVATE_TENANT1");
}
