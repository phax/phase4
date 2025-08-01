/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol;

import java.io.File;
import java.security.KeyStore;

import org.slf4j.Logger;
import org.w3c.dom.Element;

import com.helger.peppol.security.PeppolTrustStores;
import com.helger.peppol.sml.ESML;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * An example file that uses an externally supplied key store and trust store so that no disc access
 * is needed.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderInMemoryKeyStore
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MainPhase4PeppolSenderInMemoryKeyStore.class);

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    try
    {
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/external/examples/base-example.xml"))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ("Failed to read XML file to be send");

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:helger");
      final KeyStore aKS = KeyStoreHelper.loadKeyStoreDirect (EKeyStoreType.PKCS12,
                                                              "test-ap-2023.p12",
                                                              "peppol".toCharArray ());
      if (aKS == null)
        throw new IllegalStateException ();
      final IAS4CryptoFactory aInMemoryCryptoFactory = new AS4CryptoFactoryInMemoryKeyStore (aKS,
                                                                                             "cert",
                                                                                             "peppol".toCharArray (),
                                                                                             PeppolTrustStores.Config2025.TRUSTSTORE_AP_TEST);
      final EAS4UserMessageSendResult eResult = Phase4PeppolSender.builder ()
                                                                  .cryptoFactory (aInMemoryCryptoFactory)
                                                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                                                  .receiverParticipantID (aReceiverID)
                                                                  .senderPartyID ("POP000306")
                                                                  .countryC1 ("AT")
                                                                  .payload (aPayloadElement)
                                                                  .smpClient (new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                                                                                                     aReceiverID,
                                                                                                     ESML.DIGIT_TEST))
                                                                  .disableValidation ()
                                                                  .sendMessageAndCheckForReceipt ();
      LOGGER.info ("Peppol send result: " + eResult);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending Peppol message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
