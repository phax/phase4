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
package com.helger.as4.esens;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.as4.CAS4;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.pmode.IPModeIDProvider;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.PModeParty;
import com.helger.as4.model.pmode.PModePayloadService;
import com.helger.as4.model.pmode.PModeReceptionAwareness;
import com.helger.as4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.as4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.as4.model.pmode.leg.PModeLegProtocol;
import com.helger.as4.model.pmode.leg.PModeLegReliability;
import com.helger.as4.model.pmode.leg.PModeLegSecurity;
import com.helger.as4.wss.EWSSVersion;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.state.ETriState;

@Immutable
public final class ESENSPMode
{
  private ESENSPMode ()
  {}

  @Nonnull
  private static PModeLegProtocol _generatePModeLegProtocol (@Nullable final String sAddress)
  {
    return PModeLegProtocol.createForDefaultSOAPVersion (sAddress);
  }

  @Nonnull
  private static PModeLegBusinessInformation _generatePModeLegBusinessInformation ()
  {
    return new PModeLegBusinessInformation (null, CAS4.DEFAULT_ACTION_URL, null, CAS4.DEFAULT_MPC_ID);
  }

  @Nonnull
  private static PModeLegErrorHandling _generatePModeLegErrorHandling ()
  {
    return new PModeLegErrorHandling (null, null, ETriState.TRUE, ETriState.TRUE, ETriState.UNDEFINED, ETriState.TRUE);
  }

  @Nonnull
  public static PModeLegSecurity generatePModeLegSecurity ()
  {
    final PModeLegSecurity aPModeLegSecurity = new PModeLegSecurity ();
    aPModeLegSecurity.setWSSVersion (EWSSVersion.WSS_111);
    aPModeLegSecurity.setX509SignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_256);
    aPModeLegSecurity.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);
    aPModeLegSecurity.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM);
    aPModeLegSecurity.setX509EncryptionMinimumStrength (Integer.valueOf (128));
    aPModeLegSecurity.setPModeAuthorize (false);
    aPModeLegSecurity.setSendReceipt (true);
    aPModeLegSecurity.setSendReceiptNonRepudiation (true);
    aPModeLegSecurity.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.RESPONSE);
    return aPModeLegSecurity;
  }

  /**
   * One-Way Version of the esens pmode uses one-way push
   *
   * @param sInitiatorID
   *        Initiator ID
   * @param sResponderID
   *        Responder ID
   * @param sResponderAddress
   *        Responder URL
   * @param aPModeIDProvider
   *        PMode ID provider
   * @param bPersist
   *        <code>true</code> to persist the PMode <code>false</code> to have it
   *        only in memory.
   * @return New PMode
   */
  @Nonnull
  public static PMode createESENSPMode (@Nonnull @Nonempty final String sInitiatorID,
                                        @Nonnull @Nonempty final String sResponderID,
                                        @Nullable final String sResponderAddress,
                                        @Nonnull final IPModeIDProvider aPModeIDProvider,
                                        final boolean bPersist)
  {
    final PModeParty aInitiator = PModeParty.createSimple (sInitiatorID, CAS4.DEFAULT_SENDER_URL);
    final PModeParty aResponder = PModeParty.createSimple (sResponderID, CAS4.DEFAULT_RESPONDER_URL);

    final PMode aPMode = new PMode (aPModeIDProvider,
                                    aInitiator,
                                    aResponder,
                                    "urn:as4:agreement",
                                    EMEP.ONE_WAY,
                                    EMEPBinding.PUSH,
                                    new PModeLeg (_generatePModeLegProtocol (sResponderAddress),
                                                  _generatePModeLegBusinessInformation (),
                                                  _generatePModeLegErrorHandling (),
                                                  (PModeLegReliability) null,
                                                  generatePModeLegSecurity ()),
                                    (PModeLeg) null,
                                    (PModePayloadService) null,
                                    PModeReceptionAwareness.createDefault ());
    // Leg 2 stays null, because we only use one-way

    if (bPersist)
    {
      // Ensure it is stored
      MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);
    }
    return aPMode;
  }

  /**
   * Two-Way Version of the esens pmode uses two-way push-push
   *
   * @param sInitiatorID
   *        Initiator ID
   * @param sResponderID
   *        Responder ID
   * @param sResponderAddress
   *        Responder URL
   * @param aPModeIDProvider
   *        PMode ID provider
   * @param bPersist
   *        <code>true</code> to persist the PMode <code>false</code> to have it
   *        only in memory.
   * @return New PMode
   */
  @Nonnull
  public static PMode createESENSPModeTwoWay (@Nonnull @Nonempty final String sInitiatorID,
                                              @Nonnull @Nonempty final String sResponderID,
                                              @Nullable final String sResponderAddress,
                                              @Nonnull final IPModeIDProvider aPModeIDProvider,
                                              final boolean bPersist)
  {
    final PModeParty aInitiator = PModeParty.createSimple (sInitiatorID, CAS4.DEFAULT_SENDER_URL);
    final PModeParty aResponder = PModeParty.createSimple (sResponderID, CAS4.DEFAULT_RESPONDER_URL);

    final PMode aPMode = new PMode (aPModeIDProvider,
                                    aInitiator,
                                    aResponder,
                                    "urn:as4:agreement",
                                    EMEP.TWO_WAY,
                                    EMEPBinding.PUSH_PUSH,
                                    new PModeLeg (_generatePModeLegProtocol (sResponderAddress),
                                                  _generatePModeLegBusinessInformation (),
                                                  _generatePModeLegErrorHandling (),
                                                  (PModeLegReliability) null,
                                                  generatePModeLegSecurity ()),
                                    new PModeLeg (_generatePModeLegProtocol (sResponderAddress),
                                                  _generatePModeLegBusinessInformation (),
                                                  _generatePModeLegErrorHandling (),
                                                  (PModeLegReliability) null,
                                                  generatePModeLegSecurity ()),
                                    (PModePayloadService) null,
                                    PModeReceptionAwareness.createDefault ());
    if (bPersist)
    {
      // Ensure it is stored
      MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);
    }
    return aPMode;
  }
}
