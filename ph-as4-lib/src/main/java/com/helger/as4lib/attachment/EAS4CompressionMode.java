package com.helger.as4lib.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;

/**
 * Defines the allowed AS4 compression modes.
 *
 * @author Philip Helger
 */
public enum EAS4CompressionMode
{
  GZIP (CMimeType.APPLICATION_GZIP)
  {
    @Override
    @Nonnull
    public InputStream getDecompressStream (@Nonnull final InputStream aIS) throws IOException
    {
      return new GZIPInputStream (aIS);
    }

    @Override
    @Nonnull
    public OutputStream getCompressStream (@Nonnull final OutputStream aOS) throws IOException
    {
      return new GZIPOutputStream (aOS);
    }
  };

  private final IMimeType m_aMimeType;

  private EAS4CompressionMode (@Nonnull final IMimeType aMimeType)
  {
    m_aMimeType = aMimeType;
  }

  @Nonnull
  public IMimeType getMimeType ()
  {
    return m_aMimeType;
  }

  @Nonnull
  @Nonempty
  public String getMimeTypeAsString ()
  {
    return m_aMimeType.getAsString ();
  }

  @Nonnull
  public abstract InputStream getDecompressStream (@Nonnull InputStream aIS) throws IOException;

  @Nonnull
  public abstract OutputStream getCompressStream (@Nonnull OutputStream aOS) throws IOException;
}
