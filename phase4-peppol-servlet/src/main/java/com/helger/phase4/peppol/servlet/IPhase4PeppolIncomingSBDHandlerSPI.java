/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;

import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.commons.annotation.IsSPIInterface;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;

/**
 * This is the interface that must be implemented to handle incoming SBD
 * documents.
 *
 * @author Philip Helger
 */
@IsSPIInterface
public interface IPhase4PeppolIncomingSBDHandlerSPI
{
  /**
   * Handle the provided incoming StandardBusinessDocument
   *
   * @param aMessageMetadata
   *        Message metadata. Never <code>null</code>. Since v0.9.8
   * @param aHeaders
   *        The HTTP headers of the incoming request. Never <code>null</code>.
   * @param aSBDBytes
   *        The raw SBD bytes. Never <code>null</code>.
   * @param aSBD
   *        The incoming document that is never <code>null</code>. This is the
   *        pre-parsed SBD bytes.
   * @throws Exception
   *         In case it cannot be processed.
   */
  void handleIncomingSBD (@Nonnull IAS4IncomingMessageMetadata aMessageMetadata,
                          @Nonnull HttpHeaderMap aHeaders,
                          @Nonnull byte [] aSBDBytes,
                          @Nonnull StandardBusinessDocument aSBD) throws Exception;
}
