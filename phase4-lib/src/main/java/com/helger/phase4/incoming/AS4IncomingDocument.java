/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.incoming;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.NonNull;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.iface.IHasInputStream;
import com.helger.base.tostring.ToStringGenerator;

/**
 * Default implementation of {@link IAS4IncomingDocument} based on an {@link IHasInputStream} that
 * can be read multiple times - e.g. the source stream provider of an incoming attachment.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public class AS4IncomingDocument implements IAS4IncomingDocument
{
  private final IHasInputStream m_aISP;

  /**
   * Constructor
   *
   * @param aISP
   *        The input stream provider. May not be <code>null</code> and must be readable multiple
   *        times.
   */
  public AS4IncomingDocument (@NonNull final IHasInputStream aISP)
  {
    ValueEnforcer.notNull (aISP, "ISP");
    ValueEnforcer.isTrue (aISP::isReadMultiple, "The InputStream provider must be readable multiple times");
    m_aISP = aISP;
  }

  @NonNull
  public InputStream getInputStream () throws IOException
  {
    final InputStream ret;
    try
    {
      ret = m_aISP.getInputStream ();
    }
    catch (final RuntimeException ex)
    {
      // E.g. AS4DecompressException
      throw new IOException ("Failed to open the incoming document", ex);
    }
    if (ret == null)
      throw new IOException ("Failed to open the incoming document");
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ISP", m_aISP).getToString ();
  }
}
