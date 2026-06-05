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

package org.opengroup.osdu.file.stepdefs;

import java.util.concurrent.TimeUnit;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import static org.awaitility.Awaitility.await;
import org.opengroup.osdu.core.test.config.EnvLoader;

@UtilityClass
public class CommonUtility {

  private static final String FILE_ID = "file-integration-test-";

  public static String generateUniqueFileID() {
    return FILE_ID + RandomStringUtils.randomAlphanumeric(10).toLowerCase();
  }

  public static String generateFileIDExceedingLegthLimit() {
    return RandomStringUtils.randomAlphanumeric(1025).toLowerCase();
  }

  public static void customStaticWait_Timeout_Minutes() {
    int expiryTime = Integer.parseInt(getSignedURLExpiryTime());
    // adding an extra minute to avoid race condition between the actual wait and configured wait in Awaitility, so that actual wait time is always less.
    Awaitility.setDefaultTimeout(Duration.ONE_MINUTE.multiply(expiryTime).plus(Duration.ONE_MINUTE));
    await().pollDelay(expiryTime, TimeUnit.MINUTES).until(() -> true);
  }

  public static String getSignedURLExpiryTime() {
    String configured = EnvLoader.get("SIGNED_URL_EXPIRY_TIME_MINUTES");
    return StringUtils.isNotEmpty(configured) ? configured : "1";
  }
}
