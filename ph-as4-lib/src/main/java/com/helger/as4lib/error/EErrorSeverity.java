package com.helger.as4lib.error;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.error.level.IErrorLevel;

public enum EErrorSeverity
{
  FAILURE ("failure", EErrorLevel.ERROR),
  WARNING ("warning", EErrorLevel.WARN);

  private final String m_sSeverity;
  private final IErrorLevel m_aErrorLevel;

  private EErrorSeverity (@Nonnull @Nonempty final String sSeverity, @Nonnull final IErrorLevel aErrorLevel)
  {
    m_sSeverity = sSeverity;
    m_aErrorLevel = aErrorLevel;
  }

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

  public boolean isFailure ()
  {
    return this == FAILURE;
  }
}
