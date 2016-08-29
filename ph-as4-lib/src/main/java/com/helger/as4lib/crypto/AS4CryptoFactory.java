package com.helger.as4lib.crypto;

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
    final IReadableResource aRes = new ClassPathResource ("crypto.properties");
    if (!aRes.exists ())
      throw new InitializationException ("Failed to locate crypto.properties");

    try
    {
      s_aProps = new Properties ();
      s_aProps.load (aRes.getInputStream ());
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
  public static Crypto createCrypto ()
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
