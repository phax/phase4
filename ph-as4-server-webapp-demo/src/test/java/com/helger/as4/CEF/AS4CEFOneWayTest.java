package com.helger.as4.CEF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.entity.StringEntity;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.helger.as4.CAS4;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.server.MockPModeGenerator;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.collection.ext.ICommonsList;

public class AS4CEFOneWayTest extends AbstractCEFTestSetUp
{

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH is simulated to send an AS4 User
   * message to the RMSH with parameter MESSAGEPROPERTIES containing: A property
   * with attributes "name" and "type" present. A property with only attribute
   * "name" present.<br>
   * <br>
   * Predicate: <br>
   * The RMSH returns a non-repudiation receipt within a HTTP response with
   * status code 2XX.
   */
  @Test
  public void AS4_TA03 () throws Exception
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = MockEbmsHelper.getEBMSProperties ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = CreateUserMessage.createEbms3PayloadInfo (m_aPayload, null);

    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    final Ebms3PartyInfo aEbms3PartyInfo;
    aEbms3CollaborationInfo = CreateUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                              "MyServiceTypes",
                                                                              MockPModeGenerator.SOAP11_SERVICE,
                                                                              "4321",
                                                                              m_aESENSOneWayPMode.getID (),
                                                                              MockEbmsHelper.DEFAULT_AGREEMENT);
    aEbms3PartyInfo = CreateUserMessage.createEbms3PartyInfo (CAS4.DEFAULT_SENDER_URL,
                                                              INITIATOR_ID,
                                                              CAS4.DEFAULT_RESPONDER_URL,
                                                              RESPONDER_ID);

    final Ebms3MessageProperties aEbms3MessageProperties = CreateUserMessage.createEbms3MessageProperties (aEbms3Properties);
    final String sTrackerIdentifier = "trackingidentifier";
    final Ebms3Property aProp = new Ebms3Property ();
    aProp.setName (sTrackerIdentifier);
    aProp.setValue ("tracker");
    aEbms3MessageProperties.addProperty (aProp);

    // Can not do a Property without Value (type) since type does not exist
    // final Ebms3Property aPropOnlyName = new Ebms3Property ();
    // aPropOnlyName.setName ("OnlyName");
    // aEbms3MessageProperties.addProperty (aPropOnlyName);

    final AS4UserMessage aDoc = CreateUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                     aEbms3PayloadInfo,
                                                                     aEbms3CollaborationInfo,
                                                                     aEbms3PartyInfo,
                                                                     aEbms3MessageProperties,
                                                                     m_eSOAPVersion)
                                                 .setMustUnderstand (true);
    final SignedMessageCreator aClient = new SignedMessageCreator ();

    final Document aSignedDoc = aClient.createSignedMessage (aDoc.getAsSOAPDocument (m_aPayload),
                                                             m_eSOAPVersion,
                                                             null,
                                                             new AS4ResourceManager (),
                                                             false,
                                                             ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                             ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

    final NodeList nList = aSignedDoc.getElementsByTagName ("eb:MessageProperties");
    assertEquals (nList.item (0).getLastChild ().getAttributes ().getNamedItem ("name").getTextContent (),
                  sTrackerIdentifier);

    final String sResponse = sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aSignedDoc)), true, null);

    assertTrue (sResponse.contains ("NonRepudiationInformation"));
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: One-Way/Push MEP. SMSH sends an AS4 message (User Message
   * with payload) to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends a non-repudiation receipt to the SMSH.
   */
  @Test
  public void AS4_TA04 () throws Exception
  {
    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, null, new AS4ResourceManager ());

    final String sResponse = sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);

    assertTrue (sResponse.contains ("Receipt"));
    assertTrue (sResponse.contains ("NonRepudiationInformation"));
  }

  /**
   * Prerequisite:<br>
   * AS4_TA04 <br>
   * <br>
   * Predicate: <br>
   * The SMSH sends a success notification to the producer.
   */
  @Ignore
  @Test
  public void AS4_TA05 ()
  {
    // Is specified in the SPI, so everyone can configure that how it is
    // preferred
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a Message with metadata
   * information and XML payload to the SMSH.<br>
   * <br>
   * Predicate: <br>
   * The SMSH generates an AS4 message with a gzip compressed payload.
   */
  @Test
  public void AS4_TA06 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a Message with metadata
   * information and payload to the SMSH.<br>
   * <br>
   * Predicate: <br>
   * In the AS4 message generated by the SMSH, a property element with name
   * "CompressionType" and value set to "application/gzip" is present.
   */
  @Test
  public void AS4_TA07 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a message to the SMSH
   * with payload (ex: xml document) and metadata information including a
   * property element with name "MimeType" and value ("application/xml").<br>
   * <br>
   * Predicate: <br>
   * The SMSH generates an AS4 message with the property "MimeType" present and
   * set to the value specified by the producer ("application/xml").
   */
  @Test
  public void AS4_TA08 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). The SMSH is simulated to send an AS4
   * message without property "MimeType" present to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends a synchronous ebMS error response.
   */
  @Test
  public void AS4_TA09 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a message to the SMSH
   * with xml (UTF-16) payload and metadata information including payload
   * characterset info.<br>
   * <br>
   * Predicate: <br>
   * The SMSH generates an AS4 message with the property "CharacterSet" present
   * and set to the value "UTF-16".
   */
  @Test
  public void AS4_TA10 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a message to the SMSH
   * with xml (UTF-8) payload and metadata information including payload
   * characterset info.<br>
   * <br>
   * Predicate: <br>
   * The SMSH generates an AS4 message with the property "CharacterSet" present
   * and set to the value "UTF-8".
   */
  @Test
  public void AS4_TA11 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH is simulated to send an AS4 message
   * with property element "CharacterSet" set to value not conform to section
   * 4.3.3 of [XML10] (example: "!utf*"). The SMSH sends the AS4 message to the
   * RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH returns a synchronous ebMS error message.
   */
  @Test
  public void AS4_TA12 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH is simulated to send an AS4 User
   * Message with compressed but damaged payloads. The SMSH sends the AS4 User
   * Message to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a synchronous error response with error code "Code =
   * EBMS:0303, Short Description = DecompressionFailure, Severity = Failure,
   * Category = Communication".
   */
  @Test
  public void AS4_TA13 ()
  {

  }

  /**
   * Prerequisite:<br>
   * AS4_TA13. <br>
   * The User Message is bound to a PMode with parameter
   * PMode[1].ErrorHandling.Report.ProcessErrorNotifyConsumer: set to true. <br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a synchronous error response (with error code "Code =
   * EBMS:0303, Short Description = DecompressionFailure, Severity = Failure,
   * Category = Communication") and the RMSH sends an error notification to the
   * consumer of the message.
   */
  @Test
  public void AS4_TA14 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH sends an AS4 User Message with a
   * compressed payload to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH delivers the message with decompressed payload to the consumer.
   */
  @Test
  public void AS4_TA15 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH sends an AS4 User Message with a
   * several compressed payloads (XML and non XML) to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH delivers the message with decompressed payloads to the consumer.
   */
  @Test
  public void AS4_TA16 ()
  {

  }

  /**
   * Prerequisite:<br>
   * eSENS_TA13.<br>
   * The SMSH is simulated to send an AS4 User message with a compressed then
   * signed payload to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back an AS4 non-repudiation receipt.
   */
  @Test
  public void AS4_TA17 ()
  {

  }

  /**
   * Prerequisite:<br>
   * eSENS_TA13<br>
   * Simulated SMSH sends a signed AS4 User Message with a signed then
   * compressed payload to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The SMSH receives a WS-Security SOAP Fault.
   */
  @Test
  public void AS4_TA18 ()
  {

  }

  /**
   * Prerequisite:<br>
   * eSENS_TA14.<br>
   * The SMSH is simulated to send a compressed then encrypted AS4 message to
   * the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back an AS4 non-repudiation receipt.
   */
  @Test
  public void AS4_TA19 ()
  {

  }

  /**
   * Prerequisite:<br>
   * eSENS_TA14.<br>
   * Simulated SMSH sends a signed AS4 User Message with an encrypted first,
   * then compressed payload to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The SMSH receives a WS-Security SOAP Fault.
   */
  @Test
  public void AS4_TA20 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). The SMSH sends an AS4 message with a
   * compressed then encrypted and signed payload to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back an AS4 non-repudiation receipt.
   */
  @Test
  public void AS4_TA21 ()
  {

  }

  /**
   * Note: This test assertion is only valid in case TLS is handled by the AS4
   * message handler. AS4_TA22 - AS4_TA26 all TLS tests
   */
  @Ignore
  @Test
  public void AS4_TA22 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH sends an AS4 User message to the
   * RMSH. <br>
   * <br>
   * Predicate: <br>
   * In the message sender|receiver elements reference the MSHs and not the
   * (producer,consumer).
   */
  @Test
  public void AS4_TA27 ()
  {

  }

  /**
   * Note: Only when using SBDH<br>
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits two payloads, first
   * being an SBDH document, second being an actual payload (non-XML payload).
   * SMSH sends an AS4 User Message to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * Message has two additional MIME parts. The first mime part is the SBDH
   * document and the second is the actual payload
   */
  @Test
  public void AS4_TA28 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH is simulated to send an AS4 User
   * message to the RMSH with parameter PARTPROPERTIES containing: A property
   * with attributes "name" and "type" present. A property with only attribute
   * "name" present. <br>
   * <br>
   * Predicate: <br>
   * The RMSH returns a non-repudiation receipt within a HTTP response with
   * status code 2XX.
   */
  @Test
  public void AS4_TA29 ()
  {

  }
}
