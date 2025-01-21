/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;

/**
 * Default implementation of {@link IAS4IncomingProfileSelector} taking the AS4
 * profile ID from the global {@link AS4ProfileSelector}.
 *
 * @author Philip Helger
 * @since 0.13.0
 */
public class AS4IncomingProfileSelectorFromGlobal implements IAS4IncomingProfileSelector
{
  public static final AS4IncomingProfileSelectorFromGlobal INSTANCE = new AS4IncomingProfileSelectorFromGlobal ();

  private final boolean m_bValidateAgainstProfile;

  /**
   * Default constructor with validation enabled
   */
  public AS4IncomingProfileSelectorFromGlobal ()
  {
    this (DEFAULT_VALIDATE_AGAINST_PROFILE);
  }

  /**
   * Constructor
   *
   * @param bValidateAgainstProfile
   *        <code>true</code> to enable validation, <code>false</code> to
   *        disable validation against the profile
   * @since 3.0.0
   */
  public AS4IncomingProfileSelectorFromGlobal (final boolean bValidateAgainstProfile)
  {
    m_bValidateAgainstProfile = bValidateAgainstProfile;
  }

  @Nullable
  public String getAS4ProfileID (@Nonnull final IAS4IncomingMessageState aIncomingState)
  {
    return AS4ProfileSelector.getDefaultAS4ProfileID ();
  }

  public boolean validateAgainstProfile ()
  {
    return m_bValidateAgainstProfile;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ValidateAgainstProfile", m_bValidateAgainstProfile).getToString ();
  }
}
