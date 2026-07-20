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

package org.opengroup.osdu.file.util;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.opengroup.osdu.core.common.dms.model.RetrievalInstructionsRequest;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.core.test.client.model.file.FileMetadataAncestry;
import org.opengroup.osdu.core.test.client.model.file.FileMetadataRequest;
import org.opengroup.osdu.file.constants.TestConstants;
import org.opengroup.osdu.file.constants.TestPayloadValues;

@UtilityClass
public class PayloadFormatUtil {

  private static final Gson GSON = new Gson();

  public static FileMetadataRequest readMetadataPayload(String json) {
    FileMetadataRequest metadata = GSON.fromJson(json, FileMetadataRequest.class);
    return applyPlaceholders(metadata);
  }

  public static String toRequestJson(FileMetadataRequest metadata) {
    return GSON.toJson(metadata);
  }

  public static FileMetadataRequest applyPlaceholders(FileMetadataRequest metadata) {
    resolveKind(metadata);
    if (metadata.getAcl() != null) {
      resolveAcl(metadata.getAcl());
    }
    if (metadata.getLegal() != null) {
      resolveLegal(metadata.getLegal());
    }
    return metadata;
  }

  public static void updateFilePath(FileMetadataRequest metadata, String filePath) {
    if (metadata.getData() == null
        || !metadata.getData().has("DatasetProperties")
        || !metadata.getData().getAsJsonObject("DatasetProperties").has("FileSourceInfo")) {
      return;
    }
    metadata.getData()
        .getAsJsonObject("DatasetProperties")
        .getAsJsonObject("FileSourceInfo")
        .addProperty("FileSource", filePath);
  }

  public static FileMetadataRequest removeAncestry(FileMetadataRequest metadata) {
    metadata.setAncestry(null);
    return metadata;
  }

  public static FileMetadataRequest replaceAncestryWithNewValue(FileMetadataRequest metadata, String ancestryVal) {
    FileMetadataAncestry ancestry = new FileMetadataAncestry();
    ancestry.setParents(List.of(ancestryVal));
    metadata.setAncestry(ancestry);
    return metadata;
  }

  public static RetrievalInstructionsRequest readRetrievalInstructionsRequest(String json, String recordId) {
    RetrievalInstructionsRequest request = GSON.fromJson(json, RetrievalInstructionsRequest.class);
    return applyDatasetPlaceholders(request, recordId);
  }

  public static RetrievalInstructionsRequest applyDatasetPlaceholders(
      RetrievalInstructionsRequest request, String recordId) {
    if (request.getDatasetRegistryIds() == null) {
      return request;
    }
    request.setDatasetRegistryIds(
        request.getDatasetRegistryIds().stream()
            .map(id -> isRegistryIdPlaceholder(id) ? recordId : id)
            .collect(Collectors.toList()));
    return request;
  }

  private static void resolveKind(FileMetadataRequest metadata) {
    if (metadata.getKind() != null && containsTenantPlaceholder(metadata.getKind())) {
      metadata.setKind(TestPayloadValues.fileKind());
    }
  }

  private static void resolveAcl(Acl acl) {
    if (acl.getViewers() != null) {
      acl.setViewers(resolveAclEntries(acl.getViewers(), true));
    }
    if (acl.getOwners() != null) {
      acl.setOwners(resolveAclEntries(acl.getOwners(), false));
    }
  }

  private static String[] resolveAclEntries(String[] entries, boolean viewers) {
    return Arrays.stream(entries)
        .map(entry -> isAclEntryPlaceholder(entry, viewers)
            ? viewers ? TestPayloadValues.viewerPrincipal() : TestPayloadValues.ownerPrincipal()
            : entry)
        .toArray(String[]::new);
  }

  private static void resolveLegal(Legal legal) {
    if (legal.getLegaltags() == null) {
      return;
    }
    Set<String> resolvedTags = legal.getLegaltags().stream()
        .map(tag -> isLegalTagPlaceholder(tag) ? TestPayloadValues.legalTag() : tag)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    legal.setLegaltags(resolvedTags);
  }

  private static boolean containsTenantPlaceholder(String value) {
    return value.contains(TestConstants.TENANT_NAME_PLACEHOLDER);
  }

  private static boolean isAclEntryPlaceholder(String entry, boolean viewers) {
    if (entry == null) {
      return false;
    }
    String groupPlaceholder = viewers
        ? TestConstants.ACL_VIEWERS_GROUP
        : TestConstants.ACL_OWNERS_GROUP;
    return entry.contains(groupPlaceholder);
  }

  private static boolean isLegalTagPlaceholder(String tag) {
    return TestConstants.LEGAL_TAGS.equals(tag);
  }

  private static boolean isRegistryIdPlaceholder(String registryId) {
    return TestConstants.REGISTRY_ID.equals(registryId);
  }
}
