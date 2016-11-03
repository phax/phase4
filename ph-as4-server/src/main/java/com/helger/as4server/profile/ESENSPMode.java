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
package com.helger.as4server.profile;

import javax.annotation.Nonnull;

import com.helger.as4lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.model.pmode.PModeLeg;
import com.helger.as4lib.model.pmode.PModeLegBusinessInformation;
import com.helger.as4lib.model.pmode.PModeLegErrorHandling;
import com.helger.as4lib.model.pmode.PModeLegProtocol;
import com.helger.as4lib.model.pmode.PModeLegReliability;
import com.helger.as4lib.model.pmode.PModeLegSecurity;
import com.helger.as4lib.model.pmode.PModeParty;
import com.helger.as4lib.model.pmode.PModeReceptionAwareness;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.wss.EWSSVersion;
import com.helger.commons.state.ETriState;

public class ESENSPMode
{
  @Nonnull
  public static PMode getESENSPMode ()
  {
    final PMode aTestPmode = new PMode ("pm-esens-default");
    aTestPmode.setMEP (EMEP.ONE_WAY);
    aTestPmode.setMEPBinding (ETransportChannelBinding.PUSH);
    aTestPmode.setInitiator (_generateInitiatorOrResponder (true));
    aTestPmode.setResponder (_generateInitiatorOrResponder (false));
    aTestPmode.setLeg1 (new PModeLeg (_generatePModeLegProtocol (),
                                      _generatePModeLegBusinessInformation (),
                                      _generatePModeLegErrorHandling (),
                                      null,
                                      _generatePModeLegSecurity ()));
    // Leg 2 stays null, because we only use one-way
    aTestPmode.setReceptionAwareness (new PModeReceptionAwareness (ETriState.TRUE, ETriState.TRUE, ETriState.TRUE));
    return aTestPmode;
  }

  private static PModeLegErrorHandling _generatePModeLegErrorHandling ()
  {
    final PModeLegErrorHandling aPModeLegErrorHandling = new PModeLegErrorHandling ();
    aPModeLegErrorHandling.setReportAsResponse (true);
    aPModeLegErrorHandling.setReportDeliveryFailuresNotifyProducer (true);
    aPModeLegErrorHandling.setReportProcessErrorNotifyConsumer (true);
    return aPModeLegErrorHandling;
  }

  @SuppressWarnings ("boxing")
  private static PModeLegSecurity _generatePModeLegSecurity ()
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
    return aPModeLegSecurity;
  }

  @Nonnull
  private static PModeLeg _generatePModeLeg ()
  {
    final PModeLegReliability aPModeLegReliability = null;
    final PModeLegSecurity aPModeLegSecurity = null;
    return new PModeLeg (_generatePModeLegProtocol (),
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
  private static PModeLegProtocol _generatePModeLegProtocol ()
  {
    return new PModeLegProtocol ("http://localhost:8080", ESOAPVersion.SOAP_12);
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
                           "APP_2000000101",
                           "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder",
                           null,
                           null);
  }
}
