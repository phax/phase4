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

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.ESuccess;
import com.helger.commons.state.ISuccessIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.attachment.WSS4JAttachment;

/**
 * This class represents the result of a message processor SPI
 * implementation.<br>
 * Note: cannot be serializable because WSS4JAttachment is not serializable
 *
 * @author Philip Helger
 */
@Immutable
public class AS4MessageProcessorResult implements ISuccessIndicator
{
  private final ESuccess m_eSuccess;
  private final ICommonsList <WSS4JAttachment> m_aAttachments;
  private final String m_sAsyncResponseURL;

  /**
   * @param eSuccess
   *        Success or failure. May not be <code>null</code>.
   * @param aAttachments
   *        The response attachments. May be <code>null</code>.
   * @param sAsyncResponseURL
   *        The asynchronous response URLs. May be <code>null</code>.
   */
  protected AS4MessageProcessorResult (@Nonnull final ESuccess eSuccess,
                                       @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                       @Nullable final String sAsyncResponseURL)
  {
    ValueEnforcer.notNull (eSuccess, "Success");

    m_eSuccess = eSuccess;
    m_aAttachments = aAttachments;
    m_sAsyncResponseURL = sAsyncResponseURL;
  }

  public boolean isSuccess ()
  {
    return m_eSuccess.isSuccess ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <WSS4JAttachment> getAllAttachments ()
  {
    return new CommonsArrayList <> (m_aAttachments);
  }

  public boolean hasAttachments ()
  {
    return CollectionHelper.isNotEmpty (m_aAttachments);
  }

  /**
   * Add all attachments contained in this object onto the provided target
   * collection.
   *
   * @param aTarget
   *        The target collection. May not be <code>null</code>.
   */
  public void addAllAttachmentsTo (@Nonnull final Collection <? super WSS4JAttachment> aTarget)
  {
    if (m_aAttachments != null)
      aTarget.addAll (m_aAttachments);
  }

  /**
   * @return The asynchronous response URL. May be <code>null</code>.
   */
  @Nullable
  public String getAsyncResponseURL ()
  {
    return m_sAsyncResponseURL;
  }

  /**
   * @return <code>true</code> if an asynchronous response URL is present,
   *         <code>false</code> otherwise.
   */
  public boolean hasAsyncResponseURL ()
  {
    return StringHelper.hasText (m_sAsyncResponseURL);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Success", m_eSuccess)
                                       .appendIf ("Attachments", m_aAttachments, m_eSuccess::isSuccess)
                                       .appendIfNotNull ("AsyncResponseURL", m_sAsyncResponseURL)
                                       .getToString ();
  }

  /**
   * @return A new success object. No attachments, no nothing.
   */
  @Nonnull
  public static AS4MessageProcessorResult createSuccess ()
  {
    return createSuccessExt (null, null);
  }

  /**
   * Create a success message with optional attachments. Usually you don't need
   * this. Just call {@link #createSuccess()} and you are fine.
   *
   * @param aAttachments
   *        Optional list of RESPONSE (!) attachments. Don't put the incoming
   *        attachments here.
   * @param sAsyncResponseURL
   *        The optional asynchronous response URL.
   * @return Never <code>null</code>.
   * @see #createSuccess()
   * @since 0.9.7
   */
  @Nonnull
  public static AS4MessageProcessorResult createSuccessExt (@Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                                            @Nullable final String sAsyncResponseURL)
  {
    return new AS4MessageProcessorResult (ESuccess.SUCCESS, aAttachments, sAsyncResponseURL);
  }

  /**
   * Create a negative response.
   *
   * @return Never <code>null</code>.
   * @since 2.3.0
   */
  @Nonnull
  public static AS4MessageProcessorResult createFailure ()
  {
    return new AS4MessageProcessorResult (ESuccess.FAILURE, (ICommonsList <WSS4JAttachment>) null, (String) null);
  }
}
