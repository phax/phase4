/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.incoming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.CAS4;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.model.pmode.resolve.IAS4PModeResolver;
import com.helger.phase4.multihop.CAS4MultiHop;

/**
 * Test class for {@link MultiHopPModeResolver}.<br>
 * Covers D7 - resolving a PMode ID that lost its .init / .resp suffix.
 *
 * @author Philip Helger
 */
public final class MultiHopPModeResolverTest
{
  /**
   * A resolver that only knows the PMode IDs it was constructed with, and records every lookup.
   */
  private static final class MockResolver implements IAS4PModeResolver
  {
    private final ICommonsList <String> m_aKnownIDs;
    final ICommonsList <String> m_aRequestedIDs = new CommonsArrayList <> ();

    MockResolver (@NonNull final String... aKnownIDs)
    {
      m_aKnownIDs = new CommonsArrayList <> (aKnownIDs);
    }

    public IPMode findPMode (final String sPModeID,
                             @NonNull final String sService,
                             @NonNull final String sAction,
                             @NonNull final String sInitiatorID,
                             @NonNull final String sResponderID,
                             final String sAgreementRef,
                             final String sAddress)
    {
      m_aRequestedIDs.add (sPModeID);
      if (sPModeID == null || !m_aKnownIDs.contains (sPModeID))
        return null;

      return new PMode (sPModeID,
                        PModeParty.createSimple ("i", CAS4.DEFAULT_INITIATOR_URL),
                        PModeParty.createSimple ("r", CAS4.DEFAULT_RESPONDER_URL),
                        "urn:as4:agreement",
                        EMEP.ONE_WAY,
                        EMEPBinding.PUSH,
                        null,
                        null,
                        null,
                        null);
    }
  }

  private static IPMode _find (@NonNull final IAS4PModeResolver aResolver, final String sPModeID)
  {
    return aResolver.findPMode (sPModeID, "s", "a", "i", "r", "ref", null);
  }

  /**
   * D7 - an exact match is returned without trying any suffix.
   */
  @Test
  public void testExactMatchWins ()
  {
    final MockResolver aMock = new MockResolver ("the-pmode");
    final IPMode aPMode = _find (new MultiHopPModeResolver (aMock), "the-pmode");

    assertNotNull (aPMode);
    assertEquals ("the-pmode", aPMode.getID ());
    assertEquals ("Only one lookup should have been made", 1, aMock.m_aRequestedIDs.size ());
  }

  /**
   * D7 - the .resp unit is tried before the .init unit.
   */
  @Test
  public void testRespSuffixIsTriedFirst ()
  {
    final MockResolver aMock = new MockResolver ("the-pmode" + CAS4MultiHop.PMODE_SUFFIX_RESP,
                                                 "the-pmode" + CAS4MultiHop.PMODE_SUFFIX_INIT);
    final IPMode aPMode = _find (new MultiHopPModeResolver (aMock), "the-pmode");

    assertNotNull (aPMode);
    assertEquals ("the-pmode" + CAS4MultiHop.PMODE_SUFFIX_RESP, aPMode.getID ());
    assertEquals (new CommonsArrayList <> ("the-pmode", "the-pmode" + CAS4MultiHop.PMODE_SUFFIX_RESP),
                  aMock.m_aRequestedIDs);
  }

  /**
   * D7 - the .init unit is used when there is no .resp unit.
   */
  @Test
  public void testInitSuffixIsTriedSecond ()
  {
    final MockResolver aMock = new MockResolver ("the-pmode" + CAS4MultiHop.PMODE_SUFFIX_INIT);
    final IPMode aPMode = _find (new MultiHopPModeResolver (aMock), "the-pmode");

    assertNotNull (aPMode);
    assertEquals ("the-pmode" + CAS4MultiHop.PMODE_SUFFIX_INIT, aPMode.getID ());
    assertEquals (3, aMock.m_aRequestedIDs.size ());
  }

  /**
   * D7 - an unknown ID stays unresolved.
   */
  @Test
  public void testUnknownStaysNull ()
  {
    final MockResolver aMock = new MockResolver ("something-else");
    assertNull (_find (new MultiHopPModeResolver (aMock), "the-pmode"));
    assertEquals (3, aMock.m_aRequestedIDs.size ());
  }

  /**
   * D7 - with no PMode ID at all, the delegate is asked exactly once. It then falls back to
   * Service and Action on its own, and adding a suffix would make no sense.
   */
  @Test
  public void testNullPModeIDIsPassedThroughOnce ()
  {
    final MockResolver aMock = new MockResolver ("the-pmode");
    assertNull (_find (new MultiHopPModeResolver (aMock), null));
    assertEquals (1, aMock.m_aRequestedIDs.size ());
  }

  @Test
  public void testGetDelegate ()
  {
    final MockResolver aMock = new MockResolver ();
    assertSame (aMock, new MultiHopPModeResolver (aMock).getDelegate ());
  }
}
