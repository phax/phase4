/*
 * Copyright (C) 2021 Philip Helger (www.helger.com)
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
package com.helger.phase4.springboot.service;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.commons.http.HttpHeaderMap;
import com.helger.peppol.sbdh.PeppolSBDHDocument;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.servlet.IAS4MessageState;
import com.helger.phase4.springboot.enumeration.ESBDHHandlerServiceSelector;

@Scope (value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Service (ESBDHHandlerServiceSelector.Constants.CUSTOM_PEPPOL_INCOMING_VALUE)
@SuppressWarnings ("unused")
public class CustomPeppolIncomingSBDHandlerServiceImpl implements ISBDHandlerService
{
  // Non-managed Spring fields
  private IAS4IncomingMessageMetadata m_aMessageMetadata;
  private HttpHeaderMap m_aHttpHeaders;
  private Ebms3UserMessage m_aUserMessage;
  private byte [] m_aStandardBusinessDocumentBytes;
  private StandardBusinessDocument m_aStandardBusinessDocument;
  private PeppolSBDHDocument m_aPeppolStandardBusinessDocumentHeader;
  private IAS4MessageState m_aMessageState;

  // Managed Spring fields

  public CustomPeppolIncomingSBDHandlerServiceImpl ()
  {}

  public void setMessageMetadata (@Nonnull final IAS4IncomingMessageMetadata messageMetadata)
  {
    m_aMessageMetadata = messageMetadata;
  }

  public void setHttpHeaders (@Nonnull final HttpHeaderMap httpHeaders)
  {
    m_aHttpHeaders = httpHeaders;
  }

  public void setUserMessage (@Nonnull final Ebms3UserMessage userMessage)
  {
    m_aUserMessage = userMessage;
  }

  public void setStandardBusinessDocumentBytes (@Nonnull final byte [] standardBusinessDocumentBytes)
  {
    m_aStandardBusinessDocumentBytes = standardBusinessDocumentBytes;
  }

  public void setStandardBusinessDocument (@Nonnull final StandardBusinessDocument standardBusinessDocument)
  {
    m_aStandardBusinessDocument = standardBusinessDocument;
  }

  public void setPeppolStandardBusinessDocumentHeader (@Nonnull final PeppolSBDHDocument peppolStandardBusinessDocumentHeader)
  {
    m_aPeppolStandardBusinessDocumentHeader = peppolStandardBusinessDocumentHeader;
  }

  public void setMessageState (@Nonnull final IAS4MessageState messageState)
  {
    m_aMessageState = messageState;
  }

  public void handle () throws Exception
  {
    // TODO
    // Very complex logic that leads to persisting the document in a database.
  }
}
