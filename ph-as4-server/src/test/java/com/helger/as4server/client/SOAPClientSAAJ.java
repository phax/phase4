package com.helger.as4server.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPMessage;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.httpclient.HttpClientFactory;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;
import com.helger.xml.serialize.write.XMLWriter;

public class SOAPClientSAAJ
{
  public static final ConfigFile CF = new ConfigFileBuilder ().addPath ("as4test.properties").build ();

  /**
   * Starting point for the SAAJ - SOAP Client Testing
   */
  public static void main (final String args[])
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
      // ("http://localhost:8080/services/"); // Original
      // b2bholodeck
      final HttpPost aPost = new HttpPost ("http://127.0.0.1:8080/services/msh/");
      // HB2B Online
      // final HttpPost aPost = new HttpPost
      // ("http://msh.holodeck-b2b.org:8080/services/msh");
      // aPost.addHeader ("SOAPAction", "\"msh\"");
      // TODO atm only calls testMessage
      aPost.setEntity (new StringEntity (XMLWriter.getXMLString (TestMessages.testUserMessage ())));

      // aPost.setEntity (new InputStreamEntity
      // (ClassPathResource.getInputStream ("TestMessage.xml")));

      // aPost.setEntity (new InputStreamEntity
      // (ClassPathResource.getInputStream ("UserMessage.xml")));
      // aPost.setEntity (new InputStreamEntity
      // (ClassPathResource.getInputStream ("ex-mmd-push.mmd")));
      // aClient.execute (aPost, HttpClientResponseHelper.RH_XML, aContext);
      //
      // aPost.setEntity (new StringEntity (_getSignedSoapMessage ()));

      final CloseableHttpResponse httpResponse = aClient.execute (aPost);

      System.out.println ("GET Response Status:: " + httpResponse.getStatusLine ().getStatusCode ());

      final BufferedReader reader = new BufferedReader (new InputStreamReader (httpResponse.getEntity ()
                                                                                           .getContent ()));

      String inputLine;
      final StringBuffer response = new StringBuffer ();

      while ((inputLine = reader.readLine ()) != null)
      {
        response.append (inputLine);
      }
      reader.close ();

      // print result
      System.out.println (response.toString ());
      // // Create SOAP Connection
      // final SOAPConnectionFactory soapConnectionFactory =
      // SOAPConnectionFactory.newInstance ();
      // final SOAPConnection soapConnection =
      // soapConnectionFactory.createConnection ();
      //
      // // Send SOAP Message to SOAP Server
      // final String url = ;
      // final SOAPMessage soapResponse = soapConnection.call
      // (_createSOAPRequest12 (), url);
      //
      // // Process the SOAP Response
      // _printSOAPResponse (soapResponse);
      //
      // soapConnection.close ();
    }
    catch (final Exception e)
    {
      System.err.println ("Error occurred while sending SOAP Request to Server");
      e.printStackTrace ();
    }
  }

  private static SOAPMessage _createSOAPRequest11 () throws Exception
  {
    final MessageFactory messageFactory = MessageFactory.newInstance (SOAPConstants.SOAP_1_1_PROTOCOL);
    final SOAPMessage soapMessage = messageFactory.createMessage (new MimeHeaders (),
                                                                  ClassPathResource.getInputStream ("UserMessage.xml"));

    /* Print the request message */
    System.out.print ("Request SOAP Message = ");
    soapMessage.writeTo (System.out);
    System.out.println ();

    return soapMessage;
  }

  private static SOAPMessage _createSOAPRequest12 () throws Exception
  {
    final MessageFactory messageFactory = MessageFactory.newInstance (SOAPConstants.SOAP_1_2_PROTOCOL);
    final SOAPMessage soapMessage = messageFactory.createMessage (new MimeHeaders (),
                                                                  ClassPathResource.getInputStream ("UserMessage12.xml"));

    /* Print the request message */
    System.out.print ("Request SOAP Message = ");
    soapMessage.writeTo (System.out);
    System.out.println ();

    return soapMessage;
  }

  /**
   * Method used to print the SOAP Response
   */
  private static void _printSOAPResponse (final SOAPMessage soapResponse) throws Exception
  {
    System.out.print (XMLWriter.getXMLString (soapResponse.getSOAPPart ()));
    // final TransformerFactory transformerFactory =
    // TransformerFactory.newInstance ();
    // final Transformer transformer = transformerFactory.newTransformer ();
    // final Source sourceContent = soapResponse.getSOAPPart ().getContent ();
    // System.out.print ("\nResponse SOAP Message = ");
    // final StreamResult result = new StreamResult (System.out);
    // transformer.transform (sourceContent, result);
  }

  private static String _getSignedSoapMessage () throws Exception
  {
    WSSConfig.init ();
    final Crypto crypto = CryptoFactory.getInstance ();
    final WSSecSignature builder = new WSSecSignature ();
    builder.setUserInfo (CF.getAsString ("key.alias"), CF.getAsString ("key.password"));
    builder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    builder.setSignatureAlgorithm ("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
    // TODO DONT FORGET: PMode indicates the DigestAlgorithmen as Hash Function
    builder.setDigestAlgo (WSS4JConstants.SHA256);
    final Document doc = _getSoapEnvelope11 ();
    final WSSecHeader secHeader = new WSSecHeader (doc);
    secHeader.insertSecurityHeader ();
    final Document signedDoc = builder.build (doc, crypto, secHeader);
    return XMLUtils.prettyDocumentToString (signedDoc);
  }

  private static Document _getSoapEnvelope11 () throws SAXException, IOException, ParserConfigurationException
  {
    final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ();
    domFactory.setNamespaceAware (true); // never forget this!
    final DocumentBuilder builder = domFactory.newDocumentBuilder ();
    return builder.parse (new ClassPathResource ("UserMessageWithoutWSSE.xml").getInputStream ());
  }

}
