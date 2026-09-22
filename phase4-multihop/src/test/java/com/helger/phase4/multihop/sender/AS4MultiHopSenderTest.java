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
package com.helger.phase4.multihop.sender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.helger.phase4.CAS4;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.multihop.AS4MultiHopConfig;
import com.helger.phase4.multihop.CAS4MultiHop;
import com.helger.phase4.multihop.incoming.MultiHopSentMessageStore;
import com.helger.phase4.sender.AS4Sender;
import com.helger.phase4.sender.EAS4UserMessageSendResult;

/**
 * Test class for {@link AS4MultiHopSender}.<br>
 * Covers R11 (the PMode ID in AgreementRef) and D10 (the send outcome interpretation).
 *
 * @author Philip Helger
 */
public final class AS4MultiHopSenderTest
{
  /**
   * R11 - eb:AgreementRef/@pmode must carry the PMode ID without the .init / .resp suffix.
   */
  @Test
  public void testAgreementPModeIDStripsTheSuffix ()
  {
    assertEquals ("Pmode-1", AS4MultiHopSender.getAgreementPModeID ("Pmode-1" + CAS4MultiHop.PMODE_SUFFIX_INIT));
    assertEquals ("Pmode-1", AS4MultiHopSender.getAgreementPModeID ("Pmode-1" + CAS4MultiHop.PMODE_SUFFIX_RESP));

    // No suffix - unchanged
    assertEquals ("Pmode-1", AS4MultiHopSender.getAgreementPModeID ("Pmode-1"));

    // Only the trailing suffix is stripped, and only once
    assertEquals ("a.init", AS4MultiHopSender.getAgreementPModeID ("a.init.resp"));
    assertEquals ("a.initx", AS4MultiHopSender.getAgreementPModeID ("a.initx"));
  }

  /**
   * R11 - null and empty are passed through unchanged.
   */
  @Test
  public void testAgreementPModeIDEdgeCases ()
  {
    assertNull (CAS4MultiHop.getAgreementPModeID (null));
    assertEquals ("", CAS4MultiHop.getAgreementPModeID (""));
  }

  /**
   * D10 - a synchronous Receipt means success.
   */
  @Test
  public void testInterpretSyncSuccess ()
  {
    final MultiHopSendOutcomeHolder aHolder = new MultiHopSendOutcomeHolder ();
    aHolder.setResponse (200, true);

    assertSame (EAS4MultiHopSendOutcome.SIGNAL_RECEIVED_SYNC,
                AS4MultiHopSender.interpret (EAS4UserMessageSendResult.SUCCESS, aHolder));
  }

  /**
   * D10 - an HTTP 2xx with an empty body from the edge intermediary means the message was
   * accepted and the Receipt will arrive asynchronously. phase4 itself reports
   * NO_SIGNAL_MESSAGE_RECEIVED for that.
   */
  @Test
  public void testInterpretEmpty2xxIsAsyncAccepted ()
  {
    for (final int nStatus : new int [] { 200, 202, 204, 299 })
    {
      final MultiHopSendOutcomeHolder aHolder = new MultiHopSendOutcomeHolder ();
      aHolder.setResponse (nStatus, false);

      assertSame ("HTTP " + nStatus + " with an empty body must be ACCEPTED_BY_ICLOUD_ASYNC",
                  EAS4MultiHopSendOutcome.ACCEPTED_BY_ICLOUD_ASYNC,
                  AS4MultiHopSender.interpret (EAS4UserMessageSendResult.NO_SIGNAL_MESSAGE_RECEIVED, aHolder));
    }
  }

  /**
   * D10 - everything else is a failure.
   */
  @Test
  public void testInterpretFailures ()
  {
    // Non-2xx with an empty body
    final MultiHopSendOutcomeHolder aHolder1 = new MultiHopSendOutcomeHolder ();
    aHolder1.setResponse (500, false);
    assertSame (EAS4MultiHopSendOutcome.FAILED,
                AS4MultiHopSender.interpret (EAS4UserMessageSendResult.NO_SIGNAL_MESSAGE_RECEIVED, aHolder1));

    // 2xx but with content that could not be parsed as a signal message
    final MultiHopSendOutcomeHolder aHolder2 = new MultiHopSendOutcomeHolder ();
    aHolder2.setResponse (200, true);
    assertSame (EAS4MultiHopSendOutcome.FAILED,
                AS4MultiHopSender.interpret (EAS4UserMessageSendResult.NO_SIGNAL_MESSAGE_RECEIVED, aHolder2));

    // An AS4 Error was received
    final MultiHopSendOutcomeHolder aHolder3 = new MultiHopSendOutcomeHolder ();
    aHolder3.setResponse (200, true);
    assertSame (EAS4MultiHopSendOutcome.FAILED,
                AS4MultiHopSender.interpret (EAS4UserMessageSendResult.AS4_ERROR_MESSAGE_RECEIVED, aHolder3));

    // No response at all
    assertSame (EAS4MultiHopSendOutcome.FAILED,
                AS4MultiHopSender.interpret (EAS4UserMessageSendResult.TRANSPORT_ERROR,
                                             new MultiHopSendOutcomeHolder ()));
  }

  /**
   * Correction 3 - phase4 only emits eb:AgreementRef, and therefore @pmode, if the AgreementRef
   * value is non-empty. configure() must say so instead of silently producing a message without
   * the PMode ID.
   */
  @Test
  public void testConfigureRequiresAnAgreementRef ()
  {
    final PMode aPMode = new PMode ("the-pmode" + CAS4MultiHop.PMODE_SUFFIX_INIT,
                                    PModeParty.createSimple ("initiator", CAS4.DEFAULT_INITIATOR_URL),
                                    PModeParty.createSimple ("responder", CAS4.DEFAULT_RESPONDER_URL),
                                    "urn:as4:agreement",
                                    EMEP.ONE_WAY,
                                    EMEPBinding.PUSH,
                                    null,
                                    null,
                                    null,
                                    null);

    final AS4MultiHopConfig aCfg = new AS4MultiHopConfig ();
    aCfg.addAddActorOrRoleAttributePModeID (aPMode.getID ());

    try
    {
      AS4MultiHopSender.configure (AS4Sender.builderUserMessage (),
                                   aPMode,
                                   new MultiHopSendOutcomeHolder (),
                                   aCfg,
                                   new MultiHopSentMessageStore (aCfg));
      fail ("configure() must fail without an AgreementRef value");
    }
    catch (final IllegalStateException ex)
    {
      assertTrue (ex.getMessage ().contains ("AgreementRef"));
    }
  }

  /**
   * R11 - with an AgreementRef value set, configure() sets the stripped PMode ID on the builder
   * while the full PMode is used for the message itself.
   */
  @Test
  public void testConfigureStripsThePModeIDSuffix ()
  {
    final String sFullPModeID = "the-pmode" + CAS4MultiHop.PMODE_SUFFIX_INIT;
    final PMode aPMode = new PMode (sFullPModeID,
                                    PModeParty.createSimple ("initiator", CAS4.DEFAULT_INITIATOR_URL),
                                    PModeParty.createSimple ("responder", CAS4.DEFAULT_RESPONDER_URL),
                                    "urn:as4:agreement",
                                    EMEP.ONE_WAY,
                                    EMEPBinding.PUSH,
                                    null,
                                    null,
                                    null,
                                    null);

    final AS4MultiHopConfig aCfg = new AS4MultiHopConfig ();
    aCfg.addAddActorOrRoleAttributePModeID (sFullPModeID);

    final AS4Sender.BuilderUserMessage aBuilder = AS4Sender.builderUserMessage ()
                                                           .agreementRef ("urn:as4:agreement");
    AS4MultiHopSender.configure (aBuilder,
                                 aPMode,
                                 new MultiHopSendOutcomeHolder (),
                                 aCfg,
                                 new MultiHopSentMessageStore (aCfg));

    // R11 - the AgreementRef carries the ID without the suffix
    assertEquals ("the-pmode", aBuilder.pmodeID ());
    // ... while the message itself uses the full PMode
    assertSame (aPMode, aBuilder.pmode ());
  }
}
