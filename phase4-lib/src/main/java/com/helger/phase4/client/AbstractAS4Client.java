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
package com.helger.phase4.client;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.string.StringHelper;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.commons.wrapper.Wrapper;
import com.helger.phase4.crypto.AS4CryptParams;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.messaging.http.BasicHttpPoster;
import com.helger.phase4.messaging.http.HttpRetrySettings;
import com.helger.phase4.messaging.http.IHttpPoster;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.EAS4MessageType;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.PModeReceptionAwareness;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.util.AS4ResourceHelper;

import jakarta.mail.MessagingException;

/**
 * Abstract AS4 client based on HTTP client
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        Implementation type
 */
public abstract class AbstractAS4Client <IMPLTYPE extends AbstractAS4Client <IMPLTYPE>> implements
                                        IGenericImplTrait <IMPLTYPE>
{
  /**
   * @return The default message ID factory to be used.
   * @since 0.8.3
   */
  @Nonnull
  public static Supplier <String> createDefaultMessageIDFactory ()
  {
    return MessageHelperMethods::createRandomMessageID;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractAS4Client.class);

  private final EAS4MessageType m_eMessageType;
  private final AS4ResourceHelper m_aResHelper;

  private IAS4CryptoFactory m_aCryptoFactorySign;
  private IAS4CryptoFactory m_aCryptoFactoryCrypt;
  private final AS4SigningParams m_aSigningParams = new AS4SigningParams ();
  private final AS4CryptParams m_aCryptParams = new AS4CryptParams ();

  private IHttpPoster m_aHttpPoster = new BasicHttpPoster ();

  // For Message Info
  private Supplier <String> m_aMessageIDFactory = createDefaultMessageIDFactory ();
  private String m_sRefToMessageID;
  private OffsetDateTime m_aSendingDateTime;
  private ESoapVersion m_eSoapVersion = ESoapVersion.AS4_DEFAULT;

  // Retry handling
  private final HttpRetrySettings m_aHttpRetrySettings = new HttpRetrySettings ();

  protected AbstractAS4Client (@Nonnull final EAS4MessageType eMessageType,
                               @Nonnull @WillNotClose final AS4ResourceHelper aResHelper)
  {
    ValueEnforcer.notNull (eMessageType, "MessageType");
    ValueEnforcer.notNull (aResHelper, "ResHelper");
    m_eMessageType = eMessageType;
    m_aResHelper = aResHelper;
  }

  /**
   * @return The message type handled by this client. Never <code>null</code>.
   * @since 0.12.0
   */
  @Nonnull
  public final EAS4MessageType getMessageType ()
  {
    return m_eMessageType;
  }

  /**
   * @return The resource helper provided in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  public final AS4ResourceHelper getAS4ResourceHelper ()
  {
    return m_aResHelper;
  }

  /**
   * @return The currently set crypto factory for signing. <code>null</code> by
   *         default.
   * @since 2.2.0
   */
  @Nullable
  public final IAS4CryptoFactory getCryptoFactorySign ()
  {
    return m_aCryptoFactorySign;
  }

  /**
   * Set the crypto factory to be used for signing.
   *
   * @param aCryptoFactorySign
   *        The crypto factory to be used. May be <code>null</code>.
   * @return this for chaining
   * @see #setCryptoFactoryCrypt(IAS4CryptoFactory)
   * @since 2.2.0
   */
  @Nonnull
  public final IMPLTYPE setCryptoFactorySign (@Nullable final IAS4CryptoFactory aCryptoFactorySign)
  {
    m_aCryptoFactorySign = aCryptoFactorySign;
    return thisAsT ();
  }

  /**
   * @return The currently set crypto factory for crypting. <code>null</code> by
   *         default.
   * @since 2.2.0
   */
  @Nullable
  public final IAS4CryptoFactory getCryptoFactoryCrypt ()
  {
    return m_aCryptoFactoryCrypt;
  }

  /**
   * Set the crypto factory to be used for crypting.
   *
   * @param aCryptoFactoryCrypt
   *        The crypto factory to be used. May be <code>null</code>.
   * @return this for chaining
   * @see #setCryptoFactorySign(IAS4CryptoFactory)
   * @since 2.2.0
   */
  @Nonnull
  public final IMPLTYPE setCryptoFactoryCrypt (@Nullable final IAS4CryptoFactory aCryptoFactoryCrypt)
  {
    m_aCryptoFactoryCrypt = aCryptoFactoryCrypt;
    return thisAsT ();
  }

  /**
   * Set all the crypto properties at once.
   *
   * @param aCryptoFactory
   *        The crypto factory to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE setCryptoFactory (@Nullable final IAS4CryptoFactory aCryptoFactory)
  {
    return setCryptoFactorySign (aCryptoFactory).setCryptoFactoryCrypt (aCryptoFactory);
  }

  /**
   * @return The signing algorithm to use. Never <code>null</code>.
   * @since 0.9.0
   */
  @Nonnull
  @ReturnsMutableObject
  public final AS4SigningParams signingParams ()
  {
    return m_aSigningParams;
  }

  /**
   * @return The encrypt and decrypt parameters to use. Never null
   *         <code>null</code>.
   * @since 0.9.0
   */
  @Nonnull
  @ReturnsMutableObject
  public final AS4CryptParams cryptParams ()
  {
    return m_aCryptParams;
  }

  /**
   * @return The underlying HTTP poster to use. May not be <code>null</code>.
   * @since 0.13.0
   */
  @Nonnull
  public final IHttpPoster getHttpPoster ()
  {
    return m_aHttpPoster;
  }

  /**
   * Set the HTTP poster to be used. This is the instance that is responsible
   * for the HTTP transmission of the AS4 messages.
   *
   * @param aHttpPoster
   *        Instance to be used. May not be <code>null</code>.
   * @return this for chaining
   * @since 0.13.0
   */
  @Nonnull
  public final IMPLTYPE setHttpPoster (@Nonnull final IHttpPoster aHttpPoster)
  {
    ValueEnforcer.notNull (aHttpPoster, "HttpPoster");
    m_aHttpPoster = aHttpPoster;
    return thisAsT ();
  }

  /**
   * @return The Message ID factory to be used. May not be <code>null</code>.
   */
  @Nonnull
  public final Supplier <String> getMessageIDFactory ()
  {
    return m_aMessageIDFactory;
  }

  /**
   * Set a constant message ID
   *
   * @param sMessageID
   *        Message to be used. May neither be <code>null</code> nor empty.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE setMessageID (@Nonnull @Nonempty final String sMessageID)
  {
    ValueEnforcer.notEmpty (sMessageID, "MessageID");
    return setMessageIDFactory ( () -> sMessageID);
  }

  /**
   * Set the factory that creates message IDs. By default a random UUID is used.
   *
   * @param aMessageIDFactory
   *        Factory to be used. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE setMessageIDFactory (@Nonnull final Supplier <String> aMessageIDFactory)
  {
    ValueEnforcer.notNull (aMessageIDFactory, "MessageIDFactory");
    m_aMessageIDFactory = aMessageIDFactory;
    return thisAsT ();
  }

  /**
   * @return A new message ID created by the contained factory. Neither
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public final String createMessageID ()
  {
    final String ret = m_aMessageIDFactory.get ();
    if (StringHelper.hasNoText (ret))
      throw new IllegalStateException ("The contained MessageID factory created an empty MessageID!");
    return ret;
  }

  /**
   * @return The AS4 reference to the original message. My be <code>null</code>.
   */
  @Nullable
  public final String getRefToMessageID ()
  {
    return m_sRefToMessageID;
  }

  /**
   * @return <code>true</code> if an AS4 reference to the original message
   *         exists.
   */
  public final boolean hasRefToMessageID ()
  {
    return StringHelper.hasText (m_sRefToMessageID);
  }

  /**
   * Set the reference to the original AS4 message.
   *
   * @param sRefToMessageID
   *        The Message ID of the original AS4 message. May be
   *        <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE setRefToMessageID (@Nullable final String sRefToMessageID)
  {
    m_sRefToMessageID = sRefToMessageID;
    return thisAsT ();
  }

  /**
   * @return The sending time stamp of the message. If this is <code>null</code>
   *         the current time should be used in the EBMS messages.
   * @since 0.12.0
   */
  @Nullable
  public final OffsetDateTime getSendingDateTime ()
  {
    return m_aSendingDateTime;
  }

  /**
   * Ensure the sending date time is set. If it is already set, nothing happens,
   * else it is set to the current point in time.
   *
   * @return this for chaining Never <code>null</code>.
   * @since 2.2.2
   */
  @Nonnull
  public final IMPLTYPE ensureSendingDateTime ()
  {
    if (m_aSendingDateTime == null)
      m_aSendingDateTime = MetaAS4Manager.getTimestampMgr ().getCurrentDateTime ();
    return thisAsT ();
  }

  /**
   * Set the sending date time of the AS4 message. If not set, the current point
   * in time will be used onwards.
   *
   * @param aSendingDateTime
   *        The sending date time to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE setSendingDateTime (@Nullable final OffsetDateTime aSendingDateTime)
  {
    m_aSendingDateTime = aSendingDateTime;
    return thisAsT ();
  }

  /**
   * @return The SOAP version to be used. May not be <code>null</code>.
   * @since v0.9.8
   */
  @Nonnull
  public final ESoapVersion getSoapVersion ()
  {
    return m_eSoapVersion;
  }

  /**
   * This method sets the SOAP Version. AS4 - Profile default is SOAP 1.2
   *
   * @param eSoapVersion
   *        SOAP version which should be set. May not be <code>null</code>.
   * @return this for chaining
   * @since v0.9.8
   */
  @Nonnull
  public final IMPLTYPE setSoapVersion (@Nonnull final ESoapVersion eSoapVersion)
  {
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");
    m_eSoapVersion = eSoapVersion;
    return thisAsT ();
  }

  /**
   * @return The HTTP retry settings to be used. Never <code>null</code>. Modify
   *         the response object.
   * @since 0.13.0
   */
  @Nonnull
  public final HttpRetrySettings httpRetrySettings ()
  {
    return m_aHttpRetrySettings;
  }

  @Nonnull
  protected IAS4CryptoFactory internalGetCryptoFactorySign ()
  {
    if (m_aCryptoFactorySign == null)
      throw new IllegalStateException ("No CryptoFactory for signing is configured.");

    return m_aCryptoFactorySign;
  }

  @Nonnull
  protected IAS4CryptoFactory internalGetCryptoFactoryCrypt ()
  {
    if (m_aCryptoFactoryCrypt == null)
      throw new IllegalStateException ("No CryptoFactory for crypting is configured.");

    return m_aCryptoFactoryCrypt;
  }

  public final void setValuesFromPMode (@Nullable final IPMode aPMode, @Nullable final PModeLeg aLeg)
  {
    if (aPMode != null)
    {
      final PModeReceptionAwareness aRA = aPMode.getReceptionAwareness ();
      if (aRA != null && aRA.isRetryDefined ())
      {
        m_aHttpRetrySettings.setMaxRetries (aRA.getMaxRetries ());
        m_aHttpRetrySettings.setDurationBeforeRetry (Duration.ofMillis (aRA.getRetryIntervalMS ()));
      }
      else
      {
        // 0 means "no retries"
        m_aHttpRetrySettings.setMaxRetries (0);
      }
    }
    if (aLeg != null)
    {
      signingParams ().setFromPMode (aLeg.getSecurity ());
      cryptParams ().setFromPMode (aLeg.getSecurity ());
    }
  }

  /**
   * Build the AS4 message to be sent. It uses all the attributes of this class
   * to build the final message. Compression, signing and encryption happens in
   * this methods.
   *
   * @param sMessageID
   *        The message ID to be used. Neither <code>null</code> nor empty.
   * @param aCallback
   *        Optional callback for in-between states. May be <code>null</code>.
   * @return The HTTP entity to be sent. Never <code>null</code>.
   * @throws IOException
   *         in case of an IO error
   * @throws WSSecurityException
   *         In case there is an issue with signing/encryption
   * @throws MessagingException
   *         in case something happens in MIME wrapping
   */
  @Nonnull
  public abstract AS4ClientBuiltMessage buildMessage (@Nonnull @Nonempty String sMessageID,
                                                      @Nullable IAS4ClientBuildMessageCallback aCallback) throws IOException,
                                                                                                          WSSecurityException,
                                                                                                          MessagingException;

  /**
   * Send the AS4 client message created by
   * {@link #buildMessage(String, IAS4ClientBuildMessageCallback)} to the
   * provided URL. This methods does take retries into account. It synchronously
   * handles the retries and only returns after the last retry.
   *
   * @param <T>
   *        The response data type
   * @param sURL
   *        The URL to send the HTTP POST to
   * @param aResponseHandler
   *        The response handler that converts the HTTP response to a domain
   *        object. May not be <code>null</code>.
   * @param aCallback
   *        An optional callback for the different stages of building the
   *        document. May be <code>null</code>.
   * @param aOutgoingDumper
   *        An outgoing dumper to be used. Maybe <code>null</code>. If
   *        <code>null</code> the global outgoing dumper from
   *        {@link AS4DumpManager} is used.
   * @param aRetryCallback
   *        An optional callback to be invoked if a retry happens on HTTP level.
   *        May be <code>null</code>.
   * @return The sent message that contains
   * @throws IOException
   *         in case of error when building or sending the message
   * @throws WSSecurityException
   *         In case there is an issue with signing/encryption
   * @throws MessagingException
   *         in case something happens in MIME wrapping
   * @since 0.9.14
   */
  @Nonnull
  public final <T> AS4ClientSentMessage <T> sendMessageWithRetries (@Nonnull final String sURL,
                                                                    @Nonnull final HttpClientResponseHandler <? extends T> aResponseHandler,
                                                                    @Nullable final IAS4ClientBuildMessageCallback aCallback,
                                                                    @Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                                                    @Nullable final IAS4RetryCallback aRetryCallback) throws IOException,
                                                                                                                      WSSecurityException,
                                                                                                                      MessagingException
  {
    // Create a new message ID for each build!
    final String sMessageID = createMessageID ();
    final AS4ClientBuiltMessage aBuiltMsg = buildMessage (sMessageID, aCallback);
    HttpEntity aBuiltEntity = aBuiltMsg.getHttpEntity ();
    HttpHeaderMap aBuiltHttpHeaders = aBuiltMsg.getAllCustomHttpHeaders ();
    if (false)
    {
      // Test custom header only
      if (aBuiltHttpHeaders == null)
        aBuiltHttpHeaders = new HttpHeaderMap ();
      aBuiltHttpHeaders.addHeader ("X-UseEcsProxy", "1");
    }

    LOGGER.info ("phase4 --- sending.withretries:start");

    if (m_aHttpRetrySettings.isRetryEnabled () ||
        aOutgoingDumper != null ||
        AS4DumpManager.getOutgoingDumper () != null)
    {
      // Ensure a repeatable entity is provided
      aBuiltEntity = m_aResHelper.createRepeatableHttpEntity (aBuiltEntity);
    }

    // Keep the HTTP response status line for external evaluation
    final Wrapper <StatusLine> aStatusLineKeeper = new Wrapper <> ();

    // Keep the HTTP response headers for external evaluation
    final HttpHeaderMap aResponseHeaders = new HttpHeaderMap ();

    final HttpClientResponseHandler <T> aRealResponseHandler = resp -> {
      // Remember the HTTP response data
      aStatusLineKeeper.set (new StatusLine (resp));

      final Header [] aHeaders = resp.getHeaders ();
      if (aHeaders != null)
        for (final Header aHeader : aHeaders)
          aResponseHeaders.addHeader (aHeader.getName (), aHeader.getValue ());

      // Call the original handler
      return aResponseHandler.handleResponse (resp);
    };
    final T aResponseContent = m_aHttpPoster.sendGenericMessageWithRetries (sURL,
                                                                            aBuiltHttpHeaders,
                                                                            aBuiltEntity,
                                                                            sMessageID,
                                                                            m_aHttpRetrySettings,
                                                                            aRealResponseHandler,
                                                                            aOutgoingDumper,
                                                                            aRetryCallback);
    final AS4ClientSentMessage <T> ret = new AS4ClientSentMessage <> (aBuiltMsg,
                                                                      aStatusLineKeeper.get (),
                                                                      aResponseHeaders,
                                                                      aResponseContent);

    LOGGER.info ("phase4 --- sending.withretries:end");

    return ret;
  }
}
