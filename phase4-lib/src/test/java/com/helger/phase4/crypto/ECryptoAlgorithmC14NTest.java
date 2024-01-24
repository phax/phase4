/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.commons.string.StringHelper;

/**
 * Test class for class {@link ECryptoAlgorithmC14N}.
 *
 * @author Philip Helger
 */
public final class ECryptoAlgorithmC14NTest
{
  @Test
  public void testBasic ()
  {
    for (final ECryptoAlgorithmC14N e : ECryptoAlgorithmC14N.values ())
    {
      assertTrue (StringHelper.hasText (e.getID ()));
      assertTrue (StringHelper.hasText (e.getAlgorithmURI ()));
      assertSame (e, ECryptoAlgorithmC14N.getFromIDOrNull (e.getID ()));
      assertSame (e, ECryptoAlgorithmC14N.getFromIDOrDefault (e.getID (), null));
      assertSame (e, ECryptoAlgorithmC14N.getFromIDOrThrow (e.getID ()));
    }
  }
}
