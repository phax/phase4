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

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.mock.MockPModeGenerator;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.ETransportChannelBinding;
import com.helger.as4.model.pmode.EPModeSendReceiptReplyPattern;
import com.helger.as4.model.pmode.config.PModeConfig;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.as4.model.pmode.leg.PModeLegProtocol;
import com.helger.as4.model.pmode.leg.PModeLegSecurity;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.wss.EWSSVersion;
import com.helger.as4lib.ebms3header.Ebms3From;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3PartyId;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.ebms3header.Ebms3To;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.state.ETriState;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;

/**
 * All essentials need to be set and need to be not null since they are getting
 * checked, when a PMode is introduced into the system and these null checks
 * would be redundant in the profiles.
 *
 * @author bayerlma
 */
public class ESENSCompatibilityValidatorTest
{
  @ClassRule
  public static final PhotonBasicWebTestRule s_aRule = new PhotonBasicWebTestRule ();

  private static final Locale LOCALE = Locale.US;
  private final ESENSCompatibilityValidator aESENSCompatibilityValidator = new ESENSCompatibilityValidator ();

  private PModeConfig m_aPModeConfig;
  private ErrorList m_aErrorList;

  @Before
  public void setUp ()
  {
    m_aErrorList = new ErrorList ();
    m_aPModeConfig = (PModeConfig) MockPModeGenerator.getTestPModeWithSecurity (ESOAPVersion.SOAP_12).getConfig ();
  }

  @Test
  public void testValidatePModeConfigWrongMEP ()
  {
    m_aPModeConfig.setMEP (EMEP.TWO_WAY_PULL_PUSH);
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);

    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MEP")));
  }

  @Test
  public void testValidatePModeConfigWrongMEPBinding ()
  {
    m_aPModeConfig.setMEPBinding (ETransportChannelBinding.SYNC);
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);

    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MEP-Binding")));
  }

  @Test
  public void testValidatePModeConfigNoLeg ()
  {
    m_aPModeConfig.setLeg1 (null);
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("PMode is missing Leg 1")));
  }

  @Test
  public void testValidatePModeConfigNoProtocol ()
  {
    m_aPModeConfig.setLeg1 (new PModeLeg (null, null, null, null, null));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("Protocol")));
  }

  @Test
  public void testValidatePModeConfigNoProtocolAddress ()
  {
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol (null, ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          null,
                                          null,
                                          null));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("AddressProtocol")));
  }

  @Test
  public void testValidatePModeConfigProtocolAddressIsNotHttps ()
  {
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("ftp://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          null,
                                          null,
                                          null));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("non-standard AddressProtocol: ftp")));
  }

  @Test
  public void testValidatePModeConfigProtocolSOAP11NotAllowed ()
  {
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.SOAP_11),
                                          null,
                                          null,
                                          null,
                                          null));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("1.1")));
  }

  @Test
  // TODO re-enable if we know what we want
  @Ignore ("Certificate check was a TODO")
  public void testValidatePModeConfigSecurityNoX509SignatureCertificate ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPModeConfig.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureCertificate (null);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          null,
                                          null,
                                          aSecurityLeg));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("signature certificate")));
  }

  @Test
  public void testValidatePModeConfigSecurityNoX509SignatureAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPModeConfig.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureAlgorithm (null);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          null,
                                          null,
                                          aSecurityLeg));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("signature algorithm")));
  }

  @Test
  public void testValidatePModeConfigSecurityWrongX509SignatureAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPModeConfig.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_384);
    assertNotSame (aSecurityLeg.getX509SignatureAlgorithm (), ECryptoAlgorithmSign.RSA_SHA_256);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          null,
                                          null,
                                          aSecurityLeg));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains (ECryptoAlgorithmSign.RSA_SHA_256.getID ())));
  }

  @Test
  public void testValidatePModeConfigSecurityNoX509SignatureHashFunction ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPModeConfig.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureHashFunction (null);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          null,
                                          null,
                                          aSecurityLeg));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("hash function")));
  }

  @Test
  public void testValidatePModeConfigSecurityWrongX509SignatureHashFunction ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPModeConfig.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_512);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          null,
                                          null,
                                          aSecurityLeg));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains (ECryptoAlgorithmSignDigest.DIGEST_SHA_256.getID ())));
  }

  @Test
  public void testValidatePModeConfigSecurityNoX509EncryptionAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPModeConfig.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509EncryptionAlgorithm (null);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          null,
                                          null,
                                          aSecurityLeg));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("encryption algorithm")));
  }

  @Test
  public void testValidatePModeConfigSecurityWrongX509EncryptionAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPModeConfig.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.AES_192_CBC);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          null,
                                          null,
                                          aSecurityLeg));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains (ECryptoAlgorithmCrypt.AES_128_GCM.getID ())));
  }

  @SuppressWarnings ("deprecation")
  @Test
  public void testValidatePModeConfigSecurityWrongWSSVersion ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPModeConfig.getLeg1 ().getSecurity ();
    aSecurityLeg.setWSSVersion (EWSSVersion.WSS_10);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          null,
                                          null,
                                          aSecurityLeg));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("Wrong WSS Version")));
  }

  @Test
  public void testValidatePModeConfigSecurityPModeAuthorizeMandatory ()
  {
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("mandatory")));
  }

  @Test
  public void testValidatePModeConfigSecurityPModeAuthorizeTrue ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPModeConfig.getLeg1 ().getSecurity ();
    aSecurityLeg.setPModeAuthorize (true);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          null,
                                          null,
                                          aSecurityLeg));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("false")));
  }

  @Test
  public void testValidatePModeConfigSecurityResponsePatternWrongBoolean ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPModeConfig.getLeg1 ().getSecurity ();
    aSecurityLeg.setSendReceipt (true);
    aSecurityLeg.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.CALLBACK);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          null,
                                          null,
                                          aSecurityLeg));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("Only response is allowed as pattern")));
  }

  // Error Handling

  @Test
  public void testValidatePModeConfigErrorHandlingMandatory ()
  {
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          null,
                                          null,
                                          null));

    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("No ErrorHandling Parameter present but they are mandatory")));
  }

  @Test
  public void testValidatePModeConfigErrorHandlingReportAsResponseMandatory ()
  {
    final PModeLegErrorHandling aErrorHandler = new PModeLegErrorHandling (null,
                                                                           null,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          aErrorHandler,
                                          null,
                                          null));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ReportAsResponse is a mandatory PMode parameter")));
  }

  @Test
  public void testValidatePModeConfigErrorHandlingReportAsResponseWrongValue ()
  {
    final PModeLegErrorHandling aErrorHandler = new PModeLegErrorHandling (null,
                                                                           null,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED);
    aErrorHandler.setReportAsResponse (false);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          aErrorHandler,
                                          null,
                                          null));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("PMode ReportAsResponse has to be True")));
  }

  @Test
  public void testValidatePModeConfigErrorHandlingReportProcessErrorNotifyConsumerMandatory ()
  {
    final PModeLegErrorHandling aErrorHandler = new PModeLegErrorHandling (null,
                                                                           null,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          aErrorHandler,
                                          null,
                                          null));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ReportProcessErrorNotifyConsumer is a mandatory PMode parameter")));
  }

  @Test
  public void testValidatePModeConfigErrorHandlingReportProcessErrorNotifyConsumerWrongValue ()
  {
    final PModeLegErrorHandling aErrorHandler = new PModeLegErrorHandling (null,
                                                                           null,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED);
    aErrorHandler.setReportProcessErrorNotifyConsumer (false);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          aErrorHandler,
                                          null,
                                          null));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("PMode ReportProcessErrorNotifyConsumer has to be True")));
  }

  @Test
  public void testValidatePModeConfigErrorHandlingReportDeliveryFailuresNotifyProducerMandatory ()
  {
    final PModeLegErrorHandling aErrorHandler = new PModeLegErrorHandling (null,
                                                                           null,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          aErrorHandler,
                                          null,
                                          null));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ReportDeliveryFailuresNotifyProducer is a mandatory PMode parameter")));
  }

  @Test
  public void testValidatePModeConfigErrorHandlingReportDeliveryFailuresNotifyProducerWrongValue ()
  {
    final PModeLegErrorHandling aErrorHandler = new PModeLegErrorHandling (null,
                                                                           null,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED);
    aErrorHandler.setReportDeliveryFailuresNotifyProducer (false);
    m_aPModeConfig.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                          null,
                                          aErrorHandler,
                                          null,
                                          null));
    aESENSCompatibilityValidator.validatePModeConfig (m_aPModeConfig, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("PMode ReportDeliveryFailuresNotifyProducer has to be True")));
  }

  @Test
  public void testValidateUserMessageNoMessageID ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (new Ebms3MessageInfo ());
    aESENSCompatibilityValidator.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MessageID is missing")));
  }

  @Test
  public void testValidateUserMessageMoreThanOnePartyID ()
  {
    final Ebms3PartyId aFirstId = new Ebms3PartyId ();
    aFirstId.setType ("type");
    aFirstId.setValue ("value");
    final Ebms3PartyId aSecondId = new Ebms3PartyId ();
    aSecondId.setType ("type2");
    aSecondId.setValue ("value2");

    final Ebms3From aFromPart = new Ebms3From ();
    aFromPart.addPartyId (aFirstId);
    aFromPart.addPartyId (aSecondId);
    final Ebms3To aToPart = new Ebms3To ();
    aToPart.addPartyId (aFirstId);
    aToPart.addPartyId (aSecondId);
    final Ebms3PartyInfo aPartyInfo = new Ebms3PartyInfo ();
    aPartyInfo.setFrom (aFromPart);
    aPartyInfo.setTo (aToPart);

    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setPartyInfo (aPartyInfo);

    aESENSCompatibilityValidator.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("Only 1 PartyID is allowed")));
  }

  @Test
  public void testValidateSignalMessageNoMessageID ()
  {
    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();
    aSignalMessage.setMessageInfo (new Ebms3MessageInfo ());
    aESENSCompatibilityValidator.validateSignalMessage (aSignalMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MessageID is missing")));
  }

}
