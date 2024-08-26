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
package com.helger.phase4.incoming.soap;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.phase4.crypto.IAS4CryptoFactory;

/**
 * Internal WSS4J callback handler to check if a certain key alias is present in
 * the {@link IAS4CryptoFactory}, and if so return the password for accessing
 * it.
 *
 * @author Philip Helger
 */
@Immutable
public final class AS4KeyStoreCallbackHandler implements CallbackHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4KeyStoreCallbackHandler.class);

  private final IAS4CryptoFactory m_aCryptoFactoryCrypt;

  public AS4KeyStoreCallbackHandler (@Nonnull final IAS4CryptoFactory aCryptoFactoryCrypt)
  {
    ValueEnforcer.notNull (aCryptoFactoryCrypt, "CryptoFactoryCrypt");
    m_aCryptoFactoryCrypt = aCryptoFactoryCrypt;
  }

  @Nonnull
  @Nonempty
  private static String _getUsage (final int nUsage)
  {
    switch (nUsage)
    {
      case WSPasswordCallback.UNKNOWN:
        return "UNKNOWN";
      case WSPasswordCallback.DECRYPT:
        return "DECRYPT";
      case WSPasswordCallback.USERNAME_TOKEN:
        return "USERNAME_TOKEN";
      case WSPasswordCallback.SIGNATURE:
        return "SIGNATURE";
      case WSPasswordCallback.SECURITY_CONTEXT_TOKEN:
        return "SECURITY_CONTEXT_TOKEN";
      case WSPasswordCallback.CUSTOM_TOKEN:
        return "CUSTOM_TOKEN";
      case WSPasswordCallback.SECRET_KEY:
        return "SECRET_KEY";
      case WSPasswordCallback.PASSWORD_ENCRYPTOR_PASSWORD:
        return "PASSWORD_ENCRYPTOR_PASSWORD";
    }
    return "Unknown usage value " + nUsage;
  }

  public void handle (final Callback [] aCallbacks) throws IOException, UnsupportedCallbackException
  {
    for (final Callback aCallback : aCallbacks)
    {
      if (aCallback instanceof WSPasswordCallback)
      {
        final WSPasswordCallback aPasswordCallback = (WSPasswordCallback) aCallback;

        final String sKeyStoreAlias = aPasswordCallback.getIdentifier ();

        // Obtain the password from the crypto factory
        final String sKeyPassword = m_aCryptoFactoryCrypt.getKeyPasswordPerAlias (sKeyStoreAlias);
        if (sKeyPassword != null)
        {
          aPasswordCallback.setPassword (sKeyPassword);
          LOGGER.info ("Found keystore password for alias '" +
                       sKeyStoreAlias +
                       "' and usage " +
                       _getUsage (aPasswordCallback.getUsage ()));
        }
        else
        {
          LOGGER.warn ("Found unsupported keystore alias '" +
                       sKeyStoreAlias +
                       "' and usage " +
                       _getUsage (aPasswordCallback.getUsage ()));
        }
      }
      else
      {
        throw new UnsupportedCallbackException (aCallback, "Unrecognized Callback");
      }
    }
  }
}
