package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsOrderedMap;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.state.EMandatory;
import com.helger.commons.state.ETriState;
import com.helger.web.scope.mock.WebScopeTestRule;
import com.helger.xml.mock.XMLTestHelper;

public final class PModeMicroTypeConverterTest
{
  @Rule
  public final TestRule m_aTestRule = new WebScopeTestRule ();

  @Test
  public void testAsSimpleAsPossible ()
  {
    final PMode aPMode = new PMode ("id");
    XMLTestHelper.testMicroTypeConversion (aPMode);
  }

  @Test
  public void testNativToMicroElementConversion ()
  {
    final PMode aPMode = new PMode ("id");
    aPMode.setAgreement ("Agreement");
    aPMode.setMEP (EMEP.TWO_WAY_PUSH_PULL);
    aPMode.setMEPBinding (ETransportChannelBinding.SYNC);
    aPMode.setInitiator (_generateInitiatorOrResponder (true));
    aPMode.setResponder (_generateInitiatorOrResponder (false));
    aPMode.setLeg1 (_generatePModeLeg ());
    aPMode.setLeg2 (_generatePModeLeg ());
    XMLTestHelper.testMicroTypeConversion (aPMode);
    XMLTestHelper.testMicroTypeConversion (aPMode.getInitiator ());
    XMLTestHelper.testMicroTypeConversion (aPMode.getResponder ());
    XMLTestHelper.testMicroTypeConversion (aPMode.getLeg1 ());
    XMLTestHelper.testMicroTypeConversion (aPMode.getLeg2 ());
  }

  @Nonnull
  private PModeParty _generateInitiatorOrResponder (final boolean bInitiator)
  {
    if (bInitiator)
      return new PModeParty ("initiator-type", "idvalue", "sender", "test", "testpw");
    return new PModeParty ("responder-type", "idvalue2", "responder", "test2", "test2pw");
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
    return new PModeLegSecurity ("wssversion",
                                 aX509Sign,
                                 "X509SignatureCertificate",
                                 "X509SignatureHashFunction",
                                 "X509SignatureAlgorithm",
                                 aX509EncryptionEncrypt,
                                 "X509EncryptionCertificate",
                                 "X509EncryptionAlgorithm",
                                 1,
                                 "usernametokenusername",
                                 "usernametokenpassword",
                                 ETriState.TRUE,
                                 ETriState.TRUE,
                                 ETriState.TRUE,
                                 ETriState.TRUE,
                                 ETriState.TRUE,
                                 "replyPattern");
  }
}
