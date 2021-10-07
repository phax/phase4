/*
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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

import java.io.File;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.ByteArrayWrapper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.messaging.domain.MessageHelperMethods;

/**
 * This represents a single payload for an outgoing message.
 *
 * @author Philip Helger
 * @since 0.9.16
 */
@Immutable
public class Phase4OutgoingAttachment
{
  private final ByteArrayWrapper m_aDataBytes;
  private final File m_aDataFile;
  private final String m_sContentID;
  private final String m_sFilename;
  private final IMimeType m_aMimeType;
  private final EAS4CompressionMode m_eCompressionMode;
  private final Charset m_aCharset;

  protected Phase4OutgoingAttachment (@Nullable final ByteArrayWrapper aDataBytes,
                                      @Nullable final File aDataFile,
                                      @Nullable final String sContentID,
                                      @Nullable final String sFilename,
                                      @Nonnull final IMimeType aMimeType,
                                      @Nullable final EAS4CompressionMode eCompressionMode,
                                      @Nullable final Charset aCharset)
  {
    ValueEnforcer.isTrue (aDataBytes != null || aDataFile != null, "SrcData or SrcFile must be present");
    ValueEnforcer.isFalse (aDataBytes != null && aDataFile != null, "Either SrcData or SrcFile must be present but not both");
    ValueEnforcer.notNull (aMimeType, "MimeType");
    m_aDataBytes = aDataBytes;
    m_aDataFile = aDataFile;
    m_sContentID = sContentID;
    m_sFilename = sFilename;
    m_aMimeType = aMimeType;
    m_eCompressionMode = eCompressionMode;
    m_aCharset = aCharset;
  }

  /**
   * @return The data to be send as a byte array. May be <code>null</code> in
   *         which case {@link #getDataFile()} has the content.
   * @since 0.14.0
   * @see #getDataFile()
   */
  @Nullable
  public final ByteArrayWrapper getDataBytes ()
  {
    return m_aDataBytes;
  }

  /**
   * @return <code>true</code> if the data is available as a byte array,
   *         <code>false</code> if it is a file.
   */
  public final boolean hasDataBytes ()
  {
    return m_aDataBytes != null;
  }

  /**
   * @return The data to be send as a File. May be <code>null</code> in which
   *         case {@link #getDataBytes()} has the content.
   * @since 0.14.0
   * @see #getDataBytes()
   */
  @Nullable
  public final File getDataFile ()
  {
    return m_aDataFile;
  }

  /**
   * @return <code>true</code> if the data is available as a File,
   *         <code>false</code> if it is a byte array.
   */
  public final boolean hasDataFile ()
  {
    return m_aDataFile != null;
  }

  /**
   * @return The Content-ID to be used. May be <code>null</code>.
   */
  @Nullable
  public final String getContentID ()
  {
    return m_sContentID;
  }

  /**
   * @return The filename to be used. May be <code>null</code>.
   */
  @Nullable
  public final String getFilename ()
  {
    return m_sFilename;
  }

  /**
   * @return The MIME type to be used. May not be <code>null</code>.
   */
  @Nonnull
  public final IMimeType getMimeType ()
  {
    return m_aMimeType;
  }

  /**
   * @return The compression mode to be used. May be <code>null</code>.
   */
  @Nullable
  public final EAS4CompressionMode getCompressionMode ()
  {
    return m_eCompressionMode;
  }

  /**
   * @return The character set to use. May be <code>null</code>.
   * @since 0.14.0
   */
  @Nullable
  public final Charset getCharset ()
  {
    return m_aCharset;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("DataBytes", m_aDataBytes)
                                       .append ("DataFile", m_aDataFile)
                                       .append ("ContentID", m_sContentID)
                                       .append ("Filename", m_sFilename)
                                       .append ("MimeType", m_aMimeType)
                                       .append ("CompressionMode", m_eCompressionMode)
                                       .append ("Charset", m_aCharset)
                                       .getToString ();
  }

  /**
   * Create a new builder.
   *
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static Builder builder ()
  {
    return new Builder ();
  }

  /**
   * Builder class for class {@link Phase4OutgoingAttachment}. At least "data"
   * and "mimeType" must be set.
   *
   * @author Philip Helger
   */
  public static class Builder
  {
    private ByteArrayWrapper m_aDataBytes;
    private File m_aDataFile;
    private String m_sContentID;
    private String m_sFilename;
    private IMimeType m_aMimeType;
    private EAS4CompressionMode m_eCompressionMode;
    private Charset m_aCharset;

    public Builder ()
    {}

    @Nonnull
    public Builder data (@Nullable final byte [] a)
    {
      return data (a == null ? null : new ByteArrayWrapper (a, false));
    }

    @Nonnull
    public Builder data (@Nullable final ByteArrayWrapper a)
    {
      m_aDataBytes = a;
      m_aDataFile = null;
      return this;
    }

    @Nonnull
    public Builder data (@Nullable final File a)
    {
      m_aDataBytes = null;
      m_aDataFile = a;
      return this;
    }

    @Nonnull
    public Builder reandomContentID ()
    {
      return contentID (MessageHelperMethods.createRandomContentID ());
    }

    @Nonnull
    public Builder contentID (@Nullable final String s)
    {
      m_sContentID = s;
      return this;
    }

    @Nonnull
    public Builder filename (@Nullable final String s)
    {
      m_sFilename = s;
      return this;
    }

    /**
     * Shortcut for <code>mimeType (CMimeType.APPLICATION_XML)</code>
     *
     * @return this for chaining
     */
    @Nonnull
    public Builder mimeTypeXML ()
    {
      return mimeType (CMimeType.APPLICATION_XML);
    }

    @Nonnull
    public Builder mimeType (@Nullable final IMimeType a)
    {
      m_aMimeType = a;
      return this;
    }

    /**
     * Shortcut for <code>compression (EAS4CompressionMode.GZIP)</code>
     *
     * @return this for chaining
     */
    @Nonnull
    public Builder compressionGZIP ()
    {
      return compression (EAS4CompressionMode.GZIP);
    }

    @Nonnull
    public Builder compression (@Nullable final EAS4CompressionMode e)
    {
      m_eCompressionMode = e;
      return this;
    }

    /**
     * Define the charset of the outgoing attachment.
     *
     * @param a
     *        The Charset to use. May be <code>null</code>.
     * @return this for chaining
     * @since 0.14.0
     */
    @Nonnull
    public Builder charset (@Nullable final Charset a)
    {
      m_aCharset = a;
      return this;
    }

    @Nonnull
    public Phase4OutgoingAttachment build ()
    {
      if (m_aDataBytes == null && m_aDataFile == null)
        throw new IllegalStateException ("Phase4OutgoingAttachment has no 'data' element");
      if (m_aMimeType == null)
        throw new IllegalStateException ("Phase4OutgoingAttachment has no 'mimeType' element");
      return new Phase4OutgoingAttachment (m_aDataBytes,
                                           m_aDataFile,
                                           m_sContentID,
                                           m_sFilename,
                                           m_aMimeType,
                                           m_eCompressionMode,
                                           m_aCharset);
    }
  }
}
