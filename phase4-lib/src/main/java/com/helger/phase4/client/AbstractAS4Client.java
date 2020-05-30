/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.mail.MessagingException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.wss4j.common.ext.WSSecurityException;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.functional.ISupplier;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.string.StringHelper;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.httpclient.response.ResponseHandlerMicroDom;
import com.helger.phase4.crypto.AS4CryptParams;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.http.AS4HttpDebug;
import com.helger.phase4.http.BasicHttpPoster;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.PModeReceptionAwareness;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.soap.ESoapVersion;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;

/**
 * Abstract AS4 client based on HTTP client
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        Implementation type
 */
public abstract class AbstractAS4Client <IMPLTYPE extends AbstractAS4Client <IMPLTYPE>> extends BasicHttpPoster implements
                                        IGenericImplTrait <IMPLTYPE>
{
  public static final int DEFAULT_MAX_RETRIES = 0;
  public static final long DEFAULT_RETRY_INTERVAL_MS = 12_000;

  /**
   * @return The default message ID factory to be used.
   * @since 0.8.3
   */
  @Nonnull
  public static ISupplier <String> createDefaultMessageIDFactory ()
  {
    return MessageHelperMethods::createRandomMessageID;
  }

  private final AS4ResourceHelper m_aResHelper;

  private IAS4CryptoFactory m_aCryptoFactory;
  private final AS4SigningParams m_aSigningParams = new AS4SigningParams ();
  private final AS4CryptParams m_aCryptParams = new AS4CryptParams ();

  // For Message Info
  private ISupplier <String> m_aMessageIDFactory = createDefaultMessageIDFactory ();
  private String m_sRefToMessageID;
  private ESoapVersion m_eSoapVersion = ESoapVersion.AS4_DEFAULT;

  // Retry handling
  private int m_nMaxRetries = DEFAULT_MAX_RETRIES;
  private long m_nRetryIntervalMS = DEFAULT_RETRY_INTERVAL_MS;

  protected AbstractAS4Client (@Nonnull @WillNotClose final AS4ResourceHelper aResHelper)
  {
    ValueEnforcer.notNull (aResHelper, "ResHelper");
    m_aResHelper = aResHelper;
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
   * @return The currently set crypto factory. <code>null</code> by default.
   */
  @Nullable
  public final IAS4CryptoFactory getAS4CryptoFactory ()
  {
    return m_aCryptoFactory;
  }

  /**
   * Set all the crypto properties at once.
   *
   * @param aCryptoFactory
   *        The crypto factory to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE setAS4CryptoFactory (@Nullable final IAS4CryptoFactory aCryptoFactory)
  {
    m_aCryptoFactory = aCryptoFactory;
    return thisAsT ();
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
   * @return The Message ID factory to be used. May not be <code>null</code>.
   */
  @Nonnull
  public final ISupplier <String> getMessageIDFactory ()
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
  public final IMPLTYPE setMessageIDFactory (@Nonnull final ISupplier <String> aMessageIDFactory)
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

  @Nullable
  public final String getRefToMessageID ()
  {
    return m_sRefToMessageID;
  }

  public final boolean hasRefToMessageID ()
  {
    return StringHelper.hasText (m_sRefToMessageID);
  }

  @Nonnull
  public final IMPLTYPE setRefToMessageID (@Nullable final String sRefToMessageID)
  {
    m_sRefToMessageID = sRefToMessageID;
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
   * @return The maximum number of retries. Only values &gt; 0 imply a retry.
   *         The default value is {@link #DEFAULT_MAX_RETRIES}.
   * @since 0.9.0
   */
  @Nonnegative
  public final int getMaxRetries ()
  {
    return m_nMaxRetries;
  }

  /**
   * Set the maximum number of retries to be used.
   *
   * @param nMaxRetries
   *        Retry count. A value of <code>0</code> means "no retries". Must be
   *        &ge; 0.
   * @return this for chaining
   * @since 0.9.0
   */
  @Nonnull
  public final IMPLTYPE setMaxRetries (@Nonnegative final int nMaxRetries)
  {
    ValueEnforcer.isGE0 (nMaxRetries, "MaxRetries");
    m_nMaxRetries = nMaxRetries;
    return thisAsT ();
  }

  /**
   * @return The interval in milliseconds between retries. Must be &ge; 0. The
   *         default value is {@link #DEFAULT_RETRY_INTERVAL_MS}.
   * @since 0.9.0
   */
  @Nonnegative
  public final long getRetryIntervalMS ()
  {
    return m_nRetryIntervalMS;
  }

  /**
   * Set the interval in milliseconds between retries.
   *
   * @param nRetryIntervalMS
   *        Retry interval in milliseconds. Must be &ge; 0.
   * @return this for chaining
   * @since 0.9.0
   */
  @Nonnull
  public final IMPLTYPE setRetryIntervalMS (@Nonnegative final long nRetryIntervalMS)
  {
    ValueEnforcer.isGE0 (nRetryIntervalMS, "RetryIntervalMS");
    m_nRetryIntervalMS = nRetryIntervalMS;
    return thisAsT ();
  }

  @Nonnull
  protected IAS4CryptoFactory internalCreateCryptoFactory ()
  {
    if (m_aCryptoFactory == null)
      throw new IllegalStateException ("No CryptoFactory is configured.");

    return m_aCryptoFactory;
  }

  public final void setValuesFromPMode (@Nullable final IPMode aPMode, @Nullable final PModeLeg aLeg)
  {
    if (aPMode != null)
    {
      final PModeReceptionAwareness aRA = aPMode.getReceptionAwareness ();
      if (aRA != null && aRA.isRetryDefined ())
      {
        setMaxRetries (aRA.getMaxRetries ());
        setRetryIntervalMS (aRA.getRetryIntervalMS ());
      }
      else
      {
        // 0 means "no retries"
        setMaxRetries (0);
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
   * @param aOutgoingDumper
   *        An outgoing dumper to be used. Maybe <code>null</code>. If
   *        <code>null</code> the global outgoing dumper from
   *        {@link AS4DumpManager} is used.
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
                                                                    @Nonnull final ResponseHandler <? extends T> aResponseHandler,
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
    final HttpHeaderMap aBuiltHttpHeaders = aBuiltMsg.getCustomHeaders ();

    if (m_nMaxRetries > 0)
    {
      // Ensure a repeatable entity is provided
      aBuiltEntity = m_aResHelper.createRepeatableHttpEntity (aBuiltEntity);
    }

    final T aResponse = super.sendGenericMessageWithRetries (aBuiltHttpHeaders,
                                                             aBuiltEntity,
                                                             sMessageID,
                                                             sURL,
                                                             m_nMaxRetries,
                                                             m_nRetryIntervalMS,
                                                             aResponseHandler,
                                                             aOutgoingDumper,
                                                             aRetryCallback);
    return new AS4ClientSentMessage <> (aBuiltMsg, aResponse);
  }

  @Nullable
  public IMicroDocument sendMessageAndGetMicroDocument (@Nonnull final String sURL) throws WSSecurityException,
                                                                                    IOException,
                                                                                    MessagingException
  {
    final IAS4ClientBuildMessageCallback aCallback = null;
    final IAS4OutgoingDumper aOutgoingDumper = null;
    final IAS4RetryCallback aRetryCallback = null;
    final IMicroDocument ret = sendMessageWithRetries (sURL,
                                                       new ResponseHandlerMicroDom (),
                                                       aCallback,
                                                       aOutgoingDumper,
                                                       aRetryCallback).getResponse ();
    AS4HttpDebug.debug ( () -> "SEND-RESPONSE received: " + MicroWriter.getNodeAsString (ret, AS4HttpDebug.getDebugXMLWriterSettings ()));
    return ret;
  }
}
