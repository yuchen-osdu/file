/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.file.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.file.constant.FileServiceRole;

public class AuditOperationTest {

  @Test
  public void should_haveCorrectRolesForReadFileLocation() {
    List<String> roles = AuditOperation.READ_FILE_LOCATION.getRequiredGroups();
    assertEquals(1, roles.size());
    assertTrue(roles.containsAll(Collections.singletonList(FileServiceRole.EDITORS)));
  }

  @Test
  public void should_haveCorrectRolesForReadFileList() {
    List<String> roles = AuditOperation.READ_FILE_LIST.getRequiredGroups();
    assertEquals(1, roles.size());
    assertTrue(roles.containsAll(Collections.singletonList(FileServiceRole.EDITORS)));
  }

  @Test
  public void should_haveCorrectRolesForCreateLocation() {
    List<String> roles = AuditOperation.CREATE_LOCATION.getRequiredGroups();
    assertEquals(1, roles.size());
    assertTrue(roles.containsAll(Collections.singletonList(FileServiceRole.EDITORS)));
  }

  @Test
  public void should_returnUnmodifiableList() {
    List<String> roles = AuditOperation.CREATE_LOCATION.getRequiredGroups();
    assertNotNull(roles);
    assertThrows(UnsupportedOperationException.class, () -> roles.add("should-fail"));
  }

  @Test
  public void should_haveAllOperationsDefined() {
    assertEquals(3, AuditOperation.values().length);
  }
}
