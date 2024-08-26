/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.messaging.crypto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.attachment.WSS4JAttachmentCallbackHandler;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.ECryptoMode;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.wss.WSSConfigManager;
import com.helger.phase4.wss.WSSSynchronizer;

/**
 * Message singing helper.
 *
 * @author Philip Helger
 */
@Immutable
public final class AS4Signer
{
  static final String ENCRYPTION_MODE_CONTENT = "Content";
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4Signer.class);
  private static final String CID_ATTACHMENTS = MessageHelperMethods.PREFIX_CID + "Attachments";

  private AS4Signer ()
  {}

  @Nonnull
  private static Document _createSignedMessage (@Nonnull final IAS4CryptoFactory aCryptoFactorySign,
                                                @Nonnull final Document aPreSigningMessage,
                                                @Nonnull final ESoapVersion eSoapVersion,
                                                @Nonnull @Nonempty final String sMessagingID,
                                                @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                                @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                                final boolean bMustUnderstand,
                                                @Nonnull final AS4SigningParams aSigningParams) throws WSSecurityException
  {
    ValueEnforcer.notNull (aCryptoFactorySign, "CryptoFactorySign");
    ValueEnforcer.notNull (aPreSigningMessage, "PreSigningMessage");
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");
    ValueEnforcer.notEmpty (sMessagingID, "MessagingID");
    ValueEnforcer.notNull (aResHelper, "ResHelper");
    ValueEnforcer.notNull (aSigningParams, "SigningParams");

    LOGGER.info ("Now signing AS4 message [" +
                 eSoapVersion +
                 "]. KeyIdentifierType=" +
                 aSigningParams.getKeyIdentifierType ().name () +
                 "; KeyAlias=" +
                 aCryptoFactorySign.getKeyAlias () +
                 "; SignAlgo=" +
                 aSigningParams.getAlgorithmSign ().getAlgorithmURI () +
                 "; DigestAlgo=" +
                 aSigningParams.getAlgorithmSignDigest ().getAlgorithmURI () +
                 "; C14NAlgo=" +
                 aSigningParams.getAlgorithmC14N ().getAlgorithmURI ());

    // Start signing the document
    final WSSecHeader aSecHeader = new WSSecHeader (aPreSigningMessage);
    aSecHeader.insertSecurityHeader ();

    final WSSecSignature aBuilder = aSigningParams.hasWSSecSignatureCustomizer () ? aSigningParams.getWSSecSignatureCustomizer ()
                                                                                                  .createWSSecSignature (aSecHeader)
                                                                                  : new WSSecSignature (aSecHeader);
    if (aBuilder == null)
      throw new IllegalStateException ("Failed to create WSSecSignature for " + aSecHeader);

    aBuilder.setKeyIdentifierType (aSigningParams.getKeyIdentifierType ().getTypeID ());
    // Set keystore alias and key password
    aBuilder.setUserInfo (aCryptoFactorySign.getKeyAlias (),
                          aCryptoFactorySign.getKeyPasswordPerAlias (aCryptoFactorySign.getKeyAlias ()));
    aBuilder.setSignatureAlgorithm (aSigningParams.getAlgorithmSign ().getAlgorithmURI ());
    // PMode indicates the DigestAlgorithm as Hash Function
    aBuilder.setDigestAlgo (aSigningParams.getAlgorithmSignDigest ().getAlgorithmURI ());
    aBuilder.setSigCanonicalization (aSigningParams.getAlgorithmC14N ().getAlgorithmURI ());
    aBuilder.setSignatureProvider (aSigningParams.getSecurityProviderSign ());

    // true: BST ValueType "#X509v3" (the default, e.g. for Peppol)
    // false: BST ValueType "#X509PKIPathv1" (e.g. for BDEW)
    aBuilder.setUseSingleCertificate (aSigningParams.isUseSingleCertificate ());

    // Sign the Ebms3 Messaging element itself
    aBuilder.getParts ().add (new WSEncryptionPart (sMessagingID, ENCRYPTION_MODE_CONTENT));

    // Sign the SOAP body
    // Same element name for SOAP 1.1 and SOAP 1.2
    aBuilder.getParts ().add (new WSEncryptionPart ("Body", eSoapVersion.getNamespaceURI (), ENCRYPTION_MODE_CONTENT));

    if (CollectionHelper.isNotEmpty (aAttachments))
    {
      // Modify builder for attachments

      // "cid:Attachments" is a predefined ID used inside WSSecSignatureBase
      aBuilder.getParts ().add (new WSEncryptionPart (CID_ATTACHMENTS, ENCRYPTION_MODE_CONTENT));

      final WSS4JAttachmentCallbackHandler aAttachmentCallbackHandler = new WSS4JAttachmentCallbackHandler (aAttachments,
                                                                                                            aResHelper);
      aBuilder.setAttachmentCallbackHandler (aAttachmentCallbackHandler);
    }

    // Set the mustUnderstand header of the wsse:Security element as well
    final Attr aMustUnderstand = aSecHeader.getSecurityHeaderElement ()
                                           .getAttributeNodeNS (eSoapVersion.getNamespaceURI (), "mustUnderstand");
    if (aMustUnderstand != null)
      aMustUnderstand.setValue (eSoapVersion.getMustUnderstandValue (bMustUnderstand));

    // Customizer to be invoked as the last action
    if (aSigningParams.hasWSSecSignatureCustomizer ())
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Before running WSSecSignatureCustomizer.customize");
      aSigningParams.getWSSecSignatureCustomizer ().customize (aBuilder);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("After running WSSecSignatureCustomizer.customize");
    }

    return aBuilder.build (aCryptoFactorySign.getCrypto (ECryptoMode.ENCRYPT_SIGN));
  }

  /**
   * This method must be used if the message does not contain attachments, that
   * should be in a additional mime message part.
   *
   * @param aCryptoFactorySign
   *        CryptoFactory to use. May not be <code>null</code>.
   * @param aPreSigningMessage
   *        SOAP Document before signing
   * @param eSoapVersion
   *        SOAP version to use
   * @param sMessagingID
   *        The ID of the "Messaging" element to sign.
   * @param aAttachments
   *        Optional list of attachments
   * @param aResHelper
   *        Resource helper to be used.
   * @param bMustUnderstand
   *        Must understand?
   * @param aSigningParams
   *        Signing parameters. May not be <code>null</code>.
   * @return The created signed SOAP document
   * @throws WSSecurityException
   *         If an error occurs during signing
   */
  @Nonnull
  public static Document createSignedMessage (@Nonnull final IAS4CryptoFactory aCryptoFactorySign,
                                              @Nonnull final Document aPreSigningMessage,
                                              @Nonnull final ESoapVersion eSoapVersion,
                                              @Nonnull @Nonempty final String sMessagingID,
                                              @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                              @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                              final boolean bMustUnderstand,
                                              @Nonnull final AS4SigningParams aSigningParams) throws WSSecurityException
  {
    ValueEnforcer.notNull (aCryptoFactorySign, "CryptoFactorySign");
    ValueEnforcer.notNull (aPreSigningMessage, "PreSigningMessage");
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");
    ValueEnforcer.notEmpty (sMessagingID, "MessagingID");
    ValueEnforcer.notNull (aResHelper, "ResHelper");
    ValueEnforcer.notNull (aSigningParams, "SigningParams");

    LOGGER.info ("phase4 --- sign:start");

    final Document ret;
    if (AS4Configuration.isWSS4JSynchronizedSecurity ())
    {
      // Synchronize
      ret = WSSSynchronizer.call ( () -> _createSignedMessage (aCryptoFactorySign,
                                                               aPreSigningMessage,
                                                               eSoapVersion,
                                                               sMessagingID,
                                                               aAttachments,
                                                               aResHelper,
                                                               bMustUnderstand,
                                                               aSigningParams));
    }
    else
    {
      // Ensure WSSConfig is initialized
      WSSConfigManager.getInstance ();

      ret = _createSignedMessage (aCryptoFactorySign,
                                  aPreSigningMessage,
                                  eSoapVersion,
                                  sMessagingID,
                                  aAttachments,
                                  aResHelper,
                                  bMustUnderstand,
                                  aSigningParams);
    }

    LOGGER.info ("phase4 --- sign:end");

    return ret;
  }
}
