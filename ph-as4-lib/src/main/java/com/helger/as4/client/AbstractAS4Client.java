/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;

import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.http.AS4HttpDebug;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;
import com.helger.httpclient.response.ResponseHandlerMicroDom;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;

public abstract class AbstractAS4Client extends BasicAS4Sender
{
  // Keystore attributes
  private File m_aKeyStoreFile;
  private String m_sKeyStoreType = "jks";
  private String m_sKeyStoreAlias;
  private String m_sKeyStorePassword;

  // Signing additional attributes
  private ECryptoAlgorithmSign m_eCryptoAlgorithmSign;
  private ECryptoAlgorithmSignDigest m_eCryptoAlgorithmSignDigest;
  // Encryption attribute
  private ECryptoAlgorithmCrypt m_eCryptoAlgorithmCrypt;

  // For Message Info
  private String m_sMessageIDPrefix;

  private ESOAPVersion m_eSOAPVersion = ESOAPVersion.AS4_DEFAULT;

  protected AbstractAS4Client ()
  {}

  protected void _checkKeystoreAttributes ()
  {
    if (m_aKeyStoreFile == null)
      throw new IllegalStateException ("Key store file is not configured.");
    if (!m_aKeyStoreFile.exists ())
      throw new IllegalStateException ("Key store file does not exist: " + m_aKeyStoreFile.getAbsolutePath ());
    if (StringHelper.hasNoText (m_sKeyStoreType))
      throw new IllegalStateException ("Key store type is configured.");
    if (StringHelper.hasNoText (m_sKeyStoreAlias))
      throw new IllegalStateException ("Key store alias is configured.");
    if (StringHelper.hasNoText (m_sKeyStorePassword))
      throw new IllegalStateException ("Key store password is configured.");
  }

  public abstract HttpEntity buildMessage () throws Exception;

  @Nullable
  <T> T sendMessage (@Nonnull final String sURL,
                     @Nonnull final ResponseHandler <? extends T> aResponseHandler) throws Exception
  {
    final HttpEntity aRequestEntity = buildMessage ();
    return sendGenericMessage (sURL, aRequestEntity, aResponseHandler);
  }

  @Nullable
  public IMicroDocument sendMessageAndGetMicroDocument (@Nonnull final String sURL) throws Exception
  {
    final IMicroDocument ret = sendMessage (sURL, new ResponseHandlerMicroDom ());
    AS4HttpDebug.debug ( () -> "SEND-RESPONSE received: " + MicroWriter.getNodeAsString (ret));
    return ret;
  }

  public File getKeyStoreFile ()
  {
    return m_aKeyStoreFile;
  }

  /**
   * The keystore that should be used can be set here.<br>
   * MANDATORY if you want to use sign or encryption of an usermessage.
   *
   * @param aKeyStoreFile
   *        the keystore file that should be used
   */
  public void setKeyStoreFile (final File aKeyStoreFile)
  {
    m_aKeyStoreFile = aKeyStoreFile;
  }

  @Nonnull
  @Nonempty
  public String getKeyStoreType ()
  {
    return m_sKeyStoreType;
  }

  /**
   * The type of the keystore needs to be set if a keystore is used.<br>
   * MANDATORY if you want to use sign or encryption of an user message.
   * Defaults to "jks".
   *
   * @param sKeyStoreType
   *        keystore type that should be set, e.g. "jks"
   */
  public void setKeyStoreType (@Nonnull @Nonempty final String sKeyStoreType)
  {
    ValueEnforcer.notEmpty (sKeyStoreType, "KeyStoreType");
    m_sKeyStoreType = sKeyStoreType;
  }

  public String getKeyStoreAlias ()
  {
    return m_sKeyStoreAlias;
  }

  /**
   * Keystorealias needs to be set if a keystore is used<br>
   * MANDATORY if you want to use sign or encryption of an usermessage.
   *
   * @param sKeyStoreAlias
   *        alias that should be set
   */
  public void setKeyStoreAlias (final String sKeyStoreAlias)
  {
    m_sKeyStoreAlias = sKeyStoreAlias;
  }

  public String getKeyStorePassword ()
  {
    return m_sKeyStorePassword;
  }

  /**
   * Keystore password needs to be set if a keystore is used<br>
   * MANDATORY if you want to use sign or encryption of an usermessage.
   *
   * @param sKeyStorePassword
   *        password that should be set
   */
  public void setKeyStorePassword (final String sKeyStorePassword)
  {
    m_sKeyStorePassword = sKeyStorePassword;
  }

  @Nullable
  public ECryptoAlgorithmSign getCryptoAlgorithmSign ()
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
  public void setCryptoAlgorithmSign (@Nullable final ECryptoAlgorithmSign eCryptoAlgorithmSign)
  {
    m_eCryptoAlgorithmSign = eCryptoAlgorithmSign;
  }

  @Nullable
  public ECryptoAlgorithmSignDigest getCryptoAlgorithmSignDigest ()
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
  public void setCryptoAlgorithmSignDigest (@Nullable final ECryptoAlgorithmSignDigest eCryptoAlgorithmSignDigest)
  {
    m_eCryptoAlgorithmSignDigest = eCryptoAlgorithmSignDigest;
  }

  @Nullable
  public ECryptoAlgorithmCrypt getCryptoAlgorithmCrypt ()
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
  public void setCryptoAlgorithmCrypt (@Nullable final ECryptoAlgorithmCrypt eCryptoAlgorithmCrypt)
  {
    m_eCryptoAlgorithmCrypt = eCryptoAlgorithmCrypt;
  }

  @Nullable
  public String getMessageIDPrefix ()
  {
    return m_sMessageIDPrefix;
  }

  /**
   * If it is desired to set a MessagePrefix for the MessageID it can be done
   * here.
   *
   * @param sMessageIDPrefix
   *        Prefix that will be at the start of the MessageID. May be
   *        <code>null</code>.
   */
  public void setMessageIDPrefix (@Nullable final String sMessageIDPrefix)
  {
    m_sMessageIDPrefix = sMessageIDPrefix;
  }

  @Nonnull
  public ESOAPVersion getSOAPVersion ()
  {
    return m_eSOAPVersion;
  }

  /**
   * This method sets the SOAP Version. AS4 - Profile Default is SOAP 1.2
   *
   * @param eSOAPVersion
   *        SOAPVersion which should be set
   */
  public void setSOAPVersion (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    m_eSOAPVersion = eSOAPVersion;
  }
}
