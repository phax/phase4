/*
 * Copyright (C) 2015-2021 Pavel Rotek
 * pavel[dot]rotek[at]gmail[dot]com
 *
 * Copyright (C) 2021-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.entsog;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ETriState;
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
public final class ENTSOGPMode
{
  public static final String DEFAULT_AGREEMENT_ID = "urn:as4:agreement";
  /**
   * The prefix of the RECOMMENDED AgreementRef naming convention. See the ENTSOG AS4 Usage Profile,
   * section 2.3.2.
   *
   * @since 4.8.0
   */
  public static final String AGREEMENT_ID_PREFIX = "http://entsog.eu/communication/agreements/";
  /**
   * The fixed value of the <code>type</code> attribute of <code>PartyId</code>, indicating that the
   * value is an EIC code. Note the host name is "entsoe.eu" (ENTSO-E maintains the EIC scheme), not
   * "entsog.eu". See the ENTSOG AS4 Usage Profile, section "Party Identification".
   */
  public static final String ENTSOG_PARTY_ID_TYPE = "http://www.entsoe.eu/eic-codes/eic-party-codes-x";
  /**
   * The default action for business messages, if no specific action is defined in the ENTSOG AS4
   * Mapping Table. See the ENTSOG AS4 Usage Profile, section 2.3.1.2.2.
   *
   * @since 4.8.0
   */
  public static final String ACTION_DEFAULT = "http://docs.oasis-open.org/ebxml-msg/as4/200902/action";
  /**
   * The action to be used for the pre-defined test service. See the ENTSOG AS4 Usage Profile,
   * section 2.3.1.2.2.
   *
   * @since 4.8.0
   */
  public static final String ACTION_TEST_SERVICE = CAS4.DEFAULT_ACTION_URL;

  private ENTSOGPMode ()
  {}

  /**
   * Create the AgreementRef value according to the RECOMMENDED URI naming convention of the ENTSOG
   * AS4 Usage Profile, section 2.3.2:
   * <code>http://entsog.eu/communication/agreements/&lt;EIC_CODE_Party_A&gt;/&lt;EIC_CODE_Party_B&gt;/&lt;version&gt;</code>
   * where Party A is the party whose EIC code alphabetically precedes the one of Party B. Therefore
   * both parties get the same value, independent of the order the EIC codes are passed in.
   *
   * @param sEICCode1
   *        The EIC code of one party. May neither be <code>null</code> nor empty.
   * @param sEICCode2
   *        The EIC code of the other party. May neither be <code>null</code> nor empty.
   * @param nVersion
   *        The agreement version. Initially 1 and incremented for every update. Must be &gt; 0.
   * @return The AgreementRef value and never <code>null</code>.
   * @since 4.8.0
   */
  @NonNull
  @Nonempty
  public static String createAgreementID (@NonNull @Nonempty final String sEICCode1,
                                          @NonNull @Nonempty final String sEICCode2,
                                          final int nVersion)
  {
    ValueEnforcer.notEmpty (sEICCode1, "EICCode1");
    ValueEnforcer.notEmpty (sEICCode2, "EICCode2");
    ValueEnforcer.isGT0 (nVersion, "Version");

    final boolean bInOrder = sEICCode1.compareTo (sEICCode2) <= 0;
    final String sPartyA = bInOrder ? sEICCode1 : sEICCode2;
    final String sPartyB = bInOrder ? sEICCode2 : sEICCode1;
    return AGREEMENT_ID_PREFIX + sPartyA + "/" + sPartyB + "/" + nVersion;
  }

  @NonNull
  public static PModeLegProtocol generatePModeLegProtocol (@Nullable final String sAddress)
  {
    // Set the endpoint URL
    return PModeLegProtocol.createForDefaultSoapVersion (sAddress);
  }

  @NonNull
  public static PModeLegBusinessInformation generatePModeLegBusinessInformation ()
  {
    final String sService = null;
    final String sAction = ACTION_DEFAULT;
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

  @NonNull
  public static PModeLegSecurity generatePModeLegSecurity ()
  {
    final PModeLegSecurity aPModeLegSecurity = new PModeLegSecurity ();
    aPModeLegSecurity.setWSSVersion (EWSSVersion.WSS_111);
    aPModeLegSecurity.setX509SignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_256);
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
  public static PModeLeg generatePModeLeg (@Nullable final String sResponderAddress)
  {
    return new PModeLeg (generatePModeLegProtocol (sResponderAddress),
                         generatePModeLegBusinessInformation (),
                         generatePModeLegErrorHandling (),
                         (PModeLegReliability) null,
                         generatePModeLegSecurity ());
  }

  @NonNull
  public static PModePayloadService generatePModePayloadSevice ()
  {
    return new PModePayloadService (EAS4CompressionMode.GZIP);
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
   * One-Way Version of the CEF pmode uses one-way push
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
   * @return New PMode
   */
  @NonNull
  public static PMode createENTSOGPMode (@NonNull @Nonempty final String sInitiatorID,
                                         @NonNull @Nonempty final String sResponderID,
                                         @Nullable final String sResponderAddress,
                                         @NonNull final IPModeIDProvider aPModeIDProvider,
                                         final boolean bPersist)
  {
    final PModeParty aInitiator = new PModeParty (ENTSOG_PARTY_ID_TYPE,
                                                  sInitiatorID,
                                                  CAS4.DEFAULT_INITIATOR_URL,
                                                  null,
                                                  null);
    final PModeParty aResponder = new PModeParty (ENTSOG_PARTY_ID_TYPE,
                                                  sResponderID,
                                                  CAS4.DEFAULT_RESPONDER_URL,
                                                  null,
                                                  null);

    final PMode aPMode = new PMode (aPModeIDProvider.getPModeID (aInitiator, aResponder),
                                    aInitiator,
                                    aResponder,
                                    DEFAULT_AGREEMENT_ID,
                                    EMEP.ONE_WAY,
                                    EMEPBinding.PUSH,
                                    generatePModeLeg (sResponderAddress),
                                    (PModeLeg) null,
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

}
