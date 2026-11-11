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

import java.io.IOException;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.phase4.messaging.http.HttpRetryClassifier;
import com.helger.smpclient.exception.SMPClientBadResponseException;
import com.helger.smpclient.exception.SMPClientException;
import com.helger.smpclient.exception.SMPClientHttpException;
import com.helger.smpclient.exception.SMPClientSMPUnavailableException;

/**
 * Decide whether a failed SMP lookup may be retried at a later point in time. SMP queries are HTTP
 * GET requests, so replaying them is always safe.
 *
 * @author Philip Helger
 * @since 4.9.0
 */
@Immutable
public final class SMPRetryClassifier
{
  private SMPRetryClassifier ()
  {}

  /**
   * Check if a retry is feasible for the provided SMP client exception.
   * <ul>
   * <li>The SMP could not be contacted at all (unknown host, connection refused, timeout) - retry is
   * feasible</li>
   * <li>The SMP responded with an HTTP error - decided by the HTTP status code (see
   * {@link HttpRetryClassifier#classifyStatusCode(int)}), so e.g. 503 is feasible, but 400, 403 and
   * 404 are not</li>
   * <li>The SMP response was invalid (e.g. invalid signature, malformed XML) - retry is not
   * feasible</li>
   * <li>Other errors are decided by their causing {@link IOException} (see
   * {@link HttpRetryClassifier#classifyException(IOException)}). Errors without such a cause (e.g.
   * identifier or certificate mismatch) are not feasible.</li>
   * </ul>
   *
   * @param ex
   *        The exception to check. May not be <code>null</code>.
   * @return <code>true</code> if a retry is feasible, <code>false</code> if not.
   */
  public static boolean isRetryFeasible (@NonNull final SMPClientException ex)
  {
    // The SMP could not be contacted at all
    if (ex instanceof SMPClientSMPUnavailableException)
      return true;

    // An HTTP response was received - decide on the status code
    if (ex instanceof final SMPClientHttpException aHttpEx)
      return HttpRetryClassifier.classifyStatusCode (aHttpEx.getResponseStatusCode ()).isRetryFeasible (true);

    // Invalid signature, malformed or empty response, missing trust store
    if (ex instanceof SMPClientBadResponseException)
      return false;

    // Decide on the technical cause, if present
    if (ex.getCause () instanceof final IOException aIOEx)
      return HttpRetryClassifier.classifyException (aIOEx).isRetryFeasible (true);

    // E.g. identifier mismatch or certificate mismatch
    return false;
  }
}
