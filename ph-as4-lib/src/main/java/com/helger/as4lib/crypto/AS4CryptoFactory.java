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
package com.helger.as4lib.crypto;

import java.io.InputStream;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.dom.engine.WSSConfig;

import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;

@Immutable
public final class AS4CryptoFactory
{
  private static Properties s_aProps;
  private static Crypto s_aCrypto;

  static
  {
    // Init once
    WSSConfig.init ();

    // Uses crypto.properties => needs exact name crypto.properties
    IReadableResource aRes = new ClassPathResource ("private-crypto.properties");
    if (!aRes.exists ())
      aRes = new ClassPathResource ("crypto.properties");

    if (!aRes.exists ())
      throw new InitializationException ("Failed to locate crypto properties");

    try
    {
      s_aProps = new Properties ();
      try (final InputStream aIS = aRes.getInputStream ())
      {
        s_aProps.load (aIS);
      }
      s_aCrypto = CryptoFactory.getInstance (s_aProps);
    }
    catch (final Throwable t)
    {
      throw new InitializationException ("Failed to init crypto properties!", t);
    }
  }

  /** Default encrypt algorithm */
  public static final ECryptoAlgorithmCrypt ENCRYPT_DEFAULT_ALGORITHM = ECryptoAlgorithmCrypt.AES_128_GCM;

  private AS4CryptoFactory ()
  {}

  @Nonnull
  public static Crypto getCrypto ()
  {
    return s_aCrypto;
  }

  @Nullable
  public static String getKeyAlias ()
  {
    return s_aProps.getProperty ("org.apache.wss4j.crypto.merlin.keystore.alias");
  }

  @Nullable
  public static String getKeyPassword ()
  {
    return s_aProps.getProperty ("org.apache.wss4j.crypto.merlin.keystore.password");
  }
}
