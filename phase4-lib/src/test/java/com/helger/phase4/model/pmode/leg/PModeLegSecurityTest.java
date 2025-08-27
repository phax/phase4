/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.model.pmode.leg;

import org.junit.Test;

import com.helger.json.IJsonObject;
import com.helger.unittest.support.TestHelper;
import com.helger.xml.mock.XMLTestHelper;

import jakarta.annotation.Nonnull;

/**
 * Test class for class {@link PModeLegSecurity}.
 *
 * @author Philip Helger
 */
public final class PModeLegSecurityTest
{
  private static void _testJson (@Nonnull final PModeLegSecurity p)
  {
    final IJsonObject o = PModeLegSecurityJsonConverter.convertToJson (p);
    final PModeLegSecurity p2 = PModeLegSecurityJsonConverter.convertToNative (o);
    TestHelper.testDefaultImplementationWithEqualContentObject (p, p2);
    XMLTestHelper.testMicroTypeConversion (p);
  }

  @Test
  public void testBasic ()
  {
    final PModeLegSecurity x = new PModeLegSecurity ();
    _testJson (x);
  }

  @Test
  public void testDisableAll ()
  {
    final PModeLegSecurity x = new PModeLegSecurity ();
    x.disableEncryption ();
    x.disableSigning ();
    x.disableUsernameToken ();
    _testJson (x);
  }
}
