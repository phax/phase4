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
package com.helger.phase4.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.base.string.StringHelper;

/**
 * Test class for class {@link EMEP}.
 *
 * @author Philip Helger
 */
public final class EMEPTest
{
  @Test
  public void testBasic ()
  {
    for (final EMEP e : EMEP.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getID ()));
      assertTrue (StringHelper.isNotEmpty (e.getURI ()));
      assertSame (e, EMEP.getFromIDOrNull (e.getID ()));
      assertSame (e, EMEP.getFromURIOrNull (e.getURI ()));
      assertTrue (e.getMessageCount () > 0);
    }
  }

  @Test
  public void testOneWay ()
  {
    assertTrue (EMEP.ONE_WAY.isOneWay ());
    assertFalse (EMEP.ONE_WAY.isTwoWay ());
    assertEquals (1, EMEP.ONE_WAY.getMessageCount ());
  }

  @Test
  public void testTwoWay ()
  {
    assertFalse (EMEP.TWO_WAY.isOneWay ());
    assertTrue (EMEP.TWO_WAY.isTwoWay ());
    assertEquals (2, EMEP.TWO_WAY.getMessageCount ());
  }

  @Test
  public void testDefault ()
  {
    assertNotNull (EMEP.DEFAULT_EBMS);
    assertSame (EMEP.ONE_WAY, EMEP.DEFAULT_EBMS);
  }

  @Test
  public void testUnknown ()
  {
    assertNull (EMEP.getFromIDOrNull ("does-not-exist"));
    assertNull (EMEP.getFromIDOrNull (null));
    assertNull (EMEP.getFromURIOrNull (null));
    assertNull (EMEP.getFromURIOrNull (""));
  }
}
