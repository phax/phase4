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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;

import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.http.AS4HttpDebug;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.functional.ISupplier;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.httpclient.response.ResponseHandlerMicroDom;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.IKeyStoreType;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;

public abstract class AbstractAS4Client extends BasicHttpPoster
{
  public static final class BuiltMessage
  {
    private final String m_sMessageID;
    private final HttpEntity m_aHttpEntity;

    public BuiltMessage (@Nonnull @Nonempty final String sMessageID, @Nonnull final HttpEntity aHttpEntity)
    {
      m_sMessageID = ValueEnforcer.notEmpty (sMessageID, "MessageID");
      m_aHttpEntity = ValueEnforcer.notNull (aHttpEntity, "HttpEntity");
    }

    @Nonnull
    @Nonempty
    public String getMessageID ()
    {
      return m_sMessageID;
    }

    @Nonnull
    public HttpEntity getHttpEntity ()
    {
      return m_aHttpEntity;
    }

    @Override
    public String toString ()
    {
      return new ToStringGenerator (this).append ("MessageID", m_sMessageID)
                                         .append ("HttpEntity", m_aHttpEntity)
                                         .getToString ();
    }
  }

  public static final class SentMessage <T>
  {
    private final String m_sMessageID;
    private final T m_aResponse;

    public SentMessage (@Nonnull @Nonempty final String sMessageID, @Nullable final T aResponse)
    {
      m_sMessageID = ValueEnforcer.notEmpty (sMessageID, "MessageID");
      m_aResponse = aResponse;
    }

    @Nonnull
    @Nonempty
    public String getMessageID ()
    {
      return m_sMessageID;
    }

    @Nullable
    public T getResponse ()
    {
      return m_aResponse;
    }

    public boolean hasResponse ()
    {
      return m_aResponse != null;
    }

    @Override
    public String toString ()
    {
      return new ToStringGenerator (this).append ("MessageID", m_sMessageID)
                                         .append ("Response", m_aResponse)
                                         .getToString ();
    }
  }

  public static final IKeyStoreType DEFAULT_KEYSTORE_TYPE = EKeyStoreType.JKS;

  // KeyStore attributes
  private IReadableResource m_aKeyStoreRes;
  private String m_sKeyStorePassword;
  private IKeyStoreType m_aKeyStoreType = DEFAULT_KEYSTORE_TYPE;
  private String m_sKeyStoreAlias;
  private String m_sKeyStoreKeyPassword;

  // Signing additional attributes
  private ECryptoAlgorithmSign m_eCryptoAlgorithmSign;
  private ECryptoAlgorithmSignDigest m_eCryptoAlgorithmSignDigest;
  // Encryption attribute
  private ECryptoAlgorithmCrypt m_eCryptoAlgorithmCrypt;

  // For Message Info
  private ISupplier <String> m_aMessageIDFactory = () -> MessageHelperMethods.createRandomMessageID ();

  private ESOAPVersion m_eSOAPVersion = ESOAPVersion.AS4_DEFAULT;

  protected AbstractAS4Client ()
  {}

  private void _checkKeyStoreAttributes ()
  {
    if (m_aKeyStoreRes == null)
      throw new IllegalStateException ("KeyStore resources is not configured.");
    if (!m_aKeyStoreRes.exists ())
      throw new IllegalStateException ("KeyStore resources does not exist: " + m_aKeyStoreRes.getPath ());
    if (m_aKeyStoreType == null)
      throw new IllegalStateException ("KeyStore type is not configured.");
    if (m_sKeyStorePassword == null)
      throw new IllegalStateException ("KeyStore password is not configured.");
    if (StringHelper.hasNoText (m_sKeyStoreAlias))
      throw new IllegalStateException ("KeyStore alias is not configured.");
    if (m_sKeyStoreKeyPassword == null)
      throw new IllegalStateException ("Key password is not configured.");
  }

  @Nonnull
  protected AS4CryptoFactory internalCreateCryptoFactory ()
  {
    _checkKeyStoreAttributes ();

    final ICommonsMap <String, String> aCryptoProps = new CommonsLinkedHashMap <> ();
    aCryptoProps.put ("org.apache.wss4j.crypto.provider", org.apache.wss4j.common.crypto.Merlin.class.getName ());
    aCryptoProps.put ("org.apache.wss4j.crypto.merlin.keystore.file", getKeyStoreResource ().getPath ());
    aCryptoProps.put ("org.apache.wss4j.crypto.merlin.keystore.type", getKeyStoreType ().getID ());
    aCryptoProps.put ("org.apache.wss4j.crypto.merlin.keystore.password", getKeyStorePassword ());
    aCryptoProps.put ("org.apache.wss4j.crypto.merlin.keystore.alias", getKeyStoreAlias ());
    aCryptoProps.put ("org.apache.wss4j.crypto.merlin.keystore.private.password", getKeyStoreKeyPassword ());
    return new AS4CryptoFactory (aCryptoProps);
  }

  @OverrideOnDemand
  @Nonnull
  public abstract BuiltMessage buildMessage () throws Exception;

  @Nonnull
  public <T> SentMessage <T> sendMessage (@Nonnull final String sURL,
                                          @Nonnull final ResponseHandler <? extends T> aResponseHandler) throws Exception
  {
    final BuiltMessage aBuiltMsg = buildMessage ();
    final T aResponse = sendGenericMessage (sURL, aBuiltMsg.getHttpEntity (), aResponseHandler);
    return new SentMessage <> (aBuiltMsg.getMessageID (), aResponse);
  }

  @Nullable
  public IMicroDocument sendMessageAndGetMicroDocument (@Nonnull final String sURL) throws Exception
  {
    final IMicroDocument ret = sendMessage (sURL, new ResponseHandlerMicroDom ()).getResponse ();
    AS4HttpDebug.debug ( () -> "SEND-RESPONSE received: " +
                               MicroWriter.getNodeAsString (ret, AS4HttpDebug.getDebugXMLWriterSettings ()));
    return ret;
  }

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
   */
  public final void setKeyStoreResource (@Nullable final IReadableResource aKeyStoreRes)
  {
    m_aKeyStoreRes = aKeyStoreRes;
  }

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
   */
  public final void setKeyStorePassword (@Nullable final String sKeyStorePassword)
  {
    m_sKeyStorePassword = sKeyStorePassword;
  }

  @Nonnull
  public final IKeyStoreType getKeyStoreType ()
  {
    return m_aKeyStoreType;
  }

  /**
   * The type of the keystore needs to be set if a keystore is used.<br>
   * MANDATORY if you want to use sign or encryption of an user message. Defaults
   * to "jks".
   *
   * @param aKeyStoreType
   *        keystore type that should be set, e.g. "jks"
   */
  public final void setKeyStoreType (@Nonnull final IKeyStoreType aKeyStoreType)
  {
    ValueEnforcer.notNull (aKeyStoreType, "KeyStoreType");
    m_aKeyStoreType = aKeyStoreType;
  }

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
   */
  public final void setKeyStoreAlias (@Nullable final String sKeyStoreAlias)
  {
    m_sKeyStoreAlias = sKeyStoreAlias;
  }

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
   */
  public final void setKeyStoreKeyPassword (@Nullable final String sKeyStoreKeyPassword)
  {
    m_sKeyStoreKeyPassword = sKeyStoreKeyPassword;
  }

  @Nullable
  public final ECryptoAlgorithmSign getCryptoAlgorithmSign ()
  {
    return m_eCryptoAlgorithmSign;
  }

  /**
   * A signing algorithm can be set. <br>
   * MANDATORY if you want to use sign.<br>
   * Also @see {@link #setCryptoAlgorithmSignDigest(ECryptoAlgorithmSignDigest)}
   *
   * @param eCryptoAlgorithmSign
   *        the signing algorithm that should be set
   */
  public final void setCryptoAlgorithmSign (@Nullable final ECryptoAlgorithmSign eCryptoAlgorithmSign)
  {
    m_eCryptoAlgorithmSign = eCryptoAlgorithmSign;
  }

  @Nullable
  public final ECryptoAlgorithmSignDigest getCryptoAlgorithmSignDigest ()
  {
    return m_eCryptoAlgorithmSignDigest;
  }

  /**
   * A signing digest algorithm can be set. <br>
   * MANDATORY if you want to use sign.<br>
   * Also @see {@link #setCryptoAlgorithmSign(ECryptoAlgorithmSign)}
   *
   * @param eCryptoAlgorithmSignDigest
   *        the signing digest algorithm that should be set
   */
  public final void setCryptoAlgorithmSignDigest (@Nullable final ECryptoAlgorithmSignDigest eCryptoAlgorithmSignDigest)
  {
    m_eCryptoAlgorithmSignDigest = eCryptoAlgorithmSignDigest;
  }

  @Nullable
  public final ECryptoAlgorithmCrypt getCryptoAlgorithmCrypt ()
  {
    return m_eCryptoAlgorithmCrypt;
  }

  /**
   * A encryption algorithm can be set. <br>
   * MANDATORY if you want to use encryption.
   *
   * @param eCryptoAlgorithmCrypt
   *        the encryption algorithm that should be set
   */
  public final void setCryptoAlgorithmCrypt (@Nullable final ECryptoAlgorithmCrypt eCryptoAlgorithmCrypt)
  {
    m_eCryptoAlgorithmCrypt = eCryptoAlgorithmCrypt;
  }

  @Nonnull
  public final ISupplier <String> getMessageIDFactory ()
  {
    return m_aMessageIDFactory;
  }

  /**
   * Set the factory that creates message IDs. By default a random UUID is used.
   *
   * @param aMessageIDFactory
   *        Factory to be used. May not be <code>null</code>.
   */
  public final void setMessageIDFactory (@Nonnull final ISupplier <String> aMessageIDFactory)
  {
    ValueEnforcer.notNull (aMessageIDFactory, "MessageIDFactory");
    m_aMessageIDFactory = aMessageIDFactory;
  }

  @Nonnull
  @Nonempty
  protected final String createMessageID ()
  {
    final String ret = m_aMessageIDFactory.get ();
    if (StringHelper.hasNoText (ret))
      throw new IllegalStateException ("An empty MessageID was generate!");
    return ret;
  }

  @Nonnull
  public final ESOAPVersion getSOAPVersion ()
  {
    return m_eSOAPVersion;
  }

  /**
   * This method sets the SOAP Version. AS4 - Profile Default is SOAP 1.2
   *
   * @param eSOAPVersion
   *        SOAPVersion which should be set
   */
  public final void setSOAPVersion (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    m_eSOAPVersion = eSOAPVersion;
  }
}
