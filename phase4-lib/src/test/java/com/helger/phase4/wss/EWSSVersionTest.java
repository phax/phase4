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
package com.helger.phase4.wss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.base.string.StringHelper;

/**
 * Test class for class {@link EWSSVersion}.
 *
 * @author Philip Helger
 */
public final class EWSSVersionTest
{
  @Test
  public void testBasic ()
  {
    for (final EWSSVersion e : EWSSVersion.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getNamespaceURI ()));
      assertTrue (StringHelper.isNotEmpty (e.getNamespacePrefix ()));
      assertTrue (StringHelper.isNotEmpty (e.getVersion ()));
      assertSame (e, EWSSVersion.getFromVersionOrNull (e.getVersion ()));
    }
  }

  @SuppressWarnings ("deprecation")
  @Test
  public void testVersions ()
  {
    assertEquals ("1.0", EWSSVersion.WSS_10.getVersion ());
    assertEquals ("1.1", EWSSVersion.WSS_11.getVersion ());
    assertEquals ("1.1.1", EWSSVersion.WSS_111.getVersion ());
  }

  @SuppressWarnings ("deprecation")
  @Test
  public void testWSS111NamespaceMatchesWSS11 ()
  {
    // WSS 1.1.1 uses the same namespace URI and prefix as WSS 1.1
    assertEquals (EWSSVersion.WSS_11.getNamespaceURI (), EWSSVersion.WSS_111.getNamespaceURI ());
    assertEquals (EWSSVersion.WSS_11.getNamespacePrefix (), EWSSVersion.WSS_111.getNamespacePrefix ());
  }

  @Test
  public void testUnknown ()
  {
    assertNull (EWSSVersion.getFromVersionOrNull (null));
    assertNull (EWSSVersion.getFromVersionOrNull (""));
    assertNull (EWSSVersion.getFromVersionOrNull ("2.0"));
    assertSame (EWSSVersion.WSS_111, EWSSVersion.getFromVersionOrDefault ("does-not-exist", EWSSVersion.WSS_111));
  }
}
