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
package com.helger.phase4.sender;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientSettings;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.client.IAS4RetryCallback;
import com.helger.phase4.crypto.AS4CryptParams;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.crypto.IAS4DecryptParameterModifier;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.incoming.AS4IncomingProfileSelectorConstant;
import com.helger.phase4.incoming.IAS4IncomingProfileSelector;
import com.helger.phase4.messaging.http.HttpRetrySettings;
import com.helger.phase4.messaging.http.IHttpPoster;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.pmode.resolve.AS4DefaultPModeResolver;
import com.helger.phase4.model.pmode.resolve.IAS4PModeResolver;
import com.helger.phase4.util.Phase4Exception;

/**
 * Abstract builder base class with the requirements for all message types.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        The implementation type
 * @since 0.10.0
 */
@NotThreadSafe
public abstract class AbstractAS4MessageBuilder <IMPLTYPE extends AbstractAS4MessageBuilder <IMPLTYPE>> implements
                                                IGenericImplTrait <IMPLTYPE>
{
  public static final Locale DEFAULT_LOCALE = Locale.US;

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractAS4MessageBuilder.class);

  protected IHttpPoster m_aCustomHttpPoster;
  protected HttpClientFactory m_aHttpClientFactory;
  protected IAS4CryptoFactory m_aCryptoFactorySign;
  protected IAS4CryptoFactory m_aCryptoFactoryCrypt;
  protected final AS4SigningParams m_aSigningParams = new AS4SigningParams ();
  protected final AS4CryptParams m_aCryptParams = new AS4CryptParams ();
  protected String m_sMessageID;
  protected String m_sRefToMessageID;
  protected OffsetDateTime m_aSendingDateTime;
  protected ESoapVersion m_eSoapVersion;
  protected HttpRetrySettings m_aHttpRetrySettings;
  protected Locale m_aLocale = DEFAULT_LOCALE;
  protected String m_sAS4ProfileID;

  private IAS4PModeResolver m_aPModeResolver;
  private IAS4IncomingAttachmentFactory m_aIAF;
  private IAS4IncomingProfileSelector m_aIncomingProfileSelector;
  private IAS4SenderInterrupt m_aSenderInterrupt;

  protected IAS4SendingDateTimeConsumer m_aSendingDTConsumer;
  protected IAS4ClientBuildMessageCallback m_aBuildMessageCallback;
  protected IAS4OutgoingDumper m_aOutgoingDumper;
  protected IAS4IncomingDumper m_aIncomingDumper;
  protected IAS4DecryptParameterModifier m_aDecryptParameterModifier;
  protected IAS4RetryCallback m_aRetryCallback;
  protected IAS4RawResponseConsumer m_aResponseConsumer;

  /**
   * Create a new builder, with the following fields already set:<br>
   * {@link #httpClientFactory(HttpClientFactory)}<br>
   * {@link #cryptoFactory(IAS4CryptoFactory)}<br>
   * {@link #soapVersion(ESoapVersion)}
   * {@link #incomingAttachmentFactory(IAS4IncomingAttachmentFactory)}<br>
   */
  protected AbstractAS4MessageBuilder ()
  {
    // Set default values
    try
    {
      httpClientFactory (new HttpClientFactory ());
      // By default set the same for sign and crypt
      cryptoFactory (AS4CryptoFactoryConfiguration.getDefaultInstanceOrNull ());
      soapVersion (ESoapVersion.SOAP_12);
      incomingAttachmentFactory (IAS4IncomingAttachmentFactory.DEFAULT_INSTANCE);
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to init AbstractAS4MessageBuilder", ex);
    }
  }

  /**
   * @return The currently set {@link IHttpPoster}. May be <code>null</code>.
   * @since 1.3.10
   */
  @Nullable
  public final IHttpPoster customHttpPoster ()
  {
    return m_aCustomHttpPoster;
  }

  /**
   * Set the HTTP poster to be used. This is a very low level API and should
   * only be used if you know what you are doing! It allows you to overwrite how
   * the message is sent over the wire.<br>
   * Note: if this method is used with a non-<code>null</code> parameter,
   * {@link #httpClientFactory()} becomes useless
   *
   * @param aCustomHttpPoster
   *        The new HTTP poster to be used. May be <code>null</code> which means
   *        "use the default" poster.
   * @return this for chaining
   * @since 1.3.10
   */
  @Nonnull
  public final IMPLTYPE customHttpPoster (@Nullable final IHttpPoster aCustomHttpPoster)
  {
    m_aCustomHttpPoster = aCustomHttpPoster;
    return thisAsT ();
  }

  /**
   * @return The currently set {@link HttpClientFactory}. May be
   *         <code>null</code>.
   */
  @Nullable
  public final HttpClientFactory httpClientFactory ()
  {
    return m_aHttpClientFactory;
  }

  /**
   * Set the HTTP client factory to be used. If the passed settings are
   * non-<code>null</code>, a new {@link HttpClientFactory} is created with
   * them, else a <code>null</code>-{@link HttpClientFactory} is set.
   *
   * @param aHttpClientSettings
   *        The new HTTP client settings to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE httpClientFactory (@Nullable final HttpClientSettings aHttpClientSettings)
  {
    return httpClientFactory (aHttpClientSettings == null ? null : new HttpClientFactory (aHttpClientSettings));
  }

  /**
   * Set the HTTP client factory to be used. By default a default instance of
   * {@link HttpClientFactory} is used (set in the constructor) and there is no
   * need to invoke this method.
   *
   * @param aHttpClientFactory
   *        The new HTTP client factory to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE httpClientFactory (@Nullable final HttpClientFactory aHttpClientFactory)
  {
    m_aHttpClientFactory = aHttpClientFactory;
    return thisAsT ();
  }

  /**
   * @return The currently set {@link IAS4CryptoFactory} for signing. May be
   *         <code>null</code>.
   * @see #cryptoFactoryCrypt()
   * @since 2.2.0
   */
  @Nullable
  public final IAS4CryptoFactory cryptoFactorySign ()
  {
    return m_aCryptoFactorySign;
  }

  /**
   * Set the crypto factory to be used for signing. The default crypto factory
   * is set in the constructor to
   * {@link AS4CryptoFactoryConfiguration#getDefaultInstance()}.
   *
   * @param aCryptoFactorySign
   *        The crypto factory to be used. May be <code>null</code>.
   * @return this for chaining
   * @since 2.2.0
   */
  @Nonnull
  public final IMPLTYPE cryptoFactorySign (@Nullable final IAS4CryptoFactory aCryptoFactorySign)
  {
    m_aCryptoFactorySign = aCryptoFactorySign;
    return thisAsT ();
  }

  /**
   * @return The currently set {@link IAS4CryptoFactory} for crypting. May be
   *         <code>null</code>.
   * @see #cryptoFactorySign()
   * @since 2.2.0
   */
  @Nullable
  public final IAS4CryptoFactory cryptoFactoryCrypt ()
  {
    return m_aCryptoFactoryCrypt;
  }

  /**
   * Set the crypto factory to be used for crypting. The default crypto factory
   * is set in the constructor to
   * {@link AS4CryptoFactoryConfiguration#getDefaultInstanceOrNull()}.
   *
   * @param aCryptoFactoryCrypt
   *        The crypto factory to be used. May be <code>null</code>.
   * @return this for chaining
   * @since 2.2.0
   */
  @Nonnull
  public final IMPLTYPE cryptoFactoryCrypt (@Nullable final IAS4CryptoFactory aCryptoFactoryCrypt)
  {
    m_aCryptoFactoryCrypt = aCryptoFactoryCrypt;
    return thisAsT ();
  }

  /**
   * Set the crypto factory to be used for signing and crypting. The default
   * crypto factory is set in the constructor to
   * {@link AS4CryptoFactoryConfiguration#getDefaultInstanceOrNull()}.
   *
   * @param aCryptoFactory
   *        The crypto factory to be used. May be <code>null</code>.
   * @return this for chaining
   * @see #cryptoFactorySign(IAS4CryptoFactory)
   * @see #cryptoFactoryCrypt(IAS4CryptoFactory)
   */
  @Nonnull
  public final IMPLTYPE cryptoFactory (@Nullable final IAS4CryptoFactory aCryptoFactory)
  {
    return cryptoFactorySign (aCryptoFactory).cryptoFactoryCrypt (aCryptoFactory);
  }

  /**
   * Get the mutable AS4 signing parameters.
   *
   * @return The AS4 signing parameters to use for this message. Never
   *         <code>null</code>.
   * @see #withSigningParams(Consumer)
   * @see #cryptParams()
   * @see #withCryptParams(Consumer)
   * @since 2.1.4
   */
  @Nonnull
  @ReturnsMutableObject
  public final AS4SigningParams signingParams ()
  {
    return m_aSigningParams;
  }

  /**
   * Modify the AS4 signing parameters for this message. This is a version that
   * maintains chainability of the API.
   *
   * @param aConsumer
   *        Consumer for the AS4 signing parameters to use for this message.
   *        Must not be <code>null</code>.
   * @return this for chaining
   * @see #signingParams()
   * @see #cryptParams()
   * @see #withCryptParams(Consumer)
   * @since 2.1.4
   */
  @Nonnull
  @ReturnsMutableObject
  public final IMPLTYPE withSigningParams (@Nonnull final Consumer <? super AS4SigningParams> aConsumer)
  {
    ValueEnforcer.notNull (aConsumer, "Consumer");

    aConsumer.accept (m_aSigningParams);
    return thisAsT ();
  }

  /**
   * Get the mutable AS4 crypt parameters.
   *
   * @return The AS4 crypt parameters to use for this message. Never
   *         <code>null</code>.
   * @see #signingParams()
   * @see #withSigningParams(Consumer)
   * @see #withCryptParams(Consumer)
   * @since 2.1.4
   */
  @Nonnull
  @ReturnsMutableObject
  public final AS4CryptParams cryptParams ()
  {
    return m_aCryptParams;
  }

  /**
   * Modify the AS4 crypt parameters for this message. This is a version that
   * maintains chainability of the API.
   *
   * @param aConsumer
   *        Consumer for the AS4 crypt parameters to use for this message. Must
   *        not be <code>null</code>.
   * @return this for chaining
   * @see #signingParams()
   * @see #withSigningParams(Consumer)
   * @see #cryptParams()
   * @since 2.1.4
   */
  @Nonnull
  @ReturnsMutableObject
  public final IMPLTYPE withCryptParams (@Nonnull final Consumer <? super AS4CryptParams> aConsumer)
  {
    ValueEnforcer.notNull (aConsumer, "Consumer");

    aConsumer.accept (m_aCryptParams);
    return thisAsT ();
  }

  /**
   * @return The specific AS4 message ID to use. May be <code>null</code> if a
   *         random one should be generated.
   * @since 3.0.0
   */
  @Nullable
  public final String messageID ()
  {
    return m_sMessageID;
  }

  /**
   * Set the optional AS4 message ID. If this field is not set, a random message
   * ID is created.
   *
   * @param sMessageID
   *        The optional AS4 message ID to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE messageID (@Nullable final String sMessageID)
  {
    m_sMessageID = sMessageID;
    return thisAsT ();
  }

  /**
   * @return The optional AS4 reference to a previous message ID. May be
   *         <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String refToMessageID ()
  {
    return m_sRefToMessageID;
  }

  /**
   * Set the optional AS4 reference to a previous message ID. If this field is
   * not set, it will not be emitted in the message.
   *
   * @param sRefToMessageID
   *        The optional AS4 reference to a previous message ID to be used. May
   *        be <code>null</code>.
   * @return this for chaining
   * @since 1.3.2
   */
  @Nonnull
  public final IMPLTYPE refToMessageID (@Nullable final String sRefToMessageID)
  {
    m_sRefToMessageID = sRefToMessageID;
    return thisAsT ();
  }

  /**
   * @return The optional sending date time. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final OffsetDateTime sendingDateTime ()
  {
    return m_aSendingDateTime;
  }

  /**
   * Set the optional sending date time. If no time is specified, the current
   * date time will be used.
   *
   * @param aSendingDateTime
   *        The sending date time to set. May be <code>null</code>.
   * @return this for chaining
   * @since 0.12.0
   */
  @Nonnull
  public final IMPLTYPE sendingDateTime (@Nullable final OffsetDateTime aSendingDateTime)
  {
    m_aSendingDateTime = aSendingDateTime;
    return thisAsT ();
  }

  /**
   * @return The SOAP version to be used. May be <code>null</code>.
   */
  @Nullable
  public final ESoapVersion soapVersion ()
  {
    return m_eSoapVersion;
  }

  /**
   * Set the SOAP version to be used. The default is SOAP 1.2 and is set in the
   * constructor. Usually you don't need to call that method.
   *
   * @param eSoapVersion
   *        The SOAP version to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE soapVersion (@Nullable final ESoapVersion eSoapVersion)
  {
    m_eSoapVersion = eSoapVersion;
    return thisAsT ();
  }

  /**
   * @return The HTTP retry settings to be used. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final HttpRetrySettings httpRetrySettings ()
  {
    return m_aHttpRetrySettings;
  }

  /**
   * Set the HTTP retry settings to be used. If none are set, the default values
   * are used.
   *
   * @param a
   *        The HTTP retry settings to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE httpRetrySettings (@Nullable final HttpRetrySettings a)
  {
    m_aHttpRetrySettings = a;
    return thisAsT ();
  }

  /**
   * @return The locale to be used. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final Locale locale ()
  {
    return m_aLocale;
  }

  /**
   * Set the locale to use. The main purpose is to use the correct language for
   * processing error message in response messages. This field must be set. The
   * default value is {@link #DEFAULT_LOCALE}.
   *
   * @param a
   *        The locale to use. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE locale (@Nullable final Locale a)
  {
    m_aLocale = a;
    return thisAsT ();
  }

  /**
   * @return The selected AS4 profile to use. May be <code>null</code>.
   * @since 2.8.2
   */
  @Nullable
  public final String as4ProfileID ()
  {
    return m_sAS4ProfileID;
  }

  /**
   * Set the AS4 profile to be used. Must be provided for the builder to work.
   *
   * @param sAS4ProfileID
   *        The AS4 profile ID to be used.
   * @return this for chaining
   * @since 2.8.2
   */
  @Nonnull
  public final IMPLTYPE as4ProfileID (@Nullable final String sAS4ProfileID)
  {
    m_sAS4ProfileID = sAS4ProfileID;
    return thisAsT ();
  }

  /**
   * @return The currently set {@link IAS4PModeResolver}. May be
   *         <code>null</code>.
   */
  @Nullable
  public final IAS4PModeResolver pmodeResolver ()
  {
    return m_aPModeResolver;
  }

  /**
   * Set the PMode resolver to be used. This is only used to determine the PMode
   * of an eventually received synchronous response message.
   *
   * @param aPModeResolver
   *        The PMode resolver to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE pmodeResolver (@Nullable final IAS4PModeResolver aPModeResolver)
  {
    m_aPModeResolver = aPModeResolver;
    return thisAsT ();
  }

  /**
   * @return The currently set {@link IAS4IncomingAttachmentFactory}. Never
   *         <code>null</code>.
   */
  @Nonnull
  public final IAS4IncomingAttachmentFactory incomingAttachmentFactory ()
  {
    return m_aIAF;
  }

  /**
   * Set the incoming attachment factory to be used. This is only used for an
   * eventually received synchronous response message.
   *
   * @param aIAF
   *        The incoming attachment factory to be used. May not be
   *        <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE incomingAttachmentFactory (@Nonnull final IAS4IncomingAttachmentFactory aIAF)
  {
    ValueEnforcer.notNull (aIAF, "IncomingAttachmentFactory");
    m_aIAF = aIAF;
    return thisAsT ();
  }

  /**
   * @return The profile selector for incoming AS4 messages. Never
   *         <code>null</code>.
   * @since 0.13.0
   */
  @Nonnull
  public final IAS4IncomingProfileSelector incomingProfileSelector ()
  {
    return m_aIncomingProfileSelector;
  }

  /**
   * Set the selector for the AS4 profile of incoming messages. This is only
   * used for an eventually received synchronous response message.
   *
   * @param aIncomingProfileSelector
   *        The profile selector to use. May not be <code>null</code>.
   * @return this for chaining
   * @since 0.13.0
   */
  @Nonnull
  public final IMPLTYPE incomingProfileSelector (@Nonnull final IAS4IncomingProfileSelector aIncomingProfileSelector)
  {
    ValueEnforcer.notNull (aIncomingProfileSelector, "IncomingProfileSelector");
    m_aIncomingProfileSelector = aIncomingProfileSelector;
    return thisAsT ();
  }

  /**
   * @return The currently set {@link IAS4SenderInterrupt}. May be
   *         <code>null</code>.
   * @since 0.13.0
   */
  @Nullable
  public final IAS4SenderInterrupt senderInterrupt ()
  {
    return m_aSenderInterrupt;
  }

  /**
   * Set the sender interrupt to be used. This is only needed in very specific
   * cases, is <code>null</code> by default and should be handled with care.
   *
   * @param aSenderInterrupt
   *        The sender interrupt to be used. May be <code>null</code>.
   * @return this for chaining
   * @since 0.13.0
   */
  @Nonnull
  public final IMPLTYPE senderInterrupt (@Nullable final IAS4SenderInterrupt aSenderInterrupt)
  {
    m_aSenderInterrupt = aSenderInterrupt;
    return thisAsT ();
  }

  /**
   * @return The currently set {@link IAS4SendingDateTimeConsumer}. May be
   *         <code>null</code>.
   * @since 2.2.2
   */
  @Nullable
  public final IAS4SendingDateTimeConsumer sendingDateTimeConsumer ()
  {
    return m_aSendingDTConsumer;
  }

  /**
   * Set the sending date time consumer to be used. This may e.g. be needed to
   * get the effective sending date time for Peppol reporting.
   *
   * @param aSendingDTConsumer
   *        The sender date time consumer to be used. May be <code>null</code>.
   * @return this for chaining
   * @since 2.2.2
   */
  @Nonnull
  public final IMPLTYPE sendingDateTimeConsumer (@Nullable final IAS4SendingDateTimeConsumer aSendingDTConsumer)
  {
    m_aSendingDTConsumer = aSendingDTConsumer;
    return thisAsT ();
  }

  /**
   * @return The internal message callback. Usually this method is NOT needed.
   *         Use only when you know what you are doing.
   * @since 3.0.0
   */
  @Nullable
  public final IAS4ClientBuildMessageCallback buildMessageCallback ()
  {
    return m_aBuildMessageCallback;
  }

  /**
   * Set a internal message callback. Usually this method is NOT needed. Use
   * only when you know what you are doing.
   *
   * @param aBuildMessageCallback
   *        An internal to be used for the created message. May be
   *        <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE buildMessageCallback (@Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback)
  {
    m_aBuildMessageCallback = aBuildMessageCallback;
    return thisAsT ();
  }

  /**
   * @return The specific outgoing dumper of this builder. May be
   *         <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final IAS4OutgoingDumper outgoingDumper ()
  {
    return m_aOutgoingDumper;
  }

  /**
   * Set a specific outgoing dumper for this builder.
   *
   * @param aOutgoingDumper
   *        An outgoing dumper to be used. Maybe <code>null</code>. If
   *        <code>null</code> the global outgoing dumper is used.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE outgoingDumper (@Nullable final IAS4OutgoingDumper aOutgoingDumper)
  {
    m_aOutgoingDumper = aOutgoingDumper;
    return thisAsT ();
  }

  /**
   * @return The specific incoming dumper of this builder. May be
   *         <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final IAS4IncomingDumper incomingDumper ()
  {
    return m_aIncomingDumper;
  }

  /**
   * Set a specific incoming dumper for this builder.
   *
   * @param aIncomingDumper
   *        An incoming dumper to be used. Maybe <code>null</code>. If
   *        <code>null</code> the global incoming dumper is used.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE incomingDumper (@Nullable final IAS4IncomingDumper aIncomingDumper)
  {
    m_aIncomingDumper = aIncomingDumper;
    return thisAsT ();
  }

  /**
   * @return The decrypting customizing callback. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final IAS4DecryptParameterModifier decryptRequestDataModifier ()
  {
    return m_aDecryptParameterModifier;
  }

  /**
   * Set an optional customizing callback that is invoked when decrypting a
   * message, to be able to modify the decryption configuration. This is an edge
   * case e.g. to allow RSA 1.5 algorithm names.
   *
   * @param aDecryptParameterModifier
   *        The modifier callback. May be <code>null</code>.
   * @return this for chaining
   * @since 2.2.0
   */
  @Nonnull
  public final IMPLTYPE decryptRequestDataModifier (@Nullable final IAS4DecryptParameterModifier aDecryptParameterModifier)
  {
    m_aDecryptParameterModifier = aDecryptParameterModifier;
    return thisAsT ();
  }

  /**
   * @return The HTTP retry callback to be invoked. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final IAS4RetryCallback retryCallback ()
  {
    return m_aRetryCallback;
  }

  /**
   * Set an optional handler that is notified if an http sending will be
   * retried. This method is optional and must not be called prior to sending.
   *
   * @param aRetryCallback
   *        The optional retry callback. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE retryCallback (@Nullable final IAS4RetryCallback aRetryCallback)
  {
    m_aRetryCallback = aRetryCallback;
    return thisAsT ();
  }

  /**
   * @return The optional handler for the received raw response, very similar to
   *         the dumper. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final IAS4RawResponseConsumer rawResponseConsumer ()
  {
    return m_aResponseConsumer;
  }

  /**
   * Set an optional handler for the synchronous result message received from
   * the other side. This method is optional and must not be called prior to
   * sending. This method is very similar to using an
   * {@link #incomingDumper(IAS4IncomingDumper)} so you usually only need one or
   * the other.
   *
   * @param aResponseConsumer
   *        The optional response consumer. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE rawResponseConsumer (@Nullable final IAS4RawResponseConsumer aResponseConsumer)
  {
    m_aResponseConsumer = aResponseConsumer;
    return thisAsT ();
  }

  /**
   * Internal method that is invoked before the required field check is
   * performed. Override to set additional dynamically created fields if
   * necessary.<br>
   * Don't add message properties in here, because if the required fields check
   * fails than this method would be called again.<br>
   * This is called before {@link #isEveryRequiredFieldSet()}
   *
   * @return {@link ESuccess} - never <code>null</code>. Returning failure here
   *         stops sending the message.
   * @throws Phase4Exception
   *         if something goes wrong
   */
  @OverrideOnDemand
  @OverridingMethodsMustInvokeSuper
  protected ESuccess finishFields () throws Phase4Exception
  {
    if (StringHelper.hasText (m_sAS4ProfileID))
    {
      if (m_aPModeResolver == null)
        pmodeResolver (new AS4DefaultPModeResolver (m_sAS4ProfileID));
      if (m_aIncomingProfileSelector == null)
        incomingProfileSelector (new AS4IncomingProfileSelectorConstant (m_sAS4ProfileID));
    }

    return ESuccess.SUCCESS;
  }

  /**
   * Check if all mandatory fields are set. This method is called after
   * {@link #finishFields()} and before {@link #customizeBeforeSending()}
   *
   * @return <code>true</code> if all mandatory fields are set, and sending can
   *         continue.
   */
  @OverridingMethodsMustInvokeSuper
  public boolean isEveryRequiredFieldSet ()
  {
    if (m_aHttpClientFactory == null)
    {
      LOGGER.warn ("The field 'httpClientFactory' is not set");
      return false;
    }
    // m_aCryptoFactorySign may be null
    // m_aCryptoFactoryCrypt may be null
    // m_sMessageID is optional
    // m_sRefToMessageID is optional
    // m_aSendingDateTime may be null
    if (m_eSoapVersion == null)
    {
      LOGGER.warn ("The field 'soapVersion' is not set");
      return false;
    }
    // m_aHttpRetrySettings may be null
    if (m_aLocale == null)
    {
      LOGGER.warn ("The field 'locale' is not set");
      return false;
    }
    if (StringHelper.hasNoText (m_sAS4ProfileID))
    {
      LOGGER.warn ("The field 'as4ProfileID' is not set");
      return false;
    }

    if (m_aPModeResolver == null)
    {
      LOGGER.warn ("The field 'pmodeResolver' is not set");
      return false;
    }
    if (m_aIAF == null)
    {
      LOGGER.warn ("The field 'incomingAttachmentFactory' is not set");
      return false;
    }
    // m_aIncomingProfileSelector may be null
    // m_aSenderInterrupt may be null

    // m_aSendingDTConsumer may be null
    // m_aBuildMessageCallback may be null
    // m_aOutgoingDumper may be null
    // m_aIncomingDumper may be null
    // m_aDecryptRequestDataModifier may be null
    // m_aRetryCallback may be null
    // m_aResponseConsumer may be null

    // All valid
    return true;
  }

  /**
   * Internal method that is invoked after the required fields are checked but
   * before sending takes place. This is e.g. the perfect place to add custom
   * message properties. This method is called after
   * {@link #isEveryRequiredFieldSet()} and before {@link #mainSendMessage()}.
   *
   * @throws Phase4Exception
   *         if something goes wrong
   */
  @OverrideOnDemand
  protected void customizeBeforeSending () throws Phase4Exception
  {}

  /**
   * Synchronously send the AS4 message. This method is called after
   * {@link #customizeBeforeSending()}. This method may only be called by
   * {@link #sendMessage()}.
   *
   * @throws Phase4Exception
   *         In case of any error
   */
  protected abstract void mainSendMessage () throws Phase4Exception;

  /**
   * Internal method that is invoked after successful sending took place. This
   * can e.g. be used to fulfill reporting requirements etc. This method must
   * not throw an exception. This method is called after
   * {@link #mainSendMessage()}.
   *
   * @since 2.2.2
   */
  @OverrideOnDemand
  protected void afterSuccessfulSending ()
  {}

  /**
   * Synchronously send the AS4 message. First the internal "finishFields"
   * method is called, to ensure all dynamic fields are filled - on failure this
   * methods exits. Afterwards {@link #isEveryRequiredFieldSet()} is called to
   * check that all mandatory elements are set - on failure this methods exits.
   * Afterwards "customizeBeforeSending" is called to make final adjustments to
   * the message. As the very last step, the customizable sender interrupt is
   * invoked which may prevent the main message sending. As the last step
   * "mainSendMessage" is invoked and "SUCCESS" is returned.<br>
   * Note: since 0.13.0 this common implementation is in place.
   *
   * @return {@link ESuccess#FAILURE} if not all mandatory parameters are set or
   *         if sending failed, {@link ESuccess#SUCCESS} upon success. Never
   *         <code>null</code>. This result code does not reflect the semantics
   *         of a semantically correct message exchange or not. It just states,
   *         if the message was sent or nor. The rest needs to be determined
   *         separately.
   * @throws Phase4Exception
   *         In case of any error
   * @see #isEveryRequiredFieldSet()
   * @see #senderInterrupt()
   */
  @Nonnull
  public final ESuccess sendMessage () throws Phase4Exception
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("About to send the AS4 message");

    // Pre required field check
    if (finishFields ().isFailure ())
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("finishFields() prevented the AS4 message to be send");

      return ESuccess.FAILURE;
    }

    if (!isEveryRequiredFieldSet ())
    {
      LOGGER.error ("At least one mandatory field is not set and therefore the AS4 message cannot be send.");
      return ESuccess.FAILURE;
    }

    // Post required field check
    customizeBeforeSending ();

    if (m_aSenderInterrupt != null)
      if (m_aSenderInterrupt.canSendDocument ().isBreak ())
      {
        LOGGER.warn ("The AS4 sender interrupt disabled the sending of the message.");
        return ESuccess.FAILURE;
      }

    // Main sending
    mainSendMessage ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Finished main AS4 message sending without exception");

    // Post sending callback
    afterSuccessfulSending ();

    return ESuccess.SUCCESS;
  }
}
