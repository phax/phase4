package com.helger.as4server.client;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPMessage;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientResponseHelper;
import com.helger.xml.serialize.write.XMLWriter;

public class SOAPClientSAAJ
{

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

      final HttpPost aPost = new HttpPost ("https://test.erechnung.gv.at/as4/msh/");
      aPost.setEntity (new InputStreamEntity (ClassPathResource.getInputStream ("UserMessage.xml")));
      aClient.execute (aPost, HttpClientResponseHelper.RH_XML, aContext);

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

}
