/*
 * Copyright 2020  Microsoft Corporation
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

package org.opengroup.osdu.file.provider.azure.service;

import com.azure.cosmos.implementation.InternalServerErrorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.file.provider.azure.TestUtils;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
public class ServiceHelperTests {

  private ServiceHelper serviceHelper;

  @BeforeEach
  void init() {
    initMocks(this);
    serviceHelper = new ServiceHelper();
  }

  @Test
  void getContainerNameFromStandardEndpointAbsoluteFilePath_ShouldReturnContainerName() {
    String expectedContainerName = TestUtils.STAGING_CONTAINER_NAME;
    String actualContainerName = serviceHelper.getContainerNameFromAbsoluteFilePath(TestUtils.STANDARD_ENDPOINT_ABSOLUTE_FILE_PATH);
    Assertions.assertEquals(expectedContainerName, actualContainerName);
  }

  @Test
  void getContainerNameFromDNSEndpointAbsoluteFilePath_ShouldReturnContainerName() {
    String expectedContainerName = TestUtils.STAGING_CONTAINER_NAME;
    String actualContainerName = serviceHelper.getContainerNameFromAbsoluteFilePath(TestUtils.DNS_ENDPOINT_ABSOLUTE_FILE_PATH);
    Assertions.assertEquals(expectedContainerName, actualContainerName);
  }

  @Test
  void getContainerNameFromAbsoluteFilePath_ShouldThrow_IfNotAbleToParseContainerNameFromFilePath() {
    Throwable thrown = catchThrowable(() -> serviceHelper.getContainerNameFromAbsoluteFilePath(TestUtils.RELATIVE_FILE_PATH));
    then(thrown)
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining(String.format("Could not parse {%s} from file path provided {%s}",
            TestUtils.CONTAINER_NAME, TestUtils.RELATIVE_FILE_PATH));
  }

  @Test
  void getRelativeFilePathFromStandardEndpointAbsoluteFilePath_ShouldReturnContainerName() {
    String expectedFilePath = TestUtils.RELATIVE_FILE_PATH;
    String actualActualFilePath = serviceHelper.getRelativeFilePathFromAbsoluteFilePath(TestUtils.STANDARD_ENDPOINT_ABSOLUTE_FILE_PATH);
    Assertions.assertEquals(expectedFilePath, actualActualFilePath);
  }

  @Test
  void getRelativeFilePathFromDNSEndpointAbsoluteFilePath_ShouldReturnContainerName() {
    String expectedFilePath = TestUtils.RELATIVE_FILE_PATH;
    String actualActualFilePath = serviceHelper.getRelativeFilePathFromAbsoluteFilePath(TestUtils.DNS_ENDPOINT_ABSOLUTE_FILE_PATH);
    Assertions.assertEquals(expectedFilePath, actualActualFilePath);
  }

  @Test
  void getRelativeFilePathFromAbsoluteFilePath_ShouldThrow_IfNotAbleToParseContainerNameFromFilePath() {
    Throwable thrown = catchThrowable(() -> serviceHelper.getRelativeFilePathFromAbsoluteFilePath(TestUtils.RELATIVE_FILE_PATH));
    then(thrown)
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining(String.format("Could not parse {%s} from file path provided {%s}",
            TestUtils.FILE_PATH ,TestUtils.RELATIVE_FILE_PATH));
  }

  @Test
  void getFileSystemNameFromStandardEndpointAbsoluteFilePath_ShouldReturnFileSystemName() {
    String expectedContainerName = TestUtils.STAGING_FILE_SYSTEM_NAME;
    String actualContainerName = serviceHelper.getFileSystemNameFromAbsoluteDirectoryPath(TestUtils.STANDARD_ENDPOINT_ABSOLUTE_DIRECTORY_PATH);
    Assertions.assertEquals(expectedContainerName, actualContainerName);
  }

  @Test
  void getFileSystemNameFromDNSEndpointAbsoluteFilePath_ShouldReturnFileSystemName() {
    String expectedContainerName = TestUtils.STAGING_FILE_SYSTEM_NAME;
    String actualContainerName = serviceHelper.getFileSystemNameFromAbsoluteDirectoryPath(TestUtils.DNS_ENDPOINT_ABSOLUTE_DIRECTORY_PATH);
    Assertions.assertEquals(expectedContainerName, actualContainerName);
  }

  @Test
  void getFileSystemFromAbsoluteFilePath_ShouldThrow_IfNotAbleToParseContainerNameFromFilePath() {
    Throwable thrown = catchThrowable(() -> serviceHelper.getFileSystemNameFromAbsoluteDirectoryPath(TestUtils.RELATIVE_DIRECTORY_PATH));
    then(thrown)
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining(String.format("Could not parse {%s} from file path provided {%s}",
            TestUtils.FILE_SYSTEM_NAME, TestUtils.RELATIVE_DIRECTORY_PATH));
  }

  @Test
  void getRelativeDirectoryPathFromStandardEndpointAbsoluteFilePath_ShouldReturnContainerName() {
    String expectedFilePath = TestUtils.DIRECTORY_NAME;
    String actualActualFilePath = serviceHelper.getRelativeDirectoryPathFromAbsoluteDirectoryPath(TestUtils.STANDARD_ENDPOINT_ABSOLUTE_DIRECTORY_PATH);
    Assertions.assertEquals(expectedFilePath, actualActualFilePath);
  }

  @Test
  void getRelativeDirectoryPathFromDNSEndpointAbsoluteFilePath_ShouldReturnContainerName() {
    String expectedFilePath = TestUtils.DIRECTORY_NAME;
    String actualActualFilePath = serviceHelper.getRelativeDirectoryPathFromAbsoluteDirectoryPath(TestUtils.DNS_ENDPOINT_ABSOLUTE_DIRECTORY_PATH);
    Assertions.assertEquals(expectedFilePath, actualActualFilePath);
  }

  @Test
  void getRelativeDirectoryPathFromAbsoluteFilePath_ShouldThrow_IfNotAbleToParseContainerNameFromFilePath() {
    Throwable thrown = catchThrowable(() -> serviceHelper.getRelativeDirectoryPathFromAbsoluteDirectoryPath(TestUtils.DIRECTORY_NAME));
    then(thrown)
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining(String.format("Could not parse {%s} from file path provided {%s}",
            TestUtils.DIRECTORY_PATH , TestUtils.DIRECTORY_NAME));
  }

   @Test
  void getContainerNameFromDNSEndpoint_SingleDigitZone9_ShouldReturnContainerName() {
    String url = "https://datatest.z9.blob.storage.azure.net/file-persistent-area/osdu-user/0000-000/0000-000";
    String expectedContainerName = "file-persistent-area";
    String actualContainerName = serviceHelper.getContainerNameFromAbsoluteFilePath(url);
    Assertions.assertEquals(expectedContainerName, actualContainerName);
  }

  @Test
  void getRelativeFilePathFromDNSEndpoint_SingleDigitZone9_ShouldReturnFilePath() {
    String url = "https://datatest.z9.blob.storage.azure.net/file-persistent-area/osdu-user/0000-000/0000-000";
    String expectedFilePath = "osdu-user/0000-000/0000-000";
    String actualFilePath = serviceHelper.getRelativeFilePathFromAbsoluteFilePath(url);
    Assertions.assertEquals(expectedFilePath, actualFilePath);
  }

  @Test
  void getContainerNameFromDNSEndpoint_SingleDigitZones_ShouldReturnContainerName() {
    // Test single digit zones z0 through z9
    String[] singleDigitZones = {"z0", "z1", "z2", "z3", "z4", "z5", "z6", "z7", "z8", "z9"};
    String expectedContainerName = TestUtils.STAGING_CONTAINER_NAME;

    for (String zone : singleDigitZones) {
      String url = "https://" + TestUtils.STORAGE_NAME + "." + zone + ".blob.storage.azure.net/"
          + TestUtils.STAGING_CONTAINER_NAME + "/" + TestUtils.RELATIVE_FILE_PATH;
      String actualContainerName = serviceHelper.getContainerNameFromAbsoluteFilePath(url);
      Assertions.assertEquals(expectedContainerName, actualContainerName,
          "Failed for zone: " + zone);
    }
  }

  @Test
  void getContainerNameFromDNSEndpoint_DoubleDigitZonesWithLeadingZero_ShouldReturnContainerName() {
    // Test double digit zones z00 through z09
    String[] doubleDigitZones = {"z00", "z01", "z02", "z03", "z04", "z05", "z06", "z07", "z08", "z09"};
    String expectedContainerName = TestUtils.STAGING_CONTAINER_NAME;

    for (String zone : doubleDigitZones) {
      String url = "https://" + TestUtils.STORAGE_NAME + "." + zone + ".blob.storage.azure.net/"
          + TestUtils.STAGING_CONTAINER_NAME + "/" + TestUtils.RELATIVE_FILE_PATH;
      String actualContainerName = serviceHelper.getContainerNameFromAbsoluteFilePath(url);
      Assertions.assertEquals(expectedContainerName, actualContainerName,
          "Failed for zone: " + zone);
    }
  }

  @Test
  void getContainerNameFromDNSEndpoint_DoubleDigitZones_ShouldReturnContainerName() {
    // Test double digit zones z10 through z50
    String[] doubleDigitZones = {"z10", "z20", "z30", "z40", "z50", "z99"};
    String expectedContainerName = TestUtils.STAGING_CONTAINER_NAME;

    for (String zone : doubleDigitZones) {
      String url = "https://" + TestUtils.STORAGE_NAME + "." + zone + ".blob.storage.azure.net/"
          + TestUtils.STAGING_CONTAINER_NAME + "/" + TestUtils.RELATIVE_FILE_PATH;
      String actualContainerName = serviceHelper.getContainerNameFromAbsoluteFilePath(url);
      Assertions.assertEquals(expectedContainerName, actualContainerName,
          "Failed for zone: " + zone);
    }
  }

  @Test
  void getFileSystemNameFromDNSEndpoint_SingleDigitZones_ShouldReturnFileSystemName() {
    // Test single digit zones z0 through z9 for DFS endpoints
    String[] singleDigitZones = {"z0", "z1", "z2", "z3", "z4", "z5", "z6", "z7", "z8", "z9"};
    String expectedFileSystemName = TestUtils.STAGING_FILE_SYSTEM_NAME;

    for (String zone : singleDigitZones) {
      String url = "https://" + TestUtils.STORAGE_NAME + "." + zone + ".dfs.storage.azure.net/"
          + TestUtils.STAGING_FILE_SYSTEM_NAME + "/" + TestUtils.DIRECTORY_NAME;
      String actualFileSystemName = serviceHelper.getFileSystemNameFromAbsoluteDirectoryPath(url);
      Assertions.assertEquals(expectedFileSystemName, actualFileSystemName,
          "Failed for zone: " + zone);
    }
  }

  @Test
  void getFileSystemNameFromDNSEndpoint_DoubleDigitZonesWithLeadingZero_ShouldReturnFileSystemName() {
    // Test double digit zones z00 through z09 for DFS endpoints
    String[] doubleDigitZones = {"z00", "z01", "z02", "z03", "z04", "z05", "z06", "z07", "z08", "z09"};
    String expectedFileSystemName = TestUtils.STAGING_FILE_SYSTEM_NAME;

    for (String zone : doubleDigitZones) {
      String url = "https://" + TestUtils.STORAGE_NAME + "." + zone + ".dfs.storage.azure.net/"
          + TestUtils.STAGING_FILE_SYSTEM_NAME + "/" + TestUtils.DIRECTORY_NAME;
      String actualFileSystemName = serviceHelper.getFileSystemNameFromAbsoluteDirectoryPath(url);
      Assertions.assertEquals(expectedFileSystemName, actualFileSystemName,
          "Failed for zone: " + zone);
    }
  }

  @Test
  void getFileSystemNameFromDNSEndpoint_DoubleDigitZones_ShouldReturnFileSystemName() {
    // Test double digit zones z10 through z50 for DFS endpoints
    String[] doubleDigitZones = {"z10", "z20", "z30", "z40", "z50", "z99"};
    String expectedFileSystemName = TestUtils.STAGING_FILE_SYSTEM_NAME;

    for (String zone : doubleDigitZones) {
      String url = "https://" + TestUtils.STORAGE_NAME + "." + zone + ".dfs.storage.azure.net/"
          + TestUtils.STAGING_FILE_SYSTEM_NAME + "/" + TestUtils.DIRECTORY_NAME;
      String actualFileSystemName = serviceHelper.getFileSystemNameFromAbsoluteDirectoryPath(url);
      Assertions.assertEquals(expectedFileSystemName, actualFileSystemName,
          "Failed for zone: " + zone);
    }
  }

  @Test
  void getContainerNameFromDNSEndpoint_TripleDigitZone_ShouldThrow() {
    // z120 has 3 digits which should NOT match (regex allows only 1-2 digits)
    String url = "https://" + TestUtils.STORAGE_NAME + ".z120.blob.storage.azure.net/"
        + TestUtils.STAGING_CONTAINER_NAME + "/" + TestUtils.RELATIVE_FILE_PATH;
    Throwable thrown = catchThrowable(() -> serviceHelper.getContainerNameFromAbsoluteFilePath(url));
    then(thrown).isInstanceOf(InternalServerErrorException.class);
  }

  @Test
  void getContainerNameFromStandardEndpoint_NoZoneNumber_ShouldReturnContainerName() {
    // Standard endpoint format: blob.core.windows.net (no zone number)
    String url = "https://" + TestUtils.STORAGE_NAME + ".blob.core.windows.net/"
        + TestUtils.STAGING_CONTAINER_NAME + "/" + TestUtils.RELATIVE_FILE_PATH;
    String expectedContainerName = TestUtils.STAGING_CONTAINER_NAME;
    String actualContainerName = serviceHelper.getContainerNameFromAbsoluteFilePath(url);
    Assertions.assertEquals(expectedContainerName, actualContainerName);
  }

  @Test
  void getRelativeFilePathFromStandardEndpoint_NoZoneNumber_ShouldReturnFilePath() {
    // Standard endpoint format: blob.core.windows.net (no zone number)
    String url = "https://" + TestUtils.STORAGE_NAME + ".blob.core.windows.net/"
        + TestUtils.STAGING_CONTAINER_NAME + "/" + TestUtils.RELATIVE_FILE_PATH;
    String expectedFilePath = TestUtils.RELATIVE_FILE_PATH;
    String actualFilePath = serviceHelper.getRelativeFilePathFromAbsoluteFilePath(url);
    Assertions.assertEquals(expectedFilePath, actualFilePath);
  }
}
