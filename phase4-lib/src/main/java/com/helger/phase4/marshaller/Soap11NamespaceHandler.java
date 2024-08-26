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
package com.helger.phase4.marshaller;

import javax.annotation.Nonnull;

import com.helger.phase4.model.ESoapVersion;
import com.helger.xml.namespace.MapBasedNamespaceContext;

/**
 * A special XML namespace context that contains all default SOAP 1.1 mappings.
 *
 * @author Philip Helger
 * @since 2.0.0
 */
public class Soap11NamespaceHandler extends MapBasedNamespaceContext
{
  private static final class SingletonHolder
  {
    static final Soap11NamespaceHandler INSTANCE = new Soap11NamespaceHandler ();
  }

  public Soap11NamespaceHandler ()
  {
    addMapping (ESoapVersion.SOAP_11.getNamespacePrefix (), ESoapVersion.SOAP_11.getNamespaceURI ());
  }

  /**
   * @return The global instance of the namespace handler. Never
   *         <code>null</code>. Don't modify it! To modify it, please clone it
   *         and go from there.
   */
  @Nonnull
  public static Soap11NamespaceHandler getInstance ()
  {
    return SingletonHolder.INSTANCE;
  }
}
