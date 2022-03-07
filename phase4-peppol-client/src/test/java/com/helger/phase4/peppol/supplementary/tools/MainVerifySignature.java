package com.helger.phase4.peppol.supplementary.tools;

import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.HasInputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.attachment.WSS4JAttachmentCallbackHandler;
import com.helger.phase4.crypto.AS4CryptoFactoryProperties;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.error.EEbmsError;
import com.helger.phase4.servlet.soap.Phase4KeyStoreCallbackHandler;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.wss.WSSConfigManager;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

@SuppressWarnings ("unused")
public class MainVerifySignature
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainVerifySignature.class);

  @Nonnull
  private static ESuccess _verifyAndDecrypt (@Nonnull final IAS4CryptoFactory aCryptoFactory,
                                             @Nonnull final Document aSOAPDoc,
                                             @Nonnull final Locale aLocale,
                                             @Nonnull final AS4ResourceHelper aResHelper,
                                             @Nonnull final ICommonsList <WSS4JAttachment> aAttachments,
                                             @Nonnull final ErrorList aErrorList,
                                             @Nonnull final Supplier <WSSConfig> aWSSConfigSupplier)
  {
    // Signing verification and Decryption
    try
    {
      // Convert to WSS4J attachments
      final Phase4KeyStoreCallbackHandler aKeyStoreCallback = new Phase4KeyStoreCallbackHandler (aCryptoFactory);
      final WSS4JAttachmentCallbackHandler aAttachmentCallbackHandler = new WSS4JAttachmentCallbackHandler (aAttachments, aResHelper);

      // Resolve the WSS config here to ensure the context matches
      final WSSConfig aWSSConfig = aWSSConfigSupplier.get ();

      // Configure RequestData needed for the check / decrypt process!
      final RequestData aRequestData = new RequestData ();
      aRequestData.setCallbackHandler (aKeyStoreCallback);
      if (aAttachments.isNotEmpty ())
        aRequestData.setAttachmentCallbackHandler (aAttachmentCallbackHandler);
      aRequestData.setSigVerCrypto (aCryptoFactory.getCrypto ());
      aRequestData.setDecCrypto (aCryptoFactory.getCrypto ());
      aRequestData.setWssConfig (aWSSConfig);

      // Upon success, the SOAP document contains the decrypted content
      // afterwards!
      final WSSecurityEngine aSecurityEngine = new WSSecurityEngine ();
      aSecurityEngine.setWssConfig (aWSSConfig);
      final WSHandlerResult aHdlRes = aSecurityEngine.processSecurityHeader (aSOAPDoc, aRequestData);
      final List <WSSecurityEngineResult> aResults = aHdlRes.getResults ();

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
        if (aCertSet.size () == 1)
          aUsedCert = aCertSet.getAtIndex (0);
        else
          aUsedCert = null;

      // Remember in State

      // Decrypting the Attachments
      final ICommonsList <WSS4JAttachment> aResponseAttachments = aAttachmentCallbackHandler.getAllResponseAttachments ();
      for (final WSS4JAttachment aResponseAttachment : aResponseAttachments)
      {
        // Always copy to a temporary file, so that decrypted content can be
        // read more than once. By default the stream can only be read once
        // Not nice, but working :)
        final File aTempFile = aResHelper.createTempFile ();
        StreamHelper.copyInputStreamToOutputStreamAndCloseOS (aResponseAttachment.getSourceStream (),
                                                              FileHelper.getBufferedOutputStream (aTempFile));
        aResponseAttachment.setSourceStreamProvider (HasInputStream.multiple ( () -> FileHelper.getBufferedInputStream (aTempFile)));
      }

      // Remember in State
      return ESuccess.SUCCESS;
    }
    catch (final IndexOutOfBoundsException | IllegalStateException | WSSecurityException ex)
    {
      // Decryption or Signature check failed
      LOGGER.error ("Error processing the WSSSecurity Header", ex);

      // TODO we need a way to distinct
      // signature and decrypt WSSecurityException provides no such thing
      aErrorList.add (EEbmsError.EBMS_FAILED_DECRYPTION.getAsError (aLocale));
      return ESuccess.FAILURE;
    }
    catch (final IOException ex)
    {
      // Decryption or Signature check failed

      LOGGER.error ("IO error processing the WSSSecurity Header", ex);
      aErrorList.add (EEbmsError.EBMS_OTHER.getAsError (aLocale));
      return ESuccess.FAILURE;
    }
  }

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());
    try (AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final byte [] aBytes = StreamHelper.getAllBytes (FileHelper.getInputStream (new File ("src/test/resources/verify/dataport1-mustunderstand.as4in")));

      int nHttpEnd = -1;
      boolean bLastWasCR = false;
      for (int i = 0; i < aBytes.length; ++i)
      {
        final byte b = aBytes[i];
        if (b == '\n')
        {
          if (bLastWasCR)
          {
            nHttpEnd = i + 1;
            break;
          }
          bLastWasCR = true;
        }
        else
        {
          if (b != '\r')
            bLastWasCR = false;
        }
      }

      LOGGER.info ("Now at byte " + nHttpEnd);

      final Document aSOAPDoc = DOMReader.readXMLDOM (aBytes, nHttpEnd, aBytes.length - nHttpEnd);
      if (aSOAPDoc == null)
        throw new IllegalStateException ();

      final ErrorList aErrorList = new ErrorList ();
      _verifyAndDecrypt (AS4CryptoFactoryProperties.getDefaultInstance (),
                         aSOAPDoc,
                         Locale.US,
                         aResHelper,
                         new CommonsArrayList <> (),
                         aErrorList,
                         WSSConfigManager.getInstance ()::createWSSConfig);
    }
    WebScopeManager.onGlobalEnd ();
  }
}
