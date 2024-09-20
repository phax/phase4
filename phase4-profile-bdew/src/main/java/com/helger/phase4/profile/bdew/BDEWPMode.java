/*
 * Copyright (C) 2023-2024 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
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
package com.helger.phase4.profile.bdew;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsSet;
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
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;
import com.helger.phase4.wss.EWSSVersion;

/**
 * PMode creation code.
 *
 * @author Gregor Scholtysik
 * @author Philip Helger
 * @since 2.1.0
 */
@Immutable
public final class BDEWPMode
{
  // Default per section 2.3.2
  public static final String DEFAULT_AGREEMENT_ID = "https://www.bdew.de/as4/communication/agreement";

  public static final String BDEW_PARTY_ID_TYPE_GLN = "urn:oasis:names:tc:ebcore:partyid-type:iso6523:0088";
  public static final String BDEW_PARTY_ID_TYPE_BDEW = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:BDEW";
  public static final String BDEW_PARTY_ID_TYPE_DVGW = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:DVGW";
  public static final String BDEW_PARTY_ID_TYPE_BAHN = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:BAHN";

  public static final String SERVICE_TEST = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/service";
  public static final String SERVICE_PATH_SWITCH = "https://www.bdew.de/as4/communication/services/pathSwitch";
  public static final String SERVICE_MARKTPROZESSE = "https://www.bdew.de/as4/communication/services/MP";
  public static final String SERVICE_FAHRPLAN = "https://www.bdew.de/as4/communication/services/FP";
  public static final String SERVICE_REDISPATCH_2_0 = "https://www.bdew.de/as4/communication/services/RD";
  public static final String SERVICE_KWEP = "https://www.bdew.de/as4/communication/services/KW";
  public static final String SERVICE_SOGL = "https://www.bdew.de/as4/communication/services/SO";

  public static final String ACTION_DEFAULT = "http://docs.oasis-open.org/ebxml-msg/as4/200902/action";
  public static final String ACTION_TEST_SERVICE = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/test";
  public static final String ACTION_REQUEST_SWITCH = "https://www.bdew.de/as4/communication/actions/requestSwitch";
  public static final String ACTION_CONFIRM_SWITCH = "https://www.bdew.de/as4/communication/actions/confirmSwitch";

  private static final ICommonsSet <String> ALL_SERVICES = new CommonsHashSet <> (SERVICE_TEST,
                                                                                  SERVICE_PATH_SWITCH,
                                                                                  SERVICE_MARKTPROZESSE,
                                                                                  SERVICE_FAHRPLAN,
                                                                                  SERVICE_REDISPATCH_2_0,
                                                                                  SERVICE_KWEP,
                                                                                  SERVICE_SOGL);
  private static final ICommonsSet <String> ALL_ACTIONS = new CommonsHashSet <> (ACTION_DEFAULT,
                                                                                 ACTION_TEST_SERVICE,
                                                                                 ACTION_REQUEST_SWITCH,
                                                                                 ACTION_CONFIRM_SWITCH);

  private BDEWPMode ()
  {}

  @Nonnull
  public static PModeLegProtocol generatePModeLegProtocol (@Nullable final String sAddress)
  {
    // Set the endpoint URL
    return PModeLegProtocol.createForDefaultSoapVersion (sAddress);
  }

  @Nonnull
  public static PModeLegBusinessInformation generatePModeLegBusinessInformation ()
  {
    return generatePModeLegBusinessInformation (null, CAS4.DEFAULT_ACTION_URL);
  }

  @Nonnull
  public static PModeLegBusinessInformation generatePModeLegBusinessInformation (@Nullable final String sService,
                                                                                 @Nullable final String sAction)
  {
    final Long nPayloadProfileMaxKB = null;
    final String sMPCID = CAS4.DEFAULT_MPC_ID;
    return PModeLegBusinessInformation.create (sService, sAction, nPayloadProfileMaxKB, sMPCID);
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
    aPModeLegSecurity.setX509SignatureAlgorithm (ECryptoAlgorithmSign.ECDSA_SHA_256);
    // Curve to use: "brainpoolP256r1"
    // Source: BSI TR03116-3, section 9.1
    // Required by: BDEW AS4 profile section 2.2.6.2.1
    aPModeLegSecurity.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);
    aPModeLegSecurity.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM);
    aPModeLegSecurity.setX509EncryptionMinimumStrength (128);
    aPModeLegSecurity.setPModeAuthorize (false);
    aPModeLegSecurity.setSendReceipt (true);
    aPModeLegSecurity.setSendReceiptNonRepudiation (true);
    aPModeLegSecurity.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.RESPONSE);
    return aPModeLegSecurity;
  }

  @Nonnull
  public static PModeLeg generatePModeLeg (@Nullable final String sResponderAddress,
                                           @Nullable final String sService,
                                           @Nullable final String sAction)
  {
    return new PModeLeg (generatePModeLegProtocol (sResponderAddress),
                         generatePModeLegBusinessInformation (sService, sAction),
                         generatePModeLegErrorHandling (),
                         null,
                         generatePModeLegSecurity ());
  }

  @Nonnull
  public static PModePayloadService generatePModePayloadSevice ()
  {
    return new PModePayloadService (EAS4CompressionMode.GZIP);
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

  /**
   * One-Way Version of the BDEW pmode uses one-way push
   *
   * @param sInitiatorID
   *        Initiator ID
   * @param sInitiatorType
   *        Initiator type ID
   * @param sResponderID
   *        Responder ID
   * @param sResponderType
   *        Responder type ID
   * @param sResponderAddress
   *        Responder URL
   * @param aPModeIDProvider
   *        PMode ID provider
   * @param bPersist
   *        <code>true</code> to persist the PMode in the PModeManager,
   *        <code>false</code> to have it only in memory.
   * @return New PMode
   */
  @Nonnull
  public static PMode createBDEWPMode (@Nonnull @Nonempty final String sInitiatorID,
                                       @Nonnull @Nonempty final String sInitiatorType,
                                       @Nonnull @Nonempty final String sResponderID,
                                       @Nonnull @Nonempty final String sResponderType,
                                       @Nullable final String sResponderAddress,
                                       @Nonnull final IPModeIDProvider aPModeIDProvider,
                                       final boolean bPersist)
  {
    final String sService = null;
    final String sAction = null;
    return createBDEWPMode (sInitiatorID,
                            sInitiatorType,
                            sResponderID,
                            sResponderType,
                            sService,
                            sAction,
                            sResponderAddress,
                            aPModeIDProvider,
                            bPersist);
  }

  /**
   * One-Way Version of the BDEW pmode uses one-way push
   *
   * @param sInitiatorID
   *        Initiator ID
   * @param sInitiatorType
   *        Initiator type ID
   * @param sResponderID
   *        Responder ID
   * @param sResponderType
   *        Responder type ID
   * @param sService
   *        The service value
   * @param sAction
   *        The action value
   * @param sResponderAddress
   *        Responder URL
   * @param aPModeIDProvider
   *        PMode ID provider
   * @param bPersist
   *        <code>true</code> to persist the PMode in the PModeManager,
   *        <code>false</code> to have it only in memory.
   * @return New PMode
   * @since 2.8.0
   */
  @Nonnull
  public static PMode createBDEWPMode (@Nonnull @Nonempty final String sInitiatorID,
                                       @Nonnull @Nonempty final String sInitiatorType,
                                       @Nonnull @Nonempty final String sResponderID,
                                       @Nonnull @Nonempty final String sResponderType,
                                       @Nullable final String sService,
                                       @Nullable final String sAction,
                                       @Nullable final String sResponderAddress,
                                       @Nonnull final IPModeIDProvider aPModeIDProvider,
                                       final boolean bPersist)
  {
    final PModeParty aInitiator = new PModeParty (sInitiatorType, sInitiatorID, CAS4.DEFAULT_INITIATOR_URL, null, null);
    final PModeParty aResponder = new PModeParty (sResponderType, sResponderID, CAS4.DEFAULT_RESPONDER_URL, null, null);

    final PMode aPMode = new PMode (aPModeIDProvider.getPModeID (aInitiator, aResponder),
                                    aInitiator,
                                    aResponder,
                                    DEFAULT_AGREEMENT_ID,
                                    EMEP.ONE_WAY,
                                    EMEPBinding.PUSH,
                                    generatePModeLeg (sResponderAddress, sService, sAction),
                                    null,
                                    generatePModePayloadSevice (),
                                    generatePModeReceptionAwareness ());

    // Leg 2 stays null, because we only use one-way

    if (bPersist)
    {
      // Ensure it is stored
      MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);
    }
    return aPMode;
  }

  public static boolean containsService (@Nonnull final String sService)
  {
    return ALL_SERVICES.contains (sService);
  }

  public static boolean containsAction (@Nonnull final String sAction)
  {
    return ALL_ACTIONS.contains (sAction);
  }
}
