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
package com.helger.phase4.crypto;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.base.string.StringHelper;

/**
 * Test class for class {@link ECryptoKeyIdentifierType}.
 *
 * @author Philip Helger
 */
public final class ECryptoKeyIdentifierTypeTest
{
  @Test
  public void testBasic ()
  {
    for (final ECryptoKeyIdentifierType e : ECryptoKeyIdentifierType.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getID ()));
      assertSame (e, ECryptoKeyIdentifierType.getFromIDOrNull (e.getID ()));
      assertSame (e, ECryptoKeyIdentifierType.getFromIDOrDefault (e.getID (), null));
      assertSame (e, ECryptoKeyIdentifierType.getFromIDOrThrow (e.getID ()));
    }
  }

  @Test
  public void testTypeIDRoundTrip ()
  {
    for (final ECryptoKeyIdentifierType e : ECryptoKeyIdentifierType.values ())
    {
      final ECryptoKeyIdentifierType eFound = ECryptoKeyIdentifierType.getFromTypeIDOrNull (e.getTypeID ());
      assertNotNull ("Failed to find by typeID for " + e, eFound);
      assertSame (e, eFound);
    }
  }

  @Test
  public void testUnknownID ()
  {
    assertNull (ECryptoKeyIdentifierType.getFromIDOrNull ("does-not-exist"));
    assertNull (ECryptoKeyIdentifierType.getFromIDOrNull (null));
    assertNull (ECryptoKeyIdentifierType.getFromIDOrNull (""));
    assertSame (ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE,
                ECryptoKeyIdentifierType.getFromIDOrDefault ("does-not-exist",
                                                              ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE));
  }

  @Test
  public void testUnknownTypeID ()
  {
    assertNull (ECryptoKeyIdentifierType.getFromTypeIDOrNull (-999));
  }
}
