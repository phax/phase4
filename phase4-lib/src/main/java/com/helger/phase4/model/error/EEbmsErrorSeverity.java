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
package com.helger.phase4.model.error;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.error.level.IErrorLevel;
import com.helger.commons.error.level.IHasErrorLevelComparable;

/**
 * EBMS error severity with mapping to {@link IErrorLevel}.
 *
 * @author Philip Helger
 */
public enum EEbmsErrorSeverity implements IHasErrorLevelComparable <EEbmsErrorSeverity>
{
  FAILURE ("failure", EErrorLevel.ERROR),
  WARNING ("warning", EErrorLevel.WARN);

  private final String m_sSeverity;
  private final IErrorLevel m_aErrorLevel;

  EEbmsErrorSeverity (@Nonnull @Nonempty final String sSeverity, @Nonnull final IErrorLevel aErrorLevel)
  {
    m_sSeverity = sSeverity;
    m_aErrorLevel = aErrorLevel;
  }

  /**
   * @return The token for the EBMS3 message
   */
  @Nonnull
  @Nonempty
  public String getSeverity ()
  {
    return m_sSeverity;
  }

  @Nonnull
  public IErrorLevel getErrorLevel ()
  {
    return m_aErrorLevel;
  }

  @Nullable
  public static EEbmsErrorSeverity getFromErrorLevelOrNull (@Nullable final IErrorLevel aErrorLevel)
  {
    if (aErrorLevel == null)
      return null;

    return aErrorLevel.isError () ? EEbmsErrorSeverity.FAILURE : EEbmsErrorSeverity.WARNING;
  }
}
