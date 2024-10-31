/*
 * Copyright (C) 2020-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.cef;

import java.io.File;
import java.security.KeyStore;
import java.util.Locale;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.mime.CMimeType;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderConstant;
import com.helger.phase4.messaging.http.HttpRetrySettings;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.security.certificate.CertificateHelper;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.security.keystore.LoadedKeyStore;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * This is an example file for sending to the CEF eInvoicing test endpoint
 *
 * @author Philip Helger
 */
public class MainCEFeInvoicingConnectivityTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainCEFeInvoicingConnectivityTest.class);

  // Shared between sender and receiver
  private static final KeyStore TRUST_STORE;
  private static final AS4CryptoFactoryInMemoryKeyStore CF;

  // XXX change 00 to your ID
  private static final String YOUR_ID = "einvoicingct_00_gw";

  static
  {
    final LoadedKeyStore aLKS = KeyStoreHelper.loadKeyStore (EKeyStoreType.JKS,
                                                             YOUR_ID + "keystore.jks",
                                                             "test123".toCharArray ());
    if (aLKS.isFailure ())
      throw new InitializationException ("KeyStore error: " + aLKS.getErrorText (Locale.US));

    final LoadedKeyStore aLTS = KeyStoreHelper.loadKeyStore (EKeyStoreType.JKS,
                                                             "gateway_truststore.jks",
                                                             "test123".toCharArray ());
    if (aLTS.isFailure ())
      throw new InitializationException ("TrustStore error: " + aLTS.getErrorText (Locale.US));

    TRUST_STORE = aLTS.getKeyStore ();
    CF = new AS4CryptoFactoryInMemoryKeyStore (aLKS.getKeyStore (), YOUR_ID, "test123".toCharArray (), TRUST_STORE);
  }

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    try (final WebScoped w = new WebScoped ())
    {
      // Read a UBL Invoice
      final byte [] aPayloadBytes = SimpleFileIO.getAllFileBytes (new File ("src/test/resources/external/examples/base-example.xml"));
      if (aPayloadBytes == null)
        throw new IllegalStateException ();

      final IAS4ClientBuildMessageCallback x1 = new IAS4ClientBuildMessageCallback ()
      {
        public void onAS4Message (final AbstractAS4Message <?> aMsg)
        {
          final AS4UserMessage aUserMsg = (AS4UserMessage) aMsg;
          LOGGER.info ("Sending out AS4 message with message ID '" +
                       aUserMsg.getEbms3UserMessage ().getMessageInfo ().getMessageId () +
                       "'");
          LOGGER.info ("Sending out AS4 message with conversation ID '" +
                       aUserMsg.getEbms3UserMessage ().getCollaborationInfo ().getConversationId () +
                       "'");
        }

        public void onSoapDocument (@Nonnull final Document aDoc)
        {
          if (false)
            LOGGER.info ("SOAP Document:\n" +
                         XMLWriter.getNodeAsString (aDoc,
                                                    new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN)));
        }
      };
      // XXX The message ID to use in the UI
      final String sAS4MessageID = "36999089-662a-441f-95fd-470bec2b538e-100@phase4";
      final EAS4UserMessageSendResult eRes = Phase4CEFSender.builder ()
                                                            .cryptoFactory (CF)
                                                            .httpRetrySettings (new HttpRetrySettings ().setMaxRetries (0))
                                                            .action ("TC1Leg1")
                                                            .service ("tc1", "bdx:noprocess")
                                                            .senderParticipantID (new SimpleParticipantIdentifier ("connectivity-partid-qns",
                                                                                                                   YOUR_ID))
                                                            .receiverParticipantID (new SimpleParticipantIdentifier ("connectivity-partid-qns",
                                                                                                                     "domibus-gitb"))
                                                            .fromPartyIDType ("urn:oasis:names:tc:ebcore:partyid-type:unregistered")
                                                            .fromPartyID (YOUR_ID)
                                                            .fromRole (CAS4.DEFAULT_INITIATOR_URL)
                                                            .toPartyIDType ("urn:oasis:names:tc:ebcore:partyid-type:unregistered")
                                                            .toPartyID ("domibus-gitb")
                                                            .toRole (CAS4.DEFAULT_RESPONDER_URL)
                                                            .messageID (sAS4MessageID)
                                                            .payload (AS4OutgoingAttachment.builder ()
                                                                                           .data (aPayloadBytes)
                                                                                           .filename ("businessContentPayload")
                                                                                           .compressionGZIP ()
                                                                                           .mimeType (CMimeType.TEXT_XML)
                                                                                           .contentID ("message")
                                                                                           .build ())
                                                            .buildMessageCallback (x1)
                                                            .endpointDetailProvider (new AS4EndpointDetailProviderConstant (CertificateHelper.convertStringToCertficateOrNull ("-----BEGIN CERTIFICATE-----\r\n" +
                                                                                                                                                                               "MIIDOzCCAiOgAwIBAgIJAKbwaKpEwNTKMA0GCSqGSIb3DQEBCwUAMDQxDTALBgNV\r\n" +
                                                                                                                                                                               "BAoMBEdJVEIxDTALBgNVBAsMBEdJVEIxFDASBgNVBAMMC2dpdGItZW5naW5lMB4X\r\n" +
                                                                                                                                                                               "DTE0MTIyNDEzMjIzNFoXDTI0MTIyMTEzMjIzNFowNDENMAsGA1UECgwER0lUQjEN\r\n" +
                                                                                                                                                                               "MAsGA1UECwwER0lUQjEUMBIGA1UEAwwLZ2l0Yi1lbmdpbmUwggEiMA0GCSqGSIb3\r\n" +
                                                                                                                                                                               "DQEBAQUAA4IBDwAwggEKAoIBAQCpNuRRMhpd2SvNKsZe/WTxm4zuX2Zc5by3zGcm\r\n" +
                                                                                                                                                                               "uzwePdMCnCXk2FAUH67qS9r5VBa4USfiB7l1piyLrNwYWGRDo5OeWIz6Q821/1v7\r\n" +
                                                                                                                                                                               "UHq7FfB0LFPcJ+mOwrDqS+VL0MjcSW4pocJHrpFwObWHTY/R4WmW2xwGOKVh0OUL\r\n" +
                                                                                                                                                                               "UhqQsHDnDhCzFaEWhS8n1lUw3GRipwKLyYvXK8XgLceEmh+j0+cdmIj4a1L4oza/\r\n" +
                                                                                                                                                                               "UgBnCqSob+vowgClyZnGVihE9K8eLLwCSLlIiD+bXWf0VJPLXBNLdNIkRRC0QO0j\r\n" +
                                                                                                                                                                               "T9TuE5TF3SknkA5D0NFp023Alz7jieI0D6JE78QyNQN6y6QRAgMBAAGjUDBOMB0G\r\n" +
                                                                                                                                                                               "A1UdDgQWBBQpAkry20hAcvlw+4poxQC8TI+EgTAfBgNVHSMEGDAWgBQpAkry20hA\r\n" +
                                                                                                                                                                               "cvlw+4poxQC8TI+EgTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQBS\r\n" +
                                                                                                                                                                               "dfmT3E9uvhiEgVefdwXkkxqlXLQQxfjaqVRVzPTHLqdVs/nBK+iQNhqg+6eLcaGQ\r\n" +
                                                                                                                                                                               "yyDy88vwQ85rqwOFbZd05esIFXYl0pgl1pVsb7HmMNmKT3UPay3HDlHX45ZoexDU\r\n" +
                                                                                                                                                                               "pza4OcrauEM8Yg/5i9dCIPC1GiHebJpYusMVfP78b+5DAyARrHtcb0EJ8rOLxHh6\r\n" +
                                                                                                                                                                               "K2S4EHI6sqQkGHEt1z4m66LyK+vnkLGaq3y6MWEufh78eICDyyVz0DhdIhr18ZHX\r\n" +
                                                                                                                                                                               "dpcsH2VOkE36KnWSo0spEXa6ZtP8MqQ60kJgBt4XcuArKfjIGC6vB6dE0NzXngBD\r\n" +
                                                                                                                                                                               "PHgMfmHJW018/6eN/f0q\r\n" +
                                                                                                                                                                               "-----END CERTIFICATE-----"),
                                                                                                                            "https://www.itb.ec.europa.eu/cef/domibus/services/msh"))
                                                            .sendMessageAndCheckForReceipt ();
      LOGGER.info ("Sending AS4 message to CEF with result " + eRes);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
