package com.helger.as4lib.attachment;

import java.nio.charset.Charset;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  private final String m_sID;
  private Charset m_aCharset;
  private final IMimeType m_aMimeType;
  private EContentTransferEncoding m_eCTE = EContentTransferEncoding.BINARY;
  private final EAS4CompressionMode m_eCompressionMode;

  public AbstractAS4Attachment (@Nonnull final IMimeType aMimeType,
                                @Nullable final EAS4CompressionMode eCompressionMode)
  {
    ValueEnforcer.notNull (aMimeType, "MimeType");
    m_sID = "ph-as4-" + UUID.randomUUID ().toString ();
    m_aMimeType = aMimeType;
    m_eCompressionMode = eCompressionMode;
  }

  @Nonnull
  @Nonempty
  public final String getID ()
  {
    return m_sID;
  }

  @Nonnull
  public final Charset getCharset ()
  {
    return m_aCharset;
  }

  @Nonnull
  public final AbstractAS4Attachment setCharset (@Nonnull final Charset aCharset)
  {
    m_aCharset = ValueEnforcer.notNull (aCharset, "Charset");
    return this;
  }

  @Nonnull
  public final IMimeType getMimeType ()
  {
    return m_aMimeType;
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

  @Nonnull
  public final EAS4CompressionMode getCompressionMode ()
  {
    return m_eCompressionMode;
  }
}
