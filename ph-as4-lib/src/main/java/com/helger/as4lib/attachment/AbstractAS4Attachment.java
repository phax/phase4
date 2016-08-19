package com.helger.as4lib.attachment;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.mime.IMimeType;
import com.helger.mail.cte.EContentTransferEncoding;

/**
 * File based attachment.
 *
 * @author Philip Helger
 */
public abstract class AbstractAS4Attachment implements IAS4Attachment
{
  // Special http header names
  protected static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
  protected static final String CONTENT_ID = "Content-ID";

  protected final String m_sID;
  protected final IMimeType m_aMimeType;
  protected EContentTransferEncoding m_eCTE = EContentTransferEncoding.BINARY;

  public AbstractAS4Attachment (@Nonnull final IMimeType aMimeType)
  {
    ValueEnforcer.notNull (aMimeType, "MimeType");
    m_sID = UUID.randomUUID ().toString ();
    m_aMimeType = aMimeType;
  }

  @Nonnull
  @Nonempty
  public final String getID ()
  {
    return m_sID;
  }

  @Nonnull
  public final EContentTransferEncoding getContentTransferEncoding ()
  {
    return m_eCTE;
  }

  @Nonnull
  public final AbstractAS4Attachment setContentTransferEncoding (@Nonnull final EContentTransferEncoding eCTE)
  {
    m_eCTE = ValueEnforcer.notNull (eCTE, "CTE");
    return this;
  }
}
