/*
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.phase4.crypto.AS4CryptoFactoryProperties;
import com.helger.phase4.crypto.AS4CryptoProperties;
import com.helger.phase4.dump.AS4DumpReader;

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

    final AS4CryptoProperties aCP = new AS4CryptoProperties (new FileSystemResource (folder, "crypto.properties"));
    aCP.setKeyStorePath (folder.getAbsolutePath () + "/" + aCP.getKeyStorePath ());
    aCP.setTrustStorePath (folder.getAbsolutePath () + "/" + aCP.getTrustStorePath ());

    LOGGER.info ("Reading " + f.getName ());
    final byte [] aBytes = SimpleFileIO.getAllFileBytes (f);

    AS4DumpReader.decryptAS4In (aBytes,
                                new AS4CryptoFactoryProperties (aCP),
                                null,
                                aDecryptedBytes -> SimpleFileIO.writeFile (new File (folder, "payload.decrypted"), aDecryptedBytes));
  }
}
