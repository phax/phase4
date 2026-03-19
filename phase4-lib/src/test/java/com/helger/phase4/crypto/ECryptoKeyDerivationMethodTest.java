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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.wss4j.common.WSS4JConstants;
import org.junit.Test;

import com.helger.base.string.StringHelper;

/**
 * Test class for class {@link ECryptoKeyDerivationMethod}.
 *
 * @author Philip Helger
 */
public final class ECryptoKeyDerivationMethodTest
{
  @Test
  public void testBasic ()
  {
    for (final ECryptoKeyDerivationMethod e : ECryptoKeyDerivationMethod.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getID ()));
      assertSame (e, ECryptoKeyDerivationMethod.getFromIDOrNull (e.getID ()));
      assertSame (e, ECryptoKeyDerivationMethod.getFromIDOrDefault (e.getID (), null));
    }
  }

  @Test
  public void testUnknownID ()
  {
    assertNull (ECryptoKeyDerivationMethod.getFromIDOrNull ("does-not-exist"));
    assertNull (ECryptoKeyDerivationMethod.getFromIDOrNull (null));
    assertNull (ECryptoKeyDerivationMethod.getFromIDOrNull (""));
    assertSame (ECryptoKeyDerivationMethod.HKDF,
                ECryptoKeyDerivationMethod.getFromIDOrDefault ("does-not-exist", ECryptoKeyDerivationMethod.HKDF));
  }

  @Test
  public void testWSS4JConstantsMatch ()
  {
    assertEquals (WSS4JConstants.KEYDERIVATION_CONCATKDF, ECryptoKeyDerivationMethod.CONCAT_KDF.getID ());
    assertEquals (WSS4JConstants.KEYDERIVATION_HKDF, ECryptoKeyDerivationMethod.HKDF.getID ());
  }

  @Test
  public void testExpectedCount ()
  {
    assertEquals (2, ECryptoKeyDerivationMethod.values ().length);
  }
}
