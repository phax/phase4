package com.helger.as4server.client;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.helger.as4lib.attachment.AS4FileAttachment;
import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.httpclient.HttpMimeMessageEntity;
import com.helger.as4lib.message.MessageHelperMethods;
import com.helger.as4lib.mime.MimeMessageCreator;
import com.helger.as4lib.signing.SignedMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.httpclient.HttpClientFactory;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;
import com.helger.xml.serialize.read.DOMReader;

public class SOAPClientSAAJ
{
  public static final ConfigFile CF = new ConfigFileBuilder ().addPath ("as4test.properties").build ();

  public static Document getSoapEnvelope11ForTest (@Nonnull final String sPath) throws SAXException,
                                                                                IOException,
                                                                                ParserConfigurationException
  {
    final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ();
    domFactory.setNamespaceAware (true); // never forget this!
    final DocumentBuilder builder = domFactory.newDocumentBuilder ();
    return builder.parse (new ClassPathResource (sPath).getInputStream ());
  }

  /**
   * Starting point for the SAAJ - SOAP Client Testing
   *
   * @param args
   *        ignored
   */
  public static void main (final String [] args)
  {
    try
    {
      final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
      aSSLContext.init (null,
                        new TrustManager [] { new TrustManagerTrustAll (false) },
                        RandomHelper.getSecureRandom ());

      final CloseableHttpClient aClient = new HttpClientFactory (aSSLContext).createHttpClient ();
      final HttpClientContext aContext = new HttpClientContext ();
      aContext.setRequestConfig (RequestConfig.custom ().setProxy (new HttpHost ("172.30.9.12", 8080)).build ());

      // final HttpPost aPost = new HttpPost
      // ("http://127.0.0.1:8080/services/msh/");
      final HttpPost aPost = new HttpPost ("http://localhost:8080/as4");

      final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
      final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

      // No Mime Message, just SOAP + Payload in SOAP - Body
      if (true)
      {
        // final Document aDoc = TestMessages.testSignedUserMessage
        // (ESOAPVersion.SOAP_11, aPayload, aAttachments);
        final Document aDoc = TestMessages.testUserMessageSoapNotSigned (ESOAPVersion.SOAP_11, aPayload, aAttachments);
        aPost.setEntity (new StringEntity (AS4XMLHelper.serializeXML (aDoc)));
      }
      else
        if (true)
        {
          aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test.xml.gz"),
                                                   CMimeType.APPLICATION_GZIP));

          final SignedMessageCreator aSigned = new SignedMessageCreator ();
          final MimeMessage aMsg = new MimeMessageCreator (ESOAPVersion.SOAP_12).generateMimeMessage (aSigned.createSignedMessage (TestMessages.testUserMessageSoapNotSigned (ESOAPVersion.SOAP_12,
                                                                                                                                                                              aPayload,
                                                                                                                                                                              aAttachments),
                                                                                                                                   ESOAPVersion.SOAP_12,
                                                                                                                                   aAttachments,
                                                                                                                                   false),
                                                                                                      aAttachments,
                                                                                                      null);

          // Move all global mime headers to the POST request
          MessageHelperMethods.moveMIMEHeadersToHTTPHeader (aMsg, aPost);
          aPost.setEntity (new HttpMimeMessageEntity (aMsg));
        }

      // XXX reinstate if you wanna see the request that is getting sent
      System.out.println (EntityUtils.toString (aPost.getEntity ()));

      final CloseableHttpResponse aHttpResponse = aClient.execute (aPost);

      System.out.println ("GET Response Status:: " + aHttpResponse.getStatusLine ().getStatusCode ());

      // print result
      System.out.println (EntityUtils.toString (aHttpResponse.getEntity ()));
    }
    catch (final Exception e)
    {
      System.err.println ("Error occurred while sending SOAP Request to Server");
      e.printStackTrace ();
    }
  }
}
