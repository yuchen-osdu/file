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

import java.util.Collections;
import java.util.List;
import org.opengroup.osdu.file.constant.FileServiceRole;

/**
 * Defines the required authorization groups for each audited operation.
 * Maps to the same groups used in @PreAuthorize annotations on API endpoints.
 */
public enum AuditOperation {

  READ_FILE_LOCATION(Collections.singletonList(FileServiceRole.EDITORS)),
  READ_FILE_LIST(Collections.singletonList(FileServiceRole.EDITORS)),
  CREATE_LOCATION(Collections.singletonList(FileServiceRole.EDITORS));

  private final List<String> requiredGroups;

  AuditOperation(List<String> requiredGroups) {
    this.requiredGroups = Collections.unmodifiableList(requiredGroups);
  }

  /**
   * Get the list of groups that are authorized to perform this operation.
   * @return Unmodifiable list of group names
   */
  public List<String> getRequiredGroups() {
    return requiredGroups;
  }
}
