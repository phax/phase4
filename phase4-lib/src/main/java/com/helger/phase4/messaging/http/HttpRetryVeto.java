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

import com.helger.annotation.concurrent.ThreadSafe;

/**
 * Allows an HTTP response handler to veto the retry of the HTTP response it is currently handling,
 * even if the HTTP status code of the response would allow for a retry. The response is then
 * handled as usual. This is e.g. used if the response contains a SOAP Fault with a permanent
 * disposition.<br>
 * The veto is stored per thread, because the response handler is invoked synchronously in the
 * thread that sends the request.
 *
 * @author Philip Helger
 * @since 4.9.0
 */
@ThreadSafe
public final class HttpRetryVeto
{
  private static final ThreadLocal <Boolean> TL_VETO = new ThreadLocal <> ();

  private HttpRetryVeto ()
  {}

  /**
   * Veto the retry of the HTTP response that is currently handled. Must be called from within an
   * HTTP response handler passed to
   * {@link IHttpPoster#sendGenericMessageWithRetries(String, com.helger.http.header.HttpHeaderMap, org.apache.hc.core5.http.HttpEntity, String, HttpRetrySettings, org.apache.hc.core5.http.io.HttpClientResponseHandler, com.helger.phase4.dump.IAS4OutgoingDumper, com.helger.phase4.client.IAS4RetryCallback, java.util.function.Consumer)}.
   */
  public static void vetoRetry ()
  {
    TL_VETO.set (Boolean.TRUE);
  }

  /**
   * Remove any veto of the current thread.
   */
  static void clear ()
  {
    TL_VETO.remove ();
  }

  /**
   * @return <code>true</code> if the retry of the current HTTP response was vetoed,
   *         <code>false</code> if not.
   */
  static boolean isVetoed ()
  {
    return TL_VETO.get () != null;
  }
}
