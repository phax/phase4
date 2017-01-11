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
package com.helger.as4lib.attachment.outgoing;

import java.nio.charset.Charset;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.constants.CAS4;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.string.ToStringGenerator;
import com.helger.mail.cte.EContentTransferEncoding;

/**
 * File based attachment.
 *
 * @author Philip Helger
 */
public abstract class AbstractAS4OutgoingAttachment implements IAS4OutgoingAttachment
{
  private final String m_sID;
  private final IMimeType m_aOriginalMimeType;
  private final IMimeType m_aMimeType;
  private final EAS4CompressionMode m_eCompressionMode;
  private Charset m_aCharset;
  private EContentTransferEncoding m_eCTE = EContentTransferEncoding.BINARY;

  public AbstractAS4OutgoingAttachment (@Nonnull final IMimeType aMimeType,
                                        @Nullable final EAS4CompressionMode eCompressionMode)
  {
    ValueEnforcer.notNull (aMimeType, "MimeType");
    m_sID = CAS4.LIB_NAME + "-" + UUID.randomUUID ().toString ();
    m_aOriginalMimeType = aMimeType;
    // Important to change the MIME type, so that signature calculation uses the
    // wrong mechanism!
    m_aMimeType = eCompressionMode != null ? eCompressionMode.getMimeType () : aMimeType;
    m_eCompressionMode = eCompressionMode;
  }

  @Nonnull
  @Nonempty
  public final String getID ()
  {
    return m_sID;
  }

  @Nonnull
  public final IMimeType getMimeType ()
  {
    return m_aMimeType;
  }

  @Nullable
  public final EAS4CompressionMode getCompressionMode ()
  {
    return m_eCompressionMode;
  }

  @Nonnull
  public final Charset getCharset ()
  {
    return m_aCharset;
  }

  @Nonnull
  public final AbstractAS4OutgoingAttachment setCharset (@Nonnull final Charset aCharset)
  {
    m_aCharset = ValueEnforcer.notNull (aCharset, "Charset");
    return this;
  }

  @Nonnull
  public final EContentTransferEncoding getContentTransferEncoding ()
  {
    return m_eCTE;
  }

  @Nonnull
  public final AbstractAS4OutgoingAttachment setContentTransferEncoding (@Nonnull final EContentTransferEncoding eCTE)
  {
    m_eCTE = ValueEnforcer.notNull (eCTE, "CTE");
    return this;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ID", m_sID)
                                       .appendIf ("OriginalMimeType", m_aOriginalMimeType, x -> x != m_aMimeType)
                                       .append ("MimeType", m_aMimeType)
                                       .append ("CompressionMode", m_eCompressionMode)
                                       .append ("Charset", m_aCharset)
                                       .append ("CTE", m_eCTE)
                                       .toString ();
  }
}
