/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4server.receive.soap;

import java.io.File;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.as4lib.attachment.WSS4JAttachment;
import com.helger.as4lib.attachment.WSS4JAttachmentCallbackHandler;
import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.crypto.AS4CryptoFactory;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.model.pmode.config.IPModeConfig;
import com.helger.as4lib.model.pmode.leg.PModeLeg;
import com.helger.as4server.receive.AS4MessageState;
import com.helger.commons.collection.ext.CommonsHashSet;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsSet;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.xml.XMLHelper;

/**
 * This class manages the WSS4J SOAP header
 *
 * @author Philip Helger
 */
public class SOAPHeaderElementProcessorWSS4J implements ISOAPHeaderElementProcessor
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SOAPHeaderElementProcessorWSS4J.class);

  @Nonnull
  public ESuccess processHeaderElement (@Nonnull final Document aSOAPDoc,
                                        @Nonnull final Element aSecurityNode,
                                        @Nonnull final ICommonsList <WSS4JAttachment> aAttachments,
                                        @Nonnull final AS4MessageState aState,
                                        @Nonnull final ErrorList aErrorList,
                                        @Nonnull final Locale aLocale)
  {
    final IPModeConfig aPModeConfig = aState.getPModeConfig ();
    // Safety Check
    if (aPModeConfig == null)
      throw new IllegalStateException ("No PMode contained in AS4 state - seems like Ebms3 Messaging header is missing!");

    // TODO select correct leg
    final PModeLeg aPModeLeg = aPModeConfig.getLeg1 ();
    // Does security - legpart checks if not <code>null</code>
    if (aPModeLeg.getSecurity () != null)
    {
      // Get Signature Algorithm
      Element aSignedNode = XMLHelper.getFirstChildElementOfName (aSecurityNode, CAS4.DS_NS, "Signature");
      if (aSignedNode != null)
      {
        // Go through ne security nodes to find the algorithm attribute
        aSignedNode = XMLHelper.getFirstChildElementOfName (aSignedNode, CAS4.DS_NS, "SignedInfo");
        final Element aSignatureAlgorithm = XMLHelper.getFirstChildElementOfName (aSignedNode,
                                                                                  CAS4.DS_NS,
                                                                                  "SignatureMethod");
        String sAlgorithm = aSignatureAlgorithm == null ? null : aSignatureAlgorithm.getAttribute ("Algorithm");
        final ECryptoAlgorithmSign eSignAlgo = ECryptoAlgorithmSign.getFromURIOrNull (sAlgorithm);

        if (eSignAlgo == null)
        {
          s_aLogger.info ("Error processing the Security Header, your signing algorithm '" +
                          sAlgorithm +
                          "' is incorrect. Expected one of the following '" +
                          Arrays.asList (ECryptoAlgorithmSign.values ()) +
                          "' algorithms");

          aErrorList.add (EEbmsError.EBMS_FAILED_AUTHENTICATION.getAsError (aLocale));

          return ESuccess.FAILURE;
        }

        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Using signature algorithm " + eSignAlgo);

        // Get Signature Digest Algorithm
        aSignedNode = XMLHelper.getFirstChildElementOfName (aSignedNode, CAS4.DS_NS, "Reference");
        aSignedNode = XMLHelper.getFirstChildElementOfName (aSignedNode, CAS4.DS_NS, "DigestMethod");
        sAlgorithm = aSignedNode == null ? null : aSignedNode.getAttribute ("Algorithm");
        final ECryptoAlgorithmSignDigest eSignDigestAlgo = ECryptoAlgorithmSignDigest.getFromURIOrNull (sAlgorithm);

        if (eSignDigestAlgo == null)
        {
          s_aLogger.info ("Error processing the Security Header, your signing digest algorithm is incorrect. Expected one of the following'" +
                          Arrays.toString (ECryptoAlgorithmSignDigest.values ()) +
                          "' algorithms");

          aErrorList.add (EEbmsError.EBMS_FAILED_AUTHENTICATION.getAsError (aLocale));

          return ESuccess.FAILURE;
        }
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Using signature digest algorithm " + eSignDigestAlgo);
      }

      // Encrypted header
      final Element aEncryptedNode = XMLHelper.getFirstChildElementOfName (aSecurityNode, CAS4.XENC_NS, "EncryptedKey");
      if (aEncryptedNode != null)
      {
        // TODO Encrypted checks if needed beyond pmode checks, currently
        // nothing of that sort in sight

      }

      final Ebms3UserMessage aUserMessage = aState.getMessaging ().getUserMessage ().get (0);
      final boolean bBodyPayloadPresent = aState.isSoapBodyPayloadPresent ();

      // Check if Attachment IDs are the same
      for (int i = 0; i < aAttachments.size (); i++)
      {
        String sAttachmentId = aAttachments.get (i).getHeaders ().get (AttachmentUtils.MIME_HEADER_CONTENT_ID);
        sAttachmentId = sAttachmentId.substring ("<attachment=".length (), sAttachmentId.length () - 1);

        // Add +1 because the payload has index 0
        final String sHref = aUserMessage.getPayloadInfo ()
                                         .getPartInfoAtIndex ((bBodyPayloadPresent ? 1 : 0) + i)
                                         .getHref ();
        if (!sHref.contains (sAttachmentId))
        {
          s_aLogger.info ("Error processing the Attachments, the attachment '" +
                          sHref +
                          "' is not valid with what is specified in the usermessage ('" +
                          sAttachmentId +
                          "')");

          aErrorList.add (EEbmsError.EBMS_VALUE_INCONSISTENT.getAsError (aLocale));

          return ESuccess.FAILURE;
        }
      }

      // Signing Verification and Decryption
      final WSSecurityEngine aSecurityEngine = new WSSecurityEngine ();
      List <WSSecurityEngineResult> aResults = null;

      try
      {
        // Convert to WSS4J attachments
        final KeyStoreCallbackHandler aKeyStoreCallback = new KeyStoreCallbackHandler ();
        final WSS4JAttachmentCallbackHandler aAttachmentCallbackHandler = new WSS4JAttachmentCallbackHandler (aAttachments,
                                                                                                              aState.getResourceMgr ());

        // Configure RequestData needed for the check / decrpyt process!
        final RequestData aRequestData = new RequestData ();
        aRequestData.setCallbackHandler (aKeyStoreCallback);
        if (aAttachments.isNotEmpty ())
          aRequestData.setAttachmentCallbackHandler (aAttachmentCallbackHandler);
        aRequestData.setSigVerCrypto (AS4CryptoFactory.createCrypto ());
        aRequestData.setDecCrypto (AS4CryptoFactory.createCrypto ());
        aRequestData.setWssConfig (WSSConfig.getNewInstance ());

        // Upon success, the SOAP document contains the decrypted content
        // afterwards!
        aResults = aSecurityEngine.processSecurityHeader (aSOAPDoc, aRequestData).getResults ();

        // Collect all used certificates
        final ICommonsSet <X509Certificate> aCerts = new CommonsHashSet <> ();
        aResults.forEach (x -> {
          final X509Certificate aCert = (X509Certificate) x.get (WSSecurityEngineResult.TAG_X509_CERTIFICATE);
          if (aCert != null)
            aCerts.add (aCert);
        });

        if (aCerts.size () > 1)
          s_aLogger.warn ("Found " + aCerts.size () + " different certificates in message: " + aCerts);

        // Remember in State
        aState.setUsedCertificate (aCerts.getAtIndex (0));
        aState.setDecryptedSOAPDocument (aSOAPDoc);

        // Decrypting the Attachments
        final ICommonsList <WSS4JAttachment> aResponseAttachments = aAttachmentCallbackHandler.getResponseAttachments ();
        for (final WSS4JAttachment aResponseAttachment : aResponseAttachments)
        {
          final InputStream aIS = aResponseAttachment.getSourceStream ();

          // Always copy to a temporary file, so that decrypted content can be
          // read more than once.
          // Not nice, but working :)
          final File aTempFile = aState.getResourceMgr ().createTempFile ();
          StreamHelper.copyInputStreamToOutputStreamAndCloseOS (aIS,
                                                                StreamHelper.getBuffered (FileHelper.getOutputStream (aTempFile)));
          aResponseAttachment.setSourceStreamProvider ( () -> StreamHelper.getBuffered (FileHelper.getInputStream (aTempFile)));
        }

        // Remember in State
        aState.setDecryptedAttachments (aResponseAttachments);
      }
      catch (final Exception ex)
      {
        // Decryption or Signature check failed
        s_aLogger.info ("Error processing the WSSSecurity Header", ex);

        // TODO we need a way to distinct
        // signature and decrypt WSSecurityException provides no such thing
        aErrorList.add (EEbmsError.EBMS_FAILED_AUTHENTICATION.getAsError (aLocale));

        return ESuccess.FAILURE;
      }
    }
    return ESuccess.SUCCESS;
  }
}
