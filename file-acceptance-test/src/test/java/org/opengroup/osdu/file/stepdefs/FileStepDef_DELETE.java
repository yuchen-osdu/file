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

import com.google.inject.Inject;
import io.cucumber.java8.En;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.file.stepdefs.model.FileScope;
import org.opengroup.osdu.file.util.FileClientExceptionSupport;

@Slf4j
public class FileStepDef_DELETE implements En {

  @Inject
  private FileScope context;

  public FileStepDef_DELETE() {
    Given("I hit File service Delete metadata endpoint with a valid Id", () -> {
      String id = this.context.getId();
      log.info("Id to be deleted : {}", id);
      var coreResp = context.getFileClient().deleteMetadata(id);
      this.context.setDeleteResponse(coreResp);
      log.info("Delete resp - {}", coreResp.statusCode());
      assertEquals("204", String.valueOf(coreResp.statusCode()));
    });

    Given("I hit File service Delete metadata endpoint with a invalid Id", () -> {
      String id = this.context.getId();
      FileClientExceptionSupport.invokeExpectingFailure(
          () -> context.getFileClient().deleteMetadata(id), context);
      log.info("resp - {}", context.getLastStatusCode());
      assertEquals("404", String.valueOf(context.getLastStatusCode()));
    });

    Then("Delete service should respond back with {string}", (String reponseStatusCode) -> {
      assertEquals(reponseStatusCode, String.valueOf(this.context.getLastStatusCode()));
    });
  }
}
