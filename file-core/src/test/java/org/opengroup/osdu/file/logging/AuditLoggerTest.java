/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
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

package org.opengroup.osdu.file.logging;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.file.constant.FileServiceRole;

import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuditLoggerTest {

  @Mock
  private JaxRsDpsLog log;

  @Mock
  private DpsHeaders headers;

  @Mock
  private HttpServletRequest httpRequest;

  @InjectMocks
  private AuditLogger sut;

  @BeforeEach
  public void setup() {
    when(headers.getUserEmail()).thenReturn("test_user@email.com");
    when(headers.getUserAuthorizedGroupName()).thenReturn(FileServiceRole.EDITORS);
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("0.0.0.0:1234");
    when(httpRequest.getHeader("user-agent")).thenReturn("testAgent");
  }

  @Test
  public void should_writeReadFileLocationSuccessEvent() {

    sut.readFileLocationSuccess(Collections.singletonList("resources"));
    verify(log, times(1)).audit(any());
  }

  @Test
  public void should_writeReadFileLocationFailureEvent() {
    sut.readFileLocationFailure(Collections.singletonList("resources"));

    verify(log, times(1)).audit(any());
  }

  @Test
  public void should_writeReadFileListSuccessEvent() {
    sut.readFileListSuccess(Collections.singletonList("resources"));

    verify(log, times(1)).audit(any());
  }

  @Test
  public void should_writeReadFileListFailureEvent() {
    sut.readFileListFailure(Collections.singletonList("resources"));

    verify(log, times(1)).audit(any());
  }

  @Test
  public void should_writeCreateLocationSuccessEvent() {
    sut.createLocationSuccess(Collections.singletonList("resources"));

    verify(log, times(1)).audit(any());
  }

  @Test
  public void should_writeCreateLocationSuccessEvent_whenIPv4XForwardedForIPHeaderIsPopulated() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("111.111.111.111:1234");

    sut.createLocationSuccess(Collections.singletonList("resources"));

    verify(log, times(1)).audit(any());
  }

  @Test
  public void should_writeCreateLocationSuccessEvent_whenIPv4XForwardedForIPHeaderIsNotPopulated() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    when(httpRequest.getRemoteAddr()).thenReturn("0.0.0.0:1234");

    sut.createLocationSuccess(Collections.singletonList("resources"));

    verify(log, times(1)).audit(any());
  }

  @Test
  public void should_writeCreateLocationSuccessEvent_whenIPv4IPHeadersContainMultipleIPs() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("0.0.0.0:1234,0.0.0.1:1234");
    sut.createLocationSuccess(Collections.singletonList("resources"));

    verify(log, times(1)).audit(any());
  }

  @Test
  public void should_writeCreateLocationSuccessEvent_whenIPv6XForwardedForIPHeaderIsPopulated() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("[0000:0000:0000:0000:0000:0000:0000:0000]:1234");

    sut.createLocationSuccess(Collections.singletonList("resources"));

    verify(log, times(1)).audit(any());
  }

  @Test
  public void should_writeCreateLocationSuccessEvent_whenIPv6XForwardedForIPHeaderIsNotPopulated() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    when(httpRequest.getRemoteAddr()).thenReturn("[0000:0000:0000:0000:0000:0000:0000:0000]:1234");

    sut.createLocationSuccess(Collections.singletonList("resources"));

    verify(log, times(1)).audit(any());
  }

  @Test
  public void should_writeCreateLocationSuccessEvent_whenIPv6IPHeadersContainMultipleIPs() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("[0000:0000:0000:0000:0000:0000:0000:0000]:1234,[0000:0000:0000:0000:0000:0000:0000:0001]:1234");
    sut.createLocationSuccess(Collections.singletonList("resources"));

    verify(log, times(1)).audit(any());
  }

  @Test
  public void should_writeCreateLocationFailureEvent() {
    sut.createLocationFailure(Collections.singletonList("resources"));

    verify(log, times(1)).audit(any());
  }

  @Test
  public void should_throwIllegalArgumentException_whenUserEmailIsNull() {
    when(headers.getUserEmail()).thenReturn(null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      sut.createLocationFailure(Collections.singletonList("resources"));
    });
    assertNotNull(exception);
    assertEquals("User not provided for audit events.", exception.getMessage());
  }

  @Test
  public void should_throwIllegalArgumentException_whenUserIpAddressIsNull() {
    lenient().when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    lenient().when(httpRequest.getHeader("X-Client-IP")).thenReturn(null);
    lenient().when(httpRequest.getRemoteAddr()).thenReturn(null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      sut.createLocationFailure(Collections.singletonList("resources"));
    });
    assertNotNull(exception);
    assertEquals("User's IP address is not provided for audit events.", exception.getMessage());
  }

  @Test
  public void should_throwIllegalArgumentException_whenUserAgentIsNull() {
    when(httpRequest.getHeader("user-agent")).thenReturn(null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      sut.createLocationFailure(Collections.singletonList("resources"));
    });
    assertNotNull(exception);
    assertEquals("User's agent is not provided for audit events.", exception.getMessage());
  }

  @Test
  public void should_throwIllegalArgumentException_whenUserAuthorizedGroupNameIsNull() {
    when(headers.getUserAuthorizedGroupName()).thenReturn(null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      sut.createLocationFailure(Collections.singletonList("resources"));
    });
    assertNotNull(exception);
    assertEquals("User's authorized group name is not provided for audit events.", exception.getMessage());
  }

  @Test
  public void should_writeReadFileLocationFailure() {
    sut.readFileLocationFailure(Collections.singletonList("resources"));
    verify(log, times(1)).audit(any());
  }
}
