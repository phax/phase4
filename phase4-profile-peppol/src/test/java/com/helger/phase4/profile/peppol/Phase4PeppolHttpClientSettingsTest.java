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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * Test class for class {@link Phase4PeppolHttpClientSettings}
 *
 * @author Philip Helger
 */
public final class Phase4PeppolHttpClientSettingsTest
{
  @Test
  public void testBasic ()
  {
    final Phase4PeppolHttpClientSettings aSettings = new Phase4PeppolHttpClientSettings ();
    // 4.5.0 onwards: no implicit SSLContext is installed
    assertNull (aSettings.getSSLContext ());
  }

  @Test
  public void testSSLContextTrustAll () throws Exception
  {
    final Phase4PeppolHttpClientSettings aSettings = new Phase4PeppolHttpClientSettings ();
    assertSame (aSettings, aSettings.setSSLContextTrustAll ());
    assertNotNull (aSettings.getSSLContext ());
  }

  @Test
  public void testSSLContextPeppolMozillaNSS () throws Exception
  {
    final Phase4PeppolHttpClientSettings aSettings = new Phase4PeppolHttpClientSettings ();
    assertSame (aSettings, aSettings.setSSLContextPeppolMozillaNSS ());
    assertNotNull (aSettings.getSSLContext ());
  }
}
