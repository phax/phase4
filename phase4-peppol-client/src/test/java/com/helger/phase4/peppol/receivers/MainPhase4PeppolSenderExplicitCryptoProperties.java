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
package com.helger.phase4.peppol.receivers;

import java.io.File;

import org.slf4j.Logger;

import com.helger.commons.io.file.SimpleFileIO;
import com.helger.peppol.security.PeppolTrustStores;
import com.helger.peppol.sml.ESML;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.peppol.Phase4PeppolValidatonResultHandler;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phive.peppol.PeppolValidation2024_11;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.ITrustStoreDescriptor;
import com.helger.security.keystore.KeyStoreAndKeyDescriptor;
import com.helger.servlet.mock.MockServletContext;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.mgr.WebScopeManager;

/**
 * Example class that provides the configuration items "inline" and not from configuration.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderExplicitCryptoProperties
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MainPhase4PeppolSenderExplicitCryptoProperties.class);

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    try
    {
      final byte [] aPayloadBytes = SimpleFileIO.getAllFileBytes (new File ("src/test/resources/external/examples/base-example.xml"));
      if (aPayloadBytes == null)
        throw new IllegalStateException ("Failed to read XML file to be send");

      final KeyStoreAndKeyDescriptor aKSD = KeyStoreAndKeyDescriptor.builder ()
                                                                    .type (EKeyStoreType.PKCS12)
                                                                    .path ("test-ap-2025.p12")
                                                                    .password ("peppol")
                                                                    .keyAlias ("cert")
                                                                    .keyPassword ("peppol")
                                                                    .build ();
      final ITrustStoreDescriptor aTSD = PeppolTrustStores.Config2025.TRUSTSTORE_DESCRIPTOR_AP_TEST;

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9958:peppol-development-governikus-01");
      final EAS4UserMessageSendResult eResult = Phase4PeppolSender.builder ()
                                                                  .cryptoFactory (new AS4CryptoFactoryInMemoryKeyStore (aKSD,
                                                                                                                        aTSD))
                                                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                                                  .receiverParticipantID (aReceiverID)
                                                                  .senderPartyID ("POP000306")
                                                                  .countryC1 ("AT")
                                                                  .payload (aPayloadBytes)
                                                                  .smpClient (new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                                                                                                     aReceiverID,
                                                                                                     ESML.DIGIT_TEST))
                                                                  .validationConfiguration (PeppolValidation2024_11.VID_OPENPEPPOL_INVOICE_UBL_V3,
                                                                                            new Phase4PeppolValidatonResultHandler ())
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
