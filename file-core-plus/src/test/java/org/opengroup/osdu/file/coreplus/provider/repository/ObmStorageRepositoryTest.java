package org.opengroup.osdu.file.coreplus.provider.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.obm.core.Driver;
import org.opengroup.osdu.core.obm.core.EnvironmentResolver;
import org.opengroup.osdu.core.obm.core.model.ObmHttpMethod;
import org.opengroup.osdu.core.obm.core.model.ObmSignedUrlParams;
import org.opengroup.osdu.file.model.SignedObject;
import org.opengroup.osdu.file.model.SignedUrlParameters;
import org.opengroup.osdu.file.util.ExpiryTimeUtil;

@ExtendWith(MockitoExtension.class)
class ObmStorageRepositoryTest {

  @Mock private DpsHeaders headers;
  @Mock private Driver driver;
  @Mock private EnvironmentResolver environment;
  private ObmStorageRepository repository;
  private final ArgumentCaptor<ObmSignedUrlParams> params =
      ArgumentCaptor.forClass(ObmSignedUrlParams.class);
  private URL signedUrl;

  @BeforeEach
  void setUp() throws Exception {
    repository = new ObmStorageRepository(headers, driver, new ExpiryTimeUtil(), environment);
    signedUrl = URI.create("https://storage.example/bucket/folder/file?signature=value").toURL();
    when(headers.getPartitionId()).thenReturn("opendes");
    when(environment.getTransferProtocol("opendes")).thenReturn("s3://");
    when(driver.getSignedUrlWithParams(params.capture())).thenReturn(signedUrl);
  }

  @Test
  void uploadUsesPutWithDefaultExpiryAndUnsignedObjectUri() {
    SignedObject result = repository.createSignedObject("bucket", "folder/file");

    assertSignedObject(result, ObmHttpMethod.PUT, 1, TimeUnit.HOURS, Map.of());
  }

  @Test
  void downloadUsesGetWithDefaultExpiry() {
    SignedObject result = repository.getSignedObject("bucket", "folder/file");

    assertSignedObject(result, ObmHttpMethod.GET, 1, TimeUnit.HOURS, Map.of());
  }

  @Test
  void downloadPreservesBothResponseOverridesAndQuotesFilename() {
    SignedObject result = repository.getSignedObjectBasedOnParams("bucket", "folder/file",
        new SignedUrlParameters("30M", "testing,copy.txt", "text/plain"));

    assertSignedObject(result, ObmHttpMethod.GET, 30, TimeUnit.MINUTES, Map.of(
        "response-content-disposition", "attachment; filename=\"testing,copy.txt\"",
        "response-content-type", "text/plain"));
  }

  @ParameterizedTest
  @CsvSource({",", "'',''", "'',text/plain", "report.txt,''"})
  void omitsMissingResponseOverrides(String filename, String contentType) {
    SignedObject result = repository.getSignedObjectBasedOnParams("bucket", "folder/file",
        new SignedUrlParameters("2H", filename, contentType));
    Map<String, String> expected = new HashMap<>();
    if (filename != null && !filename.isEmpty()) {
      expected.put("response-content-disposition", "attachment; filename=\"report.txt\"");
    }
    if (contentType != null && !contentType.isEmpty()) {
      expected.put("response-content-type", "text/plain");
    }

    assertSignedObject(result, ObmHttpMethod.GET, 2, TimeUnit.HOURS, expected);
  }

  private void assertSignedObject(SignedObject result, ObmHttpMethod method, long duration,
      TimeUnit unit, Map<String, String> query) {
    assertEquals(signedUrl, result.getUrl());
    assertEquals(URI.create("s3://bucket/folder/file"), result.getUri());
    ObmSignedUrlParams captured = params.getValue();
    assertEquals(method, captured.getMethod());
    assertEquals("opendes", captured.getDestination().getPartitionId());
    assertEquals("bucket", captured.getBucket());
    assertEquals("folder/file", captured.getFileName());
    assertEquals(duration, captured.getExpiryDuration());
    assertEquals(unit, captured.getExpiryTimeUnit());
    assertEquals(query, captured.getQueryParams());
    verify(driver).getSignedUrlWithParams(captured);
  }
}
