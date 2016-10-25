
package com.helger.as4server.profile;

import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.helger.as4lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.as4lib.model.pmode.EPModeSendReceiptReplyPattern;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.model.pmode.PModeLeg;
import com.helger.as4lib.model.pmode.PModeLegErrorHandling;
import com.helger.as4lib.model.pmode.PModeLegProtocol;
import com.helger.as4lib.model.pmode.PModeLegSecurity;
import com.helger.as4lib.model.pmode.PModeParty;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.wss.EWSSVersion;
import com.helger.as4server.servlet.ServletTestPMode;
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

  private final ESENSCompatibilityValidator aESENSCompatibilityValidator = new ESENSCompatibilityValidator ();

  private PMode aPMode;
  private ErrorList aErrorList;

  @Before
  public void setUp ()
  {
    aErrorList = new ErrorList ();
    aPMode = ServletTestPMode.getTestPModeWithSecurity (ESOAPVersion.SOAP_12);
  }

  @Test
  public void testValidatePModeWrongMEP ()
  {
    aPMode.setMEP (EMEP.TWO_WAY_PULL_PUSH);
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);

    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ()).contains ("MEP")));
  }

  @Test
  public void testValidatePModeWrongMEPBinding ()
  {
    aPMode.setMEPBinding (ETransportChannelBinding.SYNC);
    aPMode.setResponder (new PModeParty ("sa", "id1", "role", "as", "as"));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);

    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ()).contains ("MEP-Binding")));
  }

  @Test
  public void testValidatePModeNoLeg ()
  {
    aPMode.setLeg1 (null);
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ())
                                              .contains ("PMode is missing Leg 1")));
  }

  @Test
  public void testValidatePModeNoProtocol ()
  {
    aPMode.setLeg1 (new PModeLeg (null, null, null, null, null));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ()).contains ("Protocol")));
  }

  @Test
  public void testValidatePModeNoProtocolAddress ()
  {
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol (), null, null, null, null));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ()).contains ("AddressProtocol")));
  }

  @Test
  public void testValidatePModeProtocolAddressIsNotHttps ()
  {
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("ftp://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  null,
                                  null,
                                  null));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ())
                                              .contains ("non-standard AddressProtocol: ftp")));
  }

  @Test
  public void testValidatePModeProtocolSOAP11NotAllowed ()
  {
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.SOAP_11),
                                  null,
                                  null,
                                  null,
                                  null));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ()).contains ("1.1")));
  }

  @Test
  public void testValidatePModeSecurityNoX509SignatureCertificate ()
  {
    final PModeLegSecurity aSecurityLeg = aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureCertificate (null);
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  null,
                                  null,
                                  aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ()).contains ("signature certificate")));
  }

  @Test
  public void testValidatePModeSecurityNoX509SignatureAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureAlgorithm (null);
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  null,
                                  null,
                                  aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ()).contains ("signature algorithm")));
  }

  @Test
  public void testValidatePModeSecurityWrongX509SignatureAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_512);
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  null,
                                  null,
                                  aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ())
                                              .contains (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT.getID ())));
  }

  @Test
  public void testValidatePModeSecurityNoX509SignatureHashFunction ()
  {
    final PModeLegSecurity aSecurityLeg = aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureHashFunction (null);
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  null,
                                  null,
                                  aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ()).contains ("hash function")));
  }

  @Test
  public void testValidatePModeSecurityWrongX509SignatureHashFunction ()
  {
    final PModeLegSecurity aSecurityLeg = aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_512);
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  null,
                                  null,
                                  aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ())
                                              .contains (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT.getID ())));
  }

  @Test
  public void testValidatePModeSecurityNoX509EncryptionAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509EncryptionAlgorithm (null);
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  null,
                                  null,
                                  aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ()).contains ("encryption algorithm")));
  }

  @Test
  public void testValidatePModeSecurityWrongX509EncryptionAlgorithm ()
  {
    final PModeLegSecurity aSecurityLeg = aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.CRYPT_CAST5);
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  null,
                                  null,
                                  aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ())
                                              .contains (ECryptoAlgorithmCrypt.ENCRPYTION_ALGORITHM_DEFAULT.getID ())));
  }

  @SuppressWarnings ("deprecation")
  @Test
  public void testValidatePModeSecurityWrongWSSVersion ()
  {
    final PModeLegSecurity aSecurityLeg = aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setWSSVersion (EWSSVersion.WSS_10);
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  null,
                                  null,
                                  aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ()).contains ("Wrong WSS Version")));
  }

  @Test
  public void testValidatePModeSecurityPModeAuthorizeMandatory ()
  {
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ()).contains ("mandatory")));
  }

  @Test
  public void testValidatePModeSecurityPModeAuthorizeTrue ()
  {
    final PModeLegSecurity aSecurityLeg = aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setPModeAuthorize (true);
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  null,
                                  null,
                                  aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ()).contains ("false")));
  }

  @Test
  public void testValidatePModeSecurityResponsePatternWrongBoolean ()
  {
    final PModeLegSecurity aSecurityLeg = aPMode.getLeg1 ().getSecurity ();
    aSecurityLeg.setSendReceipt (true);
    aSecurityLeg.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.CALLBACK);
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  null,
                                  null,
                                  aSecurityLeg));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ())
                                              .contains ("Only response is allowed as pattern")));
  }

  // Error Handling

  @Test
  public void testValidatePModeErrorHandlingMandatory ()
  {
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ())
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
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  aErrorHandler,
                                  null,
                                  null));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ())
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
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  aErrorHandler,
                                  null,
                                  null));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ())
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
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  aErrorHandler,
                                  null,
                                  null));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ())
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
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  aErrorHandler,
                                  null,
                                  null));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ())
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
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  aErrorHandler,
                                  null,
                                  null));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ())
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
    aPMode.setLeg1 (new PModeLeg (new PModeLegProtocol ("https://test.com", ESOAPVersion.AS4_DEFAULT),
                                  null,
                                  aErrorHandler,
                                  null,
                                  null));
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);
    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ())
                                              .contains ("PMode ReportDeliveryFailuresNotifyProducer has to be True")));
  }
}
