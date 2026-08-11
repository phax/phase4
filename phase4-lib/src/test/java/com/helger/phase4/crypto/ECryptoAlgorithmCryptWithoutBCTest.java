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
package com.helger.phase4.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

/**
 * Verifies direct enum initialization and provider-neutral OID access without Bouncy Castle. This
 * deliberately does not reflect over all enum methods because the retained legacy method
 * descriptor still references a Bouncy Castle type.
 */
public final class ECryptoAlgorithmCryptWithoutBCTest
{
  private static final String ENUM_CLASS = ECryptoAlgorithmCrypt.class.getName ();
  private static final String INTERFACE_CLASS = ICryptoAlgorithmCrypt.class.getName ();
  private static final String PROBE_CLASS = ECryptoAlgorithmCryptWithoutBCProbe.class.getName ();

  @Test
  public void testDirectEnumUseWithoutBC () throws Exception
  {
    final URL [] aURLs = { new File ("target/classes").toURI ().toURL (),
                          new File ("target/test-classes").toURI ().toURL () };
    try (final URLClassLoader aCL = new URLClassLoader (aURLs, getClass ().getClassLoader ())
    {
      @Override
      protected Class <?> loadClass (final String sName, final boolean bResolve) throws ClassNotFoundException
      {
        if (sName.startsWith ("org.bouncycastle."))
          throw new ClassNotFoundException ("Bouncy Castle deliberately hidden from test class loader");

        if (sName.equals (ENUM_CLASS) || sName.equals (INTERFACE_CLASS) || sName.equals (PROBE_CLASS))
          synchronized (getClassLoadingLock (sName))
          {
            Class <?> ret = findLoadedClass (sName);
            if (ret == null)
              ret = findClass (sName);
            if (bResolve)
              resolveClass (ret);
            return ret;
          }

        return super.loadClass (sName, bResolve);
      }
    })
    {
      try
      {
        aCL.loadClass ("org.bouncycastle.asn1.ASN1ObjectIdentifier");
        fail ("Bouncy Castle must not be visible to the isolated class loader");
      }
      catch (final ClassNotFoundException ex)
      {
        // Expected
      }

      final Class <?> aProbeClass = Class.forName (PROBE_CLASS, true, aCL);
      final String [] aActual = (String []) aProbeClass.getMethod ("getOIDStrings").invoke (null);
      assertArrayEquals (new String [] { "1.2.840.113549.3.7",
                                        "2.16.840.1.101.3.4.1.2",
                                        "2.16.840.1.101.3.4.1.6",
                                        "2.16.840.1.101.3.4.1.22",
                                        "2.16.840.1.101.3.4.1.26",
                                        "2.16.840.1.101.3.4.1.42",
                                        "2.16.840.1.101.3.4.1.46" },
                         aActual);
    }
  }
}
