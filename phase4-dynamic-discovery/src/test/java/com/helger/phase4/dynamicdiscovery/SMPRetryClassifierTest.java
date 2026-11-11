/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phase4.dynamicdiscovery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.smpclient.exception.SMPClientBadResponseException;
import com.helger.smpclient.exception.SMPClientException;
import com.helger.smpclient.httpclient.AbstractGenericSMPClient;

/**
 * Test class for class {@link SMPRetryClassifier}.
 *
 * @author Philip Helger
 */
public final class SMPRetryClassifierTest
{
  private static boolean _isRetryFeasible (@NonNull final Exception ex)
  {
    // Use the same conversion as the SMP client does
    return SMPRetryClassifier.isRetryFeasible (AbstractGenericSMPClient.getConvertedException (ex));
  }

  @Test
  public void testSMPUnavailable ()
  {
    assertTrue (_isRetryFeasible (new UnknownHostException ("smp.example.org")));
    assertTrue (_isRetryFeasible (new ConnectException ("Connection refused")));
    assertTrue (_isRetryFeasible (new ConnectTimeoutException ("x")));
    assertTrue (_isRetryFeasible (new SocketTimeoutException ("Read timed out")));
  }

  @Test
  public void testHttpStatusCodes ()
  {
    for (final int n : new int [] { 408, 425, 429, 500, 502, 503, 504 })
      assertTrue (Integer.toString (n), _isRetryFeasible (new HttpResponseException (n, "x")));
    for (final int n : new int [] { 400, 401, 403, 404, 410, 501 })
      assertFalse (Integer.toString (n), _isRetryFeasible (new HttpResponseException (n, "x")));
  }

  @Test
  public void testOtherErrors ()
  {
    // Technical causes
    assertTrue (_isRetryFeasible (new NoHttpResponseException ("x")));
    assertTrue (_isRetryFeasible (new SocketException ("Connection reset")));
    assertFalse (_isRetryFeasible (new SSLHandshakeException ("x")));

    // Invalid responses
    assertFalse (SMPRetryClassifier.isRetryFeasible (new SMPClientBadResponseException ("Malformed XML")));

    // E.g. identifier mismatch
    assertFalse (SMPRetryClassifier.isRetryFeasible (new SMPClientException ("The SMP response contained the participant identifier ...")));
  }
}
