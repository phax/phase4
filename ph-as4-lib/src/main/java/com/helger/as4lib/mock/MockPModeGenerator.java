/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.mock;

import javax.annotation.Nonnull;

import com.helger.as4lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.model.pmode.PModeConfig;
import com.helger.as4lib.model.pmode.PModeLeg;
import com.helger.as4lib.model.pmode.PModeLegBusinessInformation;
import com.helger.as4lib.model.pmode.PModeLegProtocol;
import com.helger.as4lib.model.pmode.PModeLegReliability;
import com.helger.as4lib.model.pmode.PModeLegSecurity;
import com.helger.as4lib.model.pmode.PModeParty;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.wss.EWSSVersion;

public class MockPModeGenerator
{
  public static String PMODE_CONFIG_ID_SOAP11_TEST = "pmode-test11";
  public static String PMODE_CONFIG_ID_SOAP12_TEST = "pmode-test";

  private MockPModeGenerator ()
  {}

  @Nonnull
  public static PModeConfig getTestPModeConfig (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    return getTestPModeConfigSetID (eSOAPVersion,
                                    eSOAPVersion.equals (ESOAPVersion.SOAP_12) ? PMODE_CONFIG_ID_SOAP12_TEST
                                                                               : PMODE_CONFIG_ID_SOAP11_TEST);
  }

  @Nonnull
  public static PModeConfig getTestPModeConfigSetID (@Nonnull final ESOAPVersion eSOAPVersion, final String sPModeID)
  {
    final PModeConfig aConfig = new PModeConfig (sPModeID);
    aConfig.setMEP (EMEP.ONE_WAY);
    aConfig.setMEPBinding (ETransportChannelBinding.PUSH);
    aConfig.setLeg1 (_generatePModeLeg (eSOAPVersion));
    // Leg 2 stays null, because we only use one-way
    return aConfig;
  }

  @Nonnull
  private static PMode _createTestPMode (@Nonnull final PModeConfig aConfig)
  {
    MetaAS4Manager.getPModeConfigMgr ().createPModeConfigIfNotExisting (aConfig);
    return new PMode (_generateInitiatorOrResponder (true), _generateInitiatorOrResponder (false), aConfig);
  }

  @Nonnull
  public static PMode getTestPMode (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    return _createTestPMode (getTestPModeConfig (eSOAPVersion));
  }

  @Nonnull
  public static PMode getTestPModeSetID (@Nonnull final ESOAPVersion eSOAPVersion, @Nonnull final String sPModeID)
  {
    return _createTestPMode (getTestPModeConfigSetID (eSOAPVersion, sPModeID));
  }

  @Nonnull
  public static PMode getTestPModeWithSecurity (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    final PModeConfig aConfig = getTestPModeConfig (eSOAPVersion);

    final PModeLegSecurity aPModeLegSecurity = new PModeLegSecurity ();
    aPModeLegSecurity.setWSSVersion (EWSSVersion.WSS_111);
    aPModeLegSecurity.setX509SignatureCertificate ("TODO change to real cert");
    aPModeLegSecurity.setX509SignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_256);
    aPModeLegSecurity.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);
    aPModeLegSecurity.setX509EncryptionCertificate ("TODO change to real cert");
    aPModeLegSecurity.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM);

    aConfig.setLeg1 (new PModeLeg (_generatePModeLegProtocol (eSOAPVersion),
                                   _generatePModeLegBusinessInformation (),
                                   null,
                                   null,
                                   aPModeLegSecurity));
    // Leg 2 stays null, because we only use one-way
    return _createTestPMode (aConfig);

  }

  @Nonnull
  private static PModeLeg _generatePModeLeg (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    final PModeLegReliability aPModeLegReliability = null;
    final PModeLegSecurity aPModeLegSecurity = null;
    return new PModeLeg (_generatePModeLegProtocol (eSOAPVersion),
                         _generatePModeLegBusinessInformation (),
                         null,
                         aPModeLegReliability,
                         aPModeLegSecurity);
  }

  @Nonnull
  private static PModeLegBusinessInformation _generatePModeLegBusinessInformation ()
  {
    return new PModeLegBusinessInformation ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/test",
                                            null,
                                            null,
                                            null,
                                            null,
                                            "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC");
  }

  @Nonnull
  private static PModeLegProtocol _generatePModeLegProtocol (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    return new PModeLegProtocol ("http://localhost:8080", eSOAPVersion);
  }

  @Nonnull
  private static PModeParty _generateInitiatorOrResponder (final boolean bInitiator)
  {
    if (bInitiator)
      return new PModeParty (null,
                             "APP_1000000101",
                             "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sender",
                             null,
                             null);
    return new PModeParty (null,
                           "APP_1000000101",
                           "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder",
                           null,
                           null);
  }
}
