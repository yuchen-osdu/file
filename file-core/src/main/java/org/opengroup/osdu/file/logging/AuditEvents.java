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

import static java.lang.String.format;

import com.google.common.base.Strings;
import java.util.List;
import org.opengroup.osdu.core.common.logging.audit.AuditAction;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload.AuditPayloadBuilder;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;

public class AuditEvents {

  private static final String READ_FILE_LOCATION_ACTION_ID = "FL001";
  private static final String READ_FILE_LOCATION_MESSAGE = "Read file location";

  private static final String READ_FILE_LIST_ACTION_ID = "FL002";
  private static final String READ_FILE_LIST_MESSAGE = "Read file list";

  private static final String CREATE_LOCATION_ACTION_ID = "FL003";
  private static final String CREATE_LOCATION_MESSAGE = "Create location";


  private final String user;
  private final String userIpAddress;
  private final String userAgent;
  private final String userAuthorizedGroupName;

  public AuditEvents(String user, String userIpAddress, String userAgent, String userAuthorizedGroupName) {
    this.user = requireNonEmpty(user, "User not provided for audit events.");
    this.userIpAddress = requireNonEmpty(userIpAddress, "User's IP address is not provided for audit events.");
    this.userAgent = requireNonEmpty(userAgent, "User's agent is not provided for audit events.");
    this.userAuthorizedGroupName = requireNonEmpty(userAuthorizedGroupName, "User's authorized group name is not provided for audit events.");
  }

  /**
   * Creates an AuditPayload builder pre-populated with common audit fields.
   */
  private AuditPayloadBuilder createAuditPayloadBuilder(
      List<String> requiredGroupsForAction, AuditStatus status, String actionId) {
    return AuditPayload.builder()
        .status(status)
        .user(this.user)
        .actionId(actionId)
        .requiredGroupsForAction(requiredGroupsForAction)
        .userIpAddress(this.userIpAddress)
        .userAgent(this.userAgent)
        .userAuthorizedGroupName(this.userAuthorizedGroupName);
  }

  public AuditPayload getReadFileLocationEvent(AuditStatus status, List<String> resources) {
    return createAuditPayloadBuilder(AuditOperation.READ_FILE_LOCATION.getRequiredGroups(), status, READ_FILE_LOCATION_ACTION_ID)
        .action(AuditAction.READ)
        .message(getStatusMessage(status, READ_FILE_LOCATION_MESSAGE))
        .resources(resources)
        .build();
  }

  public AuditPayload getReadFileListEvent(AuditStatus status, List<String> resources) {
    return createAuditPayloadBuilder(AuditOperation.READ_FILE_LIST.getRequiredGroups(), status, READ_FILE_LIST_ACTION_ID)
        .action(AuditAction.READ)
        .message(getStatusMessage(status, READ_FILE_LIST_MESSAGE))
        .resources(resources)
        .build();
  }

  public AuditPayload getCreateLocationEvent(AuditStatus status, List<String> resources) {
    return createAuditPayloadBuilder(AuditOperation.CREATE_LOCATION.getRequiredGroups(), status, CREATE_LOCATION_ACTION_ID)
        .action(AuditAction.CREATE)
        .message(getStatusMessage(status, CREATE_LOCATION_MESSAGE))
        .resources(resources)
        .build();
  }

  private String getStatusMessage(AuditStatus status, String message) {
    return format("%s - %s", message, status.name().toLowerCase());
  }

  private static String requireNonEmpty(String value, String message) {
    if (Strings.isNullOrEmpty(value)) {
        throw new IllegalArgumentException(message);
    }
    return value;
  }
}
