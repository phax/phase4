package com.helger.as4lib.error;

public enum EErrorSeverity
{
  FAILURE ("failure"),
  WARNING ("warning");

  private String m_sSeverity;

  private EErrorSeverity (final String sSeverity)
  {
    m_sSeverity = sSeverity;
  }

  public String getSeverity ()
  {
    return m_sSeverity;
  }

  public void setSeverity (final String sSeverity)
  {
    m_sSeverity = sSeverity;
  }

}
