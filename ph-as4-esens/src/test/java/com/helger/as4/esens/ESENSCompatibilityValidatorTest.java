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
package com.helger.as4.esens;

import static org.junit.Assert.assertTrue;

import java.util.Locale;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.helger.as4lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.mock.MockPModeGenerator;
import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.as4lib.model.pmode.EPModeSendReceiptReplyPattern;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.model.pmode.PModeConfig;
import com.helger.as4lib.model.pmode.PModeLeg;
import com.helger.as4lib.model.pmode.PModeLegErrorHandling;
import com.helger.as4lib.model.pmode.PModeLegProtocol;
import com.helger.as4lib.model.pmode.PModeLegSecurity;
import com.helger.as4lib.model.pmode.PModeParty;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.wss.EWSSVersion;
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

  private PMode m_aPMode;
  private ErrorList m_aErrorList;

  @Before
  public void setUp ()
  {
    m_aErrorList = new ErrorList ();
    m_aPMode = MockPModeGenerator.getTestPModeWithSecurity (ESOAPVersion.SOAP_12);
  }

  @Nonnull
  private PModeConfig _getConfig ()
  {
    return (PModeConfig) m_aPMode.getConfig ();
  }

  @Test
  public void testValidatePModeWrongMEP ()
  {
    _getConfig ().setMEP (EMEP.TWO_WAY_PULL_PUSH);
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);

    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MEP")));
  }

  @Test
  public void testValidatePModeWrongMEPBinding ()
  {
    _getConfig ().setMEPBinding (ETransportChannelBinding.SYNC);
    m_aPMode.setResponder (new PModeParty ("sa", "id1", "role", "as", "as"));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);

    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("MEP-Binding")));
  }

  @Test
  public void testValidatePModeNoLeg ()
  {
    _getConfig ().setLeg1 (null);
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("PMode is missing Leg 1")));
  }

  @Test
  public void testValidatePModeNoProtocol ()
  {
    _getConfig ().setLeg1 (new PModeLeg (null, null, null, null, null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("Protocol")));
  }

  @Test
  public void testValidatePModeNoProtocolAddress ()
  {
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol (), null, null, null, null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("AddressProtocol")));
  }

  @Test
  public void testValidatePModeProtocolAddressIsNotHttps ()
  {
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("ftp://test.com", ESOAPVersion.AS4_DEFAULT),
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
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.SOAP_11),
                                         null,
                                         null,
                                         null,
                                         null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("1.1")));
  }

  @Test
  public void testValidatePModeSecurityNoX509SignatureCertificate ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getConfig ().getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureCertificate (null);
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
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
    final PModeLegSecurity aSecurityLeg = m_aPMode.getConfig ().getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureAlgorithm (null);
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
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
    final PModeLegSecurity aSecurityLeg = m_aPMode.getConfig ().getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_512);
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                         null,
                                         null,
                                         null,
                                         aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT.getID ())));
  }

  @Test
  public void testValidatePModeSecurityNoX509SignatureHashFunction ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getConfig ().getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureHashFunction (null);
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
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
    final PModeLegSecurity aSecurityLeg = m_aPMode.getConfig ().getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_512);
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                         null,
                                         null,
                                         null,
                                         aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT.getID ())));
  }

  @Test
  public void testValidatePModeSecurityNoX509EncryptionAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getConfig ().getLeg1 ().getSecurity ();
    aSecurityLeg.setX509EncryptionAlgorithm (null);
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
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
    final PModeLegSecurity aSecurityLeg = m_aPMode.getConfig ().getLeg1 ().getSecurity ();
    aSecurityLeg.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.CRYPT_CAST5);
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                         null,
                                         null,
                                         null,
                                         aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains (ECryptoAlgorithmCrypt.ENCRPYTION_ALGORITHM_DEFAULT.getID ())));
  }

  @SuppressWarnings ("deprecation")
  @Test
  public void testValidatePModeSecurityWrongWSSVersion ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getConfig ().getLeg1 ().getSecurity ();
    aSecurityLeg.setWSSVersion (EWSSVersion.WSS_10);
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
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
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE).contains ("mandatory")));
  }

  @Test
  public void testValidatePModeSecurityPModeAuthorizeTrue ()
  {
    final PModeLegSecurity aSecurityLeg = m_aPMode.getConfig ().getLeg1 ().getSecurity ();
    aSecurityLeg.setPModeAuthorize (true);
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
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
    final PModeLegSecurity aSecurityLeg = m_aPMode.getConfig ().getLeg1 ().getSecurity ();
    aSecurityLeg.setSendReceipt (true);
    aSecurityLeg.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.CALLBACK);
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
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
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
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
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
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
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
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
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
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
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
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
    _getConfig ().setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                         null,
                                         aErrorHandler,
                                         null,
                                         null));
    aESENSCompatibilityValidator.validatePMode (m_aPMode, m_aErrorList);
    assertTrue (m_aErrorList.containsAny (x -> x.getErrorText (LOCALE)
                                                .contains ("PMode ReportDeliveryFailuresNotifyProducer has to be True")));
  }
}
