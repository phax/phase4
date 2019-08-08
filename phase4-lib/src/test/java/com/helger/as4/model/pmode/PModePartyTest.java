/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.as4.model.pmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for class {@link PModeParty}
 *
 * @author Philip Helger
 */
public final class PModePartyTest
{
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
    assertEquals (p, new PModeParty ("a", "b", "c", "d", "e"));
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
    assertEquals (p, PModeParty.createSimple ("b", "c"));
  }
}
