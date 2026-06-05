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

import java.io.IOException;
import lombok.experimental.UtilityClass;
import org.opengroup.osdu.core.common.model.file.FileListRequest;
import org.opengroup.osdu.core.test.util.FileApiGson;
import org.opengroup.osdu.core.test.util.ResponseUtil;

/**
 * Reads File API request payloads using the same Gson configuration as {@link FileApiGson}.
 */
@UtilityClass
public class FileApiJson {

  public static FileListRequest readFileListRequest(String resourcePath) {
    try {
      String json = FileUtils.readFromLocalFilePath(resourcePath);
      return ResponseUtil.fromJson(FileApiGson.INSTANCE, json, FileListRequest.class);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read FileListRequest payload: " + resourcePath, exception);
    }
  }
}
