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
package com.helger.phase4.incoming;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.string.ToStringGenerator;

/**
 * Implementation of {@link IAS4IncomingProfileSelector} taking a constant AS4
 * profile ID.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
@Immutable
public class AS4IncomingProfileSelectorConstant implements IAS4IncomingProfileSelector
{
  private final String m_sAS4ProfileID;
  private final boolean m_bValidateAgainstProfile;

  public AS4IncomingProfileSelectorConstant (@Nullable final String sAS4ProfileID)
  {
    this (sAS4ProfileID, DEFAULT_VALIDATE_AGAINST_PROFILE);
  }

  public AS4IncomingProfileSelectorConstant (@Nullable final String sAS4ProfileID,
                                             final boolean bValidateAgainstProfile)
  {
    m_sAS4ProfileID = sAS4ProfileID;
    m_bValidateAgainstProfile = bValidateAgainstProfile;
  }

  @Nullable
  public String getAS4ProfileID (@Nonnull final IAS4IncomingMessageState aIncomingState)
  {
    return m_sAS4ProfileID;
  }

  public boolean validateAgainstProfile ()
  {
    return m_bValidateAgainstProfile;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("AS4ProfileID", m_sAS4ProfileID)
                                       .append ("ValidateAgainstProfile", m_bValidateAgainstProfile)
                                       .getToString ();
  }
}
