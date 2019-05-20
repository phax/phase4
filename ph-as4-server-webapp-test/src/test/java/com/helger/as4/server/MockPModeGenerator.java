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
package com.helger.as4.server;

import javax.annotation.Nonnull;

import com.helger.as4.CAS4;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.pmode.DefaultPMode;
import com.helger.as4.model.pmode.IPModeIDProvider;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.PModeManager;
import com.helger.as4.model.pmode.PModeParty;
import com.helger.as4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.as4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.as4.model.pmode.leg.PModeLegProtocol;
import com.helger.as4.model.pmode.leg.PModeLegReliability;
import com.helger.as4.model.pmode.leg.PModeLegSecurity;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.wss.EWSSVersion;
import com.helger.commons.state.ETriState;

public final class MockPModeGenerator
{
  public static final String SOAP11_SERVICE = "soap11";
  private static final String DEFAULT_AGREEMENT = "urn:as4:agreements:so-that-we-have-a-non-empty-value";
  private static final String SOAP_12_PARTY_ID = "APP_000000000012";
  private static final String SOAP_11_PARTY_ID = "APP_000000000011";

  private MockPModeGenerator ()
  {}

  @Nonnull
  public static PMode getTestPMode (@Nonnull final ESOAPVersion eSOAPVersion)
  {

    final PModeParty aInitiator = _generateInitiatorOrResponder (true, eSOAPVersion);
    final PModeParty aResponder = _generateInitiatorOrResponder (false, eSOAPVersion);

    final PMode aConfig = new PMode (IPModeIDProvider.DEFAULT_DYNAMIC,
                                     aInitiator,
                                     aResponder,
                                     DEFAULT_AGREEMENT,
                                     EMEP.ONE_WAY,
                                     EMEPBinding.PUSH,
                                     _generatePModeLeg (eSOAPVersion),
                                     null,
                                     null,
                                     null);
    // Leg 2 stays null, because we only use one-way
    return aConfig;
  }

  @Nonnull
  public static PMode getTestPModeWithSecurity (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    final PMode aPMode = getTestPMode (eSOAPVersion);

    final PModeLegSecurity aPModeLegSecurity = new PModeLegSecurity ();
    aPModeLegSecurity.setWSSVersion (EWSSVersion.WSS_111);
    aPModeLegSecurity.setX509SignatureAlgorithm (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT);
    aPModeLegSecurity.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);
    aPModeLegSecurity.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.ENCRPYTION_ALGORITHM_DEFAULT);
    aPModeLegSecurity.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.RESPONSE);
    aPModeLegSecurity.setSendReceiptNonRepudiation (true);

    aPMode.setLeg1 (new PModeLeg (_generatePModeLegProtocol (eSOAPVersion),
                                  _generatePModeLegBusinessInformation (eSOAPVersion),
                                  _generatePModeLegErrorHandling (),
                                  null,
                                  aPModeLegSecurity));
    // Leg 2 stays null, because we only use one-way
    return aPMode;
  }

  @Nonnull
  private static PModeLeg _generatePModeLeg (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    final PModeLegReliability aPModeLegReliability = null;
    final PModeLegSecurity aPModeLegSecurity = null;
    return new PModeLeg (_generatePModeLegProtocol (eSOAPVersion),
                         _generatePModeLegBusinessInformation (eSOAPVersion),
                         _generatePModeLegErrorHandling (),
                         aPModeLegReliability,
                         aPModeLegSecurity);
  }

  private static PModeLegErrorHandling _generatePModeLegErrorHandling ()
  {
    return new PModeLegErrorHandling (null, null, ETriState.TRUE, ETriState.TRUE, ETriState.TRUE, ETriState.TRUE);
  }

  @Nonnull
  private static PModeLegBusinessInformation _generatePModeLegBusinessInformation (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    if (eSOAPVersion.equals (ESOAPVersion.SOAP_11))
      return new PModeLegBusinessInformation (SOAP11_SERVICE, CAS4.DEFAULT_ACTION_URL, null, CAS4.DEFAULT_MPC_ID);
    return new PModeLegBusinessInformation (null, CAS4.DEFAULT_ACTION_URL, null, CAS4.DEFAULT_MPC_ID);
  }

  @Nonnull
  private static PModeLegProtocol _generatePModeLegProtocol (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    return new PModeLegProtocol ("http://localhost:8080", eSOAPVersion);
  }

  @Nonnull
  private static PModeParty _generateInitiatorOrResponder (final boolean bInitiator,
                                                           @Nonnull final ESOAPVersion eSOAPVersion)
  {
    String sPartyID;
    if (eSOAPVersion.equals (ESOAPVersion.SOAP_11))
      sPartyID = SOAP_11_PARTY_ID;
    else
      sPartyID = SOAP_12_PARTY_ID;

    if (bInitiator)
      return PModeParty.createSimple (sPartyID, CAS4.DEFAULT_SENDER_URL);
    return PModeParty.createSimple (sPartyID, CAS4.DEFAULT_RESPONDER_URL);
  }

  public static void ensureMockPModesArePresent ()
  {
    // Delete all in the correct order
    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    for (final String sID : aPModeMgr.getAllIDs ())
      aPModeMgr.deletePMode (sID);

    // Create new one
    DefaultPMode.getOrCreateDefaultPMode (SOAP_11_PARTY_ID, SOAP_11_PARTY_ID, "http://test.mock11.org", true);
    DefaultPMode.getOrCreateDefaultPMode (SOAP_12_PARTY_ID, SOAP_12_PARTY_ID, "http://test.mock12.org", true);
    for (final ESOAPVersion e : ESOAPVersion.values ())
      aPModeMgr.createOrUpdatePMode (MockPModeGenerator.getTestPModeWithSecurity (e));
  }
}
