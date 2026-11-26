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
package com.helger.phase4.peppol.servlet;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.io.iface.IHasInputStream;
import com.helger.phase4.incoming.AS4IncomingJAXBDocument;
import com.helger.sbdh.SBDMarshaller;

/**
 * The incoming Peppol Standard Business Document, as passed to
 * {@link IPhase4PeppolIncomingSBDHandlerSPI}. The document is not held in memory: use
 * {@link #getInputStream()} to read it as often as needed, and only use
 * {@link #getStandardBusinessDocument()} if the complete document is really needed in memory.
 * <p>
 * The underlying data is backed by resources (e.g. temporary files) that are deleted when the
 * message processing is finished, so it must be consumed while the handler is invoked.
 * </p>
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@NotThreadSafe
public class Phase4PeppolIncomingSBD extends AS4IncomingJAXBDocument <StandardBusinessDocument>
{
  /**
   * Constructor
   *
   * @param aISP
   *        The input stream provider of the decrypted and decompressed payload. May not be
   *        <code>null</code> and must be readable multiple times.
   */
  public Phase4PeppolIncomingSBD (@NonNull final IHasInputStream aISP)
  {
    super (aISP, SBDMarshaller::new);
  }

  /**
   * Get the complete Standard Business Document as a JAXB object. It is parsed on the first call
   * and cached afterwards. Note: the memory needed depends on the document size, so prefer
   * {@link #getInputStream()} for large documents.
   *
   * @return The parsed Standard Business Document. Never <code>null</code>.
   * @throws IOException
   *         If the document cannot be read or parsed
   */
  @NonNull
  public StandardBusinessDocument getStandardBusinessDocument () throws IOException
  {
    return getParsedDocument ();
  }
}
