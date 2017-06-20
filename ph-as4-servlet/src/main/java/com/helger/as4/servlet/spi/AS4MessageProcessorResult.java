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
package com.helger.as4.servlet.spi;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.state.ESuccess;
import com.helger.commons.state.ISuccessIndicator;
import com.helger.commons.string.ToStringGenerator;

/**
 * This class represents the result of a message processor SPI
 * implementation.<br>
 * Note: cannot be serializable because WSS4JAttachment is not serializable
 *
 * @author Philip Helger
 */
public class AS4MessageProcessorResult implements ISuccessIndicator
{
  private final ESuccess m_eSuccess;
  private final String m_sErrorMsg;
  private final ICommonsList <WSS4JAttachment> m_aAttachments;

  protected AS4MessageProcessorResult (@Nonnull final ESuccess eSuccess,
                                       @Nullable final String sErrorMsg,
                                       @Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    m_eSuccess = ValueEnforcer.notNull (eSuccess, "Success");
    m_sErrorMsg = sErrorMsg;
    m_aAttachments = aAttachments;
  }

  public boolean isSuccess ()
  {
    return m_eSuccess.isSuccess ();
  }

  @Nullable
  public String getErrorMessage ()
  {
    return m_sErrorMsg;
  }

  public boolean hasAttachments ()
  {
    return CollectionHelper.isNotEmpty (m_aAttachments);
  }

  public void addAllAttachmentsTo (@Nonnull final Collection <WSS4JAttachment> aTarget)
  {
    if (m_aAttachments != null)
      aTarget.addAll (m_aAttachments);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <WSS4JAttachment> getAllAttachments ()
  {
    return new CommonsArrayList <> (m_aAttachments);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Success", m_eSuccess)
                                       .appendIf ("ErrorMsg", m_sErrorMsg, x -> m_eSuccess.isFailure ())
                                       .appendIf ("Attachments", m_aAttachments, x -> m_eSuccess.isSuccess ())
                                       .getToString ();
  }

  @Nonnull
  public static AS4MessageProcessorResult createSuccess ()
  {
    return createSuccess (null);
  }

  @Nonnull
  public static AS4MessageProcessorResult createSuccess (@Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    return new AS4MessageProcessorResult (ESuccess.SUCCESS, null, aAttachments);
  }

  @Nonnull
  public static AS4MessageProcessorResult createFailure (@Nonnull final String sErrorMsg)
  {
    ValueEnforcer.notNull (sErrorMsg, "ErrorMsg");
    return new AS4MessageProcessorResult (ESuccess.FAILURE, sErrorMsg, null);
  }
}
