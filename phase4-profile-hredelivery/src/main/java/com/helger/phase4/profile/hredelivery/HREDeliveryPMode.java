/*
 * Copyright (C) 2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.hredelivery;

import java.util.regex.Pattern;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.CGlobal;
import com.helger.base.state.ETriState;
import com.helger.cache.regex.RegExCache;
import com.helger.phase4.CAS4;
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * PMode creation code.
 *
 * @author Philip Helger
 */
@Immutable
public final class HREDeliveryPMode
{
  public static final String PMODE_ID = "eRacun";
  public static final String DEFAULT_AGREEMENT_ID = "urn:fdc:eracun.hr:2023:agreements:ap_provider";
  public static final String DEFAULT_PARTY_TYPE_ID = "urn:fdc:eRacun.hr:2023:identifiers:ap";
  public static final Pattern CERTIFICATE_SUBJECT_CONSTRAINT_PATTERN = RegExCache.getPattern ("^.+$");

  private HREDeliveryPMode ()
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
    final String sService = null;
    final String sServiceType = null;
    // Document type ID
    final String sAction = null;
    // Max 10 MB
    final Long nPayloadProfileMaxKB = Long.valueOf (10 * CGlobal.BYTES_PER_MEGABYTE / CGlobal.BYTES_PER_KILOBYTE_LONG);
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
    // Example uses SHA-256, spec allows a multitude of algorithms
    aPModeLegSecurity.setX509SignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_256);
    aPModeLegSecurity.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);
    // No encryption!
    aPModeLegSecurity.setX509EncryptionAlgorithm (null);
    aPModeLegSecurity.setX509EncryptionMinimumStrength (null);
    aPModeLegSecurity.setPModeAuthorize (false);
    aPModeLegSecurity.setSendReceipt (true);
    aPModeLegSecurity.setSendReceiptNonRepudiation (true);
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
    final int nMaxRetries = 10;
    final long nRetryIntervalMS = 3_000 * CGlobal.MILLISECONDS_PER_SECOND;
    final ETriState eDuplicateDetection = ETriState.TRUE;
    return new PModeReceptionAwareness (eReceptionAwareness,
                                        eRetry,
                                        nMaxRetries,
                                        nRetryIntervalMS,
                                        eDuplicateDetection);
  }

  @Nonnull
  public static PModeParty createParty (@Nonnull @Nonempty final String sPartyID, @Nonnull @Nonempty final String sRole)
  {
    // Party type is needed for Peppol
    return new PModeParty (DEFAULT_PARTY_TYPE_ID, sPartyID, sRole, null, null);
  }

  /**
   * One-Way Version of the Peppol pmode uses one-way push
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
   *        <code>true</code> to persist the PMode in the PModeManager, <code>false</code> to have
   *        it only in memory.
   * @return New PMode and never <code>null</code>.
   */
  @Nonnull
  public static PMode createPeppolPMode (@Nonnull @Nonempty final String sInitiatorID,
                                         @Nonnull @Nonempty final String sResponderID,
                                         @Nullable final String sAddress,
                                         @Nonnull final IPModeIDProvider aPModeIDProvider,
                                         final boolean bPersist)
  {
    final PModeParty aInitiator = createParty (sInitiatorID, CAS4.DEFAULT_INITIATOR_URL);
    final PModeParty aResponder = createParty (sResponderID, CAS4.DEFAULT_RESPONDER_URL);

    final PMode aPMode = new PMode (PMODE_ID,
                                    aInitiator,
                                    aResponder,
                                    DEFAULT_AGREEMENT_ID,
                                    EMEP.ONE_WAY,
                                    EMEPBinding.PUSH,
                                    generatePModeLeg (sAddress),
                                    (PModeLeg) null,
                                    (PModePayloadService) null,
                                    generatePModeReceptionAwareness ());
    // Leg 2 stays null, because we only use one-way
    // By default no compression active

    if (bPersist)
    {
      // Ensure it is stored
      MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);
    }
    return aPMode;
  }
}
