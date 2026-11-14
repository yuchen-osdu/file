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

import static org.junit.Assert.assertEquals;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpStatus;
import org.opengroup.osdu.core.test.auth.UserType;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.core.test.client.LegalTagsClient;
import org.opengroup.osdu.core.test.client.RetryConfiguration;
import org.opengroup.osdu.core.test.client.model.legal.LegalTag;
import org.opengroup.osdu.core.test.client.model.legal.LegalTagProperties;
import org.opengroup.osdu.core.test.config.TestInitializer;
import org.opengroup.osdu.file.constants.TestPayloadValues;

/**
 * Creates one legal tag for a Cucumber runner and deletes it when that runner finishes.
 * {@link LegalTagsClient} tracks the created name and {@code teardown()} removes it.
 */
@Slf4j
public class FileSuiteLegalTag {

  private static final UserType DEFAULT_USER = UserType.PRIVILEGED_USER;

  private static LegalTagsClient legalTagsClient;
  private static String suiteLegalTagName;

  @BeforeAll
  public static void createSuiteLegalTag() {
    TestInitializer initializer = TestInitializer.getSharedTestInitializer(
        FileScenarioSetup.SUPPORTED_USER_TYPES,
        FileScenarioSetup.SUPPORTED_SERVICE_TYPES,
        RetryConfiguration.none());
    legalTagsClient = new LegalTagsClient(initializer.getStringHttpClient(), DEFAULT_USER);

    suiteLegalTagName = TestPayloadValues.tenantName() + "-file-" + System.currentTimeMillis();
    // Create body must match Legal CreateLegalTag (name, description, properties only).
    // Gson omits nulls; sending isValid/id makes CIMPL legal return 400 "Failed to read request".
    LegalTagProperties properties = new LegalTagProperties(
        "A1234",
        "MyCompany",
        List.of("US"),
        "Public",
        "EAR99",
        "No Personal Data",
        "2099-12-25",
        "Public Domain Data");
    LegalTag legalTag = new LegalTag(
        null,
        null,
        suiteLegalTagName,
        properties,
        "Legal tag for file acceptance tests");
    HttpResponse<LegalTag> response = legalTagsClient.create(legalTag);
    assertEquals(
        "Failed to create legal tag " + suiteLegalTagName,
        HttpStatus.SC_CREATED,
        response.statusCode());
    TestPayloadValues.setSuiteLegalTag(suiteLegalTagName);
    log.info("Created legal tag '{}' for file acceptance tests", suiteLegalTagName);
  }

  @AfterAll
  public static void deleteSuiteLegalTag() {
    if (legalTagsClient != null) {
      legalTagsClient.teardown();
      log.info("Deleted legal tag '{}' after file acceptance tests", suiteLegalTagName);
    }
    legalTagsClient = null;
    suiteLegalTagName = null;
    TestPayloadValues.setSuiteLegalTag(null);
  }
}
