package org.opengroup.osdu.file.provider.gcp.provider.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ObmStorageUrlBuilderTest {

  @Test
  void shouldBuildUnsignedUrlFromRelativePathWithLeadingSlash() {
    String url = ObmStorageUrlBuilder.buildUnsignedUrl("https://seaweedfs.example.com", "bucket-name", "/uuid/fileId");

    assertEquals("https://seaweedfs.example.com/bucket-name/uuid/fileId", url);
  }

  @Test
  void shouldNotCreateDoubleSlashWhenProtocolAlreadyEndsWithSlash() {
    String url = ObmStorageUrlBuilder.buildUnsignedUrl("https://seaweedfs.example.com/", "bucket-name", "/uuid/fileId");

    assertEquals("https://seaweedfs.example.com/bucket-name/uuid/fileId", url);
  }

  @Test
  void shouldBuildUnsignedUrlFromRepositoryStylePath() {
    String url = ObmStorageUrlBuilder.buildUnsignedUrl("https://seaweedfs.example.com", "bucket-name", "uuid/fileId");

    assertEquals("https://seaweedfs.example.com/bucket-name/uuid/fileId", url);
  }

  @Test
  void shouldPreserveRepositoryTrailingSlashForEmptyPath() {
    String url = ObmStorageUrlBuilder.buildUnsignedUrl("https://seaweedfs.example.com", "bucket-name", "");

    assertEquals("https://seaweedfs.example.com/bucket-name/", url);
  }
}
