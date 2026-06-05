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
 * Resolved acceptance-test payload values loaded from environment at call time.
 */
@UtilityClass
public class TestPayloadValues {

  private static final String DEFAULT_FILE_KIND_SUFFIX = ":wks:dataset--File.Generic:1.0.0";

  public static String tenantName() {
    return firstConfigured("DATA_PARTITION_ID", "PRIVATE_TENANT1", "data");
  }

  public static String cloudDomain() {
    return firstConfigured("ENTITLEMENTS_DOMAIN", "example.com");
  }

  public static String viewersGroup() {
    return firstConfigured("ACL_VIEWERS", "data.default.viewers");
  }

  public static String ownersGroup() {
    return firstConfigured("ACL_OWNERS", "data.default.owners");
  }

  public static String legalTag() {
    return firstConfigured("LEGAL_TAG", "osdu-public-usa-dataset");
  }

  public static String fileKind() {
    return tenantName() + DEFAULT_FILE_KIND_SUFFIX;
  }

  public static String viewerPrincipal() {
    return viewersGroup() + "@" + tenantName() + "." + cloudDomain();
  }

  public static String ownerPrincipal() {
    return ownersGroup() + "@" + tenantName() + "." + cloudDomain();
  }

  private static String firstConfigured(String key, String fallback) {
    String value = EnvLoader.get(key);
    if (value != null && !value.isBlank()) {
      return value;
    }
    return fallback;
  }

  private static String firstConfigured(String primaryKey, String secondaryKey, String fallback) {
    String primary = EnvLoader.get(primaryKey);
    if (primary != null && !primary.isBlank()) {
      return primary;
    }
    String secondary = EnvLoader.get(secondaryKey);
    if (secondary != null && !secondary.isBlank()) {
      return secondary;
    }
    return fallback;
  }
}
