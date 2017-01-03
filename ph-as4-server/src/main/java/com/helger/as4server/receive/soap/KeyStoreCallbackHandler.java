/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4server.receive.soap;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4lib.crypto.AS4CryptoFactory;

final class KeyStoreCallbackHandler implements CallbackHandler
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (KeyStoreCallbackHandler.class);

  public void handle (final Callback [] aCallbacks) throws IOException, UnsupportedCallbackException
  {
    for (final Callback aCallback : aCallbacks)
    {
      if (aCallback instanceof WSPasswordCallback)
      {
        final WSPasswordCallback aPasswordCallback = (WSPasswordCallback) aCallback;
        if (AS4CryptoFactory.getKeyAlias ().equals (aPasswordCallback.getIdentifier ()))
        {
          aPasswordCallback.setPassword (AS4CryptoFactory.getKeyPassword ());
          s_aLogger.info ("Found keystore password for alias '" + aPasswordCallback.getIdentifier () + "'");
        }
        else
          s_aLogger.warn ("Found unsupported keystore alias '" + aPasswordCallback.getIdentifier () + "'");
      }
      else
      {
        throw new UnsupportedCallbackException (aCallback, "Unrecognized Callback");
      }
    }
  }
}
