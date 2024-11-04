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
package com.helger.phase4.incoming.soap;

import java.io.File;
import java.io.IOException;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.HasInputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.regex.RegExCache;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.attachment.WSS4JAttachmentCallbackHandler;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.crypto.ECryptoMode;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.crypto.IAS4DecryptParameterModifier;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.AS4IncomingMessageState;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.wss.WSSConfigManager;
import com.helger.phase4.wss.WSSSynchronizer;
import com.helger.xml.XMLHelper;

/**
 * This class manages the WSS4J SOAP header
 *
 * @author Philip Helger
 * @author bayerlma
 */
public class SoapHeaderElementProcessorWSS4J implements ISoapHeaderElementProcessor
{
  /** The QName for which this processor should be invoked */
  public static final QName QNAME_SECURITY = new QName ("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                                                        "Security");
  private static final Logger LOGGER = LoggerFactory.getLogger (SoapHeaderElementProcessorWSS4J.class);

  private final IAS4CryptoFactory m_aCryptoFactorySign;
  private final IAS4CryptoFactory m_aCryptoFactoryCrypt;
  private final Provider m_aSecurityProviderSignVerify;
  private final Supplier <? extends IPMode> m_aFallbackPModeProvider;
  private final IAS4DecryptParameterModifier m_aDecryptParameterModifier;

  public SoapHeaderElementProcessorWSS4J (@Nonnull final IAS4CryptoFactory aCryptoFactorySign,
                                          @Nonnull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                          @Nullable final Provider aSecurityProviderSignVerify,
                                          @Nonnull final Supplier <? extends IPMode> aFallbackPModeProvider,
                                          @Nullable final IAS4DecryptParameterModifier aDecryptParameterModifier)
  {
    ValueEnforcer.notNull (aCryptoFactorySign, "CryptoFactorySign");
    ValueEnforcer.notNull (aCryptoFactoryCrypt, "CryptoFactoryCrypt");
    ValueEnforcer.notNull (aFallbackPModeProvider, "FallbackPModeProvider");
    m_aCryptoFactorySign = aCryptoFactorySign;
    m_aCryptoFactoryCrypt = aCryptoFactoryCrypt;
    m_aSecurityProviderSignVerify = aSecurityProviderSignVerify;
    m_aFallbackPModeProvider = aFallbackPModeProvider;
    m_aDecryptParameterModifier = aDecryptParameterModifier;
  }

  @Nonnull
  private ESuccess _verifyAndDecrypt (@Nonnull final Document aSOAPDoc,
                                      @Nonnull final ICommonsList <WSS4JAttachment> aAttachments,
                                      @Nonnull final AS4IncomingMessageState aIncomingState,
                                      @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessagesTarget,
                                      @Nonnull final Supplier <? extends WSSConfig> aWSSConfigSupplier)
  {
    // Default is Leg 1, gets overwritten when a reference to a message id
    // exists and then uses leg2
    final Locale aLocale = aIncomingState.getLocale ();

    // Signing verification and Decryption
    try
    {
      // Convert to WSS4J attachments
      final AS4KeyStoreCallbackHandler aKeyStoreCallback = new AS4KeyStoreCallbackHandler (m_aCryptoFactoryCrypt);
      final WSS4JAttachmentCallbackHandler aAttachmentCallbackHandler = new WSS4JAttachmentCallbackHandler (aAttachments,
                                                                                                            aIncomingState.getResourceHelper ());

      // Resolve the WSS config here to ensure the context matches (either from
      // an instance of globally)
      final WSSConfig aWSSConfig = aWSSConfigSupplier.get ();

      if (m_aDecryptParameterModifier != null)
      {
        // Make any custom modifications necessary
        if (LOGGER.isTraceEnabled ())
          LOGGER.trace ("Before modifyWSSConfig");
        m_aDecryptParameterModifier.modifyWSSConfig (aWSSConfig);
        if (LOGGER.isTraceEnabled ())
          LOGGER.trace ("After modifyWSSConfig");
      }

      LOGGER.info ("phase4 --- verify-decrypt:start");

      // Configure RequestData needed for the check / decrypt process!
      final RequestData aRequestData = new RequestData ();
      aRequestData.setCallbackHandler (aKeyStoreCallback);
      if (aAttachments.isNotEmpty ())
        aRequestData.setAttachmentCallbackHandler (aAttachmentCallbackHandler);
      aRequestData.setSigVerCrypto (m_aCryptoFactorySign.getCrypto (ECryptoMode.DECRYPT_VERIFY));
      aRequestData.setDecCrypto (m_aCryptoFactoryCrypt.getCrypto (ECryptoMode.DECRYPT_VERIFY));
      aRequestData.setWssConfig (aWSSConfig);
      aRequestData.setSignatureProvider (m_aSecurityProviderSignVerify);

      // Enable CRL checking
      if (false)
        aRequestData.setEnableRevocation (true);

      // TODO workaround to avoid the warning (if CRL checking is enabled)
      // No Subject DN Certificate Constraints were defined. This could be a
      // security issue
      if (false)
        aRequestData.setSubjectCertConstraints (new CommonsArrayList <> (RegExCache.getPattern (".*")));

      if (m_aDecryptParameterModifier != null)
      {
        // Make any custom modifications necessary
        if (LOGGER.isTraceEnabled ())
          LOGGER.trace ("Before modifyRequestData");
        m_aDecryptParameterModifier.modifyRequestData (aRequestData);
        if (LOGGER.isTraceEnabled ())
          LOGGER.trace ("After modifyRequestData");
      }

      // Upon success, the SOAP document contains the decrypted content
      // afterwards!
      final WSSecurityEngine aSecurityEngine = new WSSecurityEngine ();
      aSecurityEngine.setWssConfig (aWSSConfig);

      // This starts the main verification - throws an exception
      final WSHandlerResult aHdlRes = aSecurityEngine.processSecurityHeader (aSOAPDoc, aRequestData);
      final List <WSSecurityEngineResult> aResults = aHdlRes.getResults ();

      LOGGER.info ("phase4 --- verify-decrypt:end");

      // Collect all unique used certificates
      final ICommonsSet <X509Certificate> aCertSet = new CommonsHashSet <> ();
      // Preferred certificate from BinarySecurityToken
      X509Certificate aPreferredCert = null;
      int nWSS4JSecurityActions = 0;
      for (final WSSecurityEngineResult aResult : aResults)
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("WSSecurityEngineResult: " + aResult);

        final Integer aAction = (Integer) aResult.get (WSSecurityEngineResult.TAG_ACTION);
        final int nAction = aAction != null ? aAction.intValue () : 0;
        nWSS4JSecurityActions |= nAction;

        final X509Certificate aCert = (X509Certificate) aResult.get (WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        if (aCert != null)
        {
          aCertSet.add (aCert);
          if (nAction == WSConstants.BST && aPreferredCert == null)
            aPreferredCert = aCert;
        }
      }
      // this determines if a signature check or a decryption happened
      aIncomingState.setSoapWSS4JSecurityActions (nWSS4JSecurityActions);

      final X509Certificate aUsedCert;
      if (aCertSet.size () > 1)
      {
        if (aPreferredCert == null)
        {
          LOGGER.warn ("Found " + aCertSet.size () + " different certificates in message. Using the first one.");
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("All gathered certificates: " + aCertSet);
          aUsedCert = aCertSet.getAtIndex (0);
        }
        else
          aUsedCert = aPreferredCert;
      }
      else
      {
        if (aCertSet.size () == 1)
          aUsedCert = aCertSet.getAtIndex (0);
        else
          aUsedCert = null;
      }

      // Remember in State
      aIncomingState.setUsedCertificate (aUsedCert);
      aIncomingState.setDecryptedSoapDocument (aSOAPDoc);

      LOGGER.info ("phase4 --- attachment.storetemp:start");

      // Decrypting the Attachments
      final ICommonsList <WSS4JAttachment> aResponseAttachments = aAttachmentCallbackHandler.getAllResponseAttachments ();
      for (final WSS4JAttachment aResponseAttachment : aResponseAttachments)
      {
        // Always copy to a temporary file, so that decrypted content can be
        // read more than once. By default the stream can only be read once
        // Not nice, but working :)
        final File aTempFile = aIncomingState.getResourceHelper ().createTempFile ();
        if (StreamHelper.copyByteStream ()
                        .from (aResponseAttachment.getSourceStream ())
                        .closeFrom (true)
                        .to (FileHelper.getBufferedOutputStream (aTempFile))
                        .closeTo (true)
                        .build ()
                        .isFailure ())
        {
          LOGGER.error ("Failed to write response attachment to temporary file '" + aTempFile.getAbsolutePath () + "'");
        }
        aResponseAttachment.setSourceStreamProvider (HasInputStream.multiple ( () -> FileHelper.getBufferedInputStream (aTempFile)));
      }

      // Remember in State
      aIncomingState.setDecryptedAttachments (aResponseAttachments);
      LOGGER.info ("phase4 --- attachment.storetemp:end");

      return ESuccess.SUCCESS;
    }
    catch (final IndexOutOfBoundsException | IllegalStateException | WSSecurityException ex)
    {
      /**
       * Error processing the WSSSecurity Header
       *
       * <pre>
       * java.lang.IndexOutOfBoundsException: null
      at java.io.ByteArrayInputStream.read(ByteArrayInputStream.java:180) ~[?:1.8.0_242]
      at org.apache.wss4j.common.util.AttachmentUtils$1.initCipher(AttachmentUtils.java:501) ~[wss4j-ws-security-common-2.3.0.jar:2.3.0]
      at org.apache.wss4j.common.util.AttachmentUtils$1.read(AttachmentUtils.java:535) ~[wss4j-ws-security-common-2.3.0.jar:2.3.0]
      at com.helger.commons.io.stream.StreamHelper._copyInputStreamToOutputStream(StreamHelper.java:218) ~[ph-commons-9.4.7.jar:9.4.7]
      at com.helger.commons.io.stream.StreamHelper.copyInputStreamToOutputStream(StreamHelper.java:312) ~[ph-commons-9.4.7.jar:9.4.7]
      at com.helger.commons.io.stream.StreamHelper.copyInputStreamToOutputStreamAndCloseOS(StreamHelper.java:429) ~[ph-commons-9.4.7.jar:9.4.7]
      at com.helger.phase4.servlet.soap.SOAPHeaderElementProcessorWSS4J._verifyAndDecrypt(SOAPHeaderElementProcessorWSS4J.java:187) ~[classes/:?]
       * </pre>
       *
       * Failed to close object org.apache.wss4j.common.util.AttachmentUtils$1
       *
       * <pre>
       * java.lang.IllegalStateException: Cipher not initialized
      at javax.crypto.Cipher.checkCipherState(Cipher.java:1749) ~[?:1.8.0_242]
      at javax.crypto.Cipher.doFinal(Cipher.java:2044) ~[?:1.8.0_242]
      at javax.crypto.CipherInputStream.close(CipherInputStream.java:330) ~[?:1.8.0_242]
      at com.helger.commons.io.stream.StreamHelper.close(StreamHelper.java:163) ~[ph-commons-9.4.7.jar:9.4.7]
      at com.helger.commons.io.stream.StreamHelper.copyInputStreamToOutputStream(StreamHelper.java:337) ~[ph-commons-9.4.7.jar:9.4.7]
      at com.helger.commons.io.stream.StreamHelper.copyInputStreamToOutputStreamAndCloseOS(StreamHelper.java:429) ~[ph-commons-9.4.7.jar:9.4.7]
      at com.helger.phase4.servlet.soap.SOAPHeaderElementProcessorWSS4J._verifyAndDecrypt(SOAPHeaderElementProcessorWSS4J.java:187) ~[classes/:?]
       * </pre>
       *
       * Error processing the WSSSecurity Header
       *
       * <pre>
      org.apache.wss4j.common.ext.WSSecurityException: Error during certificate path validation: No trusted certs found
      at org.apache.wss4j.common.crypto.Merlin.verifyTrust(Merlin.java:816) ~[task/:?]
      at org.apache.wss4j.common.crypto.Merlin.verifyTrust(Merlin.java:906) ~[task/:?]
      at org.apache.wss4j.dom.validate.SignatureTrustValidator.verifyTrustInCerts(SignatureTrustValidator.java:109) ~[task/:?]
      at org.apache.wss4j.dom.validate.SignatureTrustValidator.validate(SignatureTrustValidator.java:64) ~[task/:?]
      at org.apache.wss4j.dom.processor.SignatureProcessor.handleToken(SignatureProcessor.java:189) ~[task/:?]
      at org.apache.wss4j.dom.engine.WSSecurityEngine.processSecurityHeader(WSSecurityEngine.java:340) ~[task/:?]
      at org.apache.wss4j.dom.engine.WSSecurityEngine.processSecurityHeader(WSSecurityEngine.java:251) ~[task/:?]
      at com.helger.phase4.servlet.soap.SOAPHeaderElementProcessorWSS4J._verifyAndDecrypt(SOAPHeaderElementProcessorWSS4J.java:128) ~[task/:?]
       * </pre>
       *
       * If the SMP provided AP certificate does not match the configured one:
       *
       * <pre>
      org.apache.wss4j.common.ext.WSSecurityException: Cannot find key for certificate
      at org.apache.wss4j.dom.processor.EncryptedKeyProcessor.getPrivateKey(EncryptedKeyProcessor.java:269) ~[wss4j-ws-security-dom-2.3.1.jar:2.3.1]
      at org.apache.wss4j.dom.processor.EncryptedKeyProcessor.handleToken(EncryptedKeyProcessor.java:225) ~[wss4j-ws-security-dom-2.3.1.jar:2.3.1]
      at org.apache.wss4j.dom.processor.EncryptedKeyProcessor.handleToken(EncryptedKeyProcessor.java:90) ~[wss4j-ws-security-dom-2.3.1.jar:2.3.1]
      at org.apache.wss4j.dom.engine.WSSecurityEngine.processSecurityHeader(WSSecurityEngine.java:340) ~[wss4j-ws-security-dom-2.3.1.jar:2.3.1]
      at org.apache.wss4j.dom.engine.WSSecurityEngine.processSecurityHeader(WSSecurityEngine.java:251) ~[wss4j-ws-security-dom-2.3.1.jar:2.3.1]
      at com.helger.phase4.servlet.soap.SOAPHeaderElementProcessorWSS4J._verifyAndDecrypt(SOAPHeaderElementProcessorWSS4J.java:128) ~[phase4-lib-1.3.1.jar:1.3.1]
       * </pre>
       */

      // Decryption or Signature check failed
      final String sDetails = "Error processing the WSSSecurity Header";
      LOGGER.error (sDetails, ex);
      // TODO we need a way to differentiate signature and decrypt
      // WSSecurityException provides no such thing
      aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_FAILED_DECRYPTION.errorBuilder (aLocale)
                                                                           .errorDetail (sDetails, ex)
                                                                           .build ());
      aIncomingState.setSoapWSS4JException (ex);
      return ESuccess.FAILURE;
    }
    catch (final IOException ex)
    {
      // Decryption or Signature check failed
      final String sDetails = "IO error processing the WSSSecurity Header";
      LOGGER.error (sDetails, ex);
      aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_OTHER.errorBuilder (aLocale)
                                                               .errorDetail (sDetails, ex)
                                                               .build ());
      aIncomingState.setSoapWSS4JException (ex);
      return ESuccess.FAILURE;
    }
  }

  @Nonnull
  public ESuccess processHeaderElement (@Nonnull final Document aSoapDoc,
                                        @Nonnull final Element aSecurityNode,
                                        @Nonnull final ICommonsList <WSS4JAttachment> aAttachments,
                                        @Nonnull final AS4IncomingMessageState aIncomingState,
                                        @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessagesTarget)
  {
    // Remember the crypto factories used for this message
    aIncomingState.setCryptoFactorySign (m_aCryptoFactorySign);
    aIncomingState.setCryptoFactoryCrypt (m_aCryptoFactoryCrypt);

    // Make sure a PMode is selected
    IPMode aPMode = aIncomingState.getPMode ();
    if (aPMode == null)
      aPMode = m_aFallbackPModeProvider.get ();
    if (aPMode == null)
      throw new IllegalStateException ("No PMode contained in AS4 state - seems like Ebms3 Messaging header is missing!");

    // Default is Leg 1, gets overwritten when a reference to a message id
    // exists and then uses leg2
    final Locale aLocale = aIncomingState.getLocale ();

    PModeLeg aPModeLeg = aPMode.getLeg1 ();
    final Ebms3UserMessage aUserMessage = aIncomingState.getEbmsUserMessage ();
    if (aUserMessage != null && StringHelper.hasText (aUserMessage.getMessageInfo ().getRefToMessageId ()))
      aPModeLeg = aPMode.getLeg2 ();

    // Does security - leg part checks if not <code>null</code>
    if (aPModeLeg.getSecurity () != null)
    {
      // Get Signature Algorithm
      Element aSignedNode = XMLHelper.getFirstChildElementOfName (aSecurityNode, CAS4.DS_NS, "Signature");
      if (aSignedNode != null)
      {
        // Go through the security nodes to find the algorithm attribute
        aSignedNode = XMLHelper.getFirstChildElementOfName (aSignedNode, CAS4.DS_NS, "SignedInfo");
        final Element aSignatureAlgorithm = XMLHelper.getFirstChildElementOfName (aSignedNode,
                                                                                  CAS4.DS_NS,
                                                                                  "SignatureMethod");

        // Get Signing algorithm
        String sAlgorithm = aSignatureAlgorithm == null ? null : aSignatureAlgorithm.getAttribute ("Algorithm");
        final ECryptoAlgorithmSign eSignAlgo = ECryptoAlgorithmSign.getFromURIOrNull (sAlgorithm);
        if (eSignAlgo == null)
        {
          final String sDetails = "Error processing the Security Header, your signing algorithm '" +
                                  sAlgorithm +
                                  "' is incorrect. Expected one of the following '" +
                                  Arrays.toString (ECryptoAlgorithmSign.values ()) +
                                  "' algorithms";
          LOGGER.error (sDetails);
          aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_FAILED_AUTHENTICATION.errorBuilder (aLocale)
                                                                                   .errorDetail (sDetails)
                                                                                   .build ());
          return ESuccess.FAILURE;
        }
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Using signature algorithm " + eSignAlgo);

        // Get Signature Digest Algorithm
        aSignedNode = XMLHelper.getFirstChildElementOfName (aSignedNode, CAS4.DS_NS, "Reference");
        aSignedNode = XMLHelper.getFirstChildElementOfName (aSignedNode, CAS4.DS_NS, "DigestMethod");
        sAlgorithm = aSignedNode == null ? null : aSignedNode.getAttribute ("Algorithm");
        final ECryptoAlgorithmSignDigest eSignDigestAlgo = ECryptoAlgorithmSignDigest.getFromURIOrNull (sAlgorithm);
        if (eSignDigestAlgo == null)
        {
          final String sDetails = "Error processing the Security Header - the signing digest algorithm is incorrect." +
                                  " Expected one of the following algorithms: '" +
                                  Arrays.toString (ECryptoAlgorithmSignDigest.values ()) +
                                  "'";
          LOGGER.error (sDetails);
          aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_FAILED_AUTHENTICATION.errorBuilder (aLocale)
                                                                                   .errorDetail (sDetails)
                                                                                   .build ());
          return ESuccess.FAILURE;
        }

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Using signature digest algorithm " + eSignDigestAlgo);
      }

      // Check attachment validity only if a PartInfo element is available
      if (aUserMessage != null)
      {
        final boolean bBodyPayloadPresent = aIncomingState.isSoapBodyPayloadPresent ();

        // Check if Attachment IDs are the same
        for (int i = 0; i < aAttachments.size (); i++)
        {
          // Get "Content-ID" header
          String sAttachmentID = aAttachments.get (i).getHeaders ().get (AttachmentUtils.MIME_HEADER_CONTENT_ID);
          if (StringHelper.hasNoText (sAttachmentID))
          {
            final String sDetails = "The provided attachment ID in the 'Content-ID' header may not be empty.";
            LOGGER.error (sDetails);
            aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                                  .errorDetail (sDetails)
                                                                                  .build ());
            return ESuccess.FAILURE;
          }

          // Starts with "<attachment="?
          if (!sAttachmentID.startsWith (WSS4JAttachment.CONTENT_ID_PREFIX))
          {
            final String sDetails = "The provided attachment ID '" +
                                    sAttachmentID +
                                    "' in the 'Content-ID' header does not start with the required prefix '" +
                                    WSS4JAttachment.CONTENT_ID_PREFIX +
                                    "'";
            LOGGER.error (sDetails);
            aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                                  .errorDetail (sDetails)
                                                                                  .build ());
            return ESuccess.FAILURE;
          }

          // Ends with ">"?
          if (!sAttachmentID.endsWith (WSS4JAttachment.CONTENT_ID_SUFFIX))
          {
            final String sDetails = "The provided attachment ID '" +
                                    sAttachmentID +
                                    "' in the 'Content-ID' header does not end with the required suffix '" +
                                    WSS4JAttachment.CONTENT_ID_SUFFIX +
                                    "'";
            LOGGER.error (sDetails);
            aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                                  .errorDetail (sDetails)
                                                                                  .build ());
            return ESuccess.FAILURE;
          }

          // Strip prefix and suffix
          sAttachmentID = sAttachmentID.substring (WSS4JAttachment.CONTENT_ID_PREFIX.length (),
                                                   sAttachmentID.length () -
                                                                                                WSS4JAttachment.CONTENT_ID_SUFFIX.length ());

          // Add +1 because the payload has index 0
          final String sHref = aUserMessage.getPayloadInfo ()
                                           .getPartInfoAtIndex ((bBodyPayloadPresent ? 1 : 0) + i)
                                           .getHref ();
          if (!sHref.contains (sAttachmentID))
          {
            final String sDetails = "The usermessage part information '" +
                                    sHref +
                                    "' does not reference the respective attachment ID '" +
                                    sAttachmentID +
                                    "'";
            LOGGER.error (sDetails);
            aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                                  .errorDetail (sDetails)
                                                                                  .build ());
            return ESuccess.FAILURE;
          }
        }
      }

      final ESuccess eSuccess;
      if (AS4Configuration.isWSS4JSynchronizedSecurity ())
      {
        // Use static WSSConfig creation
        eSuccess = WSSSynchronizer.call ( () -> _verifyAndDecrypt (aSoapDoc,
                                                                   aAttachments,
                                                                   aIncomingState,
                                                                   aProcessingErrorMessagesTarget,
                                                                   WSSConfigManager::createStaticWSSConfig));
      }
      else
      {
        // Use instance WSSConfig creation
        eSuccess = _verifyAndDecrypt (aSoapDoc,
                                      aAttachments,
                                      aIncomingState,
                                      aProcessingErrorMessagesTarget,
                                      WSSConfigManager.getInstance ()::createWSSConfig);
      }
      if (eSuccess.isFailure ())
        return ESuccess.FAILURE;
    }
    else
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("PMode leg has no security defined - skipping Verification and Decryption step");
    }

    return ESuccess.SUCCESS;
  }
}
