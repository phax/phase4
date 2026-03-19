/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.edelivery2;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Standalone test that sends an eDelivery AS4 2.0 message to the EC eDelivery2 AS4 Security
 * Validator running locally on port 8080. <br>
 * <br>
 * Prerequisites:
 * <ul>
 * <li>The EC eDelivery2 AS4 Security Validator must be running locally (default port 8080)</li>
 * <li>We use the validator's own "blue" keystore for signing/encryption since the validator trusts
 * those certificates</li>
 * </ul>
 * Usage:
 * <ol>
 * <li>Clone and build the EC security validator from
 * https://ec.europa.eu/digital-building-blocks/code/projects/EDELIVERY/repos/edelivery2-as4-security-validator/browse</li>
 * <li>Start the validator with <code>./start.sh</code></li>
 * <li>Run this class as a Java application</li>
 * </ol>
 *
 * @author Philip Helger
 */
public final class MainPhase4EDelivery2SendToValidator
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MainPhase4EDelivery2SendToValidator.class);

  /** The validator's receive endpoint - signature query param tells it what algorithm to expect */
  private static final String VALIDATOR_RECEIVE_URL = "http://localhost:8080/as4/receive?signature=eddsa";

  /**
   * Path to the validator's keystore - we reuse it since the validator trusts its own "blue" certs.
   * Can be overridden via system property <code>-Dvalidator.keystore.path=...</code>
   */
  private static final String VALIDATOR_KEYSTORE_PATH = System.getProperty ("validator.keystore.path",
                                                                            System.getProperty ("user.home") +
                                                                                                       "/dev/git-thirdparty/edelivery2-as4-security-validator/edelivery2-as4-security-validator-boot/src/main/config/keystore/gateway_keystore.p12");
  private static final String VALIDATOR_KEYSTORE_PASSWORD = "security";

  /** Alias for the EdDSA signing key in the validator's keystore */
  private static final String SIGNING_KEY_ALIAS = "blue_eddsa_sign";

  /** Alias for the X25519 encryption key in the validator's keystore */
  private static final String ENCRYPTION_KEY_ALIAS = "blue_eddsa_decrypt";

  /** Party ID type as required by eDelivery AS4 2.0 */
  private static final String PARTY_ID_TYPE = "urn:oasis:names:tc:ebcore:partyid-type:unregistered";

  /** Party ID value (matching the validator's SOAP template) */
  private static final String PARTY_ID = "domibus-blue";

  private MainPhase4EDelivery2SendToValidator ()
  {}

  private static IAS4CryptoFactory _loadCryptoFactory () throws Exception
  {
    final File aKSFile = new File (VALIDATOR_KEYSTORE_PATH).getAbsoluteFile ();
    if (!aKSFile.exists ())
      throw new IllegalStateException ("Validator keystore not found at '" +
                                       aKSFile.getAbsolutePath () +
                                       "'. Please adjust VALIDATOR_KEYSTORE_PATH.");

    LOGGER.info ("Loading validator keystore from: " + aKSFile.getAbsolutePath ());

    final KeyStore aKeyStore = KeyStoreHelper.loadKeyStoreDirect (EKeyStoreType.PKCS12,
                                                                  aKSFile.getAbsolutePath (),
                                                                  VALIDATOR_KEYSTORE_PASSWORD.toCharArray ());
    if (aKeyStore == null)
      throw new IllegalStateException ("Failed to load validator keystore");

    // Use the same keystore as truststore since the validator trusts "blue" certs
    return new AS4CryptoFactoryInMemoryKeyStore (aKeyStore,
                                                 SIGNING_KEY_ALIAS,
                                                 VALIDATOR_KEYSTORE_PASSWORD.toCharArray (),
                                                 aKeyStore);
  }

  public static void main (final String [] args)
  {
    // Register BouncyCastle for Ed25519/X25519 support
    Security.addProvider (new BouncyCastleProvider ());

    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Enable message dumping for debugging
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    try (final WebScoped w = new WebScoped ())
    {
      final IAS4CryptoFactory aCryptoFactory = _loadCryptoFactory ();

      // Extract the receiver encryption certificate (X25519) from keystore
      final X509Certificate aReceiverEncCert = (X509Certificate) aCryptoFactory.getKeyStore ()
                                                                               .getCertificate (ENCRYPTION_KEY_ALIAS);
      if (aReceiverEncCert == null)
        throw new IllegalStateException ("Failed to find encryption certificate with alias '" +
                                         ENCRYPTION_KEY_ALIAS +
                                         "' in keystore");

      LOGGER.info ("Using receiver encryption certificate: " + aReceiverEncCert.getSubjectX500Principal ().getName ());

      // Simple XML payload
      final byte [] aPayloadBytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                     "<TestMessage>\n" +
                                     "  <Content>Hello from phase4 eDelivery AS4 2.0!</Content>\n" +
                                     "</TestMessage>").getBytes (StandardCharsets.UTF_8);

      // Send using the EdDSA/X25519 builder
      LOGGER.info ("--- Sending EdDSA/X25519 message to validator at " + VALIDATOR_RECEIVE_URL + " ---");

      final EAS4UserMessageSendResult eResult;
      eResult = Phase4EDelivery2Sender.builderEdDSA ()
                                      .cryptoFactory (aCryptoFactory)
                                      .senderParticipantID (Phase4EDelivery2Sender.IF.createParticipantIdentifier (PARTY_ID_TYPE,
                                                                                                                   PARTY_ID))
                                      .receiverParticipantID (Phase4EDelivery2Sender.IF.createParticipantIdentifier (PARTY_ID_TYPE,
                                                                                                                     PARTY_ID))
                                      .fromPartyIDType (PARTY_ID_TYPE)
                                      .fromPartyID (PARTY_ID)
                                      .fromRole (CAS4.DEFAULT_INITIATOR_URL)
                                      .toPartyIDType (PARTY_ID_TYPE)
                                      .toPartyID (PARTY_ID)
                                      .toRole (CAS4.DEFAULT_RESPONDER_URL)
                                      .service (CAS4.DEFAULT_SERVICE_URL)
                                      .action (CAS4.DEFAULT_ACTION_URL)
                                      .payload (AS4OutgoingAttachment.builder ().data (aPayloadBytes).mimeTypeXML ())
                                      .receiverEndpointDetails (aReceiverEncCert, VALIDATOR_RECEIVE_URL)
                                      .buildMessageCallback (new IAS4ClientBuildMessageCallback ()
                                      {
                                        public void onAS4Message (@NonNull final AbstractAS4Message <?> aMsg)
                                        {
                                          final AS4UserMessage aUserMsg = (AS4UserMessage) aMsg;
                                          LOGGER.info ("Sending AS4 message with ID: " +
                                                       aUserMsg.getEbms3UserMessage ()
                                                               .getMessageInfo ()
                                                               .getMessageId ());
                                        }
                                      })
                                      .rawResponseConsumer (aResponseMsg -> LOGGER.info ("Response received:\n  " +
                                                                                         new String (aResponseMsg.getResponseContent (),
                                                                                                     StandardCharsets.UTF_8)))
                                      .sendMessageAndCheckForReceipt ();

      LOGGER.info ("Send result: " + eResult);

      if (eResult.isSuccess ())
        LOGGER.info ("SUCCESS - eDelivery AS4 2.0 EdDSA message was accepted by the validator!");
      else
        LOGGER.error ("FAILED - Result: " + eResult);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending eDelivery AS4 2.0 message", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
