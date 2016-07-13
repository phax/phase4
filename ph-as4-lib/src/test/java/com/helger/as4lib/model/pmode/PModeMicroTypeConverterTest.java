package com.helger.as4lib.model.pmode;

import org.junit.Before;
import org.junit.Test;

import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsOrderedMap;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.state.EMandatory;
import com.helger.commons.state.ETriState;

public class PModeMicroTypeConverterTest
{

  @Before
  public void setUp ()
  {
    final PMode aPmode = new PMode ();
    aPmode.setAgreement ("Agreement");
    aPmode.setID ("id");
    aPmode.setInitiator (_generateInitiatorOrResponder (true));
    aPmode.setLegs (_generatePModeLeg ());
    aPmode.setMEP (EMEP.TWO_WAY_PUSH_PULL);
    aPmode.setMEPBinding (ETransportChannelBinding.SYNC);
    aPmode.setResponder (_generateInitiatorOrResponder (false));
  }

  @Test
  public void testNativToMicroElementConversion ()
  {
    // XMLTestHelper.testMicroTypeConversion (aObj);
  }

  private PModeParty _generateInitiatorOrResponder (final boolean bChoose)
  {
    if (bChoose)
      return new PModeParty ("", "idvalue", "sender", "test", "testpw");
    return new PModeParty ("", "idvalue2", "responder", "test2", "test2pw");
  }

  private ICommonsList <PModeLeg> _generatePModeLeg ()
  {
    final ICommonsList <PModeLeg> aPModeLegs = new CommonsArrayList<> ();
    final PModeLeg aPmodeLeg = new PModeLeg (_generatePModeLegBusinessInformation (),
                                             _generatePModeLegErrorHandling (),
                                             _generatePModeLegProtocol (),
                                             _generatePModeLegReliability (),
                                             _generatePModeLegSecurity ());
    aPModeLegs.add (aPmodeLeg);
    return aPModeLegs;
  }

  private PModeLegBusinessInformation _generatePModeLegBusinessInformation ()
  {
    return new PModeLegBusinessInformation (_generatePModePayloadProfile (),
                                            _generatePModeProperties (),
                                            20000,
                                            "action1",
                                            "mpcexample",
                                            "service");
  }

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

  private ICommonsOrderedMap <String, PModeProperty> _generatePModeProperties ()
  {
    final PModeProperty aPModeProperty = new PModeProperty ("name", "description", "datatype", EMandatory.MANDATORY);
    final ICommonsOrderedMap <String, PModeProperty> aPModeProperties = new CommonsLinkedHashMap<> ();
    aPModeProperties.put (aPModeProperty.getName (), aPModeProperty);
    return aPModeProperties;
  }

  private PModeLegErrorHandling _generatePModeLegErrorHandling ()
  {
    return new PModeLegErrorHandling (_generatePModeAddressList (),
                                      _generatePModeAddressList (),
                                      ETriState.TRUE,
                                      ETriState.TRUE,
                                      ETriState.TRUE,
                                      ETriState.TRUE);
  }

  private PModeAddressList _generatePModeAddressList ()
  {
    return new PModeAddressList ("address1");
  }

  private PModeLegProtocol _generatePModeLegProtocol ()
  {
    return new PModeLegProtocol ("addressProtocol", "soap11");
  }

  private PModeLegReliability _generatePModeLegReliability ()
  {
    final ICommonsList <String> aCorrelation = new CommonsArrayList<> ();
    aCorrelation.add ("correlation");
    return new PModeLegReliability (aCorrelation,
                                    ETriState.TRUE,
                                    ETriState.TRUE,
                                    ETriState.TRUE,
                                    "replyPattern",
                                    ETriState.TRUE,
                                    ETriState.TRUE,
                                    ETriState.TRUE,
                                    ETriState.TRUE,
                                    "ack");

  }

  private PModeLegSecurity _generatePModeLegSecurity ()
  {
    final ICommonsList <String> aX509EncryptionEncrypt = new CommonsArrayList<> ();
    aX509EncryptionEncrypt.add ("X509EncryptionEncrypt");
    final ICommonsList <String> aX509Sign = new CommonsArrayList<> ();
    aX509Sign.add ("X509Sign");
    return new PModeLegSecurity (aX509EncryptionEncrypt,
                                 1,
                                 aX509Sign,
                                 ETriState.TRUE,
                                 ETriState.TRUE,
                                 ETriState.TRUE,
                                 ETriState.TRUE,
                                 ETriState.TRUE,
                                 "replyPattern",
                                 "usernametokenpassword",
                                 "usernametokenusername",
                                 "wssversion",
                                 "X509EncryptionAlgorithm",
                                 "X509EncryptionCertificate",
                                 "X509SignatureAlgorithm",
                                 "X509SignatureCertificate",
                                 "X509SignatureHashFunction");
  }
}
