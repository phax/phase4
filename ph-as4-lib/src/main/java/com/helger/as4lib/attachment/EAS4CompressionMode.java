/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
