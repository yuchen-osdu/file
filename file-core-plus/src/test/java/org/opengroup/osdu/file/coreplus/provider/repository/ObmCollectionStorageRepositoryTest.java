package org.opengroup.osdu.file.coreplus.provider.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.obm.core.EnvironmentResolver;
import org.opengroup.osdu.core.obm.core.UrlProvider;
import org.opengroup.osdu.file.model.SignedObject;
import org.opengroup.osdu.file.model.SignedUrlParameters;

@ExtendWith(MockitoExtension.class)
class ObmCollectionStorageRepositoryTest {

  @Mock private DpsHeaders headers;
  @Mock private EnvironmentResolver environment;
  @Mock private UrlProvider urls;
  private ObmCollectionStorageRepository repository;

  @BeforeEach
  void setUp() {
    repository = new ObmCollectionStorageRepository(headers, environment, urls);
    when(headers.getPartitionId()).thenReturn("opendes");
    when(environment.getTransferProtocol("opendes")).thenReturn("https://storage.example");
  }

  @Test
  void uploadTargetsBucketRatherThanAnIndividualObject() throws Exception {
    URL url = URI.create("https://storage.example/bucket").toURL();
    when(urls.getObjectUrl("bucket", "opendes")).thenReturn(url);

    SignedObject result = repository.createSignedObject("bucket", "collection");

    assertEquals(url, result.getUrl());
    assertEquals(URI.create("https://storage.example/bucket/"), result.getUri());
    verify(urls).getObjectUrl("bucket", "opendes");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void downloadTargetsTheCollectionPath(boolean withParameters) throws Exception {
    URL url = URI.create("https://storage.example/bucket/collection").toURL();
    when(urls.getObjectUrl("bucket", "collection", "opendes")).thenReturn(url);

    SignedObject result = withParameters
        ? repository.getSignedObjectBasedOnParams("bucket", "collection", new SignedUrlParameters())
        : repository.getSignedObject("bucket", "collection");

    assertEquals(url, result.getUrl());
    assertEquals(URI.create("https://storage.example/bucket/collection"), result.getUri());
    verify(urls).getObjectUrl("bucket", "collection", "opendes");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void malformedProviderUrlBecomesBadRequest(boolean upload) throws Exception {
    if (upload) {
      when(urls.getObjectUrl("bucket", "opendes")).thenThrow(new MalformedURLException("invalid"));
    } else {
      when(urls.getObjectUrl("bucket", "collection", "opendes"))
          .thenThrow(new MalformedURLException("invalid"));
    }

    AppException error = assertThrows(AppException.class, () -> {
      if (upload) {
        repository.createSignedObject("bucket", "collection");
      } else {
        repository.getSignedObject("bucket", "collection");
      }
    });

    assertEquals(400, error.getError().getCode());
    assertEquals("Signed URL was malformed", error.getError().getMessage());
  }
}
