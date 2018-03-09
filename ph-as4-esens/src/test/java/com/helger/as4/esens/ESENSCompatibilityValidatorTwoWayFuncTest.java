/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.pmode.IPModeIDProvider;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.leg.EPModeSendReceiptReplyPattern;
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

public final class ESENSCompatibilityValidatorTwoWayFuncTest
{
  @ClassRule
  public static final PhotonBasicWebTestRule s_aRule = new PhotonBasicWebTestRule ();

  private static final Locale LOCALE = Locale.US;
  private final ESENSCompatibilityValidator aESENSCompatibilityValidator = new ESENSCompatibilityValidator ();

  private PMode m_aPMode;
  private ErrorList m_aErrorList;

  @Before
  public void setUp ()
  {
    m_aErrorList = new ErrorList ();
    m_aPMode = ESENSPMode.createESENSPModeTwoWay ("TestInitiator",
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
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);

    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MEP")));
  }

  @Test
  public void testValidatePModeWrongMEPBinding ()
  {
    // SYNC not allowed
    m_aPMode.setMEPBinding (EMEPBinding.SYNC);
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);

    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MEP binding")));
  }

  @Test
  public void testValidatePModeNoLeg ()
  {
    m_aPMode.setLeg1 (null);
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("PMode is missing Leg 1")));
  }

  @Test
  public void testValidatePModeNoLeg2 ()
  {
    m_aPMode.setLeg2 (null);
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("PMode is missing Leg 2")));
  }

  @Test
  public void testValidatePModeNoProtocol ()
  {
    m_aPMode.setLeg2 (new PModeLeg (null, null, null, null, null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("Protocol")));
  }

  @Test
  public void testValidatePModeNoProtocolAddress ()
  {
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion (null), null, null, null, null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("AddressProtocol")));
  }

  @Test
  public void testValidatePModeProtocolAddressIsNotHttp ()
  {
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("ftp://test.com"),
                                    null,
                                    null,
                                    null,
                                    null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("non-standard AddressProtocol: ftp")));
  }

  @Test
  public void testValidatePModeProtocolSOAP11NotAllowed ()
  {
    m_aPMode.setLeg2 (new PModeLeg (new PModeLegProtocol ("https://test.com",
                                                          ESOAPVersion.SOAP_11),
                                    null,
                                    null,
                                    null,
                                    null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("1.1")));
  }

  @Test
  // TODO re-enable if we know what we want
  @Ignore ("Certificate check was a TODO")
  public void testValidatePModeSecurityNoX509SignatureCertificate ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509SignatureCertificate (null);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("signature certificate")));
  }

  @Test
  public void testValidatePModeSecurityNoX509SignatureAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509SignatureAlgorithm (null);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("signature algorithm")));
  }

  @Test
  public void testValidatePModeSecurityWrongX509SignatureAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509SignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_384);
    assertNotSame (aSecurityLeg.getX509SignatureAlgorithm (), ECryptoAlgorithmSign.RSA_SHA_256);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains (ECryptoAlgorithmSign.RSA_SHA_256.getID ())));
  }

  @Test
  public void testValidatePModeSecurityNoX509SignatureHashFunction ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509SignatureHashFunction (null);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("hash function")));
  }

  @Test
  public void testValidatePModeSecurityWrongX509SignatureHashFunction ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_512);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains (ECryptoAlgorithmSignDigest.DIGEST_SHA_256.getID ())));
  }

  @Test
  public void testValidatePModeSecurityNoX509EncryptionAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509EncryptionAlgorithm (null);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("encryption algorithm")));
  }

  @Test
  public void testValidatePModeSecurityWrongX509EncryptionAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.AES_192_CBC);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains (ECryptoAlgorithmCrypt.AES_128_GCM.getID ())));
  }

  @SuppressWarnings ("deprecation")
  @Test
  public void testValidatePModeSecurityWrongWSSVersion ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setWSSVersion (EWSSVersion.WSS_10);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("Wrong WSS Version")));
  }

  @Test
  public void testValidatePModeSecurityPModeAuthorizeMandatory ()
  {
    m_aPMode.getLeg2 ().getSecurity ().setPModeAuthorize (ETriState.UNDEFINED);
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue ("Errors: " + m_aErrorList.toString (),
                m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("mandatory")));
  }

  @Test
  public void testValidatePModeSecurityPModeAuthorizeTrue ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setPModeAuthorize (true);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("false")));
  }

  @Test
  public void testValidatePModeSecurityResponsePatternWrongBoolean ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getLeg2 ().getSecurity ();
    aSecurityLeg.setSendReceipt (true);
    aSecurityLeg.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.CALLBACK);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("Only response is allowed as pattern")));
  }

  // Error Handling

  @Test
  public void testValidatePModeErrorHandlingMandatory ()
  {
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    null,
                                    null,
                                    null));

    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("No ErrorHandling Parameter present but they are mandatory")));
  }

  @Test
  public void testValidatePModeErrorHandlingReportAsResponseMandatory ()
  {
    final PModeLegErrorHandling aErrorHandler = new PModeLegErrorHandling (null,
                                                                           null,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    aErrorHandler,
                                    null,
                                    null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ReportAsResponse is a mandatory PMode parameter")));
  }

  @Test
  public void testValidatePModeErrorHandlingReportAsResponseWrongValue ()
  {
    final PModeLegErrorHandling aErrorHandler = new PModeLegErrorHandling (null,
                                                                           null,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED);
    aErrorHandler.setReportAsResponse (false);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    aErrorHandler,
                                    null,
                                    null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("PMode ReportAsResponse has to be True")));
  }

  @Test
  public void testValidatePModeErrorHandlingReportProcessErrorNotifyConsumerMandatory ()
  {
    final PModeLegErrorHandling aErrorHandler = new PModeLegErrorHandling (null,
                                                                           null,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    aErrorHandler,
                                    null,
                                    null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ReportProcessErrorNotifyConsumer is a mandatory PMode parameter")));
  }

  @Test
  public void testValidatePModeErrorHandlingReportProcessErrorNotifyConsumerWrongValue ()
  {
    final PModeLegErrorHandling aErrorHandler = new PModeLegErrorHandling (null,
                                                                           null,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED);
    aErrorHandler.setReportProcessErrorNotifyConsumer (false);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    aErrorHandler,
                                    null,
                                    null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("PMode ReportProcessErrorNotifyConsumer has to be True")));
  }

  @Test
  public void testValidatePModeErrorHandlingReportDeliveryFailuresNotifyProducerMandatory ()
  {
    final PModeLegErrorHandling aErrorHandler = new PModeLegErrorHandling (null,
                                                                           null,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    aErrorHandler,
                                    null,
                                    null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("ReportDeliveryFailuresNotifyProducer is a mandatory PMode parameter")));
  }

  @Test
  public void testValidatePModeErrorHandlingReportDeliveryFailuresNotifyProducerWrongValue ()
  {
    final PModeLegErrorHandling aErrorHandler = new PModeLegErrorHandling (null,
                                                                           null,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED);
    aErrorHandler.setReportDeliveryFailuresNotifyProducer (false);
    m_aPMode.setLeg2 (new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion ("http://test.example.org"),
                                    null,
                                    aErrorHandler,
                                    null,
                                    null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
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
