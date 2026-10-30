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
package com.helger.phase4.multihop.integration;

import org.jspecify.annotations.NonNull;

import com.helger.base.state.ETriState;
import com.helger.phase4.CAS4;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.model.pmode.PModePayloadService;
import com.helger.phase4.model.pmode.PModeReceptionAwareness;
import com.helger.phase4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.phase4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.phase4.model.pmode.leg.PModeLegProtocol;
import com.helger.phase4.model.pmode.leg.PModeLegReliability;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;
import com.helger.phase4.wss.EWSSVersion;

/**
 * Creates the PMode used by the multi-hop integration tests. It is a signing-only CEF style PMode
 * - encryption adds nothing to what multi-hop needs to prove.
 *
 * @author Philip Helger
 */
public final class MultiHopTestPModes
{
  /** The endpoint address the test PMode responds to */
  public static final String ENDPOINT_ADDRESS = "http://localhost-dummy/as4";

  private MultiHopTestPModes ()
  {}

  @NonNull
  private static PModeLegSecurity _createSecurity ()
  {
    final PModeLegSecurity ret = new PModeLegSecurity ();
    ret.setWSSVersion (EWSSVersion.WSS_111);
    ret.setX509SignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_256);
    ret.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);
    // No encryption - not needed to prove anything about multi-hop
    ret.setPModeAuthorize (false);
    ret.setSendReceipt (true);
    ret.setSendReceiptNonRepudiation (true);
    ret.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.RESPONSE);
    return ret;
  }

  @NonNull
  private static PModeLegErrorHandling _createErrorHandling ()
  {
    return new PModeLegErrorHandling (null, null, ETriState.TRUE, ETriState.TRUE, ETriState.TRUE, ETriState.TRUE);
  }

  @NonNull
  private static PModeLeg _createLeg ()
  {
    return new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion (ENDPOINT_ADDRESS),
                         PModeLegBusinessInformation.create ("svc", "theAction", null, CAS4.DEFAULT_MPC_ID),
                         _createErrorHandling (),
                         (PModeLegReliability) null,
                         _createSecurity ());
  }

  /**
   * Create the test PMode and store it in the PMode manager.
   *
   * @param sPModeID
   *        The ID to register the PMode under. To exercise D7 this is deliberately the
   *        <code>.resp</code> unit ID, while the messages carry the ID without the suffix.
   * @param sInitiatorID
   *        The initiator party ID.
   * @param sResponderID
   *        The responder party ID.
   * @return The created and stored PMode. Never <code>null</code>.
   */
  @NonNull
  public static PMode createAndStore (@NonNull final String sPModeID,
                                      @NonNull final String sInitiatorID,
                                      @NonNull final String sResponderID)
  {
    final PMode ret = new PMode (sPModeID,
                                 PModeParty.createSimple (sInitiatorID, CAS4.DEFAULT_INITIATOR_URL),
                                 PModeParty.createSimple (sResponderID, CAS4.DEFAULT_RESPONDER_URL),
                                 "urn:as4:agreement",
                                 EMEP.ONE_WAY,
                                 EMEPBinding.PUSH,
                                 _createLeg (),
                                 (PModeLeg) null,
                                 (PModePayloadService) null,
                                 PModeReceptionAwareness.createDefault ());
    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (ret);
    return ret;
  }
}
