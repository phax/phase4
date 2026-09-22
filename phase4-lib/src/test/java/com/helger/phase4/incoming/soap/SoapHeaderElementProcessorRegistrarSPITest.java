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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.KeyStore;

import javax.xml.namespace.QName;

import org.apache.wss4j.common.crypto.Crypto;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.phase4.crypto.ECryptoMode;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.incoming.AS4IncomingReceiverConfiguration;
import com.helger.phase4.incoming.crypto.AS4IncomingSecurityConfiguration;
import com.helger.phase4.model.pmode.IPMode;

/**
 * Test class for the {@link IAS4SoapHeaderElementProcessorRegistrarSPI} extension point.
 *
 * @author Philip Helger
 */
public final class SoapHeaderElementProcessorRegistrarSPITest
{
  /**
   * A crypto factory that is never actually used - {@code createDefault} only stores the reference.
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
      return null;
    }

    public char @Nullable [] getKeyPasswordPerAliasCharArray (@Nullable final String sSearchKeyAlias)
    {
      return null;
    }

    @Nullable
    public KeyStore getTrustStore ()
    {
      return null;
    }
  }

  @NonNull
  private static SoapHeaderElementProcessorRegistry _createDefaultRegistry ()
  {
    final IAS4CryptoFactory aCF = new MockCryptoFactory ();
    return SoapHeaderElementProcessorRegistry.createDefault ( (a, b, c, d, e, f, g) -> null,
                                                             aCF,
                                                             aCF,
                                                             (IPMode) null,
                                                             new AS4IncomingSecurityConfiguration (),
                                                             new AS4IncomingReceiverConfiguration ());
  }

  /**
   * The two built-in processors must still be registered first, and in the documented order,
   * because the registration order is also the execution order.
   */
  @Test
  public void testBuiltInProcessorsComeFirst ()
  {
    final ICommonsOrderedMap <QName, ISoapHeaderElementProcessor> aAll = _createDefaultRegistry ().getAllElementProcessors ();
    assertTrue (aAll.size () >= 2);

    final var aKeys = aAll.copyOfKeySet ();
    assertSame (SoapHeaderElementProcessorExtractEbms3Messaging.QNAME_MESSAGING, aKeys.getFirstOrNull ());
    assertSame (SoapHeaderElementProcessorWSS4J.QNAME_SECURITY, aKeys.getAtIndex (1));
  }

  /**
   * An SPI registered processor must be present in the default registry, so that a SOAP header
   * element unknown to phase4 and carrying "mustUnderstand" can be handled.
   */
  @Test
  public void testSPIRegisteredProcessorIsPresent ()
  {
    final SoapHeaderElementProcessorRegistry aRegistry = _createDefaultRegistry ();
    assertTrue ("The mock SPI did not register its processor",
                aRegistry.containsHeaderElementProcessor (MockSoapHeaderElementProcessorRegistrarSPI.QNAME_MOCK));
    assertNotNull (aRegistry.getHeaderElementProcessor (MockSoapHeaderElementProcessorRegistrarSPI.QNAME_MOCK));
  }

  /**
   * SPI registered processors must run after the built-in ones.
   */
  @Test
  public void testSPIRegisteredProcessorComesLast ()
  {
    final ICommonsOrderedMap <QName, ISoapHeaderElementProcessor> aAll = _createDefaultRegistry ().getAllElementProcessors ();
    assertSame (MockSoapHeaderElementProcessorRegistrarSPI.QNAME_MOCK, aAll.copyOfKeySet ().getLastOrNull ());
  }
}
