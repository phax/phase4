package com.helger.as4lib.model.pmode;

import javax.annotation.Nullable;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.commons.hashcode.HashCodeGenerator;

public class PModePayloadService
{

  private EAS4CompressionMode m_aCompressionMode;

  public PModePayloadService (@Nullable final EAS4CompressionMode aCompressionMode)
  {
    m_aCompressionMode = aCompressionMode;
  }

  public EAS4CompressionMode getCompressionMode ()
  {
    return m_aCompressionMode;
  }

  public void setCompressionMode (final EAS4CompressionMode aCompressionMode)
  {
    m_aCompressionMode = aCompressionMode;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModePayloadService rhs = (PModePayloadService) o;
    return m_aCompressionMode.equals (rhs.m_aCompressionMode);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aCompressionMode).getHashCode ();
  }
}
