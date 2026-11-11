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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;

import javax.net.ssl.SSLException;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.ConnectionRequestTimeoutException;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringParser;
import com.helger.http.CHttp;
import com.helger.httpclient.response.ExtendedHttpResponseException;
import com.helger.phase4.model.soapfault.AS4SoapFaultException;

/**
 * Decide whether a failed HTTP transmission may be retried. The guiding question is, whether the
 * request could have reached the server.
 *
 * @author Philip Helger
 * @since 4.9.0
 */
@Immutable
public final class HttpRetryClassifier
{
  private HttpRetryClassifier ()
  {}

  /**
   * Classify an HTTP response status code.
   * <ul>
   * <li>408, 425, 429 and 503 can always be retried</li>
   * <li>500, 502 and 504 can only be retried if replaying is safe</li>
   * <li>All other status codes are never retried</li>
   * </ul>
   *
   * @param nStatusCode
   *        The HTTP status code to classify.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static EHttpRetryClassification classifyStatusCode (final int nStatusCode)
  {
    return switch (nStatusCode)
    {
      case CHttp.HTTP_REQUEST_TIMEOUT, CHttp.HTTP_TOO_EARLY, CHttp.HTTP_TOO_MANY_REQUESTS, CHttp.HTTP_SERVICE_UNAVAILABLE -> EHttpRetryClassification.RETRY;
      case CHttp.HTTP_INTERNAL_SERVER_ERROR, CHttp.HTTP_BAD_GATEWAY, CHttp.HTTP_GATEWAY_TIMEOUT -> EHttpRetryClassification.RETRY_IF_REPLAY_SAFE;
      default -> EHttpRetryClassification.NO_RETRY;
    };
  }

  /**
   * Classify an exception that occurred while sending an HTTP request. The order of the checks
   * matters, because e.g. {@link ConnectTimeoutException} is a {@link SocketTimeoutException} and
   * {@link NoRouteToHostException} is a {@link SocketException}.
   *
   * @param ex
   *        The exception to classify. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static EHttpRetryClassification classifyException (@NonNull final IOException ex)
  {
    // phase4 specific exceptions that must never be retried
    if (ex instanceof AS4SoapFaultException || ex instanceof AS4HttpUnauthorizedException)
      return EHttpRetryClassification.NO_RETRY;

    // An HTTP response was received - decide on the status code. Must be checked before
    // ClientProtocolException, as it is a subclass of it
    if (ex instanceof final ExtendedHttpResponseException aResponseEx)
      return classifyStatusCode (aResponseEx.getStatusLine ().getStatusCode ());

    // Certificate or protocol problems are deterministic
    if (ex instanceof SSLException || ex instanceof ClientProtocolException)
      return EHttpRetryClassification.NO_RETRY;

    // The request was never sent
    if (ex instanceof ConnectionRequestTimeoutException ||
        ex instanceof ConnectTimeoutException ||
        ex instanceof ConnectException ||
        ex instanceof NoRouteToHostException ||
        ex instanceof UnknownHostException)
      return EHttpRetryClassification.RETRY;

    // Read timeout - the server may already have processed the request
    if (ex instanceof SocketTimeoutException)
      return EHttpRetryClassification.RETRY_IF_REPLAY_SAFE;

    // A real thread interrupt or an aborted request means "stop"
    if (ex instanceof InterruptedIOException)
      return EHttpRetryClassification.NO_RETRY;

    // Stale connection, connection reset, broken pipe etc.
    if (ex instanceof NoHttpResponseException ||
        ex instanceof ConnectionClosedException ||
        ex instanceof SocketException)
      return EHttpRetryClassification.RETRY_IF_REPLAY_SAFE;

    // Unknown - the request may have reached the server
    return EHttpRetryClassification.RETRY_IF_REPLAY_SAFE;
  }

  /**
   * Get the duration to wait as indicated by the HTTP <code>Retry-After</code> header value.
   *
   * @param sRetryAfter
   *        The header value. Either a number of seconds or an HTTP date. May be <code>null</code>.
   * @param aNow
   *        The current point in time, to calculate the duration for an HTTP date. May not be
   *        <code>null</code>.
   * @return <code>null</code> if no header value is present or if it cannot be parsed. A date in
   *         the past results in {@link Duration#ZERO}.
   */
  @Nullable
  public static Duration getRetryAfterDuration (@Nullable final String sRetryAfter, @NonNull final Instant aNow)
  {
    final String sValue = StringHelper.trim (sRetryAfter);
    if (StringHelper.isEmpty (sValue))
      return null;

    // Delta-seconds form
    final long nSeconds = StringParser.parseLong (sValue, -1);
    if (nSeconds >= 0)
      return Duration.ofSeconds (nSeconds);

    // HTTP-date form
    final Instant aDate = DateUtils.parseStandardDate (sValue);
    if (aDate == null)
      return null;
    return aDate.isAfter (aNow) ? Duration.between (aNow, aDate) : Duration.ZERO;
  }

  /**
   * Get the duration to wait as indicated by the HTTP <code>Retry-After</code> header of the HTTP
   * response contained in the provided exception.
   *
   * @param ex
   *        The exception to check. May not be <code>null</code>.
   * @param aNow
   *        The current point in time. May not be <code>null</code>.
   * @return <code>null</code> if the exception does not contain an HTTP response or if the header
   *         is not present or cannot be parsed.
   */
  @Nullable
  public static Duration getRetryAfterDuration (@NonNull final IOException ex, @NonNull final Instant aNow)
  {
    if (ex instanceof final ExtendedHttpResponseException aResponseEx)
    {
      final HttpResponse aHttpResponse = aResponseEx.getHttpResponse ();
      if (aHttpResponse != null)
      {
        final Header aHeader = aHttpResponse.getFirstHeader (HttpHeaders.RETRY_AFTER);
        if (aHeader != null)
          return getRetryAfterDuration (aHeader.getValue (), aNow);
      }
    }
    return null;
  }
}
