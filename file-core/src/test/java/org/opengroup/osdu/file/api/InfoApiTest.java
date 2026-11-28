package org.opengroup.osdu.file.api;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.info.VersionInfoBuilder;
import org.opengroup.osdu.core.common.model.info.VersionInfo;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
public class InfoApiTest {

  @Mock
  private VersionInfoBuilder versionInfoBuilder;

  @Mock
  private VersionInfo versionInfo;

  @InjectMocks
  private InfoApi infoApi;

  @Test
  public void test_info() throws IOException {
    when(versionInfoBuilder.buildVersionInfo()).thenReturn(versionInfo);
    VersionInfo versionInfo = infoApi.info();
    assertNotNull(versionInfo);
    verify(versionInfoBuilder, times(1)).buildVersionInfo();
  }
}
