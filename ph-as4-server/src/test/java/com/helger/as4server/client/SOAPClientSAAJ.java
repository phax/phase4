package com.helger.as4server.client;

import java.io.File;
import java.io.IOException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.helger.as4server.message.MessageHelperMethods;
import com.helger.commons.charset.CCharset;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.NonBlockingStringWriter;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.httpclient.HttpClientFactory;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

public class SOAPClientSAAJ
{
  public static final ConfigFile CF = new ConfigFileBuilder ().addPath ("as4test.properties").build ();

  private static final XMLWriterSettings XWS = new XMLWriterSettings ();
  static
  {
    final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext ();
    aNSCtx.addMapping ("ds", MessageHelperMethods.DS_NS);
    aNSCtx.addMapping ("eb", MessageHelperMethods.EBMS_NS);
    aNSCtx.addMapping ("wsse", MessageHelperMethods.WSSE_NS);
    aNSCtx.addMapping ("wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
    aNSCtx.addMapping ("S11", "http://schemas.xmlsoap.org/soap/envelope/");
    aNSCtx.addMapping ("cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
    aNSCtx.addMapping ("cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
    aNSCtx.addMapping ("ec", "http://www.w3.org/2001/10/xml-exc-c14n#");
    XWS.setNamespaceContext (aNSCtx);
    XWS.setPutNamespaceContextPrefixesInRoot (true);
    XWS.setIndent (EXMLSerializeIndent.NONE);
  }

  private static String _serializeXMLMy (final Node aNode)
  {
    return XMLWriter.getNodeAsString (aNode, XWS);
  }

  private static String _serializeXMLRT (final Node aNode) throws TransformerFactoryConfigurationError,
                                                           TransformerException
  {
    if (false)
      return XMLWriter.getNodeAsString (aNode, XWS);

    final Transformer transformer = TransformerFactory.newInstance ().newTransformer ();
    final NonBlockingStringWriter aSW = new NonBlockingStringWriter ();
    transformer.transform (new DOMSource (aNode), new StreamResult (aSW));
    return aSW.getAsString ();
  }

  private static String _serializeXML (final Node aNode) throws TransformerFactoryConfigurationError,
                                                         TransformerException
  {
    return true ? _serializeXMLRT (aNode) : _serializeXMLMy (aNode);
  }

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
      if (true)
      {
        final Document aDoc = TestMessages.testUserMessage ();
        SimpleFileIO.writeFile (new File ("ser-my.txt"), _serializeXMLMy (aDoc), CCharset.CHARSET_UTF_8_OBJ);
        SimpleFileIO.writeFile (new File ("ser-rt.txt"), _serializeXMLRT (aDoc), CCharset.CHARSET_UTF_8_OBJ);
        aPost.setEntity (new StringEntity (_serializeXML (aDoc)));
      }
      else
        if (true)
          // TODO
          aPost.setEntity (new HttpMimeMessageEntity (null));
        else
          aPost.setEntity (new InputStreamEntity (ClassPathResource.getInputStream (false ? "compare.xml"
                                                                                          : "TestMessage.xml")));
      System.out.println (EntityUtils.toString (aPost.getEntity ()));

      // aPost.setEntity (new InputStreamEntity
      // (ClassPathResource.getInputStream ("UserMessage.xml")));
      // aPost.setEntity (new InputStreamEntity
      // (ClassPathResource.getInputStream ("ex-mmd-push.mmd")));
      // aClient.execute (aPost, HttpClientResponseHelper.RH_XML, aContext);
      //
      // aPost.setEntity (new StringEntity (_getSignedSoapMessage ()));

      final CloseableHttpResponse httpResponse = aClient.execute (aPost);

      System.out.println ("GET Response Status:: " + httpResponse.getStatusLine ().getStatusCode ());

      // print result
      System.out.println (EntityUtils.toString (httpResponse.getEntity ()));
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
    System.out.print (XMLWriter.getNodeAsString (soapResponse.getSOAPPart (), XWS));
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
