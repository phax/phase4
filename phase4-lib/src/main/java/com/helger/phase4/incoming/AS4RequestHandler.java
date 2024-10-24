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
package com.helger.phase4.incoming;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.callback.IThrowingRunnable;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.IHasInputStream;
import com.helger.commons.io.stream.HasInputStream;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.state.ISuccessIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.httpclient.response.ResponseHandlerXml;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.AS4DecompressException;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.IAS4RetryCallback;
import com.helger.phase4.crypto.AS4CryptParams;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3PayloadInfo;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.AS4IncomingHandler.IAS4ParsedMessageCallback;
import com.helger.phase4.incoming.crypto.IAS4IncomingSecurityConfiguration;
import com.helger.phase4.incoming.mgr.AS4IncomingMessageProcessorManager;
import com.helger.phase4.incoming.soap.SoapHeaderElementProcessorRegistry;
import com.helger.phase4.incoming.spi.AS4MessageProcessorResult;
import com.helger.phase4.incoming.spi.AS4SignalMessageProcessorResult;
import com.helger.phase4.incoming.spi.IAS4IncomingMessageProcessorSPI;
import com.helger.phase4.messaging.EAS4MessageMode;
import com.helger.phase4.messaging.crypto.AS4Encryptor;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.messaging.http.AS4HttpDebug;
import com.helger.phase4.messaging.http.BasicHttpPoster;
import com.helger.phase4.messaging.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.http.HttpRetrySettings;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.AS4MimeMessageHelper;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.MEPHelper;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.message.AS4ErrorMessage;
import com.helger.phase4.model.message.AS4ReceiptMessage;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.EAS4MessageType;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;
import com.helger.phase4.model.pmode.resolve.IAS4PModeResolver;
import com.helger.phase4.profile.IAS4Profile;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.AS4XMLHelper;
import com.helger.phase4.util.Phase4Exception;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.xml.serialize.write.XMLWriter;

import jakarta.mail.MessagingException;

/**
 * Process incoming AS4 transmissions. This class is responsible for handling
 * data in a provider independent way (so e.g. not Servlet specific), only based
 * on InputStream and OutputStream. For each incoming request, a new instance of
 * this class is created.
 *
 * @author Martin Bayerl
 * @author Philip Helger
 */
@NotThreadSafe
public class AS4RequestHandler implements AutoCloseable
{
  private interface IAS4ResponseFactory
  {
    @Nonnull
    HttpEntity getHttpEntityForSending (@Nonnull IMimeType aMimeType);

    void applyToResponse (@Nonnull IAS4ResponseAbstraction aHttpResponse, @Nullable IAS4OutgoingDumper aOutgoingDumper);
  }

  private static final class AS4ResponseFactoryXML implements IAS4ResponseFactory
  {
    private final IAS4IncomingMessageMetadata m_aIncomingMessageMetadata;
    private final IAS4IncomingMessageState m_aIncomingState;
    private final String m_sResponseMessageID;
    private final Document m_aDoc;
    private final IMimeType m_aMimeType;

    public AS4ResponseFactoryXML (@Nonnull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                  @Nonnull final IAS4IncomingMessageState aIncomingState,
                                  @Nonnull @Nonempty final String sResponseMessageID,
                                  @Nonnull final Document aDoc,
                                  @Nonnull final IMimeType aMimeType)
    {
      ValueEnforcer.notNull (aIncomingMessageMetadata, "IncomingMessageMetadata");
      ValueEnforcer.notNull (aIncomingState, "IncomingState");
      ValueEnforcer.notEmpty (sResponseMessageID, "ResponseMessageID");
      ValueEnforcer.notNull (aDoc, "Doc");
      ValueEnforcer.notNull (aMimeType, "MimeType");
      m_aIncomingMessageMetadata = aIncomingMessageMetadata;
      m_aIncomingState = aIncomingState;
      m_sResponseMessageID = sResponseMessageID;
      m_aDoc = aDoc;
      m_aMimeType = aMimeType;
    }

    @Nonnull
    public HttpEntity getHttpEntityForSending (@Nonnull final IMimeType aMimType)
    {
      return new HttpXMLEntity (m_aDoc, m_aMimeType);
    }

    public void applyToResponse (@Nonnull final IAS4ResponseAbstraction aHttpResponse,
                                 @Nullable final IAS4OutgoingDumper aOutgoingDumper)
    {
      final String sXML = AS4XMLHelper.serializeXML (m_aDoc);
      final Charset aCharset = AS4XMLHelper.XWS.getCharset ();
      final byte [] aXMLBytes = sXML.getBytes (aCharset);
      aHttpResponse.setContent (aXMLBytes, aCharset);
      aHttpResponse.setMimeType (m_aMimeType);

      if (aOutgoingDumper != null)
      {
        try
        {
          // No custom headers
          final OutputStream aDumpOS = aOutgoingDumper.onBeginRequest (EAS4MessageMode.RESPONSE,
                                                                       m_aIncomingMessageMetadata,
                                                                       m_aIncomingState,
                                                                       m_sResponseMessageID,
                                                                       null,
                                                                       0);
          if (aDumpOS != null)
            try
            {
              aDumpOS.write (aXMLBytes);
            }
            finally
            {
              StreamHelper.close (aDumpOS);
              aOutgoingDumper.onEndRequest (EAS4MessageMode.RESPONSE,
                                            m_aIncomingMessageMetadata,
                                            m_aIncomingState,
                                            m_sResponseMessageID,
                                            (Exception) null);
            }
        }
        catch (final IOException ex)
        {
          LOGGER.warn ("IOException in dumping of outgoing XML response", ex);
        }
      }
    }
  }

  private static final class AS4ResponseFactoryMIME implements IAS4ResponseFactory
  {
    private final IAS4IncomingMessageMetadata m_aIncomingMessageMetadata;
    private final IAS4IncomingMessageState m_aIncomingState;
    private final String m_sResponseMessageID;
    private final AS4MimeMessage m_aMimeMsg;
    private final HttpHeaderMap m_aHttpHeaders;

    public AS4ResponseFactoryMIME (@Nonnull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                   @Nonnull final IAS4IncomingMessageState aIncomingState,
                                   @Nonnull @Nonempty final String sResponseMessageID,
                                   @Nonnull final AS4MimeMessage aMimeMsg) throws MessagingException
    {
      ValueEnforcer.notNull (aIncomingMessageMetadata, "IncomingMessageMetadata");
      ValueEnforcer.notNull (aIncomingState, "IncomingState");
      ValueEnforcer.notEmpty (sResponseMessageID, "ResponseMessageID");
      ValueEnforcer.notNull (aMimeMsg, "MimeMsg");
      m_aIncomingMessageMetadata = aIncomingMessageMetadata;
      m_aIncomingState = aIncomingState;
      m_sResponseMessageID = sResponseMessageID;
      m_aMimeMsg = aMimeMsg;
      m_aHttpHeaders = AS4MimeMessageHelper.getAndRemoveAllHeaders (m_aMimeMsg);
      if (!aMimeMsg.isRepeatable ())
        LOGGER.warn ("The response MIME message is not repeatable");
    }

    @Nonnull
    public HttpMimeMessageEntity getHttpEntityForSending (@Nonnull final IMimeType aMimType)
    {
      // Repeatable if the underlying Mime message is repeatable
      return HttpMimeMessageEntity.create (m_aMimeMsg);
    }

    public void applyToResponse (@Nonnull final IAS4ResponseAbstraction aHttpResponse,
                                 @Nullable final IAS4OutgoingDumper aOutgoingDumper)
    {
      final IHasInputStream aContent = HasInputStream.multiple ( () -> {
        try
        {
          return m_aMimeMsg.getInputStream ();
        }
        catch (final IOException | MessagingException ex)
        {
          throw new IllegalStateException ("Failed to get MIME input stream", ex);
        }
      });
      aHttpResponse.setContent (m_aHttpHeaders, aContent);
      aHttpResponse.setMimeType (MT_MULTIPART_RELATED);

      if (aOutgoingDumper != null)
      {
        try
        {
          final OutputStream aDumpOS = aOutgoingDumper.onBeginRequest (EAS4MessageMode.RESPONSE,
                                                                       m_aIncomingMessageMetadata,
                                                                       m_aIncomingState,
                                                                       m_sResponseMessageID,
                                                                       m_aHttpHeaders,
                                                                       0);
          if (aDumpOS != null)
            try
            {
              StreamHelper.copyByteStream ()
                          .from (aContent.getBufferedInputStream ())
                          .closeFrom (true)
                          .to (aDumpOS)
                          .closeTo (true)
                          .build ();
            }
            finally
            {
              aOutgoingDumper.onEndRequest (EAS4MessageMode.RESPONSE,
                                            m_aIncomingMessageMetadata,
                                            m_aIncomingState,
                                            m_sResponseMessageID,
                                            (Exception) null);
            }
        }
        catch (final IOException ex)
        {
          LOGGER.warn ("IOException in dumping of outgoing MIME response", ex);
        }
      }
    }
  }

  private static final class SPIInvocationResult implements ISuccessIndicator
  {
    private boolean m_bSuccess = false;
    private Ebms3UserMessage m_aPullReturnUserMsg;
    private String m_sAsyncResponseURL;

    public boolean isSuccess ()
    {
      return m_bSuccess;
    }

    void setSuccess (final boolean bSuccess)
    {
      m_bSuccess = bSuccess;
    }

    void setPullReturnUserMsg (@Nonnull final Ebms3UserMessage aPullReturnUserMsg)
    {
      m_aPullReturnUserMsg = aPullReturnUserMsg;
    }

    @Nullable
    public Ebms3UserMessage getPullReturnUserMsg ()
    {
      return m_aPullReturnUserMsg;
    }

    public boolean hasPullReturnUserMsg ()
    {
      return m_aPullReturnUserMsg != null;
    }

    void setAsyncResponseURL (@Nonnull final String sAsyncResponseURL)
    {
      m_sAsyncResponseURL = sAsyncResponseURL;
    }

    @Nullable
    public String getAsyncResponseURL ()
    {
      return m_sAsyncResponseURL;
    }

    public boolean hasAsyncResponseURL ()
    {
      return StringHelper.hasText (m_sAsyncResponseURL);
    }
  }

  public static final IMimeType MT_MULTIPART_RELATED = EMimeContentType.MULTIPART.buildMimeType ("related");
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4RequestHandler.class);

  private final AS4ResourceHelper m_aResHelper;
  private final IAS4IncomingMessageMetadata m_aMessageMetadata;
  private IAS4CryptoFactory m_aCryptoFactorySign;
  private IAS4CryptoFactory m_aCryptoFactoryCrypt;
  private IAS4PModeResolver m_aPModeResolver;
  private IAS4IncomingAttachmentFactory m_aIncomingAttachmentFactory;
  private IAS4IncomingSecurityConfiguration m_aIncomingSecurityConfig;
  private IAS4IncomingReceiverConfiguration m_aIncomingReceiverConfig;
  private IAS4IncomingProfileSelector m_aIncomingProfileSelector;
  private Locale m_aLocale = Locale.US;
  private IAS4IncomingDumper m_aIncomingDumper;
  private IAS4OutgoingDumper m_aOutgoingDumper;
  private IAS4RetryCallback m_aRetryCallback;
  private IAS4SoapProcessingFinalizedCallback m_aSoapProcessingFinalizedCB;

  /** By default get all message processors from the global SPI registry */
  private Supplier <? extends ICommonsList <IAS4IncomingMessageProcessorSPI>> m_aProcessorSupplier = AS4IncomingMessageProcessorManager::getAllProcessors;
  private IAS4RequestHandlerErrorConsumer m_aErrorConsumer;

  public AS4RequestHandler (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata)
  {
    ValueEnforcer.notNull (aMessageMetadata, "MessageMetadata");
    // Create dynamically here, to avoid leaving too many streams open
    m_aResHelper = new AS4ResourceHelper ();
    m_aMessageMetadata = aMessageMetadata;
  }

  public void close ()
  {
    // Delete all the temporary files etc.
    m_aResHelper.close ();
  }

  /**
   * @return The incoming message metadata as provided in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  protected final IAS4IncomingMessageMetadata getMessageMetadata ()
  {
    return m_aMessageMetadata;
  }

  /**
   * @return The {@link IAS4CryptoFactory} for signing. May be <code>null</code>
   *         if not initialized.
   * @see #getCryptoFactoryCrypt()
   * @since 3.0.0
   */
  @Nullable
  public final IAS4CryptoFactory getCryptoFactorySign ()
  {
    return m_aCryptoFactorySign;
  }

  /**
   * Set the crypto factory for signing.
   *
   * @param aCryptoFactorySign
   *        Crypto factory for signing to use. May not be <code>null</code>.
   * @return this for chaining
   * @see #setCryptoFactory(IAS4CryptoFactory)
   * @see #setCryptoFactoryCrypt(IAS4CryptoFactory)
   * @since 3.0.0
   */
  @Nonnull
  public final AS4RequestHandler setCryptoFactorySign (@Nonnull final IAS4CryptoFactory aCryptoFactorySign)
  {
    ValueEnforcer.notNull (aCryptoFactorySign, "CryptoFactorySign");
    m_aCryptoFactorySign = aCryptoFactorySign;
    return this;
  }

  /**
   * @return The {@link IAS4CryptoFactory} for crypting. May be
   *         <code>null</code> if not initialized.
   * @see #getCryptoFactorySign()
   * @since 3.0.0
   */
  @Nullable
  public final IAS4CryptoFactory getCryptoFactoryCrypt ()
  {
    return m_aCryptoFactoryCrypt;
  }

  /**
   * Set the crypto factory crypting.
   *
   * @param aCryptoFactoryCrypt
   *        Crypto factory for crypting to use. May not be <code>null</code>.
   * @return this for chaining
   * @see #setCryptoFactory(IAS4CryptoFactory)
   * @see #setCryptoFactorySign(IAS4CryptoFactory)
   * @since 3.0.0
   */
  @Nonnull
  public final AS4RequestHandler setCryptoFactoryCrypt (@Nonnull final IAS4CryptoFactory aCryptoFactoryCrypt)
  {
    ValueEnforcer.notNull (aCryptoFactoryCrypt, "CryptoFactoryCrypt");
    m_aCryptoFactoryCrypt = aCryptoFactoryCrypt;
    return this;
  }

  /**
   * Set the same crypto factory for signing and crypting. This is a sanity
   * wrapper around {@link #setCryptoFactory(IAS4CryptoFactory)}.
   *
   * @param aCryptoFactory
   *        Crypto factory to use. May not be <code>null</code>.
   * @return this for chaining
   * @see #setCryptoFactoryCrypt(IAS4CryptoFactory)
   * @see #setCryptoFactorySign(IAS4CryptoFactory)
   * @since 3.0.0
   */
  @Nonnull
  public final AS4RequestHandler setCryptoFactory (@Nonnull final IAS4CryptoFactory aCryptoFactory)
  {
    ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
    return setCryptoFactorySign (aCryptoFactory).setCryptoFactoryCrypt (aCryptoFactory);
  }

  /**
   * @return The {@link IAS4PModeResolver} to be used. May be <code>null</code>
   *         if not initialized.
   * @since 3.0.0
   */
  @Nullable
  public final IAS4PModeResolver getPModeResolver ()
  {
    return m_aPModeResolver;
  }

  /**
   * @param aPModeResolver
   *        PMode resolved to be used. May not be <code>null</code>.
   * @return this for chaining
   * @since 3.0.0
   */
  @Nonnull
  public final AS4RequestHandler setPModeResolver (@Nonnull final IAS4PModeResolver aPModeResolver)
  {
    ValueEnforcer.notNull (aPModeResolver, "PModeResolver");
    m_aPModeResolver = aPModeResolver;
    return this;
  }

  /**
   * @return The {@link IAS4IncomingAttachmentFactory} to be used. May be
   *         <code>null</code> if not initialized.
   * @since 3.0.0
   */
  @Nullable
  public final IAS4IncomingAttachmentFactory getIncomingAttachmentFactory ()
  {
    return m_aIncomingAttachmentFactory;
  }

  /**
   * @param aIAF
   *        The attachment factory for incoming attachments. May not be
   *        <code>null</code>.
   * @return this for chaining
   * @since 3.0.0
   */
  @Nonnull
  public final AS4RequestHandler setIncomingAttachmentFactory (@Nonnull final IAS4IncomingAttachmentFactory aIAF)
  {
    ValueEnforcer.notNull (aIAF, "IAF");
    m_aIncomingAttachmentFactory = aIAF;
    return this;
  }

  /**
   * @return The {@link IAS4IncomingSecurityConfiguration} to be used. May be
   *         <code>null</code> if not initialized.
   * @since 3.0.0
   */
  @Nullable
  public final IAS4IncomingSecurityConfiguration getIncomingSecurityConfiguration ()
  {
    return m_aIncomingSecurityConfig;
  }

  /**
   * @param aICS
   *        The incoming security configuration. May not be <code>null</code>.
   * @return this for chaining
   * @since 3.0.0
   */
  @Nonnull
  public final AS4RequestHandler setIncomingSecurityConfiguration (@Nonnull final IAS4IncomingSecurityConfiguration aICS)
  {
    ValueEnforcer.notNull (aICS, "ICS");
    m_aIncomingSecurityConfig = aICS;
    return this;
  }

  /**
   * @return The {@link IAS4IncomingReceiverConfiguration} to be used. May be
   *         <code>null</code> if not initialized.
   * @since 3.0.0
   */
  @Nullable
  public final IAS4IncomingReceiverConfiguration getIncomingReceiverConfiguration ()
  {
    return m_aIncomingReceiverConfig;
  }

  /**
   * @param aIRC
   *        The incoming receiver configuration. May not be <code>null</code>.
   * @return this for chaining
   * @since 3.0.0
   */
  @Nonnull
  public final AS4RequestHandler setIncomingReceiverConfiguration (@Nonnull final IAS4IncomingReceiverConfiguration aIRC)
  {
    ValueEnforcer.notNull (aIRC, "ICS");
    m_aIncomingReceiverConfig = aIRC;
    return this;
  }

  /**
   * @return The current AS4 profile selector for incoming messages. Never
   *         <code>null</code>.
   * @since 0.13.0
   */
  @Nonnull
  public final IAS4IncomingProfileSelector getIncomingProfileSelector ()
  {
    return m_aIncomingProfileSelector;
  }

  /**
   * Set the AS4 profile selector for incoming messages.
   *
   * @param aIncomingProfileSelector
   *        The new profile selector to be used. May not be <code>null</code>.
   * @return this for chaining
   * @since 0.13.0
   */
  @Nonnull
  public final AS4RequestHandler setIncomingProfileSelector (@Nonnull final IAS4IncomingProfileSelector aIncomingProfileSelector)
  {
    ValueEnforcer.notNull (aIncomingProfileSelector, "IncomingProfileSelector");
    m_aIncomingProfileSelector = aIncomingProfileSelector;
    return this;
  }

  /**
   * @return The locale for error messages. Never <code>null</code>.
   */
  @Nonnull
  public final Locale getLocale ()
  {
    return m_aLocale;
  }

  /**
   * Set the error for EBMS error messages.
   *
   * @param aLocale
   *        The locale. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final AS4RequestHandler setLocale (@Nonnull final Locale aLocale)
  {
    ValueEnforcer.notNull (aLocale, "Locale");
    m_aLocale = aLocale;
    return this;
  }

  /**
   * @return The specific dumper for incoming messages. May be
   *         <code>null</code>.
   * @since v0.9.7
   */
  @Nullable
  public final IAS4IncomingDumper getIncomingDumper ()
  {
    return m_aIncomingDumper;
  }

  /**
   * Set the specific dumper for incoming messages. If none is set, the global
   * incoming dumper is used.
   *
   * @param aIncomingDumper
   *        The specific incoming dumper. May be <code>null</code>.
   * @return this for chaining
   * @since v0.9.7
   */
  @Nonnull
  public final AS4RequestHandler setIncomingDumper (@Nullable final IAS4IncomingDumper aIncomingDumper)
  {
    m_aIncomingDumper = aIncomingDumper;
    return this;
  }

  /**
   * @return The specific dumper for outgoing messages. May be
   *         <code>null</code>.
   * @since v0.9.9
   */
  @Nullable
  public final IAS4OutgoingDumper getOutgoingDumper ()
  {
    return m_aOutgoingDumper;
  }

  /**
   * Set the specific dumper for outgoing messages. If none is set, the global
   * outgoing dumper is used.
   *
   * @param aOutgoingDumper
   *        The specific outgoing dumper. May be <code>null</code>.
   * @return this for chaining
   * @since v0.9.9
   */
  @Nonnull
  public final AS4RequestHandler setOutgoingDumper (@Nullable final IAS4OutgoingDumper aOutgoingDumper)
  {
    m_aOutgoingDumper = aOutgoingDumper;
    return this;
  }

  /**
   * @return The HTTP retry callback for outgoing messages. May be
   *         <code>null</code>.
   * @since v0.9.14
   */
  @Nullable
  public final IAS4RetryCallback getRetryCallback ()
  {
    return m_aRetryCallback;
  }

  /**
   * Set the HTTP retry callback for outgoing messages.
   *
   * @param aRetryCallback
   *        The specific retry callback. May be <code>null</code>.
   * @return this for chaining
   * @since v0.9.14
   */
  @Nonnull
  public final AS4RequestHandler setRetryCallback (@Nullable final IAS4RetryCallback aRetryCallback)
  {
    m_aRetryCallback = aRetryCallback;
    return this;
  }

  /**
   * @return The internal SOAP processing finalized callback. <code>null</code>
   *         by default.
   * @since 0.13.1
   */
  @Nullable
  public final IAS4SoapProcessingFinalizedCallback getSoapProcessingFinalizedCallback ()
  {
    return m_aSoapProcessingFinalizedCB;
  }

  /**
   * Set the internal SOAP processing finalized callback. Only use when you know
   * what you are doing. This callback is invoked both in the synchronous AND
   * the asynchronous processing. A simple way to await the finalization could
   * e.g. be a <code>java.util.concurrent.CountDownLatch</code>.
   *
   * @param aSoapProcessingFinalizedCB
   *        The callback to be invoked. May be <code>null</code>. Only
   *        non-<code>null</code> callbacks are invoked ;-)
   * @return this for chaining
   * @since 0.13.1
   */
  @Nonnull
  public final AS4RequestHandler setSoapProcessingFinalizedCallback (@Nullable final IAS4SoapProcessingFinalizedCallback aSoapProcessingFinalizedCB)
  {
    m_aSoapProcessingFinalizedCB = aSoapProcessingFinalizedCB;
    return this;
  }

  /**
   * @return The supplier used to get all SPIs. By default this is
   *         {@link AS4IncomingMessageProcessorManager#getAllProcessors()}.
   */
  @Nonnull
  public final Supplier <? extends ICommonsList <IAS4IncomingMessageProcessorSPI>> getProcessorSupplier ()
  {
    return m_aProcessorSupplier;
  }

  /**
   * Find the message processor of the specified type.
   *
   * @param aTargetClass
   *        The target processor class to search.
   * @return <code>null</code> if no such processor was found
   * @since 2.8.2
   */
  @Nonnull
  public final <T extends IAS4IncomingMessageProcessorSPI> T getProcessorOfType (@Nonnull final Class <T> aTargetClass)
  {
    for (final IAS4IncomingMessageProcessorSPI aEntry : m_aProcessorSupplier.get ())
      if (aTargetClass.isInstance (aEntry))
        return aTargetClass.cast (aEntry);
    return null;
  }

  /**
   * Set a different processor supplier
   *
   * @param aProcessorSupplier
   *        The processor supplier to be used. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final AS4RequestHandler setProcessorSupplier (@Nonnull final Supplier <? extends ICommonsList <IAS4IncomingMessageProcessorSPI>> aProcessorSupplier)
  {
    ValueEnforcer.notNull (aProcessorSupplier, "ProcessorSupplier");
    m_aProcessorSupplier = aProcessorSupplier;
    return this;
  }

  /**
   * @return An optional error consumer. <code>null</code> by default.
   * @since 0.9.7
   */
  @Nullable
  public final IAS4RequestHandlerErrorConsumer getErrorConsumer ()
  {
    return m_aErrorConsumer;
  }

  /**
   * Set an optional error consumer that is invoked with all errors determined
   * during message processing. The consumed list MUST NOT be modified.<br>
   * Note: the error consumer is ONLY called if the error list is non-empty.<br>
   * Note: the AS4 error message is sent back automatically - this is just
   * informational.
   *
   * @param aErrorConsumer
   *        The consumer to be used. May be <code>null</code>.
   * @return this for chaining
   * @since 0.9.7
   */
  @Nonnull
  public final AS4RequestHandler setErrorConsumer (@Nullable final IAS4RequestHandlerErrorConsumer aErrorConsumer)
  {
    m_aErrorConsumer = aErrorConsumer;
    return this;
  }

  /**
   * Invoke custom SPI message processors
   *
   * @param aHttpHeaders
   *        The received HTTP headers. Never <code>null</code>.
   * @param aEbmsUserMessage
   *        Current user message. Either this OR signal message must be
   *        non-<code>null</code>.
   * @param aEbmsSignalMessage
   *        The signal message to use. Either this OR user message must be
   *        non-<code>null</code>.
   * @param aPayloadNode
   *        Optional SOAP body payload (only if direct SOAP msg, not for MIME).
   *        May be <code>null</code>.
   * @param aDecryptedAttachments
   *        Original attachments from source message. May be <code>null</code>.
   * @param aPMode
   *        PMode to be used - may be <code>null</code> for Receipt messages.
   * @param aIncomingState
   *        The current state. Never <code>null</<code></code>.
   * @param aEbmsErrorMessagesTarget
   *        The list of error messages to be filled if something goes wrong.
   *        Never <code>null</code>.
   * @param aResponseAttachmentsTarget
   *        The list of attachments to be added to the response. Never
   *        <code>null</code>.
   * @param aSPIResult
   *        The result object to be filled. May not be <code>null</code>.
   */
  private void _invokeSPIsForIncoming (@Nonnull final HttpHeaderMap aHttpHeaders,
                                       @Nullable final Ebms3UserMessage aEbmsUserMessage,
                                       @Nullable final Ebms3SignalMessage aEbmsSignalMessage,
                                       @Nullable final Node aPayloadNode,
                                       @Nullable final ICommonsList <WSS4JAttachment> aDecryptedAttachments,
                                       @Nullable final IPMode aPMode,
                                       @Nonnull final IAS4IncomingMessageState aIncomingState,
                                       @Nonnull final ICommonsList <Ebms3Error> aEbmsErrorMessagesTarget,
                                       @Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachmentsTarget,
                                       @Nonnull final SPIInvocationResult aSPIResult)
  {
    ValueEnforcer.isTrue (aEbmsUserMessage != null || aEbmsSignalMessage != null,
                          "User OR Signal Message must be present");
    ValueEnforcer.isFalse (aEbmsUserMessage != null && aEbmsSignalMessage != null,
                           "Only one of User OR Signal Message may be present");

    final boolean bIsUserMessage = aEbmsUserMessage != null;
    final String sMessageID = bIsUserMessage ? aEbmsUserMessage.getMessageInfo ().getMessageId ()
                                             : aEbmsSignalMessage.getMessageInfo ().getMessageId ();

    // Get all processors
    final ICommonsList <IAS4IncomingMessageProcessorSPI> aAllProcessors = m_aProcessorSupplier.get ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Trying to invoke the following " +
                    aAllProcessors.size () +
                    " SPIs on message ID '" +
                    sMessageID +
                    "': " +
                    aAllProcessors);

    if (aAllProcessors.isEmpty ())
      LOGGER.error ("No IAS4ServletMessageProcessorSPI is available to process an incoming message");

    // Invoke ALL non-null SPIs
    for (final IAS4IncomingMessageProcessorSPI aProcessor : aAllProcessors)
      if (aProcessor != null)
        try
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Invoking AS4 message processor " + aProcessor + " for incoming message");

          // Main processing
          final AS4MessageProcessorResult aResult;
          final ICommonsList <Ebms3Error> aProcessingErrorMessages = new CommonsArrayList <> ();
          if (bIsUserMessage)
          {
            aResult = aProcessor.processAS4UserMessage (m_aMessageMetadata,
                                                        aHttpHeaders,
                                                        aEbmsUserMessage,
                                                        aPMode,
                                                        aPayloadNode,
                                                        aDecryptedAttachments,
                                                        aIncomingState,
                                                        aProcessingErrorMessages);
          }
          else
          {
            aResult = aProcessor.processAS4SignalMessage (m_aMessageMetadata,
                                                          aHttpHeaders,
                                                          aEbmsSignalMessage,
                                                          aPMode,
                                                          aIncomingState,
                                                          aProcessingErrorMessages);
          }

          // Result returned?
          if (aResult == null)
            throw new IllegalStateException ("No result object present from AS4 message processor " +
                                             aProcessor +
                                             " - this is a programming error");

          if (aProcessingErrorMessages.isNotEmpty () || aResult.isFailure ())
          {
            if (aResult.isFailure () && aProcessingErrorMessages.isEmpty ())
            {
              // For 2.7.6 - make sure that at least one processing error
              // message is contained
              aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (m_aLocale)
                                                                 .errorDetail ("An undefined generic error occurred")
                                                                 .build ());
            }

            if (LOGGER.isDebugEnabled ())
              LOGGER.debug ("AS4 message processor " +
                            aProcessor +
                            " had processing errors - breaking. Details: " +
                            aProcessingErrorMessages);

            if (aResult.isSuccess ())
              LOGGER.warn ("Processing errors are present but success was returned by a previous AS4 message processor " +
                           aProcessor +
                           " - considering the whole processing to be failed instead");

            aEbmsErrorMessagesTarget.addAll (aProcessingErrorMessages);

            // Stop processing
            aSPIResult.setSuccess (false);
            return;
          }

          // SPI invocation returned success and no errors
          {
            final String sAsyncResultURL = aResult.getAsyncResponseURL ();
            if (StringHelper.hasText (sAsyncResultURL))
            {
              // URL present
              if (aSPIResult.hasAsyncResponseURL ())
              {
                // A second processor returned a response URL - not allowed
                final String sDetails = "Invoked AS4 message processor SPI " +
                                        aProcessor +
                                        " on '" +
                                        sMessageID +
                                        "' failed: the previous processor already returned an async response URL; it is not possible to handle two URLs. Please check your SPI implementations.";
                LOGGER.error (sDetails);
                aEbmsErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (m_aLocale)
                                                                                .refToMessageInError (sMessageID)
                                                                                .errorDetail (sDetails)
                                                                                .build ());
                // Stop processing
                return;
              }
              aSPIResult.setAsyncResponseURL (sAsyncResultURL);
              LOGGER.info ("Using asynchronous response URL '" +
                           sAsyncResultURL +
                           "' for message ID '" +
                           sMessageID +
                           "'");
            }
          }

          if (bIsUserMessage)
          {
            // User message specific processing result handling

            // empty
          }
          else
          {
            // Signal message specific processing result handling
            assert aResult instanceof AS4SignalMessageProcessorResult;

            if (aEbmsSignalMessage.getReceipt () == null)
            {
              final Ebms3UserMessage aPullReturnUserMsg = ((AS4SignalMessageProcessorResult) aResult).getPullReturnUserMessage ();
              if (aSPIResult.hasPullReturnUserMsg ())
              {
                // A second processor has committed a response to the
                // pullrequest
                // Which is not allowed since only one response can be sent back
                // to the pullrequest initiator
                if (aPullReturnUserMsg != null)
                {
                  final String sDetails = "Invoked AS4 message processor SPI " +
                                          aProcessor +
                                          " on '" +
                                          sMessageID +
                                          "' failed: the previous processor already returned a usermessage; it is not possible to return two usermessage. Please check your SPI implementations.";
                  LOGGER.warn (sDetails);
                  aEbmsErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (m_aLocale)
                                                                                  .refToMessageInError (sMessageID)
                                                                                  .errorDetail (sDetails)
                                                                                  .build ());
                  // Stop processing
                  return;
                }
              }
              else
              {
                // Initial return user msg
                if (aPullReturnUserMsg == null)
                {
                  // No message contained in the MPC
                  final String sDetails = "Invoked AS4 message processor SPI " +
                                          aProcessor +
                                          " on '" +
                                          sMessageID +
                                          "' returned a failure: no UserMessage contained in the MPC";
                  LOGGER.error (sDetails);
                  aEbmsErrorMessagesTarget.add (EEbmsError.EBMS_EMPTY_MESSAGE_PARTITION_CHANNEL.errorBuilder (m_aLocale)
                                                                                               .refToMessageInError (sMessageID)
                                                                                               .errorDetail (sDetails)
                                                                                               .build ());
                  // Stop processing
                  return;
                }

                // We have something :)
                aSPIResult.setPullReturnUserMsg (aPullReturnUserMsg);
              }
            }
            else
            {
              if (LOGGER.isDebugEnabled ())
                LOGGER.debug ("The AS4 EbmsSignalMessage already has a Receipt");
            }
          }

          // Add response attachments, payloads
          aResult.addAllAttachmentsTo (aResponseAttachmentsTarget);

          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Successfully invoked AS4 message processor " + aProcessor);
        }
        catch (final AS4DecompressException ex)
        {
          final String sDetails = "Failed to decompress AS4 payload";
          LOGGER.error (sDetails, ex);
          // Hack for invalid GZip content from WSS4JAttachment.getSourceStream
          aEbmsErrorMessagesTarget.add (EEbmsError.EBMS_DECOMPRESSION_FAILURE.errorBuilder (m_aLocale)
                                                                             .refToMessageInError (sMessageID)
                                                                             .errorDetail (sDetails, ex)
                                                                             .build ());
          return;
        }
        catch (final RuntimeException ex)
        {
          // Re-throw
          throw ex;
        }
        catch (final Exception ex)
        {
          throw new IllegalStateException ("Error processing incoming AS4 message with processor " + aProcessor, ex);
        }

    // Remember success
    aSPIResult.setSuccess (true);
  }

  private void _invokeSPIsForResponse (@Nonnull final IAS4IncomingMessageState aIncomingState,
                                       @Nullable final IAS4ResponseFactory aResponseFactory,
                                       @Nullable final HttpEntity aHttpEntity,
                                       @Nonnull final IMimeType aMimeType,
                                       @Nullable final String sResponseMessageID)
  {
    // Get response payload as byte array for multiple processing by the SPIs
    final boolean bResponsePayloadIsAvailable = aResponseFactory != null;
    byte [] aResponsePayload = null;
    if (aResponseFactory != null)
    {
      final HttpEntity aRealHttpEntity = aHttpEntity != null ? aHttpEntity
                                                             : aResponseFactory.getHttpEntityForSending (aMimeType);
      if (aRealHttpEntity.isRepeatable ())
      {
        int nContentLength = (int) aRealHttpEntity.getContentLength ();
        if (nContentLength < 0)
          nContentLength = 16 * CGlobal.BYTES_PER_KILOBYTE;

        try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream (nContentLength))
        {
          aRealHttpEntity.writeTo (aBAOS);
          aResponsePayload = aBAOS.getBufferOrCopy ();
        }
        catch (final IOException ex)
        {
          LOGGER.error ("Error dumping response entity", ex);
        }
      }
      else
        LOGGER.warn ("AS4 Response entity is not repeatable and therefore not read for SPIs");
    }
    else
      LOGGER.info ("No response factory present");

    // Get all processors
    final ICommonsList <IAS4IncomingMessageProcessorSPI> aAllProcessors = m_aProcessorSupplier.get ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Trying to invoke the following " +
                    aAllProcessors.size () +
                    " SPIs on AS4 message ID '" +
                    aIncomingState.getMessageID () +
                    "' and AS4 response message ID '" +
                    sResponseMessageID +
                    ": " +
                    aAllProcessors);

    for (final IAS4IncomingMessageProcessorSPI aProcessor : aAllProcessors)
      if (aProcessor != null)
        try
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Invoking AS4 message processor " + aProcessor + " for response");

          aProcessor.processAS4ResponseMessage (m_aMessageMetadata,
                                                aIncomingState,
                                                sResponseMessageID,
                                                aResponsePayload,
                                                bResponsePayloadIsAvailable);

          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Finished invoking AS4 message processor " + aProcessor + " for response");
        }
        catch (final RuntimeException ex)
        {
          // Re-throw
          throw ex;
        }
        catch (final Exception ex)
        {
          throw new IllegalStateException ("Error invoking AS4 message processor " + aProcessor + " for response", ex);
        }
  }

  /**
   * Takes an UserMessage and switches properties to reverse the direction. So
   * previously it was C1 => C4, now its C4 => C1 Also adds attachments if there
   * are some that should be added.
   *
   * @param eSoapVersion
   *        of the message
   * @param sResponseMessageID
   *        The AS4 message ID of the response
   * @param aUserMessage
   *        the message that should be reversed
   * @param aResponseAttachments
   *        attachment that should be added
   * @return the reversed usermessage in document form
   */
  @Nonnull
  private static AS4UserMessage _createReversedUserMessage (@Nonnull final ESoapVersion eSoapVersion,
                                                            @Nonnull @Nonempty final String sResponseMessageID,
                                                            @Nonnull final Ebms3UserMessage aUserMessage,
                                                            @Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments)
  {
    // Use current time
    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (sResponseMessageID,
                                                                                            aUserMessage.getMessageInfo ()
                                                                                                        .getMessageId ());
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (false,
                                                                                            aResponseAttachments);

    // Invert from and to role from original user message
    final Ebms3PartyInfo aEbms3PartyInfo = MessageHelperMethods.createEbms3ReversePartyInfo (aUserMessage.getPartyInfo ());

    // Should be exactly the same as incoming message
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = aUserMessage.getCollaborationInfo ();

    // Need to switch C1 and C4 around from the original usermessage
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    {
      Ebms3Property aFinalRecipient = null;
      Ebms3Property aOriginalSender = null;
      for (final Ebms3Property aProp : aUserMessage.getMessageProperties ().getProperty ())
      {
        if (aProp.getName ().equals (CAS4.ORIGINAL_SENDER))
          aOriginalSender = aProp;
        else
          if (aProp.getName ().equals (CAS4.FINAL_RECIPIENT))
            aFinalRecipient = aProp;
      }

      if (aOriginalSender != null && aFinalRecipient != null)
      {
        aFinalRecipient.setName (CAS4.ORIGINAL_SENDER);
        aOriginalSender.setName (CAS4.FINAL_RECIPIENT);

        aEbms3MessageProperties.addProperty (aFinalRecipient.clone ());
        aEbms3MessageProperties.addProperty (aOriginalSender.clone ());
      }
      else
      {
        // TODO make check customizable via profile
        // Disable for now
        if (false)
        {
          if (aOriginalSender == null)
            throw new IllegalStateException ("Failed to determine new OriginalSender");
          if (aFinalRecipient == null)
            throw new IllegalStateException ("Failed to determine new FinalRecipient");
        }
      }
    }

    return AS4UserMessage.create (aEbms3MessageInfo,
                                  aEbms3PayloadInfo,
                                  aEbms3CollaborationInfo,
                                  aEbms3PartyInfo,
                                  aEbms3MessageProperties,
                                  null,
                                  eSoapVersion);
  }

  /**
   * Checks if in the given PMode isReportAsResponse is set.
   *
   * @param aLeg
   *        The PMode leg to check. May be <code>null</code>.
   * @return Returns the value if set, else DEFAULT <code>true</code>.
   */
  private static boolean _isSendErrorAsResponse (@Nullable final PModeLeg aLeg)
  {
    if (aLeg != null)
      if (aLeg.hasErrorHandling ())
        if (aLeg.getErrorHandling ().isReportAsResponseDefined ())
        {
          // Note: this is enabled in Default PMode
          return aLeg.getErrorHandling ().isReportAsResponse ();
        }
    // Default behavior
    return true;
  }

  /**
   * Checks if a ReceiptReplyPattern is set to Response or not.
   *
   * @param aPLeg
   *        to PMode leg to use. May be <code>null</code>.
   * @return Returns the value if set, else DEFAULT <code>TRUE</code>.
   */
  private static boolean _isSendReceiptAsResponse (@Nullable final PModeLeg aLeg)
  {
    if (aLeg != null && aLeg.hasSecurity ())
    {
      // Note: this is enabled in Default PMode
      return aLeg.getSecurity ().isSendReceipt () &&
             EPModeSendReceiptReplyPattern.RESPONSE.equals (aLeg.getSecurity ().getSendReceiptReplyPattern ());
    }
    // Default behaviour if the value is not set or no security is existing
    return true;
  }

  /**
   * If the PModeLegSecurity has set a Sign and Digest Algorithm the message
   * will be signed, else the message will be returned as it is.
   *
   * @param aResponseAttachments
   *        attachment that are added
   * @param aSigningParams
   *        Signing parameters
   * @param aDocToBeSigned
   *        the message that should be signed
   * @param eSoapVersion
   *        SOAPVersion that is used
   * @param sMessagingID
   *        The messaging ID to be used for signing
   * @return returns the signed response or just the input document if no
   *         X509SignatureAlgorithm and no X509SignatureHashFunction was set.
   * @throws WSSecurityException
   *         if something in the signing process goes wrong from WSS4j
   */
  @Nonnull
  private Document _signResponseIfNeeded (@Nullable final ICommonsList <WSS4JAttachment> aResponseAttachments,
                                          @Nonnull final AS4SigningParams aSigningParams,
                                          @Nonnull final Document aDocToBeSigned,
                                          @Nonnull final ESoapVersion eSoapVersion,
                                          @Nonnull @Nonempty final String sMessagingID) throws WSSecurityException
  {
    final Document ret;
    if (aSigningParams.isSigningEnabled ())
    {
      // Sign
      final boolean bMustUnderstand = true;
      ret = AS4Signer.createSignedMessage (m_aCryptoFactorySign,
                                           aDocToBeSigned,
                                           eSoapVersion,
                                           sMessagingID,
                                           aResponseAttachments,
                                           m_aResHelper,
                                           bMustUnderstand,
                                           aSigningParams.getClone ());
    }
    else
    {
      // No signing
      ret = aDocToBeSigned;
    }
    return ret;
  }

  /**
   * Checks if in the given PMode the isSendReceiptNonRepudiation is set or not.
   *
   * @param aLeg
   *        The PMode leg to check. May not be <code>null</code>.
   * @return Returns the value if set, else DEFAULT <code>false</code>.
   */
  private static boolean _isSendNonRepudiationInformation (@Nonnull final PModeLeg aLeg)
  {
    if (aLeg.hasSecurity ())
      if (aLeg.getSecurity ().isSendReceiptNonRepudiationDefined ())
        return aLeg.getSecurity ().isSendReceiptNonRepudiation ();
    // Default behavior is "false"
    return PModeLegSecurity.DEFAULT_SEND_RECEIPT_NON_REPUDIATION;
  }

  /**
   * @param aIncomingState
   *        The processing state of the incoming message. Never
   *        <code>null</code>.
   * @param aSoapDocument
   *        Received SOAP document which should be used as source for the
   *        receipt to convert it to non-repudiation information. Can be
   *        <code>null</code>.
   * @param eSoapVersion
   *        SOAPVersion which should be used
   * @param aEffectiveLeg
   *        the leg that is used to determined, how the receipt should be build
   * @param aUserMessage
   *        used if no non-repudiation information is needed, prints the
   *        usermessage in receipt. Can be <code>null</code>.
   * @param aResponseAttachments
   *        that should be sent back if needed. Can be <code>null</code>.
   * @throws WSSecurityException
   */
  @Nonnull
  private IAS4ResponseFactory _createResponseReceiptMessage (@Nonnull final IAS4IncomingMessageState aIncomingState,
                                                             @Nullable final Document aSoapDocument,
                                                             @Nonnull final ESoapVersion eSoapVersion,
                                                             @Nonnull @Nonempty final String sResponseMessageID,
                                                             @Nonnull final PModeLeg aEffectiveLeg,
                                                             @Nullable final Ebms3UserMessage aUserMessage,
                                                             @Nullable final ICommonsList <WSS4JAttachment> aResponseAttachments) throws WSSecurityException
  {
    // Create receipt
    final AS4ReceiptMessage aReceiptMessage = AS4ReceiptMessage.create (eSoapVersion,
                                                                        sResponseMessageID,
                                                                        aUserMessage,
                                                                        aSoapDocument,
                                                                        _isSendNonRepudiationInformation (aEffectiveLeg),
                                                                        null)
                                                               .setMustUnderstand (true);

    final ESoapVersion eResponseSoapVersion = aEffectiveLeg.getProtocol ().getSoapVersion ();
    if (eResponseSoapVersion != eSoapVersion)
      LOGGER.warn ("Received message with " +
                   eSoapVersion +
                   " but the Response PMode leg requires " +
                   eResponseSoapVersion);

    // Sign the Receipt
    final Document aResponseDoc = aReceiptMessage.getAsSoapDocument ();
    final AS4SigningParams aSigningParams = m_aIncomingSecurityConfig.getSigningParamsCloneOrNew ()
                                                                     .setFromPMode (aEffectiveLeg.getSecurity ());
    final Document aSignedDoc = _signResponseIfNeeded (aResponseAttachments,
                                                       aSigningParams,
                                                       aResponseDoc,
                                                       eResponseSoapVersion,
                                                       aReceiptMessage.getMessagingID ());

    // Return the signed receipt
    return new AS4ResponseFactoryXML (m_aMessageMetadata,
                                      aIncomingState,
                                      sResponseMessageID,
                                      aSignedDoc,
                                      eResponseSoapVersion.getMimeType ());
  }

  @Nonnull
  private IAS4ResponseFactory _createResponseErrorMessage (@Nonnull final IAS4IncomingMessageState aIncomingState,
                                                           @Nonnull final ESoapVersion eSoapVersion,
                                                           @Nonnull @Nonempty final String sResponseMessageID,
                                                           @Nullable final PModeLeg aEffectiveLeg,
                                                           @Nonnull @Nonempty final ICommonsList <Ebms3Error> aEbmsErrorMessages)
  {
    // Start building response error message
    final AS4ErrorMessage aErrorMsg = AS4ErrorMessage.create (eSoapVersion,
                                                              MessageHelperMethods.createEbms3MessageInfo (sResponseMessageID,
                                                                                                           aIncomingState.getMessageID ()),
                                                              aEbmsErrorMessages);

    // Call optional consumer
    if (m_aErrorConsumer != null)
      m_aErrorConsumer.onAS4ErrorMessage (aIncomingState, aEbmsErrorMessages, aErrorMsg);

    // Determine SOAP version
    final ESoapVersion eResponseSoapVersion;
    if (aEffectiveLeg != null)
    {
      eResponseSoapVersion = aEffectiveLeg.getProtocol ().getSoapVersion ();
      if (eResponseSoapVersion != eSoapVersion)
        LOGGER.warn ("Received message with " +
                     eSoapVersion +
                     " but the Response PMode leg requires " +
                     eResponseSoapVersion);
    }
    else
      eResponseSoapVersion = eSoapVersion;

    Document aResponseDoc = aErrorMsg.getAsSoapDocument ();
    if (aEffectiveLeg != null)
    {
      // Sign the Error if possible
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Trying to sign AS4 Error response");

      try
      {
        final AS4SigningParams aSigningParams = m_aIncomingSecurityConfig.getSigningParamsCloneOrNew ()
                                                                         .setFromPMode (aEffectiveLeg.getSecurity ());
        final Document aSignedDoc = _signResponseIfNeeded (null,
                                                           aSigningParams,
                                                           aResponseDoc,
                                                           eResponseSoapVersion,
                                                           aErrorMsg.getMessagingID ());
        aResponseDoc = aSignedDoc;
      }
      catch (final WSSecurityException ex)
      {
        // Signing does not work - so we send the error message unsigned
        LOGGER.warn ("Tried to sign the AS4 Error message but failed. Returning the unsigned AS4 Error instead.", ex);
      }
    }
    else
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Cannot sign AS4 Error response, because no PMode Leg was provided");
    }

    return new AS4ResponseFactoryXML (m_aMessageMetadata,
                                      aIncomingState,
                                      sResponseMessageID,
                                      aResponseDoc,
                                      eResponseSoapVersion.getMimeType ());
  }

  /**
   * Returns the MimeMessage with encrypted attachment or without depending on
   * what is configured in the PMode within Leg2.
   *
   * @param aResponseDoc
   *        the document that contains the user message
   * @param aResponseAttachments
   *        The Attachments that should be encrypted
   * @param aLeg
   *        Leg to get necessary information, EncryptionAlgorithm, SOAPVersion
   * @param sEncryptToAlias
   *        The alias into the keystore that should be used for encryption
   * @return a MimeMessage to be sent
   * @throws MessagingException
   * @throws WSSecurityException
   */
  @Nonnull
  private AS4MimeMessage _createMimeMessageForResponse (@Nonnull final Document aResponseDoc,
                                                        @Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments,
                                                        @Nonnull final ESoapVersion eSoapVersion,
                                                        @Nonnull final AS4CryptParams aCryptParms) throws WSSecurityException,
                                                                                                   MessagingException
  {
    final AS4MimeMessage aMimeMsg;
    if (aCryptParms.isCryptEnabled (LOGGER::warn))
    {
      if (aResponseAttachments.isNotEmpty ())
      {
        final boolean bMustUnderstand = true;
        aMimeMsg = AS4Encryptor.encryptToMimeMessage (eSoapVersion,
                                                      aResponseDoc,
                                                      aResponseAttachments,
                                                      m_aCryptoFactoryCrypt,
                                                      bMustUnderstand,
                                                      m_aResHelper,
                                                      aCryptParms);
      }
      else
      {
        LOGGER.info ("AS4 encryption is enabled but no response attachments are present");
        aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (eSoapVersion, aResponseDoc, aResponseAttachments);
      }
    }
    else
    {
      aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (eSoapVersion, aResponseDoc, aResponseAttachments);
    }
    if (aMimeMsg == null)
      throw new IllegalStateException ("Failed to create MimeMessage!");
    return aMimeMsg;
  }

  /**
   * With this method it is possible to send a usermessage back, the method will
   * check if signing is needed and if the message needs to be a mime message.
   *
   * @param aIncomingState
   *        The state of the incoming message. Never <code>null</code>.
   * @param eSoapVersion
   *        the SOAP version to use. May not be <code>null</code>
   * @param aResponseUserMsg
   *        the response user message that should be sent
   * @param sMessagingID
   *        ID of the "Messaging" element
   * @param aResponseAttachments
   *        attachments if any that should be added
   * @param aSigningParams
   *        Signing parameters
   * @param aCryptParams
   *        Encryption parameters
   * @throws WSSecurityException
   *         on error
   * @throws MessagingException
   *         on error
   */
  @Nonnull
  private IAS4ResponseFactory _createResponseUserMessage (@Nonnull final IAS4IncomingMessageState aIncomingState,
                                                          @Nonnull final ESoapVersion eSoapVersion,
                                                          @Nonnull final AS4UserMessage aResponseUserMsg,
                                                          @Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments,
                                                          @Nonnull final AS4SigningParams aSigningParams,
                                                          @Nonnull final AS4CryptParams aCryptParams) throws WSSecurityException,
                                                                                                      MessagingException
  {
    final String sResponseMessageID = aResponseUserMsg.getEbms3UserMessage ().getMessageInfo ().getMessageId ();
    final Document aSignedDoc = _signResponseIfNeeded (aResponseAttachments,
                                                       aSigningParams,
                                                       aResponseUserMsg.getAsSoapDocument (),
                                                       eSoapVersion,
                                                       aResponseUserMsg.getMessagingID ());

    final IAS4ResponseFactory ret;
    if (aResponseAttachments.isEmpty ())
    {
      // FIXME encryption of SOAP body is missing here
      ret = new AS4ResponseFactoryXML (m_aMessageMetadata,
                                       aIncomingState,
                                       sResponseMessageID,
                                       aSignedDoc,
                                       eSoapVersion.getMimeType ());
    }
    else
    {
      // Create (maybe encrypted) MIME message
      final AS4MimeMessage aMimeMsg = _createMimeMessageForResponse (aSignedDoc,
                                                                     aResponseAttachments,
                                                                     eSoapVersion,
                                                                     aCryptParams);
      ret = new AS4ResponseFactoryMIME (m_aMessageMetadata, aIncomingState, sResponseMessageID, aMimeMsg);
    }
    return ret;
  }

  @Nullable
  private IAS4ResponseFactory _handleSoapMessage (@Nonnull final HttpHeaderMap aHttpHeaders,
                                                  @Nonnull final Document aSoapDocument,
                                                  @Nonnull final ESoapVersion eSoapVersion,
                                                  @Nonnull final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                  @Nonnull final ICommonsList <Ebms3Error> aEbmsErrorMessagesTarget) throws WSSecurityException,
                                                                                                                     MessagingException,
                                                                                                                     Phase4Exception
  {
    // Create the SOAP header element processor list
    final SoapHeaderElementProcessorRegistry aRegistry = SoapHeaderElementProcessorRegistry.createDefault (m_aPModeResolver,
                                                                                                           m_aCryptoFactorySign,
                                                                                                           m_aCryptoFactoryCrypt,
                                                                                                           (IPMode) null,
                                                                                                           m_aIncomingSecurityConfig,
                                                                                                           m_aIncomingReceiverConfig);

    // Decompose the SOAP message
    final IAS4IncomingMessageState aIncomingState = AS4IncomingHandler.processEbmsMessage (m_aResHelper,
                                                                                           m_aLocale,
                                                                                           aRegistry,
                                                                                           aHttpHeaders,
                                                                                           aSoapDocument,
                                                                                           eSoapVersion,
                                                                                           aIncomingAttachments,
                                                                                           m_aIncomingProfileSelector,
                                                                                           aEbmsErrorMessagesTarget,
                                                                                           m_aMessageMetadata);

    // Evaluate the results of processing
    final IPMode aPMode = aIncomingState.getPMode ();
    final PModeLeg aEffectiveLeg = aIncomingState.getEffectivePModeLeg ();
    final String sMessageID = aIncomingState.getMessageID ();
    final ICommonsList <WSS4JAttachment> aDecryptedAttachments = aIncomingState.hasDecryptedAttachments () ? aIncomingState.getDecryptedAttachments ()
                                                                                                           : aIncomingState.getOriginalAttachments ();
    final Node aPayloadNode = aIncomingState.getSoapBodyPayloadNode ();
    final Ebms3UserMessage aEbmsUserMessage = aIncomingState.getEbmsUserMessage ();
    final Ebms3SignalMessage aEbmsSignalMessage = aIncomingState.getEbmsSignalMessage ();

    if (aIncomingState.isSoapHeaderElementProcessingSuccessful ())
    {
      final String sProfileID = aIncomingState.getProfileID ();

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Now checking for duplicate message with message ID '" +
                      sMessageID +
                      "' and profile ID '" +
                      sProfileID +
                      "'");

      // Run duplicate message check
      final boolean bIsDuplicate = MetaAS4Manager.getIncomingDuplicateMgr ()
                                                 .registerAndCheck (sMessageID,
                                                                    sProfileID,
                                                                    aPMode == null ? null : aPMode.getID ())
                                                 .isBreak ();
      if (bIsDuplicate)
      {
        final String sDetails = "Not invoking SPIs, because message with Message ID '" +
                                sMessageID +
                                "' was already handled (this is a duplicate)";
        LOGGER.error (sDetails);
        aEbmsErrorMessagesTarget.add (EEbmsError.EBMS_OTHER.errorBuilder (m_aLocale)
                                                           .refToMessageInError (sMessageID)
                                                           .errorDetail (sDetails)
                                                           .build ());
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Message is not a duplicate");
      }
    }

    final SPIInvocationResult aSPIResult = new SPIInvocationResult ();

    // Storing for two-way response messages
    final ICommonsList <WSS4JAttachment> aResponseAttachments = new CommonsArrayList <> ();

    // Invoke SPIs if
    // * No errors so far (sign, encrypt, ...)
    // * Valid PMode
    // * Exactly one UserMessage or SignalMessage
    // * If ping/test message then only if profile should invoke SPI
    // * No Duplicate message ID
    boolean bCanInvokeSPIs = true;
    if (aEbmsErrorMessagesTarget.isNotEmpty ())
    {
      // Previous processing errors
      bCanInvokeSPIs = false;
    }

    final IAS4Profile aAS4Profile = aIncomingState.getAS4Profile ();
    if (aAS4Profile == null)
    {
      // If no AS4 profile is present, we don't invoke SPI for ping messages
      if (aIncomingState.isPingMessage ())
        bCanInvokeSPIs = false;
    }
    else
    {
      // If an AS4 profile is present, we check if we need to pass through ping
      // messages or not
      if (aIncomingState.isPingMessage ())
        bCanInvokeSPIs = aAS4Profile.isInvokeSPIForPingMessage ();
    }
    if (aIncomingState.isPingMessage () && !bCanInvokeSPIs)
      LOGGER.info ("Received an AS4 Ping message - meaning it will NOT be handled by the custom handlers.");

    if (bCanInvokeSPIs)
    {
      // PMode may be null for receipts
      if (aPMode == null ||
          aPMode.getMEPBinding ().isSynchronous () ||
          aPMode.getMEPBinding ().isAsynchronousInitiator () ||
          aIncomingState.getEffectivePModeLegNumber () != 1)
      {
        // Call synchronous

        // Might add to aErrorMessages
        // Might add to aResponseAttachments
        // Might add to m_aPullReturnUserMsg
        _invokeSPIsForIncoming (aHttpHeaders,
                                aEbmsUserMessage,
                                aEbmsSignalMessage,
                                aPayloadNode,
                                aDecryptedAttachments,
                                aPMode,
                                aIncomingState,
                                aEbmsErrorMessagesTarget,
                                aResponseAttachments,
                                aSPIResult);
        if (aSPIResult.isFailure ())
          LOGGER.warn ("Error invoking synchronous SPIs");
        else
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Successfully invoked synchronous SPIs");

        // Notify outside world about the end of the incoming processing
        if (m_aSoapProcessingFinalizedCB != null)
          m_aSoapProcessingFinalizedCB.onProcessingFinalized (true);
      }
      else
      {
        // Call asynchronous
        // this should only apply to MEP binding PUSH_PUSH Leg 1

        // Only leg1 can be async!
        final IThrowingRunnable <Exception> r = () -> {
          // Start async processing
          final ICommonsList <Ebms3Error> aLocalErrorMessages = new CommonsArrayList <> ();
          final ICommonsList <WSS4JAttachment> aLocalResponseAttachments = new CommonsArrayList <> ();

          // Invoke SPI callbacks
          final SPIInvocationResult aAsyncSPIResult = new SPIInvocationResult ();
          _invokeSPIsForIncoming (aHttpHeaders,
                                  aEbmsUserMessage,
                                  aEbmsSignalMessage,
                                  aPayloadNode,
                                  aDecryptedAttachments,
                                  aPMode,
                                  aIncomingState,
                                  aLocalErrorMessages,
                                  aLocalResponseAttachments,
                                  aAsyncSPIResult);

          final IAS4ResponseFactory aAsyncResponseFactory;
          final String sResponseMessageID;
          if (aAsyncSPIResult.isSuccess ())
          {
            // SPI processing succeeded
            assert aLocalErrorMessages.isEmpty ();

            // The response user message has no explicit payload. All data of
            // the response user message is in the local attachments
            sResponseMessageID = MessageHelperMethods.createRandomMessageID ();
            final AS4UserMessage aResponseUserMsg = _createReversedUserMessage (eSoapVersion,
                                                                                sResponseMessageID,
                                                                                aEbmsUserMessage,
                                                                                aLocalResponseAttachments);

            // Send UserMessage
            final AS4SigningParams aSigningParams = m_aIncomingSecurityConfig.getSigningParamsCloneOrNew ()
                                                                             .setFromPMode (aEffectiveLeg.getSecurity ());
            // Use the original receiver ID as the alias into the keystore for
            // encrypting the response message
            final String sEncryptionAlias = aEbmsUserMessage.getPartyInfo ().getTo ().getPartyIdAtIndex (0).getValue ();
            final AS4CryptParams aCryptParams = m_aIncomingSecurityConfig.getCryptParamsCloneOrNew ()
                                                                         .setFromPMode (aEffectiveLeg.getSecurity ())
                                                                         .setAlias (sEncryptionAlias);

            aAsyncResponseFactory = _createResponseUserMessage (aIncomingState,
                                                                aEffectiveLeg.getProtocol ().getSoapVersion (),
                                                                aResponseUserMsg,
                                                                aResponseAttachments,
                                                                aSigningParams,
                                                                aCryptParams);
          }
          else
          {
            // SPI processing failed

            // Send ErrorMessage Undefined - see
            // https://github.com/phax/phase4/issues/4
            final AS4ErrorMessage aResponseErrorMsg = AS4ErrorMessage.create (eSoapVersion,
                                                                              aIncomingState.getMessageID (),
                                                                              aLocalErrorMessages);
            sResponseMessageID = aResponseErrorMsg.getEbms3SignalMessage ().getMessageInfo ().getMessageId ();

            // Pass error messages to the outside
            if (m_aErrorConsumer != null && aLocalErrorMessages.isNotEmpty ())
              m_aErrorConsumer.onAS4ErrorMessage (aIncomingState, aLocalErrorMessages, aResponseErrorMsg);

            aAsyncResponseFactory = new AS4ResponseFactoryXML (m_aMessageMetadata,
                                                               aIncomingState,
                                                               sResponseMessageID,
                                                               aResponseErrorMsg.getAsSoapDocument (),
                                                               eSoapVersion.getMimeType ());
          }

          // where to send it back (must be determined by SPI!)
          final String sAsyncResponseURL = aAsyncSPIResult.getAsyncResponseURL ();
          if (StringHelper.hasNoText (sAsyncResponseURL))
            throw new IllegalStateException ("No asynchronous response URL present - please check your SPI implementation");

          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Responding asynchronous to: " + sAsyncResponseURL);

          // Ensure HttpEntity is repeatable
          HttpEntity aHttpEntity = aAsyncResponseFactory.getHttpEntityForSending (eSoapVersion.getMimeType ());
          aHttpEntity = m_aResHelper.createRepeatableHttpEntity (aHttpEntity);

          // Use the prebuilt entity for dumping
          _invokeSPIsForResponse (aIncomingState,
                                  aAsyncResponseFactory,
                                  aHttpEntity,
                                  eSoapVersion.getMimeType (),
                                  sResponseMessageID);

          // invoke client with new document
          final BasicHttpPoster aSender = new BasicHttpPoster ();
          final Document aAsyncResponse;
          if (true)
          {
            final HttpHeaderMap aResponseHttpHeaders = null;
            // TODO make async send parameters customizable
            final HttpRetrySettings aRetrySettings = new HttpRetrySettings ();
            aAsyncResponse = aSender.sendGenericMessageWithRetries (sAsyncResponseURL,
                                                                    aResponseHttpHeaders,
                                                                    aHttpEntity,
                                                                    sMessageID,
                                                                    aRetrySettings,
                                                                    new ResponseHandlerXml (),
                                                                    m_aOutgoingDumper,
                                                                    m_aRetryCallback);
          }
          else
          {
            aAsyncResponse = aSender.sendGenericMessage (sAsyncResponseURL,
                                                         null,
                                                         aHttpEntity,
                                                         new ResponseHandlerXml ());
          }
          AS4HttpDebug.debug ( () -> "SEND-RESPONSE [async sent] received: " +
                                     (aAsyncResponse == null ? "null"
                                                             : XMLWriter.getNodeAsString (aAsyncResponse,
                                                                                          AS4HttpDebug.getDebugXMLWriterSettings ())));
        };

        final CompletableFuture <Void> aFuture = PhotonWorkerPool.getInstance ()
                                                                 .runThrowing (CAS4.LIB_NAME + " async processing", r);

        if (m_aSoapProcessingFinalizedCB != null)
        {
          // Give the outside world the possibility to get notified when the
          // processing is done
          aFuture.thenRun ( () -> m_aSoapProcessingFinalizedCB.onProcessingFinalized (false));
        }
      }
    }

    // Try building error message
    final String sResponseMessageID;
    final IAS4ResponseFactory ret;
    if (aIncomingState.isSoapHeaderElementProcessingSuccessful () && aIncomingState.getEbmsError () != null)
    {
      // Processing was successful, and it is an incoming Ebms Error Message
      sResponseMessageID = null;
      ret = null;
    }
    else
    {
      // Either error in header processing or
      // not an incoming Ebms Error Message (either UserMessage or a different
      // SignalMessage)

      if (aEbmsErrorMessagesTarget.isNotEmpty ())
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Creating AS4 error message with these " +
                        aEbmsErrorMessagesTarget.size () +
                        " errors: " +
                        aEbmsErrorMessagesTarget.getAllMapped (x -> StringHelper.getConcatenatedOnDemand (x.getDescriptionValue (),
                                                                                                          " / ",
                                                                                                          x.getErrorDetail ())));

        // Generate ErrorMessage if errors in the process are present and the
        // pmode wants an error response
        // When aEffectiveLeg == null, the response is true
        if (_isSendErrorAsResponse (aEffectiveLeg))
        {
          sResponseMessageID = MessageHelperMethods.createRandomMessageID ();
          ret = _createResponseErrorMessage (aIncomingState,
                                             eSoapVersion,
                                             sResponseMessageID,
                                             aEffectiveLeg,
                                             aEbmsErrorMessagesTarget);
        }
        else
        {
          // Too bad - the error message gets dismissed
          LOGGER.warn ("Not sending back the AS4 Error response, because it is prohibited in the PMode");
          sResponseMessageID = null;
          ret = null;
        }
      }
      else
      {
        // No errors occurred

        if (aEbmsSignalMessage != null && aEbmsSignalMessage.getReceipt () != null)
        {
          // Do not respond to receipt (except with error message - see above)
          sResponseMessageID = null;
          ret = null;
        }
        else
        {
          // So now the incoming message is a user message or a pull request
          if (aPMode.getMEP ().isOneWay () || aPMode.getMEPBinding ().isAsynchronous ())
          {
            // If no Error is present check if pmode declared if they want a
            // response and if this response should contain non-repudiation
            // information if applicable
            // Only get in here if pull is part of the EMEPBinding, if it is two
            // way, we need to check if the current application is currently in
            // the pull phase
            if (aPMode.getMEPBinding ().equals (EMEPBinding.PULL) ||
                (aPMode.getMEPBinding ().equals (EMEPBinding.PULL_PUSH) && aSPIResult.hasPullReturnUserMsg ()) ||
                (aPMode.getMEPBinding ().equals (EMEPBinding.PUSH_PULL) && aSPIResult.hasPullReturnUserMsg ()))
            {
              // TODO would be nice to have attachments here I guess
              final AS4UserMessage aResponseUserMsg = new AS4UserMessage (eSoapVersion,
                                                                          aSPIResult.getPullReturnUserMsg ());

              sResponseMessageID = aResponseUserMsg.getEbms3UserMessage ().getMessageInfo ().getMessageId ();
              ret = new AS4ResponseFactoryXML (m_aMessageMetadata,
                                               aIncomingState,
                                               sResponseMessageID,
                                               aResponseUserMsg.getAsSoapDocument (),
                                               eSoapVersion.getMimeType ());
            }
            else
              if (aEbmsUserMessage != null)
              {
                // We received an incoming user message and no errors occurred
                final boolean bSendReceiptAsResponse = _isSendReceiptAsResponse (aEffectiveLeg);
                if (bSendReceiptAsResponse)
                {
                  sResponseMessageID = MessageHelperMethods.createRandomMessageID ();
                  ret = _createResponseReceiptMessage (aIncomingState,
                                                       aSoapDocument,
                                                       eSoapVersion,
                                                       sResponseMessageID,
                                                       aEffectiveLeg,
                                                       aEbmsUserMessage,
                                                       aResponseAttachments);
                }
                else
                {
                  // TODO what shall we send back here?
                  LOGGER.info ("Not sending back the Receipt response, because sending Receipt response is prohibited in PMode");
                  sResponseMessageID = null;
                  ret = null;
                }
              }
              else
              {
                sResponseMessageID = null;
                ret = null;
              }
          }
          else
          {
            // synchronous TWO - WAY (= "SYNC")
            final PModeLeg aLeg2 = aPMode.getLeg2 ();
            if (aLeg2 == null)
              throw new Phase4Exception ("PMode has no leg2!");

            if (MEPHelper.isValidResponseTypeLeg2 (aPMode.getMEP (),
                                                   aPMode.getMEPBinding (),
                                                   EAS4MessageType.USER_MESSAGE))
            {
              sResponseMessageID = MessageHelperMethods.createRandomMessageID ();
              final AS4UserMessage aResponseUserMsg = _createReversedUserMessage (eSoapVersion,
                                                                                  sResponseMessageID,
                                                                                  aEbmsUserMessage,
                                                                                  aResponseAttachments);

              final AS4SigningParams aSigningParams = m_aIncomingSecurityConfig.getSigningParamsCloneOrNew ()
                                                                               .setFromPMode (aLeg2.getSecurity ());
              final String sEncryptionAlias = aEbmsUserMessage.getPartyInfo ()
                                                              .getTo ()
                                                              .getPartyIdAtIndex (0)
                                                              .getValue ();
              final AS4CryptParams aCryptParams = m_aIncomingSecurityConfig.getCryptParamsCloneOrNew ()
                                                                           .setFromPMode (aLeg2.getSecurity ())
                                                                           .setAlias (sEncryptionAlias);
              ret = _createResponseUserMessage (aIncomingState,
                                                aLeg2.getProtocol ().getSoapVersion (),
                                                aResponseUserMsg,
                                                aResponseAttachments,
                                                aSigningParams,
                                                aCryptParams);
            }
            else
            {
              // Leg2 configuration does not allow to respond with a UserMessage
              sResponseMessageID = null;
              ret = null;
            }
          }
        }
      }
    }

    // Create the HttpEntity on demand
    _invokeSPIsForResponse (aIncomingState, ret, null, eSoapVersion.getMimeType (), sResponseMessageID);

    return ret;
  }

  /**
   * This is the main handling routine when called from an abstract
   * (non-Servlet) API
   *
   * @param aRequestInputStream
   *        The input stream with the raw AS4 request data. May not be
   *        <code>null</code>.
   * @param aRequestHttpHeaders
   *        The HTTP headers of the request. May not be <code>null</code>.
   * @param aHttpResponse
   *        The AS4 response abstraction to be filled. May not be
   *        <code>null</code>.
   * @throws Phase4Exception
   *         in case the request is missing certain prerequisites. Since 0.9.11
   * @throws IOException
   *         In case of IO errors
   * @throws MessagingException
   *         MIME related errors
   * @throws WSSecurityException
   *         In case of WSS4J errors
   * @see #handleRequest(InputStream, HttpHeaderMap, IAS4ResponseAbstraction)
   *      for a more generic API
   */
  public void handleRequest (@Nonnull @WillClose final InputStream aRequestInputStream,
                             @Nonnull final HttpHeaderMap aRequestHttpHeaders,
                             @Nonnull final IAS4ResponseAbstraction aHttpResponse) throws Phase4Exception,
                                                                                   IOException,
                                                                                   MessagingException,
                                                                                   WSSecurityException
  {
    final IAS4ParsedMessageCallback aCallback = (aHttpHeaders, aSoapDocument, eSoapVersion, aIncomingAttachments) -> {
      // SOAP document and SOAP version are determined
      // Collect all runtime errors
      final ICommonsList <Ebms3Error> aErrorMessages = new CommonsArrayList <> ();
      final IAS4ResponseFactory aResponder = _handleSoapMessage (aHttpHeaders,
                                                                 aSoapDocument,
                                                                 eSoapVersion,
                                                                 aIncomingAttachments,
                                                                 aErrorMessages);
      if (aResponder != null)
      {
        // Response present -> send back
        final IAS4OutgoingDumper aRealOutgoingDumper = m_aOutgoingDumper != null ? m_aOutgoingDumper
                                                                                 : AS4DumpManager.getOutgoingDumper ();
        aResponder.applyToResponse (aHttpResponse, aRealOutgoingDumper);
      }
      else
      {
        // Success, HTTP No Content
        aHttpResponse.setStatus (CHttp.HTTP_NO_CONTENT);
      }
      AS4HttpDebug.debug ( () -> "RECEIVE-END with " + (aResponder != null ? "EBMS message" : "no content"));
    };
    AS4IncomingHandler.parseAS4Message (m_aIncomingAttachmentFactory,
                                        m_aResHelper,
                                        m_aMessageMetadata,
                                        aRequestInputStream,
                                        aRequestHttpHeaders,
                                        aCallback,
                                        m_aIncomingDumper);
  }
}
