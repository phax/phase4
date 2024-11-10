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
package com.helger.phase4.supplementary.tools;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.file.SimpleFileIO;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.crypto.AS4KeyStoreDescriptor;
import com.helger.phase4.crypto.AS4TrustStoreDescriptor;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpReader;
import com.helger.security.keystore.KeyStoreAndKeyDescriptor;
import com.helger.security.keystore.TrustStoreDescriptor;

/**
 * This is a small tool that demonstrates how the "as4in" files can be decrypted
 * later, assuming the correct certificate is provided.
 *
 * @author Philip Helger
 */
public final class MainDecipherAS4In
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainDecipherAS4In.class);

  public static void main (final String [] args) throws Exception
  {
    final File folder = new File ("src/test/resources/incoming/");
    if (!folder.isDirectory ())
      throw new IllegalStateException ();
    final File f = new File (folder, "165445-9-8a813f8d-3dda-4ef9-868e-f6d829972d4e.as4in");
    if (!f.exists ())
      throw new IllegalStateException ();

    // Change path of key store and trust store
    KeyStoreAndKeyDescriptor aKSD = AS4KeyStoreDescriptor.createFromConfig ();
    aKSD = KeyStoreAndKeyDescriptor.builder (aKSD)
                                   .path (folder.getAbsolutePath () + "/" + aKSD.getKeyStorePath ())
                                   .build ();
    TrustStoreDescriptor aTSD = AS4TrustStoreDescriptor.createFromConfig ();
    aTSD = TrustStoreDescriptor.builder (aTSD)
                               .path (folder.getAbsolutePath () + "/" + aTSD.getTrustStorePath ())
                               .build ();
    final IAS4CryptoFactory aCryptoFactory = new AS4CryptoFactoryInMemoryKeyStore (aKSD, aTSD);

    LOGGER.info ("Reading " + f.getName ());
    final byte [] aBytes = SimpleFileIO.getAllFileBytes (f);
    if (aBytes == null)
      throw new IllegalStateException ("Failed to read file content as byte array");

    AS4DumpReader.decryptAS4In ("as4-profileid",
                                aBytes,
                                aCryptoFactory,
                                aCryptoFactory,
                                null,
                                (nIndex, aDecryptedBytes) -> SimpleFileIO.writeFile (
                                                                                     new File (folder,
                                                                                               "payload-" +
                                                                                                       nIndex +
                                                                                                       ".decrypted"),
                                                                                     aDecryptedBytes));
  }
}
