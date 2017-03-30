package com.helger.as4.esensCEF;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.helger.as4.CAS4;
import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.esens.ESENSPMode;
import com.helger.as4.http.HttpMimeMessageEntity;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.mime.MimeMessageCreator;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.model.pmode.IPModeIDProvider;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.server.MockPModeGenerator;
import com.helger.as4.server.message.AbstractUserMessageTestSetUp;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;
import com.helger.xml.serialize.read.DOMReader;

public class AS4eSENSCEFTest extends AbstractUserMessageTestSetUp
{
  private static final String INITIATOR_ID = "CEF-Initiator";
  private static final String RESPONDER_ID = "CEF-Responder";
  private static final String RESPONDER_ADDRESS = "http://localhost:8080/as4";

  private PMode m_aESENSOneWayPMode;
  // private PMode m_aESENSTwoWayPMode;
  private ESOAPVersion m_eSOAPVersion;
  private Node m_aPayload;

  @Before
  public void setUpCEF ()
  {

    m_aESENSOneWayPMode = ESENSPMode.createESENSPMode (INITIATOR_ID,
                                                       RESPONDER_ID,
                                                       RESPONDER_ADDRESS,
                                                       IPModeIDProvider.DEFAULT_DYNAMIC);

    // TODO test if thats possible or not same ID but different MEP should
    // require another id
    /*
     * m_aESENSTwoWayPMode = ESENSPMode.createESENSPModeTwoWay (INITIATOR_ID,
     * RESPONDER_ID, RESPONDER_ADDRESS, IPModeIDProvider.DEFAULT_DYNAMIC);
     */

    m_eSOAPVersion = m_aESENSOneWayPMode.getLeg1 ().getProtocol ().getSOAPVersion ();
    try
    {
      m_aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    }
    catch (final SAXException e)
    {
      e.printStackTrace ();
    }
  }

  private Document testSignedUserMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                          @Nullable final Node aPayload,
                                          @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                          @Nonnull final AS4ResourceManager aResMgr) throws WSSecurityException
  {
    final SignedMessageCreator aClient = new SignedMessageCreator ();

    final Document aSignedDoc = aClient.createSignedMessage (testUserMessageSoapNotSigned (aPayload, aAttachments),
                                                             eSOAPVersion,
                                                             aAttachments,
                                                             aResMgr,
                                                             false,
                                                             ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                             ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);
    return aSignedDoc;
  }

  private Document testUserMessageSoapNotSigned (@Nullable final Node aPayload,
                                                 @Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = MockEbmsHelper.getEBMSProperties ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = CreateUserMessage.createEbms3PayloadInfo (aPayload, aAttachments);

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

    final AS4UserMessage aDoc = CreateUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                     aEbms3PayloadInfo,
                                                                     aEbms3CollaborationInfo,
                                                                     aEbms3PartyInfo,
                                                                     aEbms3MessageProperties,
                                                                     m_eSOAPVersion)
                                                 .setMustUnderstand (true);
    return aDoc.getAsSOAPDocument (aPayload);
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: One-Way/Push MEP. SMSH sends an AS4 User message to the
   * RMSH. <br>
   * <br>
   * Predicate:<br>
   * The RMSH returns a non-repudiation receipt within a HTTP response with
   * status code 2XX (for more details on http response codes please refer to
   * https://issues.oasis-open.org/browse/EBXMLMSG-57?jql=project%20%3D%20EBXMLMSG).
   *
   * @throws MessagingException
   * @throws WSSecurityException
   * @throws IOException
   */
  @Test
  public void eSENS_TA01 () throws MessagingException, WSSecurityException, IOException
  {

    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (testSignedUserMessage (m_eSOAPVersion,
                                                                                                                 m_aPayload,
                                                                                                                 null,
                                                                                                                 new AS4ResourceManager ()),
                                                                                          null);

    final String sResponse = sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);

    assertTrue (sResponse.contains ("Receipt"));
    assertTrue (sResponse.contains ("NonRepudiationInformation"));
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: Two-Way/Push-and-Push MEP. SMSH sends an AS4 User Message
   * (M1 with ID MessageId) that requires a consumer response to the RMSH. <br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a User Message (M2) with element REFTOMESSAGEID set to
   * MESSAGEID (of M1).
   */
  @Test
  public void eSENS_TA02 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: One-Way/Push MEP. SMSH and RMSH exchange several AS4 User
   * Messages.<br>
   * <br>
   * Predicate: <br>
   * Each exchanged AS4 message contains single ORIGIN and DESTINATION elements.
   * <br>
   * Goal: "Both UserMessage/PartyInfo/From and UserMessage/PartyInfo/To must
   * not include more than one PartyId element"
   */
  @Test
  public void eSENS_TA03 ()
  {
    // Code only allows only 1 Party ID, would throw exception if 2 party ids
    // were present for a single role (Initiator or Responder)
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile. (One-Way/Push MEP) SMSH is simulated to produce
   * uncompressed payloads. The SMSH sends the AS4 message to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH returns a non-repudiation receipt and delivers the message to the
   * consumer. <br>
   * <br>
   * k Goal: Do not throw an error if a uncompressed payload gets sent, also
   * check if compressed payloads are acceptable aswell.
   *
   * @throws IOException
   * @throws MessagingException
   * @throws WSSecurityException
   */
  @Test
  public void eSENS_TA04 () throws IOException, WSSecurityException, MessagingException
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml.xml"),
                                                                    CMimeType.APPLICATION_XML,
                                                                    EAS4CompressionMode.GZIP,
                                                                    s_aResMgr));

    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (testSignedUserMessage (m_eSOAPVersion,
                                                                                                                 m_aPayload,
                                                                                                                 aAttachments,
                                                                                                                 new AS4ResourceManager ()),
                                                                                          aAttachments);

    final String sResponse = sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);

    assertTrue (sResponse.contains ("Receipt"));
    assertTrue (sResponse.contains ("NonRepudiationInformation"));
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile. (One-Way/Push MEP)Producer submits a message with metadata
   * information and an XML payload to the SMSH. SMSH generates an AS4 message
   * to send to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * In the AS4 message created by the SMSH, the compressed payload is carried
   * in a separate MIME part and the soap body is empty. <br>
   * <br>
   * Goal:"Due to the mandatory use of the AS4 compression feature in this
   * profile (see section 2.2.3.3), XML payloads MAY be converted to binary
   * data, which is carried in separate MIME parts and not in the SOAP Body.
   * Compliant AS4 message always have an empty SOAP Body. "
   */
  @Test
  public void eSENS_TA05 ()
  {
    // TODO talk with philip about this
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile. (One-Way/Push MEP) Producer submits a message to the SMSH
   * with metadata information, an XML payload (leading business document) and
   * other payloads (XML and non XML). SMSH generates an AS4 message to send to
   * the RMSH.<br>
   * <br>
   * Predicate: <br>
   * In the AS4 message created by the SMSH, the compressed payloads are carried
   * in separate MIME parts and the soap body is empty.
   */
  @Test
  public void eSENS_TA06 ()
  {
    // same stuff as TA05 only one step further
  }

  /**
   * Prerequisite:<br>
   * eSENS_TA06<br>
   * SMSH sends an AS4 message to the RMSH. <br>
   * <br>
   * Predicate: <br>
   * The RMSH successfully processes the AS4 message and sends a non-repudiation
   * receipt to the SMSH.
   */
  @Test
  public void eSENS_TA07 ()
  {
    // same stuff as TA05 only one step further
  }

  /**
   * NOTE: Not testable => CEF document<br>
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile. (One-Way/Push MEP) SMSH is simulated to send an AS4 message
   * to the RMSH with non XML payloads and without a leading business document
   * payload. The SMSH sends the AS4 User Message to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a synchronous error response.
   */
  @Test
  public void eSENS_TA08 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH is simulated to send an AS4 user
   * message with a payload hyperlink reference.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a synchronous ebMS error message.
   */
  @Test
  public void eSENS_TA09 ()
  {
    // Would throw an error in our implementation since the user would have said
    // there is a payload (With the hyperlink reference) but nothing is
    // attached.
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Simulate the RMSH to not send receipts
   * (can be done by intercepting the receipts). SMSH tries to send an AS4 User
   * Message to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The SMSH retries to send the AS4 User Message (at least once).
   */
  @Test
  public void eSENS_TA10 ()
  {
    // TODO find a way to intercept receipts to trigger resend mechanism
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Simulate the RMSH to not send receipts.
   * SMSH tries to send an AS4 User Message to the RMSH. Before a TIME_OUT is
   * reached network connection is restored (RMSH is able to send a
   * non-repudiation receipt).<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back an AS4 non-repudiation receipt to the SMSH and delivers
   * only one message to the consumer and the SMSH stops resending the original
   * AS4 User Message.
   */
  @Test
  public void eSENS_TA11 ()
  {
    // Same as TA10
  }

  /**
   * NOTE: Not testable <= is the hint in the CEF pdf (just wanted to add it for
   * completion) <br>
   * <br>
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile.<br>
   * <br>
   * Predicate: <br>
   * PMode parameter " PMode[1].ErrorHandling.Report.SenderErrors" is not set.
   */
  @Ignore
  @Test
  public void eSENS_TA12 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a message with metadata
   * information and a payload to the SMSH. SMSH sends an AS4 signed message to
   * the RMSH.<br>
   * <br>
   * Predicate: <br>
   * In the AS4 Message generated by the SMSH: - Signature Hash function
   * parameter is set to http://www.w3.org/2001/04/xmlenc#sha256 - Signature
   * Algorithm parameter is set to
   * http://www.w3.org/2001/04/xmldsig-more#rsa-sha256 - Signature Certificate
   * used is the certificate of the SMSH.
   */
  @Test
  public void eSENS_TA13 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a message with metadata
   * information and a payload to the SMSH. SMSH sends an AS4 encrypted message
   * to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * In the AS4 Message generated by the SMSH: - Encryption Algorithm is set to
   * http://www.w3.org/2009/xmlenc11#aes128-gcm - Encryption Certificate used is
   * the certificate of the RMSH.
   */
  @Test
  public void eSENS_TA14 ()
  {

  }

  /**
   * Prerequisite:<br>
   * Producer submits a business document with the information “Sender” and
   * “destination” to the SMSH. SMSH and RMSH are configured to exchange AS4
   * messages according to the e-SENS profile (One-Way/Push MEP). SMSH sends an
   * AS4 User message to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The message received by the RMSH contains 2 property elements in the
   * MessageProperties node with attributes name and type. One has name =
   * "OriginalSender" and value producerID and the other has name =
   * "finalRecipient" and value consumerID (producerID and consumerID are
   * provided by the original message submitted by the producer).
   */
  @Test
  public void eSENS_TA15 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (Two-Way/Push-and-Push MEP). SMSH sends an AS4 User Message
   * to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH returns a non-repudiation receipt within a HTTP response with
   * status code 2XX.
   */
  @Test
  public void eSENS_TA16 ()
  {

  }

  /**
   * Prerequisite:<br>
   * eSENS_TA10. TIME_OUT for resending the messages is reached.<br>
   * <br>
   * Predicate: <br>
   * The SMSH reports an error to the message producer.
   */
  @Test
  public void eSENS_TA17 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: One-Way/Push MEP. Producer submits a "ping" message with
   * metadata information to the SMSH (to "ping" consumer).<br>
   * <br>
   * Predicate: <br>
   * The SMSH generates an AS4 message with values (and sends it to the RMSH):
   * UserMessage/CollaborationInfo/Service set to
   * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/service
   * UserMessage/CollaborationInfo/Action set to
   * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/test
   */
  @Test
  public void eSENS_TA18 ()
  {

  }

  /**
   * Prerequisite:<br>
   * eSENS_TA18. The consumer is reachable.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a non-repudiation receipt within a HTTP response with
   * status code 2XX and the consumer doesn't receive any message.
   */
  @Test
  public void eSENS_TA19 ()
  {

  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: One-Way/Push MEP. SMSH sends an AS4 User Message including
   * a TRACKINGIDENTIFIER property set by the producer.<br>
   * <br>
   * Predicate: <br>
   * The RMSH returns a non-repudiation receipt within a HTTP response with
   * status code 2XX and the received AS4 message contains the
   * TRACKINGIDENTIFIER property.
   */
  @Test
  public void eSENS_TA20 ()
  {

  }
}
