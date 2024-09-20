/*
 * Copyright (C) 2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.euctp;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import org.junit.Test;

import com.helger.security.keystore.EKeyStoreType;

/**
 * Test class for class {@link Phase4EuCtpHttpClientSettings}
 *
 * @author Ulrik Stehling
 * @author Philip Helger
 */
public class Phase4EuCtpHttpClientSettingsTest
{
  @Test
  public void testBasic () throws GeneralSecurityException, IOException
  {
    final KeyStore aKeyStore = EKeyStoreType.PKCS12.getKeyStore ();
    final char [] password = "justForTesting1".toCharArray ();
    aKeyStore.load (getClass ().getClassLoader ().getResourceAsStream ("crypto/testClient.keystore"), password);
    new Phase4EuCtpHttpClientSettings (aKeyStore, password);
  }
}
