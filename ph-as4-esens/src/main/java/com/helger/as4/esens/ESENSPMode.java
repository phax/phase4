/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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

import com.helger.as4.CAS4;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.mpc.MPCManager;
import com.helger.as4.model.pmode.EPModeSendReceiptReplyPattern;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.PModeParty;
import com.helger.as4.model.pmode.PModeReceptionAwareness;
import com.helger.as4.model.pmode.config.PModeConfig;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.as4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.as4.model.pmode.leg.PModeLegProtocol;
import com.helger.as4.model.pmode.leg.PModeLegReliability;
import com.helger.as4.model.pmode.leg.PModeLegSecurity;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.wss.EWSSVersion;
import com.helger.commons.state.ETriState;

public final class ESENSPMode
{
  public static final String ESENS_PMODE_CONFIG_ID = "pm-esens-default";

  private ESENSPMode ()
  {}

  @Nonnull
  public static PModeConfig createESENSPModeConfig (@Nonnull final String sAddress)
  {
    final PModeConfig aConfig = new PModeConfig (ESENS_PMODE_CONFIG_ID);
    aConfig.setMEP (EMEP.ONE_WAY);
    aConfig.setMEPBinding (EMEPBinding.PUSH);
    aConfig.setLeg1 (new PModeLeg (_generatePModeLegProtocol (sAddress),
                                   _generatePModeLegBusinessInformation (),
                                   _generatePModeLegErrorHandling (),
                                   (PModeLegReliability) null,
                                   _generatePModeLegSecurity ()));
    // Leg 2 stays null, because we only use one-way
    aConfig.setReceptionAwareness (new PModeReceptionAwareness (ETriState.TRUE, ETriState.TRUE, ETriState.TRUE));

    // Ensure it is stored
    MetaAS4Manager.getPModeConfigMgr ().createOrUpdatePModeConfig (aConfig);
    return aConfig;
  }

  @Nonnull
  public static PMode createESENSPMode (@Nonnull final String sAddress)
  {
    final PModeConfig aConfig = createESENSPModeConfig (sAddress);

    return new PMode (_generateInitiatorOrResponder (true), _generateInitiatorOrResponder (false), aConfig);
  }

  @Nonnull
  private static PModeLegErrorHandling _generatePModeLegErrorHandling ()
  {
    return new PModeLegErrorHandling (null, null, ETriState.TRUE, ETriState.TRUE, ETriState.UNDEFINED, ETriState.TRUE);
  }

  @Nonnull
  private static PModeLegSecurity _generatePModeLegSecurity ()
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

  @Nonnull
  private static PModeLegBusinessInformation _generatePModeLegBusinessInformation ()
  {
    return new PModeLegBusinessInformation (null, CAS4.DEFAULT_ACTION_URL, null, null, null, MPCManager.DEFAULT_MPC_ID);
  }

  @Nonnull
  private static PModeLegProtocol _generatePModeLegProtocol (@Nonnull final String sAddress)
  {
    return new PModeLegProtocol (sAddress, ESOAPVersion.SOAP_12);
  }

  @Nonnull
  private static PModeParty _generateInitiatorOrResponder (final boolean bInitiator)
  {
    if (bInitiator)
      return new PModeParty (null, "APP_1000000101", CAS4.DEFAULT_SENDER_URL, null, null);
    return new PModeParty (null, "APP_2000000101", CAS4.DEFAULT_RESPONDER_URL, null, null);
  }
}
