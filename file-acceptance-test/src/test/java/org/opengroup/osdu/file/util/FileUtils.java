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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;

@Slf4j
@UtilityClass
public class FileUtils {

  public String readFromLocalFilePath(String filePath) throws IOException {
    InputStream inStream = FileUtils.class.getResourceAsStream(filePath);
    BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
    StringBuilder stringBuilder = new StringBuilder();

    String eachLine;
    while ((eachLine = br.readLine()) != null) {
      stringBuilder.append(eachLine);
    }

    return stringBuilder.toString();
  }

  public boolean isNullOrEmpty(final Collection<?> c) {
    return c == null || c.isEmpty();
  }

  public int uploadFileBySignedUrl(String endPoint, String inputFilePath) throws IOException {
    String fileContent = readFromLocalFilePath(inputFilePath);

    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpPut request = new HttpPut(endPoint);
      request.addHeader("Content-Type", "text/csv");
      request.addHeader("x-ms-blob-type", "BlockBlob");
      request.setEntity(new StringEntity(fileContent, ContentType.create("text/csv")));
      return client.execute(request, HttpResponse::getCode);
    }
  }

  public String readFileBySignedUrl(URL fileURL) throws IOException {
    URLConnection conn = fileURL.openConnection();
    try (InputStream inputStream = conn.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  public void readFileBySignedUrlAndWriteToLocalFile(String fileURL, String outputFilePath) {
    try {
      URL url = new URL(fileURL);
      URLConnection conn = url.openConnection();
      BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String inputLine;
      String fileName = System.getProperty("user.dir") + outputFilePath;
      File file = new File(fileName);
      if (!file.exists()) {
        file.createNewFile();
      }
      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);
      while ((inputLine = br.readLine()) != null) {
        bw.write(inputLine);
      }
      bw.close();
      br.close();
    } catch (MalformedURLException e) {
      log.error("Invalid signed URL: {}", fileURL, e);
    } catch (IOException e) {
      log.error("Failed to read signed URL and write to local file: {}", outputFilePath, e);
    }
  }
}
