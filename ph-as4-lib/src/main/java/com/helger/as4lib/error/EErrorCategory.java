package com.helger.as4lib.error;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;

public enum EErrorCategory
{
  CONTENT ("Content"),
  COMMUNICATION ("Communication"),
  UNPACKAGING ("Unpackaging"),
  PROCESSING ("Processing");

  private final String m_sContent;

  private EErrorCategory (@Nonnull @Nonempty final String sContent)
  {
    m_sContent = sContent;
  }

  @Nonnull
  @Nonempty
  public String getContent ()
  {
    return m_sContent;
  }
}
