package com.helger.as4server.message;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.helger.as4lib.attachment.AS4FileAttachment;
import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.encrypt.EncryptionCreator;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.httpclient.HttpMimeMessageEntity;
import com.helger.as4lib.mime.MimeMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.xml.SerializerXML;
import com.helger.as4server.client.TestMessages;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.httpclient.HttpClientFactory;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Tests the basic functionality of sending UserMessages with SOAP Version 1.1.
 * IMPORTANT: If these tests are expected to work there needs to be a local
 * holodeck server running.
 *
 * @author bayerlma
 */
@RunWith (Parameterized.class)
public class SendingUserMessageTest
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return Arrays.asList (new Object [] [] { { ESOAPVersion.SOAP_11 }, { ESOAPVersion.SOAP_12 } });
  }

  private final ESOAPVersion m_eSOAPVersion;
  private int m_nStatusCode;
  private String m_sResponse;
  private CloseableHttpClient aClient;
  private HttpPost aPost;

  public SendingUserMessageTest (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = eSOAPVersion;
  }

  @Before
  public void setUp () throws KeyManagementException, NoSuchAlgorithmException
  {
    final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
    aSSLContext.init (null, new TrustManager [] { new TrustManagerTrustAll (false) }, RandomHelper.getSecureRandom ());

    aClient = new HttpClientFactory (aSSLContext).createHttpClient ();
    final HttpClientContext aContext = new HttpClientContext ();
    aContext.setRequestConfig (RequestConfig.custom ().setProxy (new HttpHost ("172.30.9.12", 8080)).build ());

    aPost = new HttpPost ("http://127.0.0.1:8080/services/msh/");
  }

  @Test
  public void testUserMessageWithSOAPBodyPayloadNoMimeSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    final Document aDoc = TestMessages.testUserMessage (m_eSOAPVersion, aPayload, aAttachments);
    _sendMessage (new StringEntity (SerializerXML.serializeXML (aDoc)), true, null);
  }

  @Test
  public void testUserMessageWithSOAPBodyPayloadNoMimeEncryptedSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    Document aDoc = TestMessages.testUserMessage (m_eSOAPVersion, aPayload, aAttachments);

    aDoc = new EncryptionCreator ().encryptSoapBodyPayload (m_eSOAPVersion, aDoc, false);
    System.out.println (SerializerXML.serializeXML (aDoc));
    _sendMessage (new StringEntity (SerializerXML.serializeXML (aDoc)), true, null);
  }

  // @Test
  // public void deleteAfterTesting () throws Exception
  // {
  //
  // final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<>
  // ();
  // aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile
  // ("attachment/test.xml.gz"),
  // CMimeType.APPLICATION_GZIP));
  // Document aDoc = TestMessages.testUserMessage (m_eSOAPVersion, null,
  // aAttachments);
  //
  // aDoc = new EncryptionCreator ().tTEST (m_eSOAPVersion, aDoc, false,
  // aAttachments);
  // System.out.println (SerializerXML.serializeXML (aDoc));
  // _sendMessage (new StringEntity (SerializerXML.serializeXML (aDoc)), true,
  // null);
  // }

  @Test
  public void testUserMessageNoSOAPBodyPayloadNoAttachmentSuccess () throws Exception
  {
    final Document aDoc = TestMessages.testUserMessage (m_eSOAPVersion, null, null);
    System.out.println (SerializerXML.serializeXML (aDoc));
    _sendMessage (new StringEntity (SerializerXML.serializeXML (aDoc)), true, null);
  }

  @Test
  public void testUserMessageWithSOAPBodyPayloadWithMimeSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    final MimeMessage aMsg = TestMessages.testMIMEMessageGenerated (TestMessages.testUserMessage (m_eSOAPVersion,
                                                                                                  aPayload,
                                                                                                  null),
                                                                    m_eSOAPVersion);
    MessageHelperMethods.moveMIMEHeadersToHTTPHeader (aMsg, aPost);
    _sendMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageWithOneAttachmentWithMimeSuccess () throws Exception
  {
    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test.xml.gz"),
                                             CMimeType.APPLICATION_GZIP));

    final CreateSignedMessage aSigned = new CreateSignedMessage ();
    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (aSigned.createSignedMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                null,
                                                aAttachments),
     m_eSOAPVersion,
     aAttachments,
     false), aAttachments, null);
    MessageHelperMethods.moveMIMEHeadersToHTTPHeader (aMsg, aPost);
    _sendMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageWithManyAttachmentsWithMimeSuccess () throws WSSecurityException, Exception
  {
    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test.xml.gz"),
                                             CMimeType.APPLICATION_GZIP));
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test-img.jpg"),
                                             CMimeType.IMAGE_JPG));
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test-img2.jpg"),
                                             CMimeType.IMAGE_JPG));

    final CreateSignedMessage aSigned = new CreateSignedMessage ();
    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (aSigned.createSignedMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                null,
                                                aAttachments),
     m_eSOAPVersion,
     aAttachments,
     false), aAttachments, null);
    MessageHelperMethods.moveMIMEHeadersToHTTPHeader (aMsg, aPost);
    _sendMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageWithMimeEncryptedSuccess () throws Exception
  {
    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/shortXML.xml"),
                                             CMimeType.TEXT_XML));

    final CreateSignedMessage aSigned = new CreateSignedMessage ();
    final Document aDoc = aSigned.createSignedMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                  null,
                                                                                                  aAttachments),
                                                       m_eSOAPVersion,
                                                       aAttachments,
                                                       false);

    final MimeMessage aMsg = new EncryptionCreator ().encryptMimeMessage (m_eSOAPVersion, aDoc, false, aAttachments);
    MessageHelperMethods.moveMIMEHeadersToHTTPHeader (aMsg, aPost);
    if (false)
      aMsg.writeTo (System.out);
    _sendMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testEmptyMessage () throws Exception
  {
    // Has to be empty String, since there is no EBMS exception coming back
    _sendMessage (new StringEntity (""), false, "");
  }

  @Test (expected = IllegalStateException.class)
  public void testEmptyUserMessage ()
  {
    TestMessages.emptyUserMessage (m_eSOAPVersion, null, null);
    fail ();

  }

  @Test
  public void testPayloadChangedAfterSigning () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    final Document aDoc = TestMessages.testUserMessage (m_eSOAPVersion, aPayload, aAttachments);
    final NodeList nList = aDoc.getElementsByTagName (m_eSOAPVersion.getNamespacePrefix () + ":Body");
    for (int i = 0; i < nList.getLength (); i++)
    {
      final Node nNode = nList.item (i);
      final Element eElement = (Element) nNode;
      eElement.setAttribute ("INVALID", "INVALID");
      System.out.println ("Manager ID : " + eElement.getAttribute ("INVALID"));
    }
    _sendMessage (new StringEntity (SerializerXML.serializeXML (aDoc)),
                  false,
                  EEbmsError.EBMS_FAILED_AUTHENTICATION.getErrorCode ());
  }

  @Test
  public void testWrongAttachmentID () throws Exception
  {

    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test.xml.gz"),
                                             CMimeType.APPLICATION_GZIP));

    final CreateSignedMessage aSigned = new CreateSignedMessage ();

    final Document aDoc = aSigned.createSignedMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                  null,
                                                                                                  aAttachments),
                                                       m_eSOAPVersion,
                                                       aAttachments,
                                                       false);

    final NodeList nList = aDoc.getElementsByTagName ("eb:PartInfo");
    for (int i = 0; i < nList.getLength (); i++)
    {
      final Node nNode = nList.item (i);
      final Element eElement = (Element) nNode;
      if (eElement.hasAttribute ("href"))
        eElement.setAttribute ("href", "cid:invalid");
    }
    System.out.println (SerializerXML.serializeXML (aDoc));
    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (aDoc, aAttachments, null);
    MessageHelperMethods.moveMIMEHeadersToHTTPHeader (aMsg, aPost);
    _sendMessage (new HttpMimeMessageEntity (aMsg), false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void testSendUnsignedMessageSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    final Document aDoc = TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion, aPayload, null);

    System.out.println (SerializerXML.serializeXML (aDoc));
    _sendMessage (new StringEntity (SerializerXML.serializeXML (aDoc)), true, null);
  }

  @Test
  public void testFalsePartyIDToTriggerPModeError () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    final Document aDoc = TestMessages.testUserMessageSoapNotSignedNotPModeConform (m_eSOAPVersion, aPayload, null);

    _sendMessage (new StringEntity (SerializerXML.serializeXML (aDoc)),
                  false,
                  EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }

  private void _sendMessage (final HttpEntity aHttpEntity,
                             final boolean bSuccess,
                             final String sErrorCode) throws IOException
  {
    aPost.setEntity (aHttpEntity);

    final CloseableHttpResponse aHttpResponse = aClient.execute (aPost);

    m_nStatusCode = aHttpResponse.getStatusLine ().getStatusCode ();
    m_sResponse = EntityUtils.toString (aHttpResponse.getEntity ());

    System.out.println ("GET Response Status:: " + m_nStatusCode);

    // print result
    System.out.println (m_sResponse);

    if (bSuccess)
    {
      assertTrue ("Server responded with an error or error code." +
                  "StatusCode: " +
                  m_nStatusCode +
                  "\nResponse: " +
                  m_sResponse,
                  m_nStatusCode == 200 && !m_sResponse.contains ("Error"));
    }
    else
      assertTrue ("Server responded with success message but failure was expected." +
                  "StatusCode: " +
                  m_nStatusCode +
                  "\nResponse: " +
                  m_sResponse,
                  m_sResponse.contains (sErrorCode));
  }
}
