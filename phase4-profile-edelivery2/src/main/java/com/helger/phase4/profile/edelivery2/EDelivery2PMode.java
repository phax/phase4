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
package com.helger.phase4.profile.edelivery2;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.state.ETriState;
import com.helger.phase4.CAS4;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.model.pmode.PModePayloadService;
import com.helger.phase4.model.pmode.PModeReceptionAwareness;
import com.helger.phase4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.phase4.model.pmode.leg.PModeAddressList;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.phase4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.phase4.model.pmode.leg.PModeLegProtocol;
import com.helger.phase4.model.pmode.leg.PModeLegReliability;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;
import com.helger.phase4.wss.EWSSVersion;

/**
 * eDelivery AS4 2.0 PMode creation code.
 *
 * @author Philip Helger
 * @since 4.4.0
 */
@Immutable
public final class EDelivery2PMode
{
  public static final String DEFAULT_AGREEMENT_ID = "urn:as4:agreement";

  private EDelivery2PMode ()
  {}

  @NonNull
  public static PModeLegProtocol generatePModeLegProtocol (@Nullable final String sAddress)
  {
    return PModeLegProtocol.createForDefaultSoapVersion (sAddress);
  }

  @NonNull
  public static PModeLegBusinessInformation generatePModeLegBusinessInformation ()
  {
    final String sService = null;
    final String sAction = CAS4.DEFAULT_ACTION_URL;
    final Long nPayloadProfileMaxKB = null;
    final String sMPCID = CAS4.DEFAULT_MPC_ID;
    return PModeLegBusinessInformation.create (sService, sAction, nPayloadProfileMaxKB, sMPCID);
  }

  @NonNull
  public static PModeLegErrorHandling generatePModeLegErrorHandling ()
  {
    final PModeAddressList aReportSenderErrorsTo = null;
    final PModeAddressList aReportReceiverErrorsTo = null;
    final ETriState eReportAsResponse = ETriState.TRUE;
    final ETriState eReportProcessErrorNotifyConsumer = ETriState.TRUE;
    final ETriState eReportProcessErrorNotifyProducer = ETriState.TRUE;
    final ETriState eReportDeliveryFailuresNotifyProducer = ETriState.TRUE;
    return new PModeLegErrorHandling (aReportSenderErrorsTo,
                                      aReportReceiverErrorsTo,
                                      eReportAsResponse,
                                      eReportProcessErrorNotifyConsumer,
                                      eReportProcessErrorNotifyProducer,
                                      eReportDeliveryFailuresNotifyProducer);
  }

  /**
   * Generate PMode leg security for the Common Usage Profile (EdDSA/X25519).
   *
   * @return Never <code>null</code>.
   */
  @NonNull
  public static PModeLegSecurity generatePModeLegSecurityEdDSA ()
  {
    final PModeLegSecurity aPModeLegSecurity = new PModeLegSecurity ();
    aPModeLegSecurity.setWSSVersion (EWSSVersion.WSS_111);
    aPModeLegSecurity.setX509SignatureAlgorithm (ECryptoAlgorithmSign.EDDSA_ED25519);
    aPModeLegSecurity.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);
    aPModeLegSecurity.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM);
    aPModeLegSecurity.setX509EncryptionMinimumStrength (128);
    aPModeLegSecurity.setPModeAuthorize (false);
    aPModeLegSecurity.setSendReceipt (true);
    aPModeLegSecurity.setSendReceiptNonRepudiation (true);
    aPModeLegSecurity.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.RESPONSE);
    return aPModeLegSecurity;
  }

  /**
   * Generate PMode leg security for the Alternative Elliptic Curve Profile (ECDSA/ECDH-ES with
   * secp256r1).
   *
   * @return Never <code>null</code>.
   */
  @NonNull
  public static PModeLegSecurity generatePModeLegSecurityECDSA ()
  {
    final PModeLegSecurity aPModeLegSecurity = new PModeLegSecurity ();
    aPModeLegSecurity.setWSSVersion (EWSSVersion.WSS_111);
    aPModeLegSecurity.setX509SignatureAlgorithm (ECryptoAlgorithmSign.ECDSA_SHA_256);
    aPModeLegSecurity.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);
    aPModeLegSecurity.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM);
    aPModeLegSecurity.setX509EncryptionMinimumStrength (128);
    aPModeLegSecurity.setPModeAuthorize (false);
    aPModeLegSecurity.setSendReceipt (true);
    aPModeLegSecurity.setSendReceiptNonRepudiation (true);
    aPModeLegSecurity.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.RESPONSE);
    return aPModeLegSecurity;
  }

  @NonNull
  public static PModeLeg generatePModeLeg (@Nullable final String sResponderAddress,
                                           @NonNull final PModeLegSecurity aSecurity)
  {
    return new PModeLeg (generatePModeLegProtocol (sResponderAddress),
                         generatePModeLegBusinessInformation (),
                         generatePModeLegErrorHandling (),
                         (PModeLegReliability) null,
                         aSecurity);
  }

  @NonNull
  public static PModeReceptionAwareness generatePModeReceptionAwareness ()
  {
    final ETriState eReceptionAwareness = ETriState.TRUE;
    final ETriState eRetry = ETriState.TRUE;
    final int nMaxRetries = 1;
    final long nRetryIntervalMS = 10_000;
    final ETriState eDuplicateDetection = ETriState.TRUE;
    return new PModeReceptionAwareness (eReceptionAwareness,
                                        eRetry,
                                        nMaxRetries,
                                        nRetryIntervalMS,
                                        eDuplicateDetection);
  }

  /**
   * One-Way Version of the eDelivery 2.0 pmode uses one-way push
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
   *        <code>true</code> to persist the PMode in the PModeManager, <code>false</code> to have
   *        it only in memory.
   * @param aSecurity
   *        The security settings to use (EdDSA or ECDSA). May not be <code>null</code>.
   * @return New PMode
   */
  @NonNull
  public static PMode createEDelivery2PMode (@NonNull @Nonempty final String sInitiatorID,
                                             @NonNull @Nonempty final String sResponderID,
                                             @Nullable final String sResponderAddress,
                                             @NonNull final IPModeIDProvider aPModeIDProvider,
                                             final boolean bPersist,
                                             @NonNull final PModeLegSecurity aSecurity)
  {
    final PModeParty aInitiator = PModeParty.createSimple (sInitiatorID, CAS4.DEFAULT_INITIATOR_URL);
    final PModeParty aResponder = PModeParty.createSimple (sResponderID, CAS4.DEFAULT_RESPONDER_URL);

    final PMode aPMode = new PMode (aPModeIDProvider.getPModeID (aInitiator, aResponder),
                                    aInitiator,
                                    aResponder,
                                    DEFAULT_AGREEMENT_ID,
                                    EMEP.ONE_WAY,
                                    EMEPBinding.PUSH,
                                    generatePModeLeg (sResponderAddress, aSecurity),
                                    (PModeLeg) null,
                                    (PModePayloadService) null,
                                    generatePModeReceptionAwareness ());

    if (bPersist)
    {
      MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);
    }
    return aPMode;
  }

  /**
   * Two-Way Version of the eDelivery 2.0 pmode uses two-way push-push
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
   *        <code>true</code> to persist the PMode <code>false</code> to have it only in memory.
   * @param aSecurity
   *        The security settings to use (EdDSA or ECDSA). May not be <code>null</code>.
   * @return New PMode
   */
  @NonNull
  public static PMode createEDelivery2PModeTwoWay (@NonNull @Nonempty final String sInitiatorID,
                                                   @NonNull @Nonempty final String sResponderID,
                                                   @Nullable final String sResponderAddress,
                                                   @NonNull final IPModeIDProvider aPModeIDProvider,
                                                   final boolean bPersist,
                                                   @NonNull final PModeLegSecurity aSecurity)
  {
    final PModeParty aInitiator = PModeParty.createSimple (sInitiatorID, CAS4.DEFAULT_INITIATOR_URL);
    final PModeParty aResponder = PModeParty.createSimple (sResponderID, CAS4.DEFAULT_RESPONDER_URL);

    final PMode aPMode = new PMode (aPModeIDProvider.getPModeID (aInitiator, aResponder),
                                    aInitiator,
                                    aResponder,
                                    DEFAULT_AGREEMENT_ID,
                                    EMEP.TWO_WAY,
                                    EMEPBinding.PUSH_PUSH,
                                    generatePModeLeg (sResponderAddress, aSecurity),
                                    generatePModeLeg (sResponderAddress, aSecurity),
                                    (PModePayloadService) null,
                                    PModeReceptionAwareness.createDefault ());
    if (bPersist)
    {
      MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);
    }
    return aPMode;
  }
}
