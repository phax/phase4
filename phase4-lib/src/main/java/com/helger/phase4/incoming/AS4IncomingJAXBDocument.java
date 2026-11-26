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
package com.helger.phase4.incoming;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.iface.IHasInputStream;
import com.helger.jaxb.GenericJAXBMarshaller;

/**
 * An {@link AS4IncomingDocument} that can also be provided as a JAXB object. The JAXB object is only
 * created on the first request and then cached, because it may need a lot of memory for large
 * documents.
 *
 * @author Philip Helger
 * @param <JAXBTYPE>
 *        The JAXB type of the document
 * @since 5.0.0
 */
@NotThreadSafe
public class AS4IncomingJAXBDocument <JAXBTYPE> extends AS4IncomingDocument
{
  private final Supplier <? extends GenericJAXBMarshaller <JAXBTYPE>> m_aMarshallerFactory;
  private JAXBTYPE m_aParsedDocument;

  /**
   * Constructor
   *
   * @param aISP
   *        The input stream provider. May not be <code>null</code> and must be readable multiple
   *        times.
   * @param aMarshallerFactory
   *        The factory for the marshaller to read the document. May not be <code>null</code>.
   */
  public AS4IncomingJAXBDocument (@NonNull final IHasInputStream aISP,
                                  @NonNull final Supplier <? extends GenericJAXBMarshaller <JAXBTYPE>> aMarshallerFactory)
  {
    super (aISP);
    ValueEnforcer.notNull (aMarshallerFactory, "MarshallerFactory");
    m_aMarshallerFactory = aMarshallerFactory;
  }

  /**
   * Get the document as a JAXB object. It is read on the first call and cached afterwards. Note:
   * the memory needed depends on the document size.
   *
   * @return The JAXB object. Never <code>null</code>.
   * @throws IOException
   *         If the document cannot be read or parsed
   */
  @NonNull
  public final JAXBTYPE getParsedDocument () throws IOException
  {
    JAXBTYPE ret = m_aParsedDocument;
    if (ret == null)
    {
      try (final InputStream aIS = getInputStream ())
      {
        ret = m_aMarshallerFactory.get ().read (aIS);
      }
      if (ret == null)
        throw new IOException ("Failed to parse the incoming document");
      m_aParsedDocument = ret;
    }
    return ret;
  }
}
