package com.helger.as4lib.model.pmode;

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
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.mock.XMLTestHelper;

public class PModeMicroTypeConverterTest
{
  @Test
  public void testNativToMicroElementConversion ()
  {
    final PMode aPMode = new PMode ();
    aPMode.setAgreement ("Agreement");
    aPMode.setID ("id");
    aPMode.setInitiator (_generateInitiatorOrResponder (true));
    aPMode.setLegs (_generatePModeLeg ());
    aPMode.setMEP (EMEP.TWO_WAY_PUSH_PULL);
    aPMode.setMEPBinding (ETransportChannelBinding.SYNC);
    aPMode.setResponder (_generateInitiatorOrResponder (false));
    XMLTestHelper.testMicroTypeConversion (aPMode);
    System.out.println (MicroWriter.getXMLString (MicroTypeConverter.convertToMicroElement (aPMode, "PMode")));
  }

  private PModeParty _generateInitiatorOrResponder (final boolean bChoose)
  {
    if (bChoose)
      return new PModeParty ("", "idvalue", "sender", "test", "testpw");
    return new PModeParty ("", "idvalue2", "responder", "test2", "test2pw");
  }

  private ICommonsList <PModeLeg> _generatePModeLeg ()
  {
    return new CommonsArrayList<> (new PModeLeg (_generatePModeLegProtocol (),
                                                 _generatePModeLegBusinessInformation (),
                                                 _generatePModeLegErrorHandling (),
                                                 _generatePModeLegReliability (),
                                                 _generatePModeLegSecurity ()),
                                   new PModeLeg (_generatePModeLegProtocol (),
                                                 _generatePModeLegBusinessInformation (),
                                                 _generatePModeLegErrorHandling (),
                                                 _generatePModeLegReliability (),
                                                 _generatePModeLegSecurity ()));
  }

  private PModeLegBusinessInformation _generatePModeLegBusinessInformation ()
  {
    return new PModeLegBusinessInformation ("service",
                                            "action1",
                                            _generatePModeProperties (),
                                            _generatePModePayloadProfile (),
                                            20000,
                                            "mpcexample");
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
