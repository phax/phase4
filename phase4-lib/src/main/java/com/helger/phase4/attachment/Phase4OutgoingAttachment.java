/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.ByteArrayWrapper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
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
  private final ByteArrayWrapper m_aSrcData;
  private final String m_sContentID;
  private final String m_sFilename;
  private final IMimeType m_aMimeType;
  private final EAS4CompressionMode m_eCompressionMode;

  public Phase4OutgoingAttachment (@Nonnull final ByteArrayWrapper aSrcData,
                                   @Nullable final String sContentID,
                                   @Nullable final String sFilename,
                                   @Nonnull final IMimeType aMimeType,
                                   @Nullable final EAS4CompressionMode eCompressionMode)
  {
    ValueEnforcer.notNull (aSrcData, "SrcData");
    ValueEnforcer.notNull (aMimeType, "MimeType");
    m_aSrcData = aSrcData;
    m_sContentID = sContentID;
    m_sFilename = sFilename;
    m_aMimeType = aMimeType;
    m_eCompressionMode = eCompressionMode;
  }

  /**
   * @return The data to be send. Never <code>null</code>.
   */
  @Nonnull
  public ByteArrayWrapper getData ()
  {
    return m_aSrcData;
  }

  /**
   * @return The Content-ID to be used. May be <code>null</code>.
   */
  @Nullable
  public String getContentID ()
  {
    return m_sContentID;
  }

  /**
   * @return The filename to be used. May be <code>null</code>.
   */
  @Nullable
  public String getFilename ()
  {
    return m_sFilename;
  }

  /**
   * @return The MIME type to be used. May not be <code>null</code>.
   */
  @Nonnull
  public IMimeType getMimeType ()
  {
    return m_aMimeType;
  }

  /**
   * @return The compression mode to be used. May not be <code>null</code>.
   */
  @Nullable
  public EAS4CompressionMode getCompressionMode ()
  {
    return m_eCompressionMode;
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
    private ByteArrayWrapper m_aData;
    private String m_sContentID;
    private String m_sFilename;
    private IMimeType m_aMimeType;
    private EAS4CompressionMode m_eCompressionMode;

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
      m_aData = a;
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

    @Nonnull
    public Phase4OutgoingAttachment build ()
    {
      return new Phase4OutgoingAttachment (m_aData, m_sContentID, m_sFilename, m_aMimeType, m_eCompressionMode);
    }
  }
}
