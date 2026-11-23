package org.opengroup.osdu.file.coreplus.provider.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.model.file.FileLocation;
import org.opengroup.osdu.core.common.model.file.LocationResponse;
import org.opengroup.osdu.file.model.SignedUrl;

class LocationMapperTest {

  @Test
  void mapsPublicUploadFieldsWithoutLeakingInternalStorageLocation() throws Exception {
    SignedUrl signed = SignedUrl.builder()
        .url(URI.create("https://storage.example/upload?signature=value").toURL())
        .uri(URI.create("s3://internal-bucket/private"))
        .fileSource("/upload/file").build();
    FileLocation location = FileLocation.builder().fileID("file-id")
        .location("s3://internal-bucket/private").build();

    LocationResponse result = new LocationMapper().buildLocationResponse(signed, location);

    assertEquals("file-id", result.getFileID());
    assertEquals(Map.of("SignedURL", signed.getUrl().toString(), "FileSource", "/upload/file"),
        result.getLocation());
  }
}
