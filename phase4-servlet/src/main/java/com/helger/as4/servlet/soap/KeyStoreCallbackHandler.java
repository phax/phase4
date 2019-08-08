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
package com.helger.as4.servlet.soap;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.phase4.crypto.AS4CryptoProperties;

final class KeyStoreCallbackHandler implements CallbackHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (KeyStoreCallbackHandler.class);
  private final AS4CryptoProperties m_aCryptoProperties;

  public KeyStoreCallbackHandler (@Nonnull final AS4CryptoProperties aCryptoProperties)
  {
    ValueEnforcer.notNull (aCryptoProperties, "CryptoProperties");
    m_aCryptoProperties = aCryptoProperties;
  }

  public void handle (final Callback [] aCallbacks) throws IOException, UnsupportedCallbackException
  {
    for (final Callback aCallback : aCallbacks)
    {
      if (aCallback instanceof WSPasswordCallback)
      {
        final WSPasswordCallback aPasswordCallback = (WSPasswordCallback) aCallback;
        if (m_aCryptoProperties.getKeyAlias ().equals (aPasswordCallback.getIdentifier ()))
        {
          aPasswordCallback.setPassword (m_aCryptoProperties.getKeyPassword ());
          LOGGER.info ("Found keystore password for alias '" + aPasswordCallback.getIdentifier () + "'");
        }
        else
          LOGGER.warn ("Found unsupported keystore alias '" + aPasswordCallback.getIdentifier () + "'");
      }
      else
      {
        throw new UnsupportedCallbackException (aCallback, "Unrecognized Callback");
      }
    }
  }
}
