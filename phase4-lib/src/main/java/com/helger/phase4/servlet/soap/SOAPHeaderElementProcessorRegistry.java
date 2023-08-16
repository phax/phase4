/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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
package com.helger.phase4.servlet.soap;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.equals.EqualsHelper;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.crypto.IAS4IncomingSecurityConfiguration;
import com.helger.phase4.crypto.IAS4PModeAwareCryptoFactory;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.resolve.IPModeResolver;

/**
 * This class manages the SOAP header element processors. This is used to
 * validate the "must understand" SOAP requirement. It manages all instances of
 * {@link ISOAPHeaderElementProcessor}.
 *
 * @author Philip Helger
 * @author Gregor Scholtysik
 */
@NotThreadSafe
public class SOAPHeaderElementProcessorRegistry
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SOAPHeaderElementProcessorRegistry.class);
  private final ICommonsOrderedMap <QName, ISOAPHeaderElementProcessor> m_aMap = new CommonsLinkedHashMap <> ();

  public SOAPHeaderElementProcessorRegistry ()
  {}

  public void registerHeaderElementProcessor (@Nonnull final QName aQName,
                                              @Nonnull final ISOAPHeaderElementProcessor aProcessor)
  {
    ValueEnforcer.notNull (aQName, "QName");
    ValueEnforcer.notNull (aProcessor, "Processor");

    if (m_aMap.containsKey (aQName))
      throw new IllegalArgumentException ("A processor for QName " + aQName.toString () + " is already registered!");
    m_aMap.put (aQName, aProcessor);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Successfully registered SOAP header element processor for " + aQName.toString ());
  }

  @Nullable
  public ISOAPHeaderElementProcessor getHeaderElementProcessor (@Nullable final QName aQName)
  {
    if (aQName == null)
      return null;
    return m_aMap.get (aQName);
  }

  public boolean containsHeaderElementProcessor (@Nullable final QName aQName)
  {
    return aQName != null && m_aMap.containsKey (aQName);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsOrderedMap <QName, ISOAPHeaderElementProcessor> getAllElementProcessors ()
  {
    return m_aMap.getClone ();
  }

  @Nonnull
  public static SOAPHeaderElementProcessorRegistry createDefault (@Nonnull final IPModeResolver aPModeResolver,
                                                                  @Nonnull final IAS4CryptoFactory aCryptoFactorySign,
                                                                  @Nonnull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                                                  @Nullable final IPMode aFallbackPMode,
                                                                  @Nonnull final IAS4IncomingSecurityConfiguration aIncomingSecurityConfiguration)
  {
    // Register all SOAP header element processors
    // Registration order matches execution order!
    final SOAPHeaderElementProcessorRegistry ret = new SOAPHeaderElementProcessorRegistry ();

    // callback notifying a IAS4PModeAwareCryptoFactory about a successful PMode
    // resolution
    final Consumer <IPMode> aPModeConsumer = aPMode -> {
      if (aCryptoFactorySign instanceof IAS4PModeAwareCryptoFactory)
      {
        ((IAS4PModeAwareCryptoFactory) aCryptoFactorySign).setContextPMode (aPMode);
      }

      // Avoid setting it twice on the same object
      if (!EqualsHelper.identityEqual (aCryptoFactorySign, aCryptoFactoryCrypt))
        if (aCryptoFactoryCrypt instanceof IAS4PModeAwareCryptoFactory)
        {
          ((IAS4PModeAwareCryptoFactory) aCryptoFactoryCrypt).setContextPMode (aPMode);
        }
    };

    ret.registerHeaderElementProcessor (SOAPHeaderElementProcessorExtractEbms3Messaging.QNAME_MESSAGING,
                                        new SOAPHeaderElementProcessorExtractEbms3Messaging (aPModeResolver,
                                                                                             aPModeConsumer));

    // WSS4J must be after Ebms3Messaging handler!
    final Supplier <? extends IPMode> aFallbackPModeProvider = () -> aFallbackPMode;
    ret.registerHeaderElementProcessor (SOAPHeaderElementProcessorWSS4J.QNAME_SECURITY,
                                        new SOAPHeaderElementProcessorWSS4J (aCryptoFactorySign,
                                                                             aCryptoFactoryCrypt,
                                                                             aIncomingSecurityConfiguration.getSecurityProviderSign (),
                                                                             aFallbackPModeProvider,
                                                                             aIncomingSecurityConfiguration.getDecryptParameterModifier ()));
    return ret;
  }
}
