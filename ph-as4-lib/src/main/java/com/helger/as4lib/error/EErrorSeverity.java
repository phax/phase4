package com.helger.as4lib.error;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;

public enum EErrorSeverity
{
  FAILURE ("failure"),
  WARNING ("warning");

  private final String m_sSeverity;

  private EErrorSeverity (@Nonnull @Nonempty final String sSeverity)
  {
    m_sSeverity = sSeverity;
  }

  @Nonnull
  @Nonempty
  public String getSeverity ()
  {
    return m_sSeverity;
  }

  public boolean isFailure ()
  {
    return this == FAILURE;
  }
}
