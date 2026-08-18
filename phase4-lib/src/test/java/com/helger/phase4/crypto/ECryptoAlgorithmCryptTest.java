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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.junit.Test;

import com.helger.base.string.StringHelper;

/**
 * Test class for class {@link ECryptoAlgorithmCrypt}.
 *
 * @author Philip Helger
 */
public final class ECryptoAlgorithmCryptTest
{
  @Test
  @SuppressWarnings ("deprecation")
  public void testBasic ()
  {
    for (final ECryptoAlgorithmCrypt e : ECryptoAlgorithmCrypt.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getID ()));
      assertTrue (StringHelper.isNotEmpty (e.getOIDString ()));
      assertNotNull (e.getOID ());
      assertEquals (e.getOIDString (), e.getOID ().getId ());
      assertSame (e.getOID (), e.getOID ());
      // Must be the canonical interned instance, so that "==" against the Bouncy Castle
      // constants (e.g. CMSAlgorithm.AES128_GCM) keeps working
      assertSame (new ASN1ObjectIdentifier (e.getOIDString ()).intern (), e.getOID ());
      assertTrue (StringHelper.isNotEmpty (e.getAlgorithmURI ()));
      assertSame (e, ECryptoAlgorithmCrypt.getFromIDOrNull (e.getID ()));
      assertSame (e, ECryptoAlgorithmCrypt.getFromIDOrDefault (e.getID (), null));
      assertSame (e, ECryptoAlgorithmCrypt.getFromIDOrThrow (e.getID ()));
    }
  }
}
