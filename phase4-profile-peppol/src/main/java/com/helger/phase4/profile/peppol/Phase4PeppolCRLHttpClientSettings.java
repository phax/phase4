/*
 * Copyright (C) 2019-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.peppol;

import org.apache.hc.core5.util.Timeout;

/**
 * Special {@link Phase4PeppolHttpClientSettings} optimized for CRL downloads.
 *
 * @author Philip Helger
 * @since 4.5.0
 */
public class Phase4PeppolCRLHttpClientSettings extends Phase4PeppolHttpClientSettings
{
  public static final Timeout DEFAULT_CRL_CONNECT_TIMEOUT = Timeout.ofSeconds (5);
  public static final Timeout DEFAULT_CRL_RESPONSE_TIMEOUT = Timeout.ofSeconds (15);

  public Phase4PeppolCRLHttpClientSettings ()
  {
    setConnectTimeout (DEFAULT_CRL_CONNECT_TIMEOUT);
    setResponseTimeout (DEFAULT_CRL_RESPONSE_TIMEOUT);
  }
}
