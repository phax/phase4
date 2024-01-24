/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.string.StringHelper;

/**
 * Defines the allowed AS4 compression modes.
 *
 * @author Philip Helger
 */
public enum EAS4CompressionMode implements IHasID <String>
{
  /** GZip compression mode */
  GZIP ("gzip", CMimeType.APPLICATION_GZIP, ".gz")
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

  private final String m_sID;
  private final IMimeType m_aMimeType;
  private final String m_sFileExtension;

  EAS4CompressionMode (@Nonnull @Nonempty final String sID,
                       @Nonnull final IMimeType aMimeType,
                       @Nonnull @Nonempty final String sFileExtension)
  {
    m_sID = sID;
    m_aMimeType = aMimeType;
    m_sFileExtension = sFileExtension;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * @return The MIME type of the compression mode. Never <code>null</code>.
   */
  @Nonnull
  public IMimeType getMimeType ()
  {
    return m_aMimeType;
  }

  /**
   * @return The string representation of the MIME type of the compression mode.
   *         Never <code>null</code>.
   */
  @Nonnull
  @Nonempty
  public String getMimeTypeAsString ()
  {
    return m_aMimeType.getAsString ();
  }

  /**
   * @return The file extension including the leading dot (e.g. ".gz")
   */
  @Nonnull
  @Nonempty
  public String getFileExtension ()
  {
    return m_sFileExtension;
  }

  /**
   * Get an {@link InputStream} to decompress the provided {@link InputStream}.
   *
   * @param aIS
   *        The source {@link InputStream}. May not be <code>null</code>.
   * @return The decompressing {@link InputStream}
   * @throws IOException
   *         In case of IO error
   */
  @Nonnull
  public abstract InputStream getDecompressStream (@Nonnull InputStream aIS) throws IOException;

  /**
   * Get an {@link OutputStream} to compress the provided {@link OutputStream}.
   *
   * @param aOS
   *        The source {@link OutputStream}. May not be <code>null</code>.
   * @return The compressing {@link OutputStream}
   * @throws IOException
   *         In case of IO error
   */
  @Nonnull
  public abstract OutputStream getCompressStream (@Nonnull OutputStream aOS) throws IOException;

  @Nullable
  public static EAS4CompressionMode getFromMimeTypeStringOrNull (@Nullable final String sMimeType)
  {
    if (StringHelper.hasNoText (sMimeType))
      return null;
    return EnumHelper.findFirst (EAS4CompressionMode.class, x -> x.getMimeTypeAsString ().equals (sMimeType));
  }

  @Nullable
  public static EAS4CompressionMode getFromIDOrNull (final String sID)
  {
    return EnumHelper.getFromIDOrNull (EAS4CompressionMode.class, sID);
  }
}
