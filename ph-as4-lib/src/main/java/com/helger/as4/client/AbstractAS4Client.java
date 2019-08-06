/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.as4.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;

import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.HttpEntityWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4.crypto.AS4CryptParams;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.AS4CryptoProperties;
import com.helger.as4.crypto.AS4SigningParams;
import com.helger.as4.dump.AS4DumpManager;
import com.helger.as4.dump.IAS4OutgoingDumper;
import com.helger.as4.http.AS4HttpDebug;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.model.pmode.PModeReceptionAwareness;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceHelper;
import com.helger.as4.util.MultiOutputStream;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.functional.ISupplier;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.commons.wrapper.Wrapper;
import com.helger.httpclient.response.ResponseHandlerMicroDom;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.IKeyStoreType;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;

/**
 * Abstract AS4 client based on HTTP client
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        Implementation type
 */
public abstract class AbstractAS4Client <IMPLTYPE extends AbstractAS4Client <IMPLTYPE>> extends BasicHttpPoster
                                        implements
                                        IGenericImplTrait <IMPLTYPE>
{
  public static final IKeyStoreType DEFAULT_KEYSTORE_TYPE = EKeyStoreType.JKS;
  public static final int DEFAULT_MAX_RETRIES = 0;
  public static final long DEFAULT_RETRY_INTERVAL_MS = 12_000;
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractAS4Client.class);

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

  // KeyStore attributes
  private IKeyStoreType m_aKeyStoreType = DEFAULT_KEYSTORE_TYPE;
  private IReadableResource m_aKeyStoreRes;
  private String m_sKeyStorePassword;
  private String m_sKeyStoreAlias;
  private String m_sKeyStoreKeyPassword;
  // Alternative
  private AS4CryptoFactory m_aCryptoFactory;
  private final AS4SigningParams m_aSigningParams = new AS4SigningParams ();
  private final AS4CryptParams m_aCryptParams = new AS4CryptParams ();

  // For Message Info
  private ISupplier <String> m_aMessageIDFactory = createDefaultMessageIDFactory ();
  private String m_sRefToMessageID;
  private ESOAPVersion m_eSOAPVersion = ESOAPVersion.AS4_DEFAULT;

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
   * @return The keystore type to use. Never <code>null</code>. Default is
   *         {@link #DEFAULT_KEYSTORE_TYPE}.
   */
  @Nonnull
  public final IKeyStoreType getKeyStoreType ()
  {
    return m_aKeyStoreType;
  }

  /**
   * The type of the keystore needs to be set if a keystore is used.<br>
   * MANDATORY if you want to use sign or encryption of an user message.
   * Defaults to "jks".
   *
   * @param aKeyStoreType
   *        keystore type that should be set, e.g. "jks". May not be
   *        <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE setKeyStoreType (@Nonnull final IKeyStoreType aKeyStoreType)
  {
    ValueEnforcer.notNull (aKeyStoreType, "KeyStoreType");
    m_aKeyStoreType = aKeyStoreType;
    m_aCryptoFactory = null;
    return thisAsT ();
  }

  /**
   * @return The keystore resource to use. May be <code>null</code>.
   */
  @Nullable
  public final IReadableResource getKeyStoreResource ()
  {
    return m_aKeyStoreRes;
  }

  /**
   * The keystore that should be used can be set here.<br>
   * MANDATORY if you want to use sign or encryption of an usermessage.
   *
   * @param aKeyStoreRes
   *        the keystore file that should be used
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE setKeyStoreResource (@Nullable final IReadableResource aKeyStoreRes)
  {
    m_aKeyStoreRes = aKeyStoreRes;
    m_aCryptoFactory = null;
    return thisAsT ();
  }

  /**
   * @return The keystore password to use. May be <code>null</code>.
   */
  @Nullable
  public final String getKeyStorePassword ()
  {
    return m_sKeyStorePassword;
  }

  /**
   * KeyStore password needs to be set if a keystore is used<br>
   * MANDATORY if you want to use sign or encryption of an usermessage.
   *
   * @param sKeyStorePassword
   *        password that should be set
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE setKeyStorePassword (@Nullable final String sKeyStorePassword)
  {
    m_sKeyStorePassword = sKeyStorePassword;
    m_aCryptoFactory = null;
    return thisAsT ();
  }

  /**
   * @return The keystore key alias to use. May be <code>null</code>.
   */
  @Nullable
  public final String getKeyStoreAlias ()
  {
    return m_sKeyStoreAlias;
  }

  /**
   * KeyStorealias needs to be set if a keystore is used<br>
   * MANDATORY if you want to use sign or encryption of an usermessage.
   *
   * @param sKeyStoreAlias
   *        alias that should be set
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE setKeyStoreAlias (@Nullable final String sKeyStoreAlias)
  {
    m_sKeyStoreAlias = sKeyStoreAlias;
    m_aCryptoFactory = null;
    return thisAsT ();
  }

  /**
   * @return The keystore key password to use. May be <code>null</code>.
   */

  @Nullable
  public final String getKeyStoreKeyPassword ()
  {
    return m_sKeyStoreKeyPassword;
  }

  /**
   * Key password needs to be set if a keystore is used<br>
   * MANDATORY if you want to use sign or encryption of an usermessage.
   *
   * @param sKeyStoreKeyPassword
   *        password that should be set
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE setKeyStoreKeyPassword (@Nullable final String sKeyStoreKeyPassword)
  {
    m_sKeyStoreKeyPassword = sKeyStoreKeyPassword;
    m_aCryptoFactory = null;
    return thisAsT ();
  }

  @Nullable
  public final AS4CryptoFactory getAS4CryptoFactory ()
  {
    return m_aCryptoFactory;
  }

  @Nonnull
  public final IMPLTYPE setAS4CryptoFactory (@Nullable final AS4CryptoFactory aCryptoFactory)
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
   */
  @Nonnull
  public final ESOAPVersion getSOAPVersion ()
  {
    return m_eSOAPVersion;
  }

  /**
   * This method sets the SOAP Version. AS4 - Profile default is SOAP 1.2
   *
   * @param eSOAPVersion
   *        SOAPVersion which should be set. MAy not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE setSOAPVersion (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    m_eSOAPVersion = eSOAPVersion;
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

  private void _checkKeyStoreAttributes ()
  {
    if (m_aCryptoFactory == null)
    {
      if (m_aKeyStoreType == null)
        throw new IllegalStateException ("KeyStore type is not configured.");
      if (m_aKeyStoreRes == null)
        throw new IllegalStateException ("KeyStore resources is not configured.");
      if (!m_aKeyStoreRes.exists ())
        throw new IllegalStateException ("KeyStore resource does not exist: " + m_aKeyStoreRes.getPath ());
      if (m_sKeyStorePassword == null)
        throw new IllegalStateException ("KeyStore password is not configured.");
      if (StringHelper.hasNoText (m_sKeyStoreAlias))
        throw new IllegalStateException ("KeyStore alias is not configured.");
      if (m_sKeyStoreKeyPassword == null)
        throw new IllegalStateException ("Key password is not configured.");
    }
  }

  @Nonnull
  protected AS4CryptoFactory internalCreateCryptoFactory ()
  {
    _checkKeyStoreAttributes ();

    // Shortcut?
    if (m_aCryptoFactory != null)
      return m_aCryptoFactory;

    final ICommonsMap <String, String> aCryptoProps = new CommonsLinkedHashMap <> ();
    aCryptoProps.put ("org.apache.wss4j.crypto.provider", org.apache.wss4j.common.crypto.Merlin.class.getName ());
    aCryptoProps.put (AS4CryptoProperties.KEYSTORE_TYPE, getKeyStoreType ().getID ());
    aCryptoProps.put (AS4CryptoProperties.KEYSTORE_FILE, getKeyStoreResource ().getPath ());
    aCryptoProps.put (AS4CryptoProperties.KEYSTORE_PASSWORD, getKeyStorePassword ());
    aCryptoProps.put (AS4CryptoProperties.KEY_ALIAS, getKeyStoreAlias ());
    aCryptoProps.put (AS4CryptoProperties.KEY_PASSWORD, getKeyStoreKeyPassword ());
    return new AS4CryptoFactory (aCryptoProps);
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
   * @throws Exception
   *         in case something goes wrong
   */
  @Nonnull
  public abstract AS4ClientBuiltMessage buildMessage (@Nonnull @Nonempty String sMessageID,
                                                      @Nullable IAS4ClientBuildMessageCallback aCallback) throws Exception;

  @Nonnull
  private HttpEntity _createRepeatableEntity (@Nonnull final HttpEntity aSrcEntity) throws IOException
  {
    if (aSrcEntity.isRepeatable ())
      return aSrcEntity;

    // First serialize the content once to a file, so that a repeatable entity
    // can be created
    final File aTempFile = m_aResHelper.createTempFile ();

    LOGGER.info ("Converting " +
                 aSrcEntity +
                 " to a repeatable HTTP entity using file " +
                 aTempFile.getAbsolutePath ());

    try (final OutputStream aOS = FileHelper.getBufferedOutputStream (aTempFile))
    {
      aSrcEntity.writeTo (aOS);
    }

    // Than use the FileEntity as the basis
    final FileEntity aRepeatableEntity = new FileEntity (aTempFile);
    aRepeatableEntity.setContentType (aSrcEntity.getContentType ());
    aRepeatableEntity.setContentEncoding (aSrcEntity.getContentEncoding ());
    aRepeatableEntity.setChunked (aSrcEntity.isChunked ());
    return aRepeatableEntity;
  }

  @Nonnull
  private HttpEntity _createDumpingEntity (@Nullable final IAS4OutgoingDumper aDumper,
                                           @Nonnull final HttpEntity aSrcEntity,
                                           @Nonnull @Nonempty final String sMessageID,
                                           @Nullable final HttpHeaderMap aCustomHeaders,
                                           @Nonnegative final int nTry,
                                           @Nonnull final Wrapper <OutputStream> aDumpOSHolder) throws IOException
  {
    if (aDumper == null)
    {
      // No dumper
      return aSrcEntity;
    }

    final OutputStream aDumpOS = aDumper.onBeginRequest (sMessageID, aCustomHeaders, nTry);
    if (aDumpOS == null)
    {
      // No dumping needed
      return aSrcEntity;
    }

    aDumpOSHolder.set (aDumpOS);
    return new HttpEntityWrapper (aSrcEntity)
    {
      @Override
      public InputStream getContent () throws IOException
      {
        throw new UnsupportedOperationException ();
      }

      @Override
      public void writeTo (@Nonnull @WillNotClose final OutputStream aHttpOS) throws IOException
      {
        final MultiOutputStream aMultiOS = new MultiOutputStream (aHttpOS, aDumpOS);
        // write to both stream
        super.writeTo (aMultiOS);
        // Flush both, but do not close both
        aMultiOS.flush ();
      }
    };
  }

  /**
   * Send the AS4 client message created by
   * {@link #buildMessage(IAS4ClientBuildMessageCallback)} to the provided URL.
   * This methods does take retries into account. It synchronously handles the
   * retries and only returns after the last retry.
   *
   * @param <T>
   *        The response data type
   * @param sURL
   *        The URL to send the HTTP POST to
   * @param aResponseHandler
   *        The response handler that converts the HTTP response to a domain
   *        object. May not be <code>null</code>.
   * @param aCallback
   *        The optional callback that is invoked during the creation of the
   *        {@link AS4ClientBuiltMessage}. It can be used to access several
   *        states of message creation. May be <code>null</code>.
   * @return The sent message that contains
   * @throws Exception
   *         in case of error when building or sending the message
   */
  @Nonnull
  public final <T> AS4ClientSentMessage <T> sendMessageWithRetries (@Nonnull final String sURL,
                                                                    @Nonnull final ResponseHandler <? extends T> aResponseHandler,
                                                                    @Nullable final IAS4ClientBuildMessageCallback aCallback) throws Exception
  {
    // Create a new message ID for each build!
    final String sMessageID = createMessageID ();
    final AS4ClientBuiltMessage aBuiltMsg = buildMessage (sMessageID, aCallback);
    final HttpEntity aBuiltEntity = aBuiltMsg.getHttpEntity ();

    final IAS4OutgoingDumper aDumper = AS4DumpManager.getOutgoingDumper ();
    final Wrapper <OutputStream> aDumpOSHolder = new Wrapper <> ();
    try
    {
      if (m_nMaxRetries > 0)
      {
        // Send with retry

        // Ensure a repeatable entity is provided
        final HttpEntity aRepeatableEntity = _createRepeatableEntity (aBuiltEntity);

        final int nMaxTries = 1 + m_nMaxRetries;
        for (int nTry = 0; nTry < nMaxTries; nTry++)
        {
          if (nTry > 0)
            LOGGER.info ("Retry #" + nTry + " for sending message with ID '" + sMessageID + "'");

          try
          {
            // Create a new one every time (for new filename, new timestamp,
            // etc.)
            final HttpEntity aDumpingEntity = _createDumpingEntity (aDumper,
                                                                    aRepeatableEntity,
                                                                    sMessageID,
                                                                    aBuiltMsg.getCustomHeaders (),
                                                                    nTry,
                                                                    aDumpOSHolder);

            // Dump only for the first try - the remaining tries
            final T aResponse = sendGenericMessage (sURL,
                                                    aDumpingEntity,
                                                    aBuiltMsg.getCustomHeaders (),
                                                    aResponseHandler);
            return new AS4ClientSentMessage <> (aBuiltMsg, aResponse);
          }
          catch (final IOException ex)
          {
            // Last try? -> propagate exception
            if (nTry == nMaxTries - 1)
              throw ex;

            // Sleep and try again afterwards
            ThreadHelper.sleep (m_nRetryIntervalMS);
          }
          finally
          {
            // Close the dump output stream (if any)
            StreamHelper.close (aDumpOSHolder.get ());
          }
        }
        throw new IllegalStateException ("Should never be reached (after maximum of " + nMaxTries + " tries)!");
      }
      else
      {
        final HttpEntity aDumpingEntity = _createDumpingEntity (aDumper,
                                                                aBuiltEntity,
                                                                sMessageID,
                                                                aBuiltMsg.getCustomHeaders (),
                                                                0,
                                                                aDumpOSHolder);

        try
        {
          // Send without retry
          final T aResponse = sendGenericMessage (sURL,
                                                  aDumpingEntity,
                                                  aBuiltMsg.getCustomHeaders (),
                                                  aResponseHandler);
          return new AS4ClientSentMessage <> (aBuiltMsg, aResponse);
        }
        finally
        {
          // Close the dump output stream (if any)
          StreamHelper.close (aDumpOSHolder.get ());
        }
      }
    }
    finally
    {
      // Add the possibility to close open resources
      if (aDumper != null)
        aDumper.onEndRequest (sMessageID);
    }
  }

  @Nullable
  public IMicroDocument sendMessageAndGetMicroDocument (@Nonnull final String sURL) throws Exception
  {
    final IMicroDocument ret = sendMessageWithRetries (sURL, new ResponseHandlerMicroDom (), null).getResponse ();
    AS4HttpDebug.debug ( () -> "SEND-RESPONSE received: " +
                               MicroWriter.getNodeAsString (ret, AS4HttpDebug.getDebugXMLWriterSettings ()));
    return ret;
  }
}
