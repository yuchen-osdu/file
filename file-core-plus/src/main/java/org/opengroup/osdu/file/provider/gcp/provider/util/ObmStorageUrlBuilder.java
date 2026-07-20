/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
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

package org.opengroup.osdu.file.provider.gcp.provider.util;

public final class ObmStorageUrlBuilder {

  private ObmStorageUrlBuilder() {
  }

  public static String buildUnsignedUrl(String transferProtocol, String bucketName, String filePath) {
    return normalizeTransferProtocol(transferProtocol) + bucketName + normalizePath(filePath);
  }

  private static String normalizeTransferProtocol(String transferProtocol) {
    if (transferProtocol.endsWith("/")) {
      return transferProtocol;
    }
    return transferProtocol + "/";
  }

  private static String normalizePath(String filePath) {
    if (filePath.isEmpty()) {
      return "/";
    }
    if (filePath.startsWith("/")) {
      return filePath;
    }
    return "/" + filePath;
  }
}
