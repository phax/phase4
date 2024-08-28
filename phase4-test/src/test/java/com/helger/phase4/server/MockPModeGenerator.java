/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.server;

import javax.annotation.Nonnull;

import com.helger.commons.state.ETriState;
import com.helger.phase4.CAS4;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.pmode.DefaultPMode;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.IPModeManager;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.phase4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.phase4.model.pmode.leg.PModeLegProtocol;
import com.helger.phase4.model.pmode.leg.PModeLegReliability;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;
import com.helger.phase4.wss.EWSSVersion;

public final class MockPModeGenerator
{
  public static final String SOAP11_SERVICE = "soap11";
  private static final String DEFAULT_AGREEMENT = "urn:as4:agreements:so-that-we-have-a-non-empty-value";
  private static final String SOAP_12_PARTY_ID = "APP_000000000012";
  private static final String SOAP_11_PARTY_ID = "APP_000000000011";

  private MockPModeGenerator ()
  {}

  @Nonnull
  private static PModeParty _createInitiatorOrResponder (final boolean bInitiator,
                                                         @Nonnull final ESoapVersion eSOAPVersion)
  {
    final String sPartyID = eSOAPVersion.equals (ESoapVersion.SOAP_11) ? SOAP_11_PARTY_ID : SOAP_12_PARTY_ID;
    return PModeParty.createSimple (sPartyID, bInitiator ? CAS4.DEFAULT_INITIATOR_URL : CAS4.DEFAULT_RESPONDER_URL);
  }

  @Nonnull
  private static PModeLegProtocol _createPModeLegProtocol (@Nonnull final ESoapVersion eSOAPVersion)
  {
    return new PModeLegProtocol ("http://localhost:8080", eSOAPVersion);
  }

  @Nonnull
  private static PModeLegBusinessInformation _createPModeLegBusinessInformation (@Nonnull final ESoapVersion eSOAPVersion)
  {
    return PModeLegBusinessInformation.create (eSOAPVersion.equals (ESoapVersion.SOAP_11) ? SOAP11_SERVICE : null,
                                               CAS4.DEFAULT_ACTION_URL,
                                               null,
                                               CAS4.DEFAULT_MPC_ID);
  }

  @Nonnull
  private static PModeLegErrorHandling _createPModeLegErrorHandling ()
  {
    return new PModeLegErrorHandling (null, null, ETriState.TRUE, ETriState.TRUE, ETriState.TRUE, ETriState.TRUE);
  }

  @Nonnull
  private static PModeLeg _createPModeLeg (@Nonnull final ESoapVersion eSOAPVersion)
  {
    final PModeLegReliability aPModeLegReliability = null;
    final PModeLegSecurity aPModeLegSecurity = null;
    return new PModeLeg (_createPModeLegProtocol (eSOAPVersion),
                         _createPModeLegBusinessInformation (eSOAPVersion),
                         _createPModeLegErrorHandling (),
                         aPModeLegReliability,
                         aPModeLegSecurity);
  }

  @Nonnull
  public static PMode getTestPMode (@Nonnull final ESoapVersion eSOAPVersion)
  {
    final PModeParty aInitiator = _createInitiatorOrResponder (true, eSOAPVersion);
    final PModeParty aResponder = _createInitiatorOrResponder (false, eSOAPVersion);

    final PMode aConfig = new PMode (IPModeIDProvider.DEFAULT_DYNAMIC.getPModeID (aInitiator, aResponder),
                                     aInitiator,
                                     aResponder,
                                     DEFAULT_AGREEMENT,
                                     EMEP.ONE_WAY,
                                     EMEPBinding.PUSH,
                                     _createPModeLeg (eSOAPVersion),
                                     null,
                                     null,
                                     null);
    // Leg 2 stays null, because we only use one-way
    return aConfig;
  }

  @Nonnull
  public static PMode getTestPModeWithSecurity (@Nonnull final ESoapVersion eSOAPVersion)
  {
    final PMode aPMode = getTestPMode (eSOAPVersion);

    final PModeLegSecurity aPModeLegSecurity = new PModeLegSecurity ();
    aPModeLegSecurity.setWSSVersion (EWSSVersion.WSS_111);
    aPModeLegSecurity.setX509SignatureAlgorithm (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT);
    aPModeLegSecurity.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);
    aPModeLegSecurity.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.ENCRPYTION_ALGORITHM_DEFAULT);
    aPModeLegSecurity.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.RESPONSE);
    aPModeLegSecurity.setSendReceipt (true);
    aPModeLegSecurity.setSendReceiptNonRepudiation (true);
    // Required for compatibility with the Test profile PModes
    aPModeLegSecurity.setPModeAuthorize (false);

    aPMode.setLeg1 (new PModeLeg (_createPModeLegProtocol (eSOAPVersion),
                                  _createPModeLegBusinessInformation (eSOAPVersion),
                                  _createPModeLegErrorHandling (),
                                  null,
                                  aPModeLegSecurity));
    // Leg 2 stays null, because we only use one-way
    return aPMode;
  }

  public static void ensureMockPModesArePresent ()
  {
    // Delete all in the correct order
    final IPModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    for (final String sID : aPModeMgr.getAllIDs ())
      aPModeMgr.deletePMode (sID);

    // Create default ones
    DefaultPMode.getOrCreateDefaultPMode (SOAP_11_PARTY_ID, SOAP_11_PARTY_ID, "http://test.mock11.org", true);
    DefaultPMode.getOrCreateDefaultPMode (SOAP_12_PARTY_ID, SOAP_12_PARTY_ID, "http://test.mock12.org", true);

    for (final ESoapVersion e : ESoapVersion.values ())
      aPModeMgr.createOrUpdatePMode (MockPModeGenerator.getTestPModeWithSecurity (e));
  }
}
