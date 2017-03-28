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
package com.helger.as4.model.pmode;

import javax.annotation.Nonnull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.as4.AS4TestRule;
import com.helger.as4.CAS4;
import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.as4.model.pmode.leg.PModeAddressList;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.as4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.as4.model.pmode.leg.PModeLegProtocol;
import com.helger.as4.model.pmode.leg.PModeLegReliability;
import com.helger.as4.model.pmode.leg.PModeLegSecurity;
import com.helger.as4.model.pmode.leg.PModePayloadProfile;
import com.helger.as4.model.pmode.leg.PModeProperty;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.wss.EWSSVersion;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsOrderedMap;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.state.EMandatory;
import com.helger.commons.state.ETriState;
import com.helger.xml.mock.XMLTestHelper;

/**
 * Test class for class {@link PModeMicroTypeConverter}.
 *
 * @author Philip Helger
 */
public final class PModeMicroTypeConverterTest
{
  @Rule
  public final TestRule m_aTestRule = new AS4TestRule ();

  @Test
  public void testNativToMicroElementConversion ()
  {
    final PModeParty aInitiator = _generateInitiatorOrResponder (true);
    final PModeParty aResponder = _generateInitiatorOrResponder (false);

    final PMode aPMode = new PMode (IPModeIDProvider.DEFAULT_DYNAMIC,
                                    aInitiator,
                                    aResponder,
                                    "Agreement",
                                    EMEP.TWO_WAY,
                                    EMEPBinding.SYNC,
                                    _generatePModeLeg (),
                                    _generatePModeLeg (),
                                    _generatePayloadService (),
                                    _generatePModeReceptionAwareness ());
    XMLTestHelper.testMicroTypeConversion (aPMode);
    XMLTestHelper.testMicroTypeConversion (aPMode.getInitiator ());
    XMLTestHelper.testMicroTypeConversion (aPMode.getResponder ());
    XMLTestHelper.testMicroTypeConversion (aPMode.getLeg1 ());
    XMLTestHelper.testMicroTypeConversion (aPMode.getLeg2 ());
    XMLTestHelper.testMicroTypeConversion (aPMode.getPayloadService ());
    XMLTestHelper.testMicroTypeConversion (aPMode.getReceptionAwareness ());
  }

  @Nonnull
  private PModePayloadService _generatePayloadService ()
  {
    return new PModePayloadService (EAS4CompressionMode.GZIP);
  }

  @Nonnull
  private PModeReceptionAwareness _generatePModeReceptionAwareness ()
  {
    return PModeReceptionAwareness.createDefault ();
  }

  @Nonnull
  private PModeParty _generateInitiatorOrResponder (final boolean bInitiator)
  {
    if (bInitiator)
      return new PModeParty ("initiator-type", "idvalue", CAS4.DEFAULT_SENDER_URL, "test", "testpw");
    return new PModeParty ("responder-type", "idvalue2", CAS4.DEFAULT_RESPONDER_URL, "test2", "test2pw");
  }

  @Nonnull
  private PModeLeg _generatePModeLeg ()
  {
    return new PModeLeg (_generatePModeLegProtocol (),
                         _generatePModeLegBusinessInformation (),
                         _generatePModeLegErrorHandling (),
                         _generatePModeLegReliability (),
                         _generatePModeLegSecurity ());
  }

  @Nonnull
  private PModeLegBusinessInformation _generatePModeLegBusinessInformation ()
  {
    return new PModeLegBusinessInformation ("service",
                                            "action1",
                                            _generatePModeProperties (),
                                            _generatePModePayloadProfile (),
                                            20000,
                                            "mpcexample");
  }

  @Nonnull
  @ReturnsMutableCopy
  private ICommonsOrderedMap <String, PModePayloadProfile> _generatePModePayloadProfile ()
  {
    final PModePayloadProfile aPModePayloadProfile = new PModePayloadProfile ("name",
                                                                              new MimeType (EMimeContentType.EXAMPLE,
                                                                                            "example"),
                                                                              "xsdfilename",
                                                                              20001,
                                                                              EMandatory.MANDATORY);
    final ICommonsOrderedMap <String, PModePayloadProfile> aPModePayloadProfiles = new CommonsLinkedHashMap<> ();
    aPModePayloadProfiles.put (aPModePayloadProfile.getName (), aPModePayloadProfile);
    return aPModePayloadProfiles;
  }

  @Nonnull
  @ReturnsMutableCopy
  private ICommonsOrderedMap <String, PModeProperty> _generatePModeProperties ()
  {
    final PModeProperty aPModeProperty = new PModeProperty ("name",
                                                            "description",
                                                            PModeProperty.DATA_TYPE_STRING,
                                                            EMandatory.MANDATORY);
    final ICommonsOrderedMap <String, PModeProperty> aPModeProperties = new CommonsLinkedHashMap<> ();
    aPModeProperties.put (aPModeProperty.getName (), aPModeProperty);
    return aPModeProperties;
  }

  @Nonnull
  private PModeLegErrorHandling _generatePModeLegErrorHandling ()
  {
    return new PModeLegErrorHandling (_generatePModeAddressList (),
                                      _generatePModeAddressList (),
                                      ETriState.TRUE,
                                      ETriState.TRUE,
                                      ETriState.TRUE,
                                      ETriState.TRUE);
  }

  @Nonnull
  private PModeAddressList _generatePModeAddressList ()
  {
    return new PModeAddressList ("address1");
  }

  @Nonnull
  private PModeLegProtocol _generatePModeLegProtocol ()
  {
    return new PModeLegProtocol ("addressProtocol", ESOAPVersion.SOAP_11);
  }

  @Nonnull
  private PModeLegReliability _generatePModeLegReliability ()
  {
    final ICommonsList <String> aCorrelation = new CommonsArrayList<> ("correlation", "correlation2");
    return new PModeLegReliability (ETriState.TRUE,
                                    ETriState.TRUE,
                                    "ack",
                                    ETriState.TRUE,
                                    "replyPattern",
                                    ETriState.TRUE,
                                    ETriState.TRUE,
                                    ETriState.TRUE,
                                    aCorrelation,
                                    ETriState.TRUE);

  }

  @Nonnull
  private PModeLegSecurity _generatePModeLegSecurity ()
  {
    final ICommonsList <String> aX509EncryptionEncrypt = new CommonsArrayList<> ("X509EncryptionEncrypt",
                                                                                 "X509EncryptionEncrypt2");
    final ICommonsList <String> aX509Sign = new CommonsArrayList<> ("X509Sign", "X509Sign2");
    return new PModeLegSecurity (EWSSVersion.WSS_111,
                                 aX509Sign,
                                 aX509Sign,
                                 "X509SignatureCertificate",
                                 ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT,
                                 ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                 aX509EncryptionEncrypt,
                                 aX509EncryptionEncrypt,
                                 "X509EncryptionCertificate",
                                 ECryptoAlgorithmCrypt.AES_128_GCM,
                                 1,
                                 "usernametokenusername",
                                 "usernametokenpassword",
                                 ETriState.TRUE,
                                 ETriState.TRUE,
                                 ETriState.TRUE,
                                 ETriState.TRUE,
                                 ETriState.TRUE,
                                 EPModeSendReceiptReplyPattern.RESPONSE,
                                 ETriState.TRUE);
  }
}
