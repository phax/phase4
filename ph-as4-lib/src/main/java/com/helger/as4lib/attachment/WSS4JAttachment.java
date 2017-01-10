/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4lib.attachment;

import java.io.InputStream;

import javax.annotation.Nonnull;

import org.apache.wss4j.common.ext.Attachment;

import com.helger.as4lib.util.AS4ResourceManager;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.IHasInputStream;
import com.helger.commons.string.ToStringGenerator;

/**
 * Special WSS4J attachment with an InputStream provider instead of a fixed
 * InputStream
 * 
 * @author bayerlma
 * @author Philip Helger
 */
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

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ID", getId ())
                                       .append ("MimeType", getMimeType ())
                                       .append ("Headers", getHeaders ())
                                       .append ("ResourceManager", m_aResMgr)
                                       .append ("ISP", m_aISP)
                                       .toString ();
  }
}
