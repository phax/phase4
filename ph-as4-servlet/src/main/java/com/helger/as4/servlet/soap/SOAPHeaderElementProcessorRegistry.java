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
package com.helger.as4.servlet.soap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsOrderedMap;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;

/**
 * This class manages the SOAP header element processors. This is used to
 * validate the "must understand" SOAP requirement. It manages all instances of
 * {@link ISOAPHeaderElementProcessor}.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class SOAPHeaderElementProcessorRegistry extends AbstractGlobalSingleton
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SOAPHeaderElementProcessorRegistry.class);
  @GuardedBy ("m_aRWLock")
  private final ICommonsOrderedMap <QName, ISOAPHeaderElementProcessor> m_aMap = new CommonsLinkedHashMap<> ();

  @Deprecated
  @UsedViaReflection
  public SOAPHeaderElementProcessorRegistry ()
  {}

  @Nonnull
  public static SOAPHeaderElementProcessorRegistry getInstance ()
  {
    return getGlobalSingleton (SOAPHeaderElementProcessorRegistry.class);
  }

  public void registerHeaderElementProcessor (@Nonnull final QName aQName,
                                              @Nonnull final ISOAPHeaderElementProcessor aProcessor)
  {
    ValueEnforcer.notNull (aQName, "QName");
    ValueEnforcer.notNull (aProcessor, "Processor");

    m_aRWLock.writeLocked ( () -> {
      if (m_aMap.containsKey (aQName))
        throw new IllegalArgumentException ("A processor for QName " + aQName.toString () + " is already registered!");
      m_aMap.put (aQName, aProcessor);
    });
    s_aLogger.info ("Successfully registered SOAP header element processor for " + aQName.toString ());
  }

  @Nullable
  public ISOAPHeaderElementProcessor getHeaderElementProcessor (@Nullable final QName aQName)
  {
    if (aQName == null)
      return null;
    return m_aRWLock.readLocked ( () -> m_aMap.get (aQName));
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsOrderedMap <QName, ISOAPHeaderElementProcessor> getAllElementProcessors ()
  {
    return m_aRWLock.readLocked ( () -> m_aMap.getClone ());
  }
}
