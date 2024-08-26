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
package com.helger.phase4.incoming.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3UserMessage;

/**
 * This class represents the result of a message processor SPI
 * implementation.<br>
 * Note: cannot be serializable because WSS4JAttachment is not serializable
 *
 * @author Philip Helger
 */
@Immutable
public class AS4SignalMessageProcessorResult extends AS4MessageProcessorResult
{
  private final Ebms3UserMessage m_aPullReturnUserMessage;

  /**
   * @param eSuccess
   *        Success or failure. May not be <code>null</code>.
   * @param aAttachments
   *        The response attachments. May be <code>null</code>.
   * @param sAsyncResponseURL
   *        The asynchronous response URLs. May be <code>null</code>.
   * @param aPullReturnUserMessage
   *        The user message to return. May be <code>null</code>.
   */
  protected AS4SignalMessageProcessorResult (@Nonnull final ESuccess eSuccess,
                                             @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                             @Nullable final String sAsyncResponseURL,
                                             @Nullable final Ebms3UserMessage aPullReturnUserMessage)
  {
    super (eSuccess, aAttachments, sAsyncResponseURL);
    m_aPullReturnUserMessage = aPullReturnUserMessage;
  }

  /**
   * @return Optional response user message for all "pull" based SPI
   *         invocations. May be <code>null</code>.
   */
  @Nullable
  public Ebms3UserMessage getPullReturnUserMessage ()
  {
    return m_aPullReturnUserMessage;
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("PullReturnUserMessage", m_aPullReturnUserMessage)
                            .getToString ();
  }

  @Nonnull
  public static AS4SignalMessageProcessorResult createSuccess ()
  {
    return createSuccess (null, null, null);
  }

  @Nonnull
  public static AS4SignalMessageProcessorResult createSuccess (@Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                                               @Nullable final String sAsyncResponseURL,
                                                               @Nullable final Ebms3UserMessage aPullReturnUserMessage)
  {
    return new AS4SignalMessageProcessorResult (ESuccess.SUCCESS,
                                                aAttachments,
                                                sAsyncResponseURL,
                                                aPullReturnUserMessage);
  }

  @Nonnull
  public static AS4SignalMessageProcessorResult createFailure ()
  {
    return new AS4SignalMessageProcessorResult (ESuccess.FAILURE, null, null, null);
  }
}
