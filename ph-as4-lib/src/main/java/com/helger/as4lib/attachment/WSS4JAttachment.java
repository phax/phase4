package com.helger.as4lib.attachment;

import java.io.InputStream;

import javax.annotation.Nonnull;

import org.apache.wss4j.common.ext.Attachment;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.IHasInputStream;

public class WSS4JAttachment extends Attachment
{
  private IHasInputStream m_aISP;

  public WSS4JAttachment ()
  {}

  @Override
  public InputStream getSourceStream ()
  {
    final InputStream ret = m_aISP.getInputStream ();
    if (ret == null)
      throw new IllegalStateException ("Failed to get InputStream from " + m_aISP);
    // TODO remember me
    return ret;
  }

  @Override
  @Deprecated
  public void setSourceStream (final InputStream sourceStream)
  {
    throw new UnsupportedOperationException ("Use setSourceStreamProvider instead");
  }

  public void setSourceStreamProvider (@Nonnull final IHasInputStream aISP)
  {
    ValueEnforcer.notNull (aISP, "InputStreamProvider");
    m_aISP = aISP;
  }
}
