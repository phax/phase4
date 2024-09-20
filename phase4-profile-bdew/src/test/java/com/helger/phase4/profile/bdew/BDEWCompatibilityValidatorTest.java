/*
 * Copyright (C) 2023-2024 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
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
package com.helger.phase4.profile.bdew;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.helger.bc.PBCProvider;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.state.ETriState;
import com.helger.phase4.CAS4;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.ebms3header.Ebms3AgreementRef;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3From;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3PartyId;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3To;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.AS4IncomingMessageMetadata;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.phase4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.phase4.model.pmode.leg.PModeLegProtocol;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;
import com.helger.phase4.profile.IAS4ProfileValidator.EAS4ProfileValidationMode;
import com.helger.phase4.wss.EWSSVersion;
import com.helger.photon.app.mock.PhotonAppWebTestRule;

/**
 * All essentials need to be set and need to be not null since they are getting
 * checked, when a PMode is introduced into the system and these null checks
 * would be redundant in the profiles.
 *
 * @author Gregor Scholtysik
 */
public final class BDEWCompatibilityValidatorTest
{
  @ClassRule
  public static final PhotonAppWebTestRule RULE = new PhotonAppWebTestRule ();

  private static final Locale LOCALE = Locale.US;
  private static final BDEWCompatibilityValidator VALIDATOR = new BDEWCompatibilityValidator ();

  private PMode m_aPMode;
  private ErrorList m_aErrorList;

  @BeforeClass
  public static void beforeClass ()
  {
    // Required for certificate check
    Security.addProvider (PBCProvider.getProvider ());
  }

  @Before
  public void before ()
  {
    m_aErrorList = new ErrorList ();
    m_aPMode = BDEWPMode.createBDEWPMode ("TestInitiator",
                                          BDEWPMode.BDEW_PARTY_ID_TYPE_BDEW,
                                          "TestResponder",
                                          BDEWPMode.BDEW_PARTY_ID_TYPE_BDEW,
                                          "http://localhost:8080",
                                          IPModeIDProvider.DEFAULT_DYNAMIC,
                                          true);
  }

  @Test
  public void testValidatePModeAgreementMandatory ()
  {
    m_aPMode.setAgreement (null);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);

    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("PMode.Agreement must be set to '" +
                                                           BDEWPMode.DEFAULT_AGREEMENT_ID +
                                                           "'")));
  }

  @Test
  public void testValidatePModeAgreementWrongValue ()
  {
    m_aPMode.setAgreement ("http://test.example.org");
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);

    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("PMode.Agreement must be set to '" +
                                                           BDEWPMode.DEFAULT_AGREEMENT_ID +
                                                           "'")));
  }

  @Test
  public void testValidatePModeWrongMEP ()
  {
    m_aPMode.setMEP (EMEP.TWO_WAY);
    // Only one-way push allowed
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
  public void testValidatePModeInitiatorRoleWrongValue ()
  {
    final PModeParty aInitiatorParty = PModeParty.createSimple ("id", "http://test.example.org");

    m_aPMode.setInitiator (aInitiatorParty);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("Initiator.Role must be set to '" +
                                                           CAS4.DEFAULT_INITIATOR_URL +
                                                           "'")));
  }

  @Test
  public void testValidatePModeResponderRoleWrongValue ()
  {
    final PModeParty aResponderParty = PModeParty.createSimple ("id", "http://test.example.org");

    m_aPMode.setResponder (aResponderParty);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("Responder.Role must be set to '" +
                                                           CAS4.DEFAULT_RESPONDER_URL +
                                                           "'")));
  }

  @Test
  public void testValidatePModeNoLeg ()
  {
    m_aPMode.setLeg1 (null);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("PMode.Leg[1] is missing")));
  }

  @Test
  public void testValidatePModeNoProtocol ()
  {
    m_aPMode.setLeg1 (new PModeLeg (null, null, null, null, null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("Protocol is missing")));
  }

  @Test
  @Ignore ("The response address is most of the time not set")
  public void testValidatePModeNoProtocolAddress ()
  {
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion (null), null, null, null, null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("AddressProtocol is missing")));
  }

  @Test
  public void testValidatePModeProtocolAddressIsNotHttp ()
  {
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("ftp://test.com"),
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
    m_aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESoapVersion.SOAP_11),
                                    null,
                                    null,
                                    null,
                                    null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("SoapVersion '1.1' is unsupported")));
  }

  @Test
  public void testValidatePModeSecurityNoX509SignatureAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureAlgorithm (null);
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    PModeLegErrorHandling.createUndefined (),
                                    null,
                                    aSecurityLeg));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("X509SignatureAlgorithm is missing")));
  }

  @Test
  public void testValidatePModeSecurityWrongX509SignatureAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureAlgorithm (ECryptoAlgorithmSign.ECDSA_SHA_384);
    assertNotSame (ECryptoAlgorithmSign.ECDSA_SHA_256, aSecurityLeg.getX509SignatureAlgorithm ());
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains (ECryptoAlgorithmSign.ECDSA_SHA_256.getID ())));
  }

  @Test
  public void testValidatePModeSecurityNoX509SignatureHashFunction ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureHashFunction (null);
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
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
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_512);
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
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
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509EncryptionAlgorithm (null);
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
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
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.AES_192_CBC);
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
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
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setWSSVersion (EWSSVersion.WSS_10);
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
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
    m_aPMode.getLeg1 ().getSecurity ().setPModeAuthorize (ETriState.UNDEFINED);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue ("Errors: " + m_aErrorList.toString (),
                m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("Security.PModeAuthorize is missing")));
  }

  @Test
  public void testValidatePModeSecurityPModeAuthorizeTrue ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setPModeAuthorize (true);
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("false")));
  }

  @Test
  public void testValidatePModeSecuritySendReceiptMandatory ()
  {
    m_aPMode.getLeg1 ().getSecurity ().setSendReceipt (ETriState.UNDEFINED);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue ("Errors: " + m_aErrorList.toString (),
                m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("Security.SendReceipt must be defined and set to 'true'")));
  }

  @Test
  public void testValidatePModeSecuritySendReceiptTrue ()
  {
    m_aPMode.getLeg1 ().getSecurity ().setSendReceipt (ETriState.FALSE);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue ("Errors: " + m_aErrorList.toString (),
                m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("Security.SendReceipt must be defined and set to 'true'")));
  }

  @Test
  public void testValidatePModeSecurityResponsePatternWrongBoolean ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setSendReceipt (true);
    aSecurityLeg.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.CALLBACK);
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
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
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    null));

    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("PMode.Leg[1].ErrorHandling is missing")));
  }

  @Test
  public void testValidatePModeErrorHandlingReportAsResponseMandatory ()
  {
    final PModeLegErrorHandling aErrorHandler = PModeLegErrorHandling.createUndefined ();
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
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
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
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
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
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
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
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
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
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
    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    null,
                                    aErrorHandler,
                                    null,
                                    null));
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ErrorHandling.Report.ProcessErrorNotifyProducer should be 'true'")));
  }

  @Test
  public void testValidatePModeBusinessInfoWrongService ()
  {
    final PModeLegBusinessInformation aBusinessInformation = BDEWPMode.generatePModeLegBusinessInformation ();
    aBusinessInformation.setService ("http://test.example.org");
    aBusinessInformation.setAction (BDEWPMode.ACTION_DEFAULT);

    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    aBusinessInformation,
                                    null,
                                    null,
                                    null));

    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("BusinessInfo.Service 'http://test.example.org' is unsupported")));
  }

  @Test
  public void testValidatePModeBusinessInfoWrongAction ()
  {
    final PModeLegBusinessInformation aBusinessInformation = BDEWPMode.generatePModeLegBusinessInformation ();
    aBusinessInformation.setService (BDEWPMode.SERVICE_TEST);
    aBusinessInformation.setAction ("http://test.example.org");

    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("http://test.example.org"),
                                    aBusinessInformation,
                                    null,
                                    null,
                                    null));

    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("BusinessInfo.Action 'http://test.example.org' is unsupported")));
  }

  @Test
  public void testValidatePModeNoX509EncryptionMinimalStrength ()
  {
    m_aPMode.getLeg1 ().getSecurity ().setX509EncryptionMinimumStrength (null);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("X509Encryption.MinimalStrength must be defined and set to 128")));
  }

  @Test
  public void testValidatePModeX509EncryptionMinimalStrengthWrongValue ()
  {
    m_aPMode.getLeg1 ().getSecurity ().setX509EncryptionMinimumStrength (256);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("X509Encryption.MinimalStrength must be defined and set to 128")));
  }

  @Test
  public void testValidatePModeReceptionAwarenessMandatory ()
  {
    m_aPMode.getReceptionAwareness ().setReceptionAwareness (ETriState.UNDEFINED);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ReceptionAwareness must be defined and set to 'true'")));
  }

  @Test
  public void testValidatePModeReceptionAwarenessWrongValue ()
  {
    m_aPMode.getReceptionAwareness ().setReceptionAwareness (ETriState.FALSE);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ReceptionAwareness must be defined and set to 'true'")));
  }

  @Test
  public void testValidatePModeReceptionAwarenessRetryMandatory ()
  {
    m_aPMode.getReceptionAwareness ().setRetry (ETriState.UNDEFINED);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ReceptionAwareness.Retry must be defined and set to 'true'")));
  }

  @Test
  public void testValidatePModeReceptionAwarenessRetryWrongValue ()
  {
    m_aPMode.getReceptionAwareness ().setRetry (ETriState.FALSE);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ReceptionAwareness.Retry must be defined and set to 'true'")));
  }

  @Test
  public void testValidatePModeReceptionAwarenessDuplicateDetectionMandatory ()
  {
    m_aPMode.getReceptionAwareness ().setDuplicateDetection (ETriState.UNDEFINED);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ReceptionAwareness.DuplicateDetection must be defined and set to 'true'")));
  }

  @Test
  public void testValidatePModeReceptionAwarenessDuplicateDetectionWrongValue ()
  {
    m_aPMode.getReceptionAwareness ().setDuplicateDetection (ETriState.FALSE);
    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ReceptionAwareness.DuplicateDetection must be defined and set to 'true'")));
  }

  @Test
  public void testValidatePModeCorrect ()
  {
    final PModeLegBusinessInformation aBusinessInformation = BDEWPMode.generatePModeLegBusinessInformation ();
    aBusinessInformation.setService (BDEWPMode.SERVICE_TEST);
    aBusinessInformation.setAction (BDEWPMode.ACTION_TEST_SERVICE);

    final PModeLegErrorHandling aErrorHandler = PModeLegErrorHandling.createUndefined ();
    aErrorHandler.setReportAsResponse (true);
    aErrorHandler.setReportProcessErrorNotifyConsumer (true);
    aErrorHandler.setReportProcessErrorNotifyProducer (true);

    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setSendReceipt (true);
    aSecurityLeg.setPModeAuthorize (false);

    m_aPMode.setLeg1 (new PModeLeg (PModeLegProtocol.createForDefaultSoapVersion ("https://test.example.org"),
                                    aBusinessInformation,
                                    aErrorHandler,
                                    null,
                                    aSecurityLeg));

    VALIDATOR.validatePMode (m_aPMode, m_aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue (m_aErrorList.isEmpty ());
  }

  @Test
  public void testValidateUserMessageNoMessageInfo ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (null);
    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MessageInfo is missing")));
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
  public void testValidateUserMessageRefToMessageIDNotSet ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (new Ebms3MessageInfo ());
    aUserMessage.getMessageInfo ().setRefToMessageId ("RefToMessageId");
    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("MessageInfo/RefToMessageId must not be set")));
  }

  @Test
  public void testValidateUserMessageNoPartyInfo ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (new Ebms3MessageInfo ());
    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("PartyInfo is missing")));
  }

  @Test
  public void testValidateUserMessageNoPartyInfoFromAndTo ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (new Ebms3MessageInfo ());
    aUserMessage.setPartyInfo (new Ebms3PartyInfo ());
    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("PartyInfo/From is missing")));
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("PartyInfo/To is missing")));
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
  public void testValidateUserMessagePartyInfoWrongRoles ()
  {
    final Ebms3PartyId aFirstId = MessageHelperMethods.createEbms3PartyId ("type", "value");
    final Ebms3PartyId aSecondId = MessageHelperMethods.createEbms3PartyId ("type2", "value2");

    final Ebms3From aFromPart = new Ebms3From ();
    aFromPart.addPartyId (aFirstId);
    aFromPart.addPartyId (aSecondId);
    aFromPart.setRole ("http://test.example.org");

    final Ebms3To aToPart = new Ebms3To ();
    aToPart.addPartyId (aFirstId);
    aToPart.addPartyId (aSecondId);
    aToPart.setRole ("http://test.example.org");

    final Ebms3PartyInfo aPartyInfo = new Ebms3PartyInfo ();
    aPartyInfo.setFrom (aFromPart);
    aPartyInfo.setTo (aToPart);

    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setPartyInfo (aPartyInfo);

    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("PartyInfo/From/Role must be set to '" +
                                                           CAS4.DEFAULT_INITIATOR_URL +
                                                           "'")));
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("PartyInfo/To/Role must be set to '" +
                                                           CAS4.DEFAULT_RESPONDER_URL +
                                                           "'")));
  }

  @Test
  public void testValidateUserMessageNoCollaborationInfo ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (new Ebms3MessageInfo ());
    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("CollaborationInfo is missing")));
  }

  @Test
  public void testValidateUserMessageNoCollaborationInfoAgreementRef ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (new Ebms3MessageInfo ());
    aUserMessage.setCollaborationInfo (new Ebms3CollaborationInfo ());
    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("CollaborationInfo/AgreementRef must be set!")));
  }

  @Test
  public void testValidateUserMessageEmptyCollaborationInfoAgreementRef ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (new Ebms3MessageInfo ());
    final Ebms3CollaborationInfo collaborationInfo = new Ebms3CollaborationInfo ();
    collaborationInfo.setAgreementRef ("");
    aUserMessage.setCollaborationInfo (collaborationInfo);
    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("CollaborationInfo/AgreementRef value is missing")));
  }

  @Test
  public void testValidateUserMessageWrongCollaborationInfoAgreementRef ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (new Ebms3MessageInfo ());
    final Ebms3CollaborationInfo collaborationInfo = new Ebms3CollaborationInfo ();
    collaborationInfo.setAgreementRef ("AgreementRef");
    aUserMessage.setCollaborationInfo (collaborationInfo);
    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("CollaborationInfo/AgreementRef value must equal " +
                                                           BDEWPMode.DEFAULT_AGREEMENT_ID)));
  }

  @Test
  public void testValidateUserMessageWrongCollaborationInfoAgreementRefPMode ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (new Ebms3MessageInfo ());
    final Ebms3CollaborationInfo collaborationInfo = new Ebms3CollaborationInfo ();
    final Ebms3AgreementRef agreementRef = new Ebms3AgreementRef ();
    agreementRef.setPmode ("PModeId");
    collaborationInfo.setAgreementRef (agreementRef);
    aUserMessage.setCollaborationInfo (collaborationInfo);
    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("CollaborationInfo/PMode must not be set!")));
  }

  @Test
  public void testValidateUserMessageWrongCollaborationInfoAgreementRefType ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (new Ebms3MessageInfo ());
    final Ebms3CollaborationInfo collaborationInfo = new Ebms3CollaborationInfo ();
    final Ebms3AgreementRef agreementRef = new Ebms3AgreementRef ();
    agreementRef.setType ("Type");
    collaborationInfo.setAgreementRef (agreementRef);
    aUserMessage.setCollaborationInfo (collaborationInfo);
    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("CollaborationInfo/Type must not be set!")));
  }

  @Test
  public void testValidateUserMessageWrongCollaborationInfoService ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (new Ebms3MessageInfo ());
    final Ebms3CollaborationInfo aCollaborationInfo = new Ebms3CollaborationInfo ();
    aCollaborationInfo.setService ("http://test.example.org");

    aUserMessage.setCollaborationInfo (aCollaborationInfo);
    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("CollaborationInfo/Service 'http://test.example.org' is unsupported")));
  }

  @Test
  public void testValidateUserMessageWrongCollaborationInfoAction ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setMessageInfo (new Ebms3MessageInfo ());
    final Ebms3CollaborationInfo aCollaborationInfo = new Ebms3CollaborationInfo ();
    aCollaborationInfo.setAction ("http://test.example.org");

    aUserMessage.setCollaborationInfo (aCollaborationInfo);
    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("CollaborationInfo/Action 'http://test.example.org' is unsupported")));
  }

  @Test
  public void testValidateUserMessageCorrect ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();

    final Ebms3MessageInfo aMessageInfo = new Ebms3MessageInfo ();
    aMessageInfo.setMessageId (UUID.randomUUID ().toString ());
    aUserMessage.setMessageInfo (aMessageInfo);

    final Ebms3From aFromPart = new Ebms3From ();
    aFromPart.addPartyId (new Ebms3PartyId ("1"));
    aFromPart.setRole (CAS4.DEFAULT_INITIATOR_URL);
    final Ebms3To aToPart = new Ebms3To ();
    aToPart.addPartyId (new Ebms3PartyId ("2"));
    aToPart.setRole (CAS4.DEFAULT_RESPONDER_URL);
    final Ebms3PartyInfo aPartyInfo = new Ebms3PartyInfo ();
    aPartyInfo.setFrom (aFromPart);
    aPartyInfo.setTo (aToPart);
    aUserMessage.setPartyInfo (aPartyInfo);

    final Ebms3CollaborationInfo aCollaborationInfo = new Ebms3CollaborationInfo ();
    aCollaborationInfo.setAgreementRef (BDEWPMode.DEFAULT_AGREEMENT_ID);
    aCollaborationInfo.setService (BDEWPMode.SERVICE_TEST);
    aCollaborationInfo.setAction (BDEWPMode.ACTION_TEST_SERVICE);

    aUserMessage.setCollaborationInfo (aCollaborationInfo);

    VALIDATOR.validateUserMessage (aUserMessage, m_aErrorList);
    assertTrue (m_aErrorList.isEmpty ());
  }

  @Test
  public void testValidateSignalMessageNoMessageID ()
  {
    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();
    aSignalMessage.setMessageInfo (new Ebms3MessageInfo ());
    VALIDATOR.validateSignalMessage (aSignalMessage, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MessageInfo/MessageId is missing")));
  }

  @Test
  public void testValidateInitiatorIdentityNonEmtMakoTls () throws CertificateException, NoSuchProviderException
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    final AS4IncomingMessageMetadata aIncomingMessageMetadata = AS4IncomingMessageMetadata.createForRequest ();

    // Set as TLS certificates
    final CertificateFactory aCertificateFactory = CertificateFactory.getInstance ("X509",
                                                                                   BouncyCastleProvider.PROVIDER_NAME);
    @SuppressWarnings ("unchecked")
    final Collection <X509Certificate> aCertificates = (Collection <X509Certificate>) aCertificateFactory.generateCertificates (BDEWCompatibilityValidator.class.getResourceAsStream ("nonemtmako.cert"));
    assertNotNull (aCertificates);
    aIncomingMessageMetadata.setRemoteTlsCerts (aCertificates.toArray (new X509Certificate [0]));

    // Check compliance
    VALIDATOR.validateInitiatorIdentity (aUserMessage, null, aIncomingMessageMetadata, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("is not an EMT/MAKO certificate")));
  }

  @Test
  public void testValidateInitiatorIdentityNonEmtMakoSig () throws CertificateException, NoSuchProviderException
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    final AS4IncomingMessageMetadata aIncomingMessageMetadata = AS4IncomingMessageMetadata.createForRequest ();

    // Build as signature certificate
    final CertificateFactory aCertificateFactory = CertificateFactory.getInstance ("X509",
                                                                                   BouncyCastleProvider.PROVIDER_NAME);
    final X509Certificate aCertificate = (X509Certificate) aCertificateFactory.generateCertificate (BDEWCompatibilityValidator.class.getResourceAsStream ("nonemtmako.cert"));
    assertNotNull (aCertificate);

    // Check compliance
    VALIDATOR.validateInitiatorIdentity (aUserMessage, aCertificate, aIncomingMessageMetadata, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("is not an EMT/MAKO certificate")));
  }
}
