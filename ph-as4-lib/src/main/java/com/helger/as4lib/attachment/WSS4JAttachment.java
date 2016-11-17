package com.helger.as4lib.attachment;

import java.io.InputStream;

import javax.annotation.Nonnull;

import org.apache.wss4j.common.ext.Attachment;

import com.helger.as4lib.util.AS4ResourceManager;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.IHasInputStream;

public class WSS4JAttachment extends Attachment
{
  private final AS4ResourceManager m_aResMgr;
  private IHasInputStream m_aISP;

  public WSS4JAttachment (@Nonnull final AS4ResourceManager aResMgr)
  {
    m_aResMgr = ValueEnforcer.notNull (aResMgr, "ResMgr");
  }

  @Override
  public InputStream getSourceStream ()
  {
    final InputStream ret = m_aISP.getInputStream ();
    if (ret == null)
      throw new IllegalStateException ("Failed to get InputStream from " + m_aISP);
    m_aResMgr.addCloseable (ret);
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
