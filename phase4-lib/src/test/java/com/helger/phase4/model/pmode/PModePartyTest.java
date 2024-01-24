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
package com.helger.phase4.model.pmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.annotation.Nonnull;

import org.junit.Test;

import com.helger.commons.mock.CommonsTestHelper;
import com.helger.json.IJsonObject;
import com.helger.xml.mock.XMLTestHelper;

/**
 * Test class for class {@link PModeParty}
 *
 * @author Philip Helger
 */
public final class PModePartyTest
{
  private static void _testJson (@Nonnull final PModeParty p)
  {
    final IJsonObject o = PModePartyJsonConverter.convertToJson (p);
    final PModeParty p2 = PModePartyJsonConverter.convertToNative (o);
    CommonsTestHelper.testDefaultImplementationWithEqualContentObject (p, p2);
  }

  @Test
  public void testBasic ()
  {
    final PModeParty p = new PModeParty ("a", "b", "c", "d", "e");
    assertEquals ("a", p.getIDType ());
    assertTrue (p.hasIDType ());
    assertEquals ("b", p.getIDValue ());
    assertEquals ("a:b", p.getID ());
    assertEquals ("c", p.getRole ());
    assertEquals ("d", p.getUserName ());
    assertTrue (p.hasUserName ());
    assertEquals ("e", p.getPassword ());
    assertTrue (p.hasPassword ());

    CommonsTestHelper.testDefaultImplementationWithEqualContentObject (p, new PModeParty ("a", "b", "c", "d", "e"));
    XMLTestHelper.testMicroTypeConversion (p);
    _testJson (p);
  }

  @Test
  public void testSimple ()
  {
    final PModeParty p = PModeParty.createSimple ("b", "c");
    assertNull (p.getIDType ());
    assertFalse (p.hasIDType ());
    assertEquals ("b", p.getIDValue ());
    assertEquals ("b", p.getID ());
    assertEquals ("c", p.getRole ());
    assertNull (p.getUserName ());
    assertFalse (p.hasUserName ());
    assertNull (p.getPassword ());
    assertFalse (p.hasPassword ());

    CommonsTestHelper.testDefaultImplementationWithEqualContentObject (p, PModeParty.createSimple ("b", "c"));
    XMLTestHelper.testMicroTypeConversion (p);
    _testJson (p);
  }
}
