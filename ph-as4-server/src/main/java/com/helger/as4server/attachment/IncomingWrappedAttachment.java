/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4server.attachment;

import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.attachment.WSS4JAttachment;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.ToStringGenerator;
import com.helger.http.CHTTPHeader;

/**
 * Wraps an existing (decrypted) WSS4J attachment.
 *
 * @author Philip Helger
 */
public class IncomingWrappedAttachment extends AbstractIncomingAttachment
{
  private final WSS4JAttachment m_aSrc;

  public IncomingWrappedAttachment (@Nonnull final WSS4JAttachment aSrc)
  {
    m_aSrc = ValueEnforcer.notNull (aSrc, "SrcAttachment");
  }

  @Nullable
  public String getContentID ()
  {
    return m_aSrc.getId ();
  }

  @Nullable
  public String getContentTransferEncoding ()
  {
    return m_aSrc.getHeaders ().get (CHTTPHeader.CONTENT_TRANSFER_ENCODING);
  }

  @Nullable
  public String getContentType ()
  {
    return m_aSrc.getMimeType ();
  }

  @Nonnull
  public WSS4JAttachment getAsWSS4JAttachment ()
  {
    return m_aSrc;
  }

  @Nonnull
  public InputStream getInputStream ()
  {
    return m_aSrc.getSourceStream ();
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("Src", m_aSrc).toString ();
  }
}
