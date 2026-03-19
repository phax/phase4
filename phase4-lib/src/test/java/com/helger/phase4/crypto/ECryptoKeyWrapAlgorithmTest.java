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
 * Test class for class {@link ECryptoKeyWrapAlgorithm}.
 *
 * @author Philip Helger
 */
public final class ECryptoKeyWrapAlgorithmTest
{
  @Test
  public void testBasic ()
  {
    for (final ECryptoKeyWrapAlgorithm e : ECryptoKeyWrapAlgorithm.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getID ()));
      assertSame (e, ECryptoKeyWrapAlgorithm.getFromIDOrNull (e.getID ()));
      assertSame (e, ECryptoKeyWrapAlgorithm.getFromIDOrDefault (e.getID (), null));
    }
  }

  @Test
  public void testUnknownID ()
  {
    assertNull (ECryptoKeyWrapAlgorithm.getFromIDOrNull ("does-not-exist"));
    assertNull (ECryptoKeyWrapAlgorithm.getFromIDOrNull (null));
    assertNull (ECryptoKeyWrapAlgorithm.getFromIDOrNull (""));
    assertSame (ECryptoKeyWrapAlgorithm.AES_128,
                ECryptoKeyWrapAlgorithm.getFromIDOrDefault ("does-not-exist", ECryptoKeyWrapAlgorithm.AES_128));
  }

  @Test
  public void testWSS4JConstantsMatch ()
  {
    assertEquals (WSS4JConstants.KEYWRAP_AES128, ECryptoKeyWrapAlgorithm.AES_128.getID ());
    assertEquals (WSS4JConstants.KEYWRAP_AES192, ECryptoKeyWrapAlgorithm.AES_192.getID ());
    assertEquals (WSS4JConstants.KEYWRAP_AES256, ECryptoKeyWrapAlgorithm.AES_256.getID ());
    assertEquals (WSS4JConstants.KEYWRAP_TRIPLEDES, ECryptoKeyWrapAlgorithm.TRIPLE_DES.getID ());
  }

  @Test
  public void testExpectedCount ()
  {
    assertEquals (4, ECryptoKeyWrapAlgorithm.values ().length);
  }
}
