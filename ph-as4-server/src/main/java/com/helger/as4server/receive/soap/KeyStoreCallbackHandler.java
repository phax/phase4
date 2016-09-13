package com.helger.as4server.receive.soap;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;

import com.helger.as4lib.crypto.AS4CryptoFactory;

public class KeyStoreCallbackHandler implements CallbackHandler
{
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
        }
      }
      else
      {
        throw new UnsupportedCallbackException (aCallback, "Unrecognized Callback");
      }
    }
  }
}
