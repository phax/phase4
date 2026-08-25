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
package com.helger.phase4.wss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Pattern;

import org.junit.Test;

/** Verifies a complete AS4 sign/verify cycle without BC, ph-bc, or Cryptacular. */
public final class AS4SignatureWithoutBCTest
{
  private static final String PROBE_CLASS = AS4SignatureWithoutBCProbe.class.getName ();

  @Test
  public void testSignAndVerifyWithoutBC () throws Exception
  {
    final String sTestClasspath = System.getProperty ("surefire.test.class.path");
    if (sTestClasspath == null)
      throw new IllegalStateException ("The Surefire test classpath is unavailable");

    final String [] aClasspathEntries = sTestClasspath.split (Pattern.quote (File.pathSeparator));
    final URL [] aURLs = new URL [aClasspathEntries.length];
    for (int i = 0; i < aClasspathEntries.length; ++i)
      aURLs[i] = new File (aClasspathEntries[i]).toURI ().toURL ();

    final ClassLoader aOldContextClassLoader = Thread.currentThread ().getContextClassLoader ();
    try (final URLClassLoader aCL = new URLClassLoader (aURLs, ClassLoader.getPlatformClassLoader ())
    {
      @Override
      protected Class <?> loadClass (final String sName, final boolean bResolve) throws ClassNotFoundException
      {
        if (sName.startsWith ("org.bouncycastle.") ||
            sName.startsWith ("com.helger.bc.") ||
            sName.startsWith ("org.cryptacular."))
          throw new ClassNotFoundException ("Optional crypto/SAML dependency deliberately hidden from test class loader");
        return super.loadClass (sName, bResolve);
      }
    })
    {
      Thread.currentThread ().setContextClassLoader (aCL);
      assertThrows (ClassNotFoundException.class,
                    () -> aCL.loadClass ("org.bouncycastle.asn1.ASN1Primitive"));
      assertThrows (ClassNotFoundException.class, () -> aCL.loadClass ("com.helger.bc.PBCProvider"));
      assertThrows (ClassNotFoundException.class, () -> aCL.loadClass ("org.cryptacular.util.CipherUtil"));

      final Class <?> aProbeClass = Class.forName (PROBE_CLASS, true, aCL);
      assertEquals ("DIRECT_REF", aProbeClass.getMethod ("verify").invoke (null));
    }
    finally
    {
      Thread.currentThread ().setContextClassLoader (aOldContextClassLoader);
    }
  }
}
