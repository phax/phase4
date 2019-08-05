/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as4.client;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.crypto.AS4CryptParams;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.AS4SigningParams;
import com.helger.as4.http.HttpMimeMessageEntity;
import com.helger.as4.http.HttpXMLEntity;
import com.helger.as4.messaging.crypto.AS4Encryptor;
import com.helger.as4.messaging.crypto.AS4Signer;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.mime.MimeMessageCreator;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;
import com.helger.httpclient.HttpClientFactory;
import com.helger.xml.serialize.read.DOMReader;

public final class MainAS4Client
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainAS4Client.class);

  private MainAS4Client ()
  {}

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
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      String sURL = "http://127.0.0.1:8080/as4";
      if (false)
        sURL = "http://msh.holodeck-b2b.org:8080/msh";

      // Deactivate if not sending to localholodeck
      if (false)
        sURL = "http://localhost:8080/msh/";

      final HttpClientFactory aHCF = new HttpClientFactory ();
      if (sURL.startsWith ("https"))
        aHCF.setSSLContextTrustAll ();
      if (true)
      {
        aHCF.setProxy (new HttpHost ("172.30.9.6", 8080));
        aHCF.addNonProxyHostsFromPipeString ("localhost|127.0.0.1");
      }
      final CloseableHttpClient aClient = aHCF.createHttpClient ();

      LOGGER.info ("Sending to " + sURL);
      final HttpPost aPost = new HttpPost (sURL);

      final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
      final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
      final ESOAPVersion eSOAPVersion = ESOAPVersion.SOAP_12;
      final AS4CryptoFactory aCryptoFactory = AS4CryptoFactory.DEFAULT_INSTANCE;

      // No Mime Message Not signed or encrypted, just SOAP + Payload in SOAP -
      // Body
      if (true)
      {
        // final Document aDoc = TestMessages.testSignedUserMessage
        // (ESOAPVersion.SOAP_11, aPayload, aAttachments);
        final AS4UserMessage aMsg = MockClientMessages.testUserMessageSoapNotSigned (eSOAPVersion,
                                                                                     aPayload,
                                                                                     aAttachments);
        final Document aDoc = aMsg.getAsSOAPDocument (aPayload);
        aPost.setEntity (new HttpXMLEntity (aDoc, eSOAPVersion));
      }
      else
        // BodyPayload SIGNED
        if (false)
        {
          final Document aDoc = MockClientMessages.testSignedUserMessage (eSOAPVersion,
                                                                          aPayload,
                                                                          aAttachments,
                                                                          aResHelper);
          aPost.setEntity (new HttpXMLEntity (aDoc, eSOAPVersion));
        }
        // BodyPayload ENCRYPTED
        else
          if (false)
          {
            final AS4UserMessage aMsg = MockClientMessages.testUserMessageSoapNotSigned (eSOAPVersion,
                                                                                         aPayload,
                                                                                         aAttachments);
            Document aDoc = aMsg.getAsSOAPDocument (aPayload);
            aDoc = AS4Encryptor.encryptSoapBodyPayload (aCryptoFactory,
                                                        eSOAPVersion,
                                                        aDoc,
                                                        false,
                                                        AS4CryptParams.createDefault ().setAlias ("dummy"));

            aPost.setEntity (new HttpXMLEntity (aDoc, eSOAPVersion));
          }
          else
            if (true)
            {
              aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/test.xml.gz"),
                                                                              CMimeType.APPLICATION_GZIP,
                                                                              null,
                                                                              aResHelper));
              final AS4UserMessage aMsg = MockClientMessages.testUserMessageSoapNotSigned (eSOAPVersion,
                                                                                           null,
                                                                                           aAttachments);
              final MimeMessage aMimeMsg = MimeMessageCreator.generateMimeMessage (eSOAPVersion,
                                                                                   AS4Signer.createSignedMessage (aCryptoFactory,
                                                                                                                  aMsg.getAsSOAPDocument (null),
                                                                                                                  eSOAPVersion,
                                                                                                                  aMsg.getMessagingID (),
                                                                                                                  aAttachments,
                                                                                                                  aResHelper,
                                                                                                                  false,
                                                                                                                  AS4SigningParams.createDefault ()),
                                                                                   aAttachments);

              // Move all global mime headers to the POST request
              MessageHelperMethods.forEachHeaderAndRemoveAfterwards (aMimeMsg, aPost::addHeader);
              aPost.setEntity (new HttpMimeMessageEntity (aMimeMsg));
            }
            else
              if (false)
              {
                Document aDoc = MockClientMessages.testSignedUserMessage (eSOAPVersion,
                                                                          aPayload,
                                                                          aAttachments,
                                                                          aResHelper);
                aDoc = AS4Encryptor.encryptSoapBodyPayload (aCryptoFactory,
                                                            eSOAPVersion,
                                                            aDoc,
                                                            false,
                                                            AS4CryptParams.createDefault ().setAlias ("dummy"));
                aPost.setEntity (new HttpXMLEntity (aDoc, eSOAPVersion));
              }
              else
                throw new IllegalStateException ("Some test message should be selected :)");

      // XXX reinstate if you wanna see the request that is getting sent
      LOGGER.info (EntityUtils.toString (aPost.getEntity ()));

      final CloseableHttpResponse aHttpResponse = aClient.execute (aPost);

      LOGGER.info ("GET Response Status:: " + aHttpResponse.getStatusLine ().getStatusCode ());

      // print result
      LOGGER.info (EntityUtils.toString (aHttpResponse.getEntity ()));
    }
    catch (final Exception e)
    {
      LOGGER.error ("Error occurred while sending SOAP Request to Server", e);
    }
  }
}
