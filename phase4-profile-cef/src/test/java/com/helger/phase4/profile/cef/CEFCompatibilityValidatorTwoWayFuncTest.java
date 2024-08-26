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
package com.helger.phase4.profile.cef;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.helger.commons.error.list.ErrorList;
import com.helger.commons.state.ETriState;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.ebms3header.Ebms3From;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3PartyId;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3To;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.phase4.model.pmode.leg.PModeLegProtocol;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;
import com.helger.phase4.profile.IAS4ProfileValidator.EAS4ProfileValidationMode;
import com.helger.phase4.wss.EWSSVersion;
import com.helger.photon.app.mock.PhotonAppWebTestRule;

/**
 * Test class for two-way pmode creation.
 *
 * @author Philip Helger
 */
public final class CEFCompatibilityValidatorTwoWayFuncTest
{
  @ClassRule
  public static final PhotonAppWebTestRule RULE = new PhotonAppWebTestRule ();

  private static final Locale LOCALE = Locale.US;
  private static final CEFCompatibilityValidator VALIDATOR = new CEFCompatibilityValidator ().setExpectFourCornerModel (true);

  private PMode m_aPMode;
  private ErrorList m_aErrorList;

  @Before
  public void before ()
  {
    m_aErrorList = new ErrorList ();
    m_aPMode = CEFPMode.createCEFPModeTwoWay ("TestInitiator",
                                              "TestResponder",
                                              "http://localhost:8080",
                                              IPModeIDProvider.DEFAULT_DYNAMIC,
                                              false);
  }

  @Test
  public void testValidatePModeWrongMEP ()
  {
    m_aPMode.setMEP (EMEP.TWO_WAY);
    // Only 2-way push-push allowed
    m_aPMode.setMEPBinding (EMEPBinding.PULL);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);

    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MEP")));
  }

  @Test
  public void testValidatePModeWrongMEPBinding ()
  {
    // SYNC not allowed
    m_aPMode.setMEPBinding (EMEPBinding.SYNC);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);

    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MEP binding")));
  }

  @Test
  public void testValidatePModeNoLeg ()
  {
    m_aPMode.setLeg1 (null);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("PMode.Leg[1] is missing")));
  }

  @Test
  public void testValidatePModeNoLeg2 ()
  {
    m_aPMode.setLeg2 (null);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("PMode.Leg[2] is missing")));
  }

  @Test
  public void testValidatePModeNoProtocol ()
  {
    m_aPMode.setLeg2 (new PModeLeg (null, null, null, null, null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("Protocol is missing")));
  }

  @Test
  @Ignore ("The response address is most of the time not set")
  public void testValidatePModeNoProtocolAddress ()
  {
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion (null), null, null, null, null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("AddressProtocol is missing")));
  }

  @Test
  public void testValidatePModeProtocolAddressIsNotHttp ()
  {
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("ftp://test.com"),
                                    null,
                                    null,
                                    null,
                                    null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("AddressProtocol 'ftp' is unsupported")));
  }

  @Test
  public void testValidatePModeProtocolSOAP11NotAllowed ()
  {
    m_aPMode.setLeg2 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESoapVersion.SOAP_11),
                                    null,
                                    null,
                                    null,
                                    null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("SoapVersion '1.1' is unsupported")));
  }

  @Test
  @Ignore ("The X509 certificate is always null, as it is received from the SMP")
  public void testValidatePModeSecurityNoX509SignatureCertificate ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509SignatureCertificate (null);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("X509SignatureCertificate is missing")));
  }

  @Test
  public void testValidatePModeSecurityNoX509SignatureAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509SignatureAlgorithm (null);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("X509SignatureAlgorithm is missing")));
  }

  @Test
  public void testValidatePModeSecurityWrongX509SignatureAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509SignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_384);
    assertNotSame (ECryptoAlgorithmSign.RSA_SHA_256, aSecurityLeg.getX509SignatureAlgorithm ());
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains (ECryptoAlgorithmSign.RSA_SHA_256.getID ())));
  }

  @Test
  public void testValidatePModeSecurityNoX509SignatureHashFunction ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509SignatureHashFunction (null);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("X509SignatureHashFunction is missing")));
  }

  @Test
  public void testValidatePModeSecurityWrongX509SignatureHashFunction ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_512);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains (ECryptoAlgorithmSignDigest.DIGEST_SHA_256.getID ())));
  }

  @Test
  public void testValidatePModeSecurityNoX509EncryptionAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509EncryptionAlgorithm (null);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("X509EncryptionAlgorithm is missing")));
  }

  @Test
  public void testValidatePModeSecurityWrongX509EncryptionAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.AES_192_CBC);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains (ECryptoAlgorithmCrypt.AES_128_GCM.getID ())));
  }

  @SuppressWarnings ("deprecation")
  @Test
  public void testValidatePModeSecurityWrongWSSVersion ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setWSSVersion (EWSSVersion.WSS_10);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("Security.WSSVersion must use the value WSS_111 instead of WSS_10")));
  }

  @Test
  public void testValidatePModeSecurityPModeAuthorizeMandatory ()
  {
    m_aPMode.getLeg2 ().getSecurity ().setPModeAuthorize (ETriState.UNDEFINED);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue ("Errors: " + m_aErrorList.toString (),
                m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("Security.PModeAuthorize is missing")));
  }

  @Test
  public void testValidatePModeSecurityPModeAuthorizeTrue ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setPModeAuthorize (true);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("false")));
  }

  @Test
  public void testValidatePModeSecurityResponsePatternWrongBoolean ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setSendReceipt (true);
    aSecurityLeg.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.CALLBACK);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("Security.SendReceiptReplyPattern must use the value RESPONSE instead of CALLBACK")));
  }

  // Error Handling

  @Test
  public void testValidatePModeErrorHandlingMandatory ()
  {
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    null));

    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("PMode.Leg[2].ErrorHandling is missing")));
  }

  @Test
  public void testValidatePModeErrorHandlingReportAsResponseMandatory ()
  {
    final PModeLegErrorHandling aErrorHandler = PModeLegErrorHandling.createUndefined ();
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    aErrorHandler,
                                    null,
                                    null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ErrorHandling.Report.AsResponse is missing")));
  }

  @Test
  public void testValidatePModeErrorHandlingReportAsResponseWrongValue ()
  {
    final PModeLegErrorHandling aErrorHandler = PModeLegErrorHandling.createUndefined ();
    aErrorHandler.setReportAsResponse (false);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    aErrorHandler,
                                    null,
                                    null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ErrorHandling.Report.AsResponse must be 'true'")));
  }

  @Test
  public void testValidatePModeErrorHandlingReportProcessErrorNotifyConsumerMandatory ()
  {
    final PModeLegErrorHandling aErrorHandler = PModeLegErrorHandling.createUndefined ();
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    aErrorHandler,
                                    null,
                                    null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ErrorHandling.Report.ProcessErrorNotifyConsumer is missing")));
  }

  @Test
  public void testValidatePModeErrorHandlingReportProcessErrorNotifyConsumerWrongValue ()
  {
    final PModeLegErrorHandling aErrorHandler = PModeLegErrorHandling.createUndefined ();
    aErrorHandler.setReportProcessErrorNotifyConsumer (false);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    aErrorHandler,
                                    null,
                                    null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ErrorHandling.Report.ProcessErrorNotifyConsumer should be 'true'")));
  }

  @Test
  public void testValidatePModeErrorHandlingReportDeliveryFailuresNotifyProducerMandatory ()
  {
    final PModeLegErrorHandling aErrorHandler = PModeLegErrorHandling.createUndefined ();
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    aErrorHandler,
                                    null,
                                    null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ErrorHandling.Report.ProcessErrorNotifyProducer is missing")));
  }

  @Test
  public void testValidatePModeErrorHandlingReportDeliveryFailuresNotifyProducerWrongValue ()
  {
    final PModeLegErrorHandling aErrorHandler = PModeLegErrorHandling.createUndefined ();
    aErrorHandler.setReportProcessErrorNotifyProducer (false);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    aErrorHandler,
                                    null,
                                    null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ErrorHandling.Report.ProcessErrorNotifyProducer should be 'true'")));
  }

  @Test
  public void testValidateUserMessageNoMessageID ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (new Ebms3MessageInfo ());
    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MessageInfo/MessageId is missing")));
  }

  @Test
  public void testValidateUserMessageMoreThanOnePartyID ()
  {
    final Ebms3PartyId aFirstId = MessageHelperMethods.createEbms3PartyId ("type", "value");
    final Ebms3PartyId aSecondId = MessageHelperMethods.createEbms3PartyId ("type2", "value2");

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

    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("must contain no more than one PartyID")));
  }

  @Test
  public void testValidateSignalMessageNoMessageID ()
  {
    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();
    aSignalMessage.setMessageInfo (new Ebms3MessageInfo ());
    VALIDATOR.validateSignalMessage (aSignalMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MessageId is missing")));
  }
}
