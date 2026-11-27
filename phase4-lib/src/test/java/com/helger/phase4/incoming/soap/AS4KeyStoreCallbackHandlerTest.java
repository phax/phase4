/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.security.KeyStore;

import javax.security.auth.callback.Callback;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.phase4.crypto.ECryptoMode;
import com.helger.phase4.crypto.IAS4CryptoFactory;

/**
 * Test class for class {@link AS4KeyStoreCallbackHandler}.
 *
 * @author Philip Helger
 */
public final class AS4KeyStoreCallbackHandlerTest
{
  private static final String KEY_ALIAS = "mykey";
  private static final String KEY_PASSWORD = "secret";

  /**
   * A crypto factory that only knows the password of a single key alias.
   */
  private static final class MockCryptoFactory implements IAS4CryptoFactory
  {
    @Nullable
    public Crypto getCrypto (@NonNull final ECryptoMode eCryptoMode)
    {
      return null;
    }

    @Nullable
    public KeyStore getKeyStore ()
    {
      return null;
    }

    public KeyStore.@Nullable PrivateKeyEntry getPrivateKeyEntry ()
    {
      return null;
    }

    @Nullable
    public String getKeyAlias ()
    {
      return KEY_ALIAS;
    }

    public char @Nullable [] getKeyPasswordPerAliasCharArray (@Nullable final String sSearchKeyAlias)
    {
      return KEY_ALIAS.equals (sSearchKeyAlias) ? KEY_PASSWORD.toCharArray () : null;
    }

    @Nullable
    public KeyStore getTrustStore ()
    {
      return null;
    }
  }

  @Test
  public void testDecryptAndSignature () throws Exception
  {
    final AS4KeyStoreCallbackHandler aHandler = new AS4KeyStoreCallbackHandler (new MockCryptoFactory ());

    final WSPasswordCallback aDecrypt = new WSPasswordCallback (KEY_ALIAS, WSPasswordCallback.DECRYPT);
    final WSPasswordCallback aSignature = new WSPasswordCallback (KEY_ALIAS, WSPasswordCallback.SIGNATURE);
    final WSPasswordCallback aUnknownAlias = new WSPasswordCallback ("other", WSPasswordCallback.DECRYPT);
    aHandler.handle (new Callback [] { aDecrypt, aSignature, aUnknownAlias });

    assertEquals (KEY_PASSWORD, aDecrypt.getPassword ());
    assertEquals (KEY_PASSWORD, aSignature.getPassword ());
    assertNull (aUnknownAlias.getPassword ());
  }

  @Test
  public void testUsernameTokenIsNotAnswered () throws Exception
  {
    final AS4KeyStoreCallbackHandler aHandler = new AS4KeyStoreCallbackHandler (new MockCryptoFactory ());

    // A UsernameToken user name equal to the key alias must not reveal the key password
    final WSPasswordCallback aUsernameToken = new WSPasswordCallback (KEY_ALIAS, WSPasswordCallback.USERNAME_TOKEN);
    final WSPasswordCallback aSecretKey = new WSPasswordCallback (KEY_ALIAS, WSPasswordCallback.SECRET_KEY);
    aHandler.handle (new Callback [] { aUsernameToken, aSecretKey });

    assertNull (aUsernameToken.getPassword ());
    assertNull (aSecretKey.getPassword ());
  }
}
