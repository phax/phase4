/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.crypto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.config.AS4Configuration;

/**
 * The phase4 crypto settings. By default the properties are read from the files
 * "private-crypto.properties" or "crypto.properties". Alternatively the
 * properties can be provided in the code. See {@link AS4CryptoProperties} for
 * the list of supported property names.
 *
 * @author Philip Helger
 * @deprecated Since 0.11.0; use {@link AS4CryptoFactoryProperties} with
 *             initialization from {@link AS4Configuration} instead.
 */
@Immutable
@Deprecated
public class AS4CryptoFactoryPropertiesFile extends AS4CryptoFactoryProperties
{
  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  private static AS4CryptoFactoryPropertiesFile DEFAULT_INSTANCE = null;

  /**
   * @return The default instance, created by reading the default
   *         "crypto.properties" file. If this file is not present, than this
   *         method returns <code>null</code>.
   * @deprecated Use {@link AS4CryptoFactoryProperties#getDefaultInstance()}
   *             instead.
   */
  @Deprecated
  @Nonnull
  public static AS4CryptoFactoryPropertiesFile getDefaultInstance ()
  {
    // Try in read lock first
    AS4CryptoFactoryPropertiesFile ret = s_aRWLock.readLockedGet ( () -> DEFAULT_INSTANCE);

    if (ret == null)
    {
      s_aRWLock.writeLock ().lock ();
      try
      {
        // Read again in write lock
        ret = DEFAULT_INSTANCE;
        if (ret == null)
        {
          // Create it
          ret = DEFAULT_INSTANCE = new AS4CryptoFactoryPropertiesFile ((String) null);
        }
      }
      finally
      {
        s_aRWLock.writeLock ().unlock ();
      }
    }
    return ret;
  }

  /**
   * Read crypto properties from the specified file path.
   *
   * @param sCryptoPropertiesPath
   *        The class path to read the properties file from. It is
   *        <code>null</code> or empty, than the default file
   *        "crypto.properties" is read.
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static AS4CryptoProperties readCryptoPropertiesFromFile (@Nullable final String sCryptoPropertiesPath)
  {
    AS4CryptoProperties aCryptoProps;
    if (StringHelper.hasNoText (sCryptoPropertiesPath))
    {
      // Uses crypto.properties => needs exact name crypto.properties
      aCryptoProps = new AS4CryptoProperties (new ClassPathResource ("private-crypto.properties"));
      if (!aCryptoProps.isRead ())
        aCryptoProps = new AS4CryptoProperties (new ClassPathResource ("crypto.properties"));
    }
    else
    {
      // Use provided filename
      aCryptoProps = new AS4CryptoProperties (new ClassPathResource (sCryptoPropertiesPath));
    }
    return aCryptoProps;
  }

  /**
   * Should be used if you want to use a non-default crypto properties to create
   * your Crypto-Instance. This constructor reads the properties from a file.
   *
   * @param sCryptoPropertiesPath
   *        when this parameter is <code>null</code>, the default values will
   *        get used. Else it will try to invoke the given properties and read
   *        them throws an exception if it does not work.
   * @throws InitializationException
   *         If the file could not be loaded
   */
  public AS4CryptoFactoryPropertiesFile (@Nullable final String sCryptoPropertiesPath)
  {
    super (readCryptoPropertiesFromFile (sCryptoPropertiesPath));
    if (!cryptoProperties ().isRead ())
      throw new InitializationException ("Failed to locate crypto properties in '" + sCryptoPropertiesPath + "'");
  }

  /**
   * This constructor takes the crypto properties directly. See the
   * {@link com.helger.phase4.client.AbstractAS4Client} for a usage example.
   *
   * @param aCryptoProps
   *        The properties to be used. May not be <code>null</code>. Note: the
   *        object is cloned internally to avoid outside modification.
   * @deprecated Since 0.11.0; use the class {@link AS4CryptoFactoryProperties}
   *             directly.
   */
  @Deprecated
  public AS4CryptoFactoryPropertiesFile (@Nonnull final AS4CryptoProperties aCryptoProps)
  {
    super (aCryptoProps);
  }
}
