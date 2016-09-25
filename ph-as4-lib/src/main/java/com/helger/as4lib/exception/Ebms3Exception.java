/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.exception;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.error.EEbmsError;
import com.helger.commons.string.StringHelper;

public class Ebms3Exception extends Exception
{
  private final EEbmsError m_eError;
  private final String m_sAdditionalInformation;
  private final String m_sRefToMessageId;

  public Ebms3Exception (@Nonnull final EEbmsError eError,
                         @Nullable final String sAdditionalInformation,
                         @Nullable final String sRefToMessageId)
  {
    super (StringHelper.getImplodedNonEmpty (" - ",
                                             eError.getErrorCode (),
                                             eError.getShortDescription (),
                                             sAdditionalInformation));
    m_eError = eError;
    m_sAdditionalInformation = sAdditionalInformation;
    m_sRefToMessageId = sRefToMessageId;
  }

  @Nonnull
  public EEbmsError getError ()
  {
    return m_eError;
  }

  @Nullable
  public String getAdditionalInformation ()
  {
    return m_sAdditionalInformation;
  }

  @Nullable
  public String getRefToMessageID ()
  {
    return m_sRefToMessageId;
  }
}
