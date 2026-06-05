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

import com.google.inject.Inject;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.test.auth.UserType;
import org.opengroup.osdu.core.test.client.FileClient;
import org.opengroup.osdu.core.test.client.RetryConfiguration;
import org.opengroup.osdu.core.test.client.StorageClient;
import org.opengroup.osdu.core.test.config.TestInitializer;
import org.opengroup.osdu.core.test.service.ServiceType;
import org.opengroup.osdu.file.stepdefs.model.FileScope;

/**
 * Cucumber lifecycle hooks for all runners (pre-integration, integration, post-integration).
 * Must not be extended by step-definition classes (Cucumber restriction).
 */
@Slf4j
@SuppressWarnings("unused")
public class FileScenarioSetup {

  static final List<UserType> SUPPORTED_USER_TYPES = List.of(UserType.PRIVILEGED_USER);
  static final List<ServiceType> SUPPORTED_SERVICE_TYPES =
      List.of(ServiceType.FILE_V2, ServiceType.STORAGE_V2);

  private static final UserType DEFAULT_USER = UserType.PRIVILEGED_USER;

  @Inject
  private FileScope context;

  @Before
  public void setup() {
    TestInitializer initializer =
        TestInitializer.getSharedTestInitializer(
            SUPPORTED_USER_TYPES, SUPPORTED_SERVICE_TYPES, RetryConfiguration.none());
    context.setupClients(
        new FileClient(initializer.getStringHttpClient(), DEFAULT_USER),
        new StorageClient(initializer.getStringHttpClient(), DEFAULT_USER));
  }

  @After
  public void teardown() {
    context.teardownClients();
  }
}
