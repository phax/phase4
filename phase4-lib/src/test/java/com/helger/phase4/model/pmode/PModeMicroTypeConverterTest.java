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
package com.helger.phase4.model.pmode;

import static org.junit.Assert.assertNotNull;

import javax.annotation.Nonnull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.mock.CommonsTestHelper;
import com.helger.commons.state.EMandatory;
import com.helger.commons.state.ETriState;
import com.helger.json.IJsonObject;
import com.helger.phase4.AS4TestRule;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.phase4.model.pmode.leg.PModeAddressList;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.phase4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.phase4.model.pmode.leg.PModeLegProtocol;
import com.helger.phase4.model.pmode.leg.PModeLegReliability;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;
import com.helger.phase4.model.pmode.leg.PModePayloadProfile;
import com.helger.phase4.model.pmode.leg.PModeProperty;
import com.helger.phase4.wss.EWSSVersion;
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

  private static void _testPMode (@Nonnull final PMode aPMode)
  {
    XMLTestHelper.testMicroTypeConversion (aPMode);
    if (aPMode.hasInitiator ())
      XMLTestHelper.testMicroTypeConversion (aPMode.getInitiator ());
    if (aPMode.hasResponder ())
      XMLTestHelper.testMicroTypeConversion (aPMode.getResponder ());
    if (aPMode.hasLeg1 ())
      XMLTestHelper.testMicroTypeConversion (aPMode.getLeg1 ());
    if (aPMode.hasLeg2 ())
      XMLTestHelper.testMicroTypeConversion (aPMode.getLeg2 ());
    if (aPMode.hasPayloadService ())
      XMLTestHelper.testMicroTypeConversion (aPMode.getPayloadService ());
    if (aPMode.hasReceptionAwareness ())
      XMLTestHelper.testMicroTypeConversion (aPMode.getReceptionAwareness ());

    final IJsonObject o = aPMode.getAsJson ();
    assertNotNull (o);
    final PMode aPMode2 = PModeJsonConverter.convertToNative (o);
    assertNotNull (aPMode2);
    CommonsTestHelper.testDefaultImplementationWithEqualContentObject (aPMode, aPMode2);
  }

  @Test
  public void testNativToMicroElementConversion ()
  {
    final PModeParty aInitiator = _createInitiatorOrResponder (true);
    final PModeParty aResponder = _createInitiatorOrResponder (false);

    _testPMode (new PMode (aInitiator.getID () + "-" + aResponder.getID (),
                           aInitiator,
                           aResponder,
                           "Agreement",
                           EMEP.TWO_WAY,
                           EMEPBinding.SYNC,
                           _createPModeLeg (),
                           _createPModeLeg (),
                           _createPayloadService (),
                           _createPModeReceptionAwareness ()));
    _testPMode (new PMode (aInitiator.getID () + "-" + aResponder.getID (),
                           null,
                           null,
                           "Agreement",
                           EMEP.TWO_WAY,
                           EMEPBinding.SYNC,
                           null,
                           null,
                           null,
                           null));
  }

  @Nonnull
  private PModePayloadService _createPayloadService ()
  {
    return new PModePayloadService (EAS4CompressionMode.GZIP);
  }

  @Nonnull
  private PModeReceptionAwareness _createPModeReceptionAwareness ()
  {
    return PModeReceptionAwareness.createDefault ();
  }

  @Nonnull
  private PModeParty _createInitiatorOrResponder (final boolean bInitiator)
  {
    if (bInitiator)
      return new PModeParty ("initiator-type", "idvalue", CAS4.DEFAULT_INITIATOR_URL, "test", "testpw");
    return new PModeParty ("responder-type", "idvalue2", CAS4.DEFAULT_RESPONDER_URL, "test2", "test2pw");
  }

  @Nonnull
  private PModeLeg _createPModeLeg ()
  {
    return new PModeLeg (_createPModeLegProtocol (),
                         _createPModeLegBusinessInformation (),
                         _createPModeLegErrorHandling (),
                         _createPModeLegReliability (),
                         _createPModeLegSecurity ());
  }

  @Nonnull
  private PModeLegBusinessInformation _createPModeLegBusinessInformation ()
  {
    return new PModeLegBusinessInformation ("service",
                                            "stype",
                                            "action1",
                                            _createPModeProperties (),
                                            _createPModePayloadProfile (),
                                            Long.valueOf (20000),
                                            "mpcexample");
  }

  @Nonnull
  @ReturnsMutableCopy
  private ICommonsOrderedMap <String, PModePayloadProfile> _createPModePayloadProfile ()
  {
    final PModePayloadProfile aPModePayloadProfile = new PModePayloadProfile ("name",
                                                                              new MimeType (EMimeContentType.EXAMPLE, "example"),
                                                                              "xsdfilename",
                                                                              Integer.valueOf (20001),
                                                                              EMandatory.MANDATORY);
    final ICommonsOrderedMap <String, PModePayloadProfile> aPModePayloadProfiles = new CommonsLinkedHashMap <> ();
    aPModePayloadProfiles.put (aPModePayloadProfile.getName (), aPModePayloadProfile);
    return aPModePayloadProfiles;
  }

  @Nonnull
  @ReturnsMutableCopy
  private ICommonsOrderedMap <String, PModeProperty> _createPModeProperties ()
  {
    final PModeProperty aPModeProperty = new PModeProperty ("name", "description", PModeProperty.DATA_TYPE_STRING, EMandatory.MANDATORY);
    final ICommonsOrderedMap <String, PModeProperty> aPModeProperties = new CommonsLinkedHashMap <> ();
    aPModeProperties.put (aPModeProperty.getName (), aPModeProperty);
    return aPModeProperties;
  }

  @Nonnull
  private PModeLegErrorHandling _createPModeLegErrorHandling ()
  {
    return new PModeLegErrorHandling (_createPModeAddressList (),
                                      _createPModeAddressList (),
                                      ETriState.TRUE,
                                      ETriState.TRUE,
                                      ETriState.TRUE,
                                      ETriState.TRUE);
  }

  @Nonnull
  private PModeAddressList _createPModeAddressList ()
  {
    return new PModeAddressList ("address1");
  }

  @Nonnull
  private PModeLegProtocol _createPModeLegProtocol ()
  {
    return new PModeLegProtocol ("addressProtocol", ESoapVersion.SOAP_11);
  }

  @Nonnull
  private PModeLegReliability _createPModeLegReliability ()
  {
    final ICommonsList <String> aCorrelation = new CommonsArrayList <> ("correlation", "correlation2");
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
  private PModeLegSecurity _createPModeLegSecurity ()
  {
    final ICommonsList <String> aX509EncryptionEncrypt = new CommonsArrayList <> ("X509EncryptionEncrypt", "X509EncryptionEncrypt2");
    final ICommonsList <String> aX509Sign = new CommonsArrayList <> ("X509Sign", "X509Sign2");
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
                                 Integer.valueOf (1),
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
