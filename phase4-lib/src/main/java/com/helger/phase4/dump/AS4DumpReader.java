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
package com.helger.phase4.dump;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.IHasInputStream;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mutable.MutableInt;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.AS4IncomingMessageMetadata;
import com.helger.phase4.incoming.AS4IncomingProfileSelectorConstant;
import com.helger.phase4.incoming.AS4IncomingReceiverConfiguration;
import com.helger.phase4.incoming.AS4RequestHandler;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.incoming.IAS4ResponseAbstraction;
import com.helger.phase4.incoming.crypto.AS4IncomingSecurityConfiguration;
import com.helger.phase4.incoming.spi.AS4MessageProcessorResult;
import com.helger.phase4.incoming.spi.AS4SignalMessageProcessorResult;
import com.helger.phase4.incoming.spi.IAS4IncomingMessageProcessorSPI;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.resolve.AS4DefaultPModeResolver;
import com.helger.phase4.util.Phase4Exception;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.web.scope.mgr.WebScoped;

import jakarta.mail.MessagingException;

/**
 * Utility method to read dump files later.
 *
 * @author Philip Helger
 * @since 1.3.1
 */
public final class AS4DumpReader
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4DumpReader.class);

  @FunctionalInterface
  public interface IDecryptedPayloadConsumer
  {
    /**
     * Get invoked for every decrypted attachment.
     *
     * @param nAttachmentIndex
     *        0-based attachment index.
     * @param aPayload
     *        Decrypted payload. Never <code>null</code>.
     */
    void accept (@Nonnegative int nAttachmentIndex, @Nonnull byte [] aPayload);
  }

  private AS4DumpReader ()
  {}

  /**
   * Utility method to just read and consume the leading HTTP headers from a
   * dump. Usually this method is not called explicitly but invoked directly by
   * {@link #decryptAS4In(String, byte[], IAS4CryptoFactory, IAS4CryptoFactory, Consumer, IDecryptedPayloadConsumer)}
   *
   * @param aAS4InData
   *        The byte array with the dump. May not be <code>null</code>.
   * @param aHttpHeaderConsumer
   *        The optional consumer for the read HTTP headers. May be
   *        <code>null</code>.
   * @param aHttpEndIndexConsumer
   *        An optional consumer for the number of header data bytes read, so
   *        that the start index of the payload can easily be determined. May be
   *        <code>null</code>.
   * @since 2.1.0
   */
  public static void readAndSkipInitialHttpHeaders (@Nonnull final byte [] aAS4InData,
                                                    @Nullable final Consumer <HttpHeaderMap> aHttpHeaderConsumer,
                                                    @Nullable final IntConsumer aHttpEndIndexConsumer)
  {
    ValueEnforcer.notNull (aAS4InData, "AS4InData");

    final HttpHeaderMap hm = new HttpHeaderMap ();
    int nHttpStart = 0;
    int nHttpEnd = -1;

    // Read all the HTTP headers
    boolean bLastWasCR = false;
    for (int i = 0; i < aAS4InData.length; ++i)
    {
      final byte b = aAS4InData[i];
      if (b == '\n')
      {
        // Do we have 2 consecutive newlines?
        if (bLastWasCR)
        {
          // Remember index in byte array
          nHttpEnd = i;
          break;
        }
        bLastWasCR = true;

        // The full header line
        final String sLine = new String (aAS4InData, nHttpStart, i - nHttpStart, StandardCharsets.ISO_8859_1);

        // Split in name and value
        final String [] aParts = StringHelper.getExplodedArray (':', sLine, 2);

        // Remember
        hm.addHeader (aParts[0].trim (), aParts[1].trim ());

        // Remember start of the next line
        nHttpStart = i + 1;
      }
      else
      {
        // No newline
        if (b != '\r')
          bLastWasCR = false;
      }
    }

    // Invoke consumers
    if (aHttpHeaderConsumer != null)
      aHttpHeaderConsumer.accept (hm);

    if (aHttpEndIndexConsumer != null)
      aHttpEndIndexConsumer.accept (nHttpEnd);
  }

  /**
   * Utility method to decrypt dumped .as4in message late.<br>
   * Note: this method was mainly created for internal use and does not win the
   * prize for the most sexy piece of software in the world ;-)
   *
   * @param sAS4ProfileID
   *        The AS4 profile ID to use. May neither be <code>null</code> nor
   *        empty.
   * @param aAS4InData
   *        The byte array with the dumped data.
   * @param aCryptoFactorySign
   *        The Crypto factory to be used. May not be <code>null</code>.
   * @param aCryptoFactoryCrypt
   *        The Crypto factory to be used for decrypting. This crypto factory
   *        must use the private key that can be used to decrypt this particular
   *        message. May not be <code>null</code>.
   * @param aHttpHeaderConsumer
   *        An optional HTTP Header map consumer. May be <code>null</code>.
   * @param aDecryptedConsumer
   *        The consumer for the decrypted payload - whatever that is :). May
   *        not be <code>null</code>.
   * @throws WSSecurityException
   *         In case of error
   * @throws Phase4Exception
   *         In case of error
   * @throws IOException
   *         In case of error
   * @throws MessagingException
   *         In case of error
   */
  public static void decryptAS4In (@Nonnull @Nonempty final String sAS4ProfileID,
                                   @Nonnull final byte [] aAS4InData,
                                   @Nonnull final IAS4CryptoFactory aCryptoFactorySign,
                                   @Nonnull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                   @Nullable final Consumer <HttpHeaderMap> aHttpHeaderConsumer,
                                   @Nonnull final IDecryptedPayloadConsumer aDecryptedConsumer) throws WSSecurityException,
                                                                                                Phase4Exception,
                                                                                                IOException,
                                                                                                MessagingException
  {
    ValueEnforcer.notEmpty (sAS4ProfileID, "AS4ProfileID");
    ValueEnforcer.notNull (aAS4InData, "AS4InData");
    ValueEnforcer.notNull (aCryptoFactorySign, "CryptoFactorySign");
    ValueEnforcer.notNull (aCryptoFactoryCrypt, "CryptoFactoryCrypt");
    ValueEnforcer.notNull (aDecryptedConsumer, "DecryptedConsumer");

    final HttpHeaderMap hm = new HttpHeaderMap ();
    final MutableInt aHttpEndIndex = new MutableInt (-1);
    readAndSkipInitialHttpHeaders (aAS4InData, hm::setAllHeaders, aHttpEndIndex::set);
    final int nHttpEnd = aHttpEndIndex.intValue ();

    // In case somebody cares about the HTTP headers
    if (aHttpHeaderConsumer != null)
      aHttpHeaderConsumer.accept (hm);

    LOGGER.info ("Now at byte " + nHttpEnd + " having " + hm.getCount () + " HTTP headers");

    final boolean bGlobalScopePresent = WebScopeManager.isGlobalScopePresent ();
    if (!bGlobalScopePresent)
    {
      // Make sure one is present - for standalone use
      WebScopeManager.onGlobalBegin (MockServletContext.create ());
    }

    try (final WebScoped w = new WebScoped ();
        final AS4RequestHandler aHandler = new AS4RequestHandler (AS4IncomingMessageMetadata.createForRequest ()))
    {
      // Set default values in handler
      aHandler.setCryptoFactorySign (aCryptoFactorySign);
      aHandler.setCryptoFactoryCrypt (aCryptoFactoryCrypt);
      aHandler.setPModeResolver (new AS4DefaultPModeResolver (sAS4ProfileID));
      aHandler.setIncomingProfileSelector (new AS4IncomingProfileSelectorConstant (sAS4ProfileID));
      aHandler.setIncomingAttachmentFactory (IAS4IncomingAttachmentFactory.DEFAULT_INSTANCE);
      aHandler.setIncomingSecurityConfiguration (AS4IncomingSecurityConfiguration.createDefaultInstance ());
      aHandler.setIncomingReceiverConfiguration (new AS4IncomingReceiverConfiguration ());

      final IAS4IncomingMessageProcessorSPI aSPI = new IAS4IncomingMessageProcessorSPI ()
      {
        public AS4MessageProcessorResult processAS4UserMessage (@Nonnull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                                                @Nonnull final HttpHeaderMap aHttpHeaders,
                                                                @Nonnull final Ebms3UserMessage aUserMessage,
                                                                @Nonnull final IPMode aPMode,
                                                                @Nullable final Node aPayload,
                                                                @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                                @Nonnull final IAS4IncomingMessageState aIncomingState,
                                                                @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
        {
          try
          {
            // Once we're here, the payload is decrypted

            int nIndex = 0;
            // For all attachments
            if (aIncomingAttachments != null)
              for (final WSS4JAttachment aAttachment : aIncomingAttachments)
              {
                // Read current
                final byte [] aDecryptedBytes = StreamHelper.getAllBytes (aAttachment.getInputStreamProvider ());
                if (aDecryptedBytes == null)
                {
                  LOGGER.error ("Failed to read decrypted payload of attachment #" + nIndex);
                }
                else
                {
                  // Invoke the consumer
                  aDecryptedConsumer.accept (nIndex, aDecryptedBytes);
                  LOGGER.info ("Handled decrypted payload #" + nIndex + " with " + aDecryptedBytes.length + " bytes");
                }

                nIndex++;
              }
            return AS4MessageProcessorResult.createSuccess ();
          }
          catch (final Exception ex)
          {
            throw new IllegalStateException (ex);
          }
        }

        public AS4SignalMessageProcessorResult processAS4SignalMessage (final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                                                        final HttpHeaderMap aHttpHeaders,
                                                                        final Ebms3SignalMessage aSignalMessage,
                                                                        final IPMode aPMode,
                                                                        final IAS4IncomingMessageState aIncomingState,
                                                                        final ICommonsList <Ebms3Error> aProcessingErrorMessages)
        {
          LOGGER.error ("Unexpected signal msg. Can only handle user messages.");
          return AS4SignalMessageProcessorResult.createSuccess ();
        }

        public void processAS4ResponseMessage (@Nonnull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                               @Nonnull final IAS4IncomingMessageState aIncomingState,
                                               @Nonnull @Nonempty final String sResponseMessageID,
                                               @Nullable final byte [] aResponseBytes,
                                               final boolean bResponsePayloadIsAvailable)
        {}
      };
      aHandler.setProcessorSupplier ( () -> new CommonsArrayList <> (aSPI));
      aHandler.handleRequest (new NonBlockingByteArrayInputStream (aAS4InData, nHttpEnd, aAS4InData.length - nHttpEnd),
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
      if (!bGlobalScopePresent)
      {
        // If we created one, also end it here
        WebScopeManager.onGlobalEnd ();
      }
    }
  }
}
