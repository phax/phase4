package com.helger.as4lib.error;

public enum EErrorCategory
{
  CONTENT ("Content"),
  COMMUNICATION ("Communication"),
  UNPACKAGING ("Unpackaging"),
  PROCESSING ("Processing");

  private String m_sContent;

  private EErrorCategory (final String sContent)
  {
    m_sContent = sContent;
  }

  public String getsContent ()
  {
    return m_sContent;
  }

  public void setsContent (final String sContent)
  {
    m_sContent = sContent;
  }

}
