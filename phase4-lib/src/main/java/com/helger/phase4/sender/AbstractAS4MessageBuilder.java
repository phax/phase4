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
package com.helger.phase4.sender;

import java.time.LocalDateTime;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.state.ESuccess;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientSettings;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.client.IAS4RawResponseConsumer;
import com.helger.phase4.client.IAS4RetryCallback;
import com.helger.phase4.crypto.AS4CryptoFactoryProperties;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.http.HttpRetrySettings;
import com.helger.phase4.model.pmode.resolve.DefaultPModeResolver;
import com.helger.phase4.model.pmode.resolve.IPModeResolver;
import com.helger.phase4.servlet.AS4IncomingProfileSelectorFromGlobal;
import com.helger.phase4.servlet.IAS4IncomingProfileSelector;
import com.helger.phase4.soap.ESoapVersion;
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

  protected HttpClientFactory m_aHttpClientFactory;
  protected IAS4CryptoFactory m_aCryptoFactory;
  protected String m_sMessageID;
  protected LocalDateTime m_aSendingDateTime;
  protected ESoapVersion m_eSoapVersion;
  protected HttpRetrySettings m_aHttpRetrySettings;
  protected Locale m_aLocale = DEFAULT_LOCALE;

  private IPModeResolver m_aPModeResolver;
  private IAS4IncomingAttachmentFactory m_aIAF;
  private IAS4IncomingProfileSelector m_aIncomingProfileSelector;
  private IAS4SenderInterrupt m_aSenderInterrupt;

  protected IAS4ClientBuildMessageCallback m_aBuildMessageCallback;
  protected IAS4OutgoingDumper m_aOutgoingDumper;
  protected IAS4IncomingDumper m_aIncomingDumper;
  protected IAS4RetryCallback m_aRetryCallback;
  protected IAS4RawResponseConsumer m_aResponseConsumer;

  /**
   * Create a new builder, with the following fields already set:<br>
   * {@link #httpClientFactory(HttpClientFactory)}<br>
   * {@link #cryptoFactory(IAS4CryptoFactory)}<br>
   * {@link #soapVersion(ESoapVersion)}
   * {@link #pmodeResolver(IPModeResolver)}<br>
   * {@link #incomingAttachmentFactory(IAS4IncomingAttachmentFactory)}<br>
   */
  public AbstractAS4MessageBuilder ()
  {
    // Set default values
    try
    {
      httpClientFactory (new HttpClientFactory ());
      cryptoFactory (AS4CryptoFactoryProperties.getDefaultInstance ());
      soapVersion (ESoapVersion.SOAP_12);
      pmodeResolver (DefaultPModeResolver.DEFAULT_PMODE_RESOLVER);
      incomingAttachmentFactory (IAS4IncomingAttachmentFactory.DEFAULT_INSTANCE);
      incomingProfileSelector (AS4IncomingProfileSelectorFromGlobal.INSTANCE);
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to init AbstractAS4MessageBuilder", ex);
    }
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
   * provided, a new {@link HttpClientFactory} is created with them.
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
   * Set the HTTP client factory to be used. By default an instance of
   * {@link HttpClientFactory} is used and there is no need to invoke this
   * method.
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
   * @return The currently set {@link IAS4CryptoFactory}. May be
   *         <code>null</code>.
   */
  @Nullable
  public final IAS4CryptoFactory cryptoFactory ()
  {
    return m_aCryptoFactory;
  }

  /**
   * Set the crypto factory to be used. The default crypto factory uses the
   * properties from the file "crypto.properties".
   *
   * @param aCryptoFactory
   *        The crypto factory to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE cryptoFactory (@Nullable final IAS4CryptoFactory aCryptoFactory)
  {
    m_aCryptoFactory = aCryptoFactory;
    return thisAsT ();
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
   * Set the optional sending date time. If no time is specified, the current
   * date time is used.
   *
   * @param aSendingDateTime
   *        The sending date time to set. May be <code>null</code>.
   * @return this for chaining
   * @since 0.12.0
   */
  @Nonnull
  public final IMPLTYPE sendingDateTime (@Nullable final LocalDateTime aSendingDateTime)
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
   * Set the SOAP version to be used. Default is SOAP 1.2
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
   * @return The currently set {@link IPModeResolver}. May be <code>null</code>.
   */
  @Nullable
  public final IPModeResolver pmodeResolver ()
  {
    return m_aPModeResolver;
  }

  /**
   * Set the PMode resolver to be used.
   *
   * @param aPModeResolver
   *        The PMode resolver to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE pmodeResolver (@Nullable final IPModeResolver aPModeResolver)
  {
    m_aPModeResolver = aPModeResolver;
    return thisAsT ();
  }

  /**
   * @return The currently set {@link IAS4IncomingAttachmentFactory}. Never
   *         <code>null</code>.
   */
  @Nullable
  public final IAS4IncomingAttachmentFactory incomingAttachmentFactory ()
  {
    return m_aIAF;
  }

  /**
   * Set the incoming attachment factory to be used.
   *
   * @param aIAF
   *        The incoming attachment factory to be used. May not be
   *        <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE incomingAttachmentFactory (@Nonnull final IAS4IncomingAttachmentFactory aIAF)
  {
    ValueEnforcer.notNull (aIAF, "IAF");
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
   * Set the selector for the AS4 profile of incoming messages.
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
   * Set an optional handler for the synchronous result message received from
   * the other side. This method is optional and must not be called prior to
   * sending.
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

  @OverridingMethodsMustInvokeSuper
  public boolean isEveryRequiredFieldSet ()
  {
    if (m_aHttpClientFactory == null)
      return false;
    // m_aCryptoFactory may be null
    // m_sMessageID is optional
    // m_aSendingDateTime may be null
    if (m_eSoapVersion == null)
      return false;

    // m_nMaxRetries doesn't matter
    // m_nRetryIntervalMS doesn't matter
    if (m_aLocale == null)
      return false;
    // m_aPModeResolver may be null
    // IIncomingAttachmentFactory may be null

    // m_aBuildMessageCallback may be null
    // m_aOutgoingDumper may be null
    // m_aIncomingDumper may be null
    // m_aRetryCallback may be null
    // m_aResponseConsumer may be null

    // All valid
    return true;
  }

  /**
   * Internal method that is invoked before the required field check is
   * performed. Override to set additional dynamically created fields if
   * necessary.<br>
   * Don't add message properties in here, because if the required fields check
   * fails than this method would be called again.
   *
   * @return {@link ESuccess} - never <code>null</code>. Returning failure here
   *         stops sending the message.
   * @throws Phase4Exception
   *         if something goes wrong
   */
  @OverrideOnDemand
  protected ESuccess finishFields () throws Phase4Exception
  {
    return ESuccess.SUCCESS;
  }

  /**
   * Internal method that is invoked after the required fields are checked but
   * before sending takes place. This is e.g. the perfect place to add custom
   * message properties.
   *
   * @throws Phase4Exception
   *         if something goes wrong
   */
  protected void customizeBeforeSending () throws Phase4Exception
  {}

  /**
   * Synchronously send the AS4 message. This method may only be called by
   * {@link #sendMessage()}
   *
   * @throws Phase4Exception
   *         In case of any error
   */
  protected abstract void mainSendMessage () throws Phase4Exception;

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
    // Pre required field check
    if (finishFields ().isFailure ())
      return ESuccess.FAILURE;

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

    return ESuccess.SUCCESS;
  }

}
