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
package com.helger.phase4.messaging.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.hc.client5.http.CircularRedirectException;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.client5.http.impl.classic.RequestFailedException;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.ConnectionRequestTimeoutException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.http.message.StatusLine;
import org.junit.Test;

import com.helger.http.CHttp;
import com.helger.httpclient.response.ExtendedHttpResponseException;

/**
 * Test class for class {@link HttpRetryClassifier}.
 *
 * @author Philip Helger
 */
public final class HttpRetryClassifierTest
{
  @Test
  public void testClassifyStatusCode ()
  {
    for (final int n : new int [] { 408, 425, 429, 503 })
      assertSame (EHttpRetryClassification.RETRY, HttpRetryClassifier.classifyStatusCode (n));
    for (final int n : new int [] { 500, 502, 504 })
      assertSame (EHttpRetryClassification.RETRY_IF_REPLAY_SAFE, HttpRetryClassifier.classifyStatusCode (n));
    for (final int n : new int [] { 200, 204, 301, 400, 401, 403, 404, 405, 415, 501, 505 })
      assertSame (EHttpRetryClassification.NO_RETRY, HttpRetryClassifier.classifyStatusCode (n));
  }

  @Test
  public void testIsRetryFeasible ()
  {
    assertTrue (EHttpRetryClassification.RETRY.isRetryFeasible (true));
    assertTrue (EHttpRetryClassification.RETRY.isRetryFeasible (false));
    assertTrue (EHttpRetryClassification.RETRY_IF_REPLAY_SAFE.isRetryFeasible (true));
    assertFalse (EHttpRetryClassification.RETRY_IF_REPLAY_SAFE.isRetryFeasible (false));
    assertFalse (EHttpRetryClassification.NO_RETRY.isRetryFeasible (true));
    assertFalse (EHttpRetryClassification.NO_RETRY.isRetryFeasible (false));
  }

  @Test
  public void testClassifyException ()
  {
    // Never sent
    assertSame (EHttpRetryClassification.RETRY,
                HttpRetryClassifier.classifyException (new ConnectionRequestTimeoutException ("x")));
    assertSame (EHttpRetryClassification.RETRY, HttpRetryClassifier.classifyException (new ConnectException ("x")));
    assertSame (EHttpRetryClassification.RETRY,
                HttpRetryClassifier.classifyException (new HttpHostConnectException ("x")));
    assertSame (EHttpRetryClassification.RETRY,
                HttpRetryClassifier.classifyException (new ConnectTimeoutException ("x")));
    assertSame (EHttpRetryClassification.RETRY,
                HttpRetryClassifier.classifyException (new NoRouteToHostException ("x")));
    assertSame (EHttpRetryClassification.RETRY, HttpRetryClassifier.classifyException (new UnknownHostException ("x")));

    // May have reached the server
    assertSame (EHttpRetryClassification.RETRY_IF_REPLAY_SAFE,
                HttpRetryClassifier.classifyException (new NoHttpResponseException ("x")));
    assertSame (EHttpRetryClassification.RETRY_IF_REPLAY_SAFE,
                HttpRetryClassifier.classifyException (new SocketTimeoutException ("x")));
    assertSame (EHttpRetryClassification.RETRY_IF_REPLAY_SAFE,
                HttpRetryClassifier.classifyException (new ConnectionClosedException ("x")));
    assertSame (EHttpRetryClassification.RETRY_IF_REPLAY_SAFE,
                HttpRetryClassifier.classifyException (new SocketException ("Connection reset")));
    // Unknown
    assertSame (EHttpRetryClassification.RETRY_IF_REPLAY_SAFE,
                HttpRetryClassifier.classifyException (new IOException ("x")));

    // Never
    assertSame (EHttpRetryClassification.NO_RETRY, HttpRetryClassifier.classifyException (new SSLException ("x")));
    assertSame (EHttpRetryClassification.NO_RETRY,
                HttpRetryClassifier.classifyException (new SSLHandshakeException ("x")));
    assertSame (EHttpRetryClassification.NO_RETRY,
                HttpRetryClassifier.classifyException (new ClientProtocolException ("x")));
    assertSame (EHttpRetryClassification.NO_RETRY,
                HttpRetryClassifier.classifyException (new ClientProtocolException (new CircularRedirectException ("x"))));
    assertSame (EHttpRetryClassification.NO_RETRY,
                HttpRetryClassifier.classifyException (new InterruptedIOException ("x")));
    assertSame (EHttpRetryClassification.NO_RETRY,
                HttpRetryClassifier.classifyException (new RequestFailedException ("x")));
    assertSame (EHttpRetryClassification.NO_RETRY,
                HttpRetryClassifier.classifyException (new AS4HttpUnauthorizedException ("x")));
  }

  @Test
  public void testClassifyResponseException ()
  {
    final BasicHttpResponse aResponse = new BasicHttpResponse (CHttp.HTTP_SERVICE_UNAVAILABLE);
    final ExtendedHttpResponseException ex = new ExtendedHttpResponseException (new StatusLine (aResponse),
                                                                                aResponse,
                                                                                null,
                                                                                StandardCharsets.ISO_8859_1);
    assertSame (EHttpRetryClassification.RETRY, HttpRetryClassifier.classifyException (ex));

    final BasicHttpResponse aResponse2 = new BasicHttpResponse (CHttp.HTTP_BAD_REQUEST);
    final ExtendedHttpResponseException ex2 = new ExtendedHttpResponseException (new StatusLine (aResponse2),
                                                                                 aResponse2,
                                                                                 null,
                                                                                 StandardCharsets.ISO_8859_1);
    assertSame (EHttpRetryClassification.NO_RETRY, HttpRetryClassifier.classifyException (ex2));
  }

  @Test
  public void testGetRetryAfterDuration ()
  {
    final Instant aNow = Instant.parse ("2026-11-10T12:00:00Z");
    assertNull (HttpRetryClassifier.getRetryAfterDuration ((String) null, aNow));
    assertNull (HttpRetryClassifier.getRetryAfterDuration ("", aNow));
    assertNull (HttpRetryClassifier.getRetryAfterDuration ("bla", aNow));
    assertNull (HttpRetryClassifier.getRetryAfterDuration ("-5", aNow));

    assertEquals (Duration.ZERO, HttpRetryClassifier.getRetryAfterDuration ("0", aNow));
    assertEquals (Duration.ofSeconds (120), HttpRetryClassifier.getRetryAfterDuration (" 120 ", aNow));

    // HTTP-date form
    assertEquals (Duration.ofSeconds (90),
                  HttpRetryClassifier.getRetryAfterDuration ("Tue, 10 Nov 2026 12:01:30 GMT", aNow));
    // In the past
    assertEquals (Duration.ZERO, HttpRetryClassifier.getRetryAfterDuration ("Tue, 10 Nov 2026 11:00:00 GMT", aNow));
  }

  @Test
  public void testGetRetryAfterDurationFromException ()
  {
    final Instant aNow = Instant.now ();
    assertNull (HttpRetryClassifier.getRetryAfterDuration (new IOException ("x"), aNow));

    final BasicHttpResponse aResponse = new BasicHttpResponse (CHttp.HTTP_TOO_MANY_REQUESTS);
    aResponse.addHeader (HttpHeaders.RETRY_AFTER, "7");
    final ExtendedHttpResponseException ex = new ExtendedHttpResponseException (new StatusLine (aResponse),
                                                                                aResponse,
                                                                                null,
                                                                                StandardCharsets.ISO_8859_1);
    assertEquals (Duration.ofSeconds (7), HttpRetryClassifier.getRetryAfterDuration (ex, aNow));
  }
}
