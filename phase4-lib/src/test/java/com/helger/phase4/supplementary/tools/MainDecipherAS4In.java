/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.phase4.supplementary.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.IHasInputStream;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.AS4CryptoFactoryProperties;
import com.helger.phase4.crypto.AS4CryptoProperties;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.EAS4MessageMode;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.resolve.DefaultPModeResolver;
import com.helger.phase4.servlet.AS4IncomingMessageMetadata;
import com.helger.phase4.servlet.AS4RequestHandler;
import com.helger.phase4.servlet.IAS4MessageState;
import com.helger.phase4.servlet.IAS4ResponseAbstraction;
import com.helger.phase4.servlet.spi.AS4MessageProcessorResult;
import com.helger.phase4.servlet.spi.AS4SignalMessageProcessorResult;
import com.helger.phase4.servlet.spi.IAS4ServletMessageProcessorSPI;
import com.helger.phase4.util.Phase4Exception;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xml.XMLHelper;

/**
 * This is a small tool that demonstrates how the "as4in" files can be decrypted
 * later, assuming the correct certificate is provided.
 *
 * @author Philip Helger
 */
public final class MainDecipherAS4In
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainDecipherAS4In.class);

  @Nullable
  public static Element getChildElement (@Nonnull final Element aStartElement, @Nonnull final String... aTagNames)
  {
    ValueEnforcer.notEmpty (aTagNames, "TagNames");
    Element aCurElement = aStartElement;
    for (final String sTagName : aTagNames)
    {
      aCurElement = XMLHelper.getFirstChildElementOfName (aCurElement, sTagName);
      if (aCurElement == null)
        return null;
    }
    return aCurElement;
  }

  public static void decrypt (@Nonnull final byte [] aAS4InData,
                              final IAS4CryptoFactory aCF,
                              @Nonnull final Consumer <byte []> aDecryptedConsumer) throws WSSecurityException,
                                                                                    Phase4Exception,
                                                                                    IOException,
                                                                                    MessagingException
  {
    final HttpHeaderMap hm = new HttpHeaderMap ();
    int nHttpStart = 0;
    int nHttpEnd = -1;
    boolean bLastWasCR = false;
    for (int i = 0; i < aAS4InData.length; ++i)
    {
      final byte b = aAS4InData[i];
      if (b == '\n')
      {
        if (bLastWasCR)
        {
          nHttpEnd = i;
          break;
        }
        bLastWasCR = true;
        final String sLine = new String (aAS4InData, nHttpStart, i - nHttpStart, StandardCharsets.ISO_8859_1);
        final String [] aParts = StringHelper.getExplodedArray (':', sLine, 2);
        hm.addHeader (aParts[0].trim (), aParts[1].trim ());
        nHttpStart = i + 1;
      }
      else
      {
        if (b != '\r')
          bLastWasCR = false;
      }
    }

    LOGGER.info ("Now at byte " + nHttpEnd + " having " + hm.getCount () + " HTTP headers");

    WebScopeManager.onGlobalBegin (MockServletContext.create ());
    try (final WebScoped w = new WebScoped ();
        final AS4RequestHandler rh = new AS4RequestHandler (aCF,
                                                            DefaultPModeResolver.DEFAULT_PMODE_RESOLVER,
                                                            IAS4IncomingAttachmentFactory.DEFAULT_INSTANCE,
                                                            new AS4IncomingMessageMetadata (EAS4MessageMode.REQUEST)))
    {
      final IAS4ServletMessageProcessorSPI aSPI = new IAS4ServletMessageProcessorSPI ()
      {
        public AS4MessageProcessorResult processAS4UserMessage (final IAS4IncomingMessageMetadata aMessageMetadata,
                                                                final HttpHeaderMap aHttpHeaders,
                                                                final Ebms3UserMessage aUserMessage,
                                                                final IPMode aPMode,
                                                                final Node aPayload,
                                                                final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                                final IAS4MessageState aState,
                                                                final ICommonsList <Ebms3Error> aProcessingErrorMessages)
        {
          try
          {
            final byte [] aDecryptedBytes = StreamHelper.getAllBytes (aIncomingAttachments.getFirst ()
                                                                                          .getInputStreamProvider ());
            aDecryptedConsumer.accept (aDecryptedBytes);
            LOGGER.info ("Wrote decrypted payload with " + aDecryptedBytes.length + " bytes");
            return AS4MessageProcessorResult.createSuccess ();
          }
          catch (final Exception ex)
          {
            throw new IllegalStateException (ex);
          }
        }

        public AS4SignalMessageProcessorResult processAS4SignalMessage (final IAS4IncomingMessageMetadata aMessageMetadata,
                                                                        final HttpHeaderMap aHttpHeaders,
                                                                        final Ebms3SignalMessage aSignalMessage,
                                                                        final IPMode aPMode,
                                                                        final IAS4MessageState aState,
                                                                        final ICommonsList <Ebms3Error> aProcessingErrorMessages)
        {
          LOGGER.error ("Unexpected signal msg");
          return AS4SignalMessageProcessorResult.createSuccess ();
        }
      };
      rh.setProcessorSupplier ( () -> new CommonsArrayList <> (aSPI));
      rh.handleRequest (new NonBlockingByteArrayInputStream (aAS4InData, nHttpEnd, aAS4InData.length - nHttpEnd),
                        hm,
                        new IAS4ResponseAbstraction ()
                        {
                          public void setStatus (final int nStatusCode)
                          {}

                          public void setMimeType (final IMimeType aMimeType)
                          {}

                          public void setContent (final HttpHeaderMap aHeaderMap, final IHasInputStream aHasIS)
                          {}

                          public void setContent (final byte [] aResultBytes, final Charset aCharset)
                          {}
                        });
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }

  public static void main (final String [] args) throws Exception
  {
    final File folder = new File ("src/test/resources/incoming/2021/01/01");
    if (!folder.isDirectory ())
      throw new IllegalStateException ();
    final File f = new File (folder, "20210101_000001.as4in");
    if (!f.exists ())
      throw new IllegalStateException ();

    final AS4CryptoProperties aCP = new AS4CryptoProperties (new FileSystemResource (folder, "crypto.properties"));
    aCP.setKeyStorePath (folder.getAbsolutePath () + "/" + aCP.getKeyStorePath ());
    aCP.setTrustStorePath (folder.getAbsolutePath () + "/" + aCP.getTrustStorePath ());

    LOGGER.info ("Reading " + f.getName ());
    final byte [] aBytes = SimpleFileIO.getAllFileBytes (f);

    decrypt (aBytes,
             new AS4CryptoFactoryProperties (aCP),
             aDecryptedBytes -> SimpleFileIO.writeFile (new File (folder, "payload.decrypted"), aDecryptedBytes));
  }
}
