/*
 * Copyright (C) 2019-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.euctp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.state.ETriState;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.EAS4CompressionMode;
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
 * PMode creation code.
 *
 * @author Philip Helger
 */
@Immutable
public final class EuCtpPMode
{
  public static final String DEFAULT_AGREEMENT_ID = "EU-ICS2-TI-V2.0";
  public static final String DEFAULT_AGREEMENT_TYPE = "ics2-interface-version";
  public static final String DEFAULT_PARTY_TYPE_ID = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:eu-customs:EORI";
  public static final String DEFAULT_CUSTOMS_PARTY_TYPE_ID = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:eu-customs:auth";

  // required for connectivity tests
  public static final String ACTION_TEST = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/test";
  public static final String DEFAULT_SERVICE = EEuCtpService.TRADER_TO_CUSTOMS.getValue ();
  public static final String DEFAULT_SERVICE_TYPE = "eu-customs-service-type";

  public static final boolean DEFAULT_SEND_RECEIPT = true;
  public static final boolean DEFAULT_SEND_RECEIPT_NON_REPUDIATION = true;

  private EuCtpPMode ()
  {}

  /**
   * @param sAddress
   *        The endpoint address URL. Maybe <code>null</code>.
   * @return The new {@link PModeLegProtocol}. Never <code>null</code>.
   */
  @Nonnull
  public static PModeLegProtocol generatePModeLegProtocol (@Nullable final String sAddress)
  {
    // Set the endpoint URL
    return PModeLegProtocol.createForDefaultSoapVersion (sAddress);
  }

  @Nonnull
  public static PModeLegBusinessInformation generatePModeLegBusinessInformation ()
  {
    // Process ID
    final String sService = DEFAULT_SERVICE;
    final String sServiceType = DEFAULT_SERVICE_TYPE;
    // Document type ID
    final String sAction = null;
    final Long nPayloadProfileMaxKB = null;
    final String sMPCID = CAS4.DEFAULT_MPC_ID;
    return new PModeLegBusinessInformation (sService, sServiceType, sAction, null, null, nPayloadProfileMaxKB, sMPCID);
  }

  @Nonnull
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

  @Nonnull
  public static PModeLegSecurity generatePModeLegSecurity ()
  {
    final PModeLegSecurity aPModeLegSecurity = new PModeLegSecurity ();
    aPModeLegSecurity.setWSSVersion (EWSSVersion.WSS_111);
    aPModeLegSecurity.setX509SignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_256);
    aPModeLegSecurity.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);
    aPModeLegSecurity.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM);
    aPModeLegSecurity.setX509EncryptionMinimumStrength (128);
    aPModeLegSecurity.setPModeAuthorize (false);
    aPModeLegSecurity.setSendReceipt (DEFAULT_SEND_RECEIPT);
    aPModeLegSecurity.setSendReceiptNonRepudiation (DEFAULT_SEND_RECEIPT_NON_REPUDIATION);
    aPModeLegSecurity.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.RESPONSE);
    return aPModeLegSecurity;
  }

  /**
   * @param sAddress
   *        The endpoint address URL. Maybe <code>null</code>.
   * @return The new {@link PModeLeg}. Never <code>null</code>.
   * @see #generatePModeLegProtocol(String)
   */
  @Nonnull
  public static PModeLeg generatePModeLeg (@Nullable final String sAddress)
  {
    return new PModeLeg (generatePModeLegProtocol (sAddress),
                         generatePModeLegBusinessInformation (),
                         generatePModeLegErrorHandling (),
                         (PModeLegReliability) null,
                         generatePModeLegSecurity ());
  }

  @Nonnull
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

  @Nonnull
  public static PModeParty createParty (@Nullable final String sPartyTypeID,
                                        @Nonnull @Nonempty final String sPartyID,
                                        @Nonnull @Nonempty final String sRole)
  {
    // Party type is needed for EuCTP
    return new PModeParty (sPartyTypeID, sPartyID, sRole, null, null);
  }

  @Nonnull
  public static PModePayloadService generatePModePayloadService ()
  {
    return new PModePayloadService (EAS4CompressionMode.GZIP);
  }

  /**
   * One-Way Version of the EuCTP pmode uses one-way push
   *
   * @param sInitiatorID
   *        Initiator ID. May neither be <code>null</code> nor empty.
   * @param sResponderID
   *        Responder ID. May neither be <code>null</code> nor empty.
   * @param sAddress
   *        Endpoint address URL. May be <code>null</code>.
   * @param aPModeIDProvider
   *        PMode ID provider. May not be <code>null</code>.
   * @param bPersist
   *        <code>true</code> to persist the PMode in the PModeManager,
   *        <code>false</code> to have it only in memory.
   * @return New PMode and never <code>null</code>.
   */
  @Nonnull
  public static PMode createEuCtpPushPMode (@Nonnull @Nonempty final String sInitiatorID,
                                            @Nonnull @Nonempty final String sResponderID,
                                            @Nullable final String sAddress,
                                            @Nonnull final IPModeIDProvider aPModeIDProvider,
                                            final boolean bPersist)
  {
    final PModeParty aInitiator = createParty (DEFAULT_PARTY_TYPE_ID, sInitiatorID, CAS4.DEFAULT_INITIATOR_URL);
    final PModeParty aResponder = createParty (DEFAULT_CUSTOMS_PARTY_TYPE_ID, sResponderID, CAS4.DEFAULT_RESPONDER_URL);

    final PMode aPMode = new PMode (aPModeIDProvider.getPModeID (aInitiator, aResponder),
                                    aInitiator,
                                    aResponder,
                                    DEFAULT_AGREEMENT_ID,
                                    EMEP.ONE_WAY,
                                    EMEPBinding.PUSH,
                                    generatePModeLeg (sAddress),
                                    null,
                                    generatePModePayloadService (),
                                    generatePModeReceptionAwareness ());

    if (bPersist)
    {
      // Ensure it is stored
      MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);
    }
    return aPMode;
  }

  /**
   * One-Way Version of the EuCTP pmode uses one-way pull
   *
   * @param sInitiatorID
   *        Initiator ID. May neither be <code>null</code> nor empty.
   * @param sResponderID
   *        Responder ID. May neither be <code>null</code> nor empty.
   * @param sAddress
   *        Endpoint address URL. May be <code>null</code>.
   * @param aPModeIDProvider
   *        PMode ID provider. May not be <code>null</code>.
   * @param bPersist
   *        <code>true</code> to persist the PMode in the PModeManager,
   *        <code>false</code> to have it only in memory.
   * @return New PMode and never <code>null</code>.
   */
  @Nonnull
  public static PMode createEuCtpPullPMode (@Nonnull @Nonempty final String sInitiatorID,
                                            @Nonnull @Nonempty final String sResponderID,
                                            @Nullable final String sAddress,
                                            @Nonnull final IPModeIDProvider aPModeIDProvider,
                                            final boolean bPersist)
  {
    final PModeParty aInitiator = createParty (DEFAULT_PARTY_TYPE_ID, sInitiatorID, CAS4.DEFAULT_INITIATOR_URL);
    final PModeParty aResponder = createParty (DEFAULT_CUSTOMS_PARTY_TYPE_ID, sResponderID, CAS4.DEFAULT_RESPONDER_URL);

    final PMode aPMode = new PMode (aPModeIDProvider.getPModeID (aInitiator, aResponder),
                                    aInitiator,
                                    aResponder,
                                    DEFAULT_AGREEMENT_ID,
                                    EMEP.ONE_WAY,
                                    EMEPBinding.PULL,
                                    generatePModeLeg (sAddress),
                                    generatePModeLeg (sAddress),
                                    generatePModePayloadService (),
                                    generatePModeReceptionAwareness ());

    if (bPersist)
    {
      // Ensure it is stored
      MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);
    }
    return aPMode;
  }
}
