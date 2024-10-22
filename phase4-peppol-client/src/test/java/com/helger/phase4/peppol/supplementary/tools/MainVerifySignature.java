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
package com.helger.phase4.peppol.supplementary.tools;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

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
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.mutable.MutableInt;
import com.helger.commons.state.ESuccess;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.attachment.WSS4JAttachmentCallbackHandler;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.ECryptoMode;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpReader;
import com.helger.phase4.incoming.soap.AS4KeyStoreCallbackHandler;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.wss.WSSConfigManager;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * This is a small tool that takes the dump of an incoming AS4 message (from a
 * File) and tries to verify if the signature is correct or not. Therefore
 * relevant parts of the phase4 signature verification process have been
 * extracted.<br>
 * This tool can currently only check receipts that are SOAP messages only. MIME
 * messages are currently not supported.
 *
 * @author Philip Helger
 */
public class MainVerifySignature
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainVerifySignature.class);

  @Nonnull
  private static ESuccess _verifyAndDecrypt (@Nonnull final AS4ResourceHelper aResHelper,
                                             @Nonnull final IAS4CryptoFactory aCryptoFactory,
                                             @Nonnull final Document aSOAPDoc,
                                             @Nonnull final ICommonsList <WSS4JAttachment> aAttachments,
                                             @Nonnull final Supplier <WSSConfig> aWSSConfigSupplier)
  {
    // Signing verification and Decryption
    try
    {
      // Convert to WSS4J attachments
      final AS4KeyStoreCallbackHandler aKeyStoreCallback = new AS4KeyStoreCallbackHandler (aCryptoFactory);
      final WSS4JAttachmentCallbackHandler aAttachmentCallbackHandler = new WSS4JAttachmentCallbackHandler (aAttachments,
                                                                                                            aResHelper);

      // Resolve the WSS config here to ensure the context matches
      final WSSConfig aWSSConfig = aWSSConfigSupplier.get ();

      // Configure RequestData needed for the check / decrypt process!
      final RequestData aRequestData = new RequestData ();
      aRequestData.setCallbackHandler (aKeyStoreCallback);
      if (aAttachments.isNotEmpty ())
        aRequestData.setAttachmentCallbackHandler (aAttachmentCallbackHandler);
      aRequestData.setSigVerCrypto (aCryptoFactory.getCrypto (ECryptoMode.DECRYPT_VERIFY));
      aRequestData.setDecCrypto (aCryptoFactory.getCrypto (ECryptoMode.DECRYPT_VERIFY));
      aRequestData.setWssConfig (aWSSConfig);

      // Upon success, the SOAP document contains the decrypted content
      // afterwards!
      final WSSecurityEngine aSecurityEngine = new WSSecurityEngine ();
      aSecurityEngine.setWssConfig (aWSSConfig);

      // This starts the main verification - throws an exception
      final WSHandlerResult aHdlRes = aSecurityEngine.processSecurityHeader (aSOAPDoc, aRequestData);
      final List <WSSecurityEngineResult> aResults = aHdlRes.getResults ();

      // Collect all unique used certificates
      final ICommonsSet <X509Certificate> aCertSet = new CommonsHashSet <> ();
      // Preferred certificate from BinarySecurityToken
      X509Certificate aPreferredCert = null;
      for (final WSSecurityEngineResult aResult : aResults)
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("WSSecurityEngineResult: " + aResult);

        final Integer aAction = (Integer) aResult.get (WSSecurityEngineResult.TAG_ACTION);
        final int nAction = aAction != null ? aAction.intValue () : 0;

        final X509Certificate aCert = (X509Certificate) aResult.get (WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        if (aCert != null)
        {
          aCertSet.add (aCert);
          if (nAction == WSConstants.BST)
          {
            if (aPreferredCert == null)
              aPreferredCert = aCert;
            else
              LOGGER.error ("Found a second BST certificate in the message. Weird.");
          }
        }
      }

      if (aPreferredCert != null)
        LOGGER.info ("Found this certificate Subject CN: " + aPreferredCert.getSubjectX500Principal ());

      LOGGER.info ("The certificate verification looks good");

      return ESuccess.SUCCESS;
    }
    catch (final Exception ex)
    {
      // Decryption or Signature check failed
      LOGGER.error ("Error processing the WSSSecurity Header", ex);
      return ESuccess.FAILURE;
    }
  }

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      // The file to read
      final File aFile = new File ("src/test/resources/external/verify/dataport1-mustunderstand.as4in");
      if (!aFile.exists ())
        throw new IllegalStateException ("The file " + aFile.getAbsolutePath () + " does not exist");

      final byte [] aBytes = SimpleFileIO.getAllFileBytes (aFile);
      if (aBytes == null)
        throw new IllegalStateException ("Failed to read file content as byte array");

      // Skip all the HTTP headers etc.
      final MutableInt aHttpEnd = new MutableInt (-1);
      AS4DumpReader.readAndSkipInitialHttpHeaders (aBytes, null, aHttpEnd::set);
      final int nHttpEnd = aHttpEnd.intValue ();

      // Remember the index until we skipped
      LOGGER.info ("Now at byte " + nHttpEnd);

      // Expects the main payload to be a SOAP message
      final Document aSOAPDoc = DOMReader.readXMLDOM (aBytes, nHttpEnd, aBytes.length - nHttpEnd);
      if (aSOAPDoc == null)
        throw new IllegalStateException ("Failed to read the payload as XML. Maybe it is a MIME message? MIME messages are unfortunately not yet supported.");

      // Main action
      final IAS4CryptoFactory aCryptoFactory = AS4CryptoFactoryConfiguration.getDefaultInstance ();
      final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
      _verifyAndDecrypt (aResHelper,
                         aCryptoFactory,
                         aSOAPDoc,
                         aAttachments,
                         WSSConfigManager.getInstance ()::createWSSConfig);
    }
    WebScopeManager.onGlobalEnd ();
  }
}
