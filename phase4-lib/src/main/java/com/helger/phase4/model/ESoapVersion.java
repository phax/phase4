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
package com.helger.phase4.model;

import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.string.StringHelper;

/**
 * Enumeration with all known and supported SOAP versions.
 *
 * @author Philip Helger
 */
public enum ESoapVersion
{
  SOAP_11 ("http://schemas.xmlsoap.org/soap/envelope/", "S11", CMimeType.TEXT_XML, "1.1"),
  SOAP_12 ("http://www.w3.org/2003/05/soap-envelope", "S12", CMimeType.APPLICATION_SOAP_XML, "1.2");

  /** According to spec 2.1. Feature Set */
  public static final ESoapVersion AS4_DEFAULT = ESoapVersion.SOAP_12;

  private final String m_sNamespaceURI;
  private final String m_sNamespacePrefix;
  private final IMimeType m_aMimeType;
  private final String m_sVersion;

  ESoapVersion (@Nonnull @Nonempty final String sNamespaceURI,
                @Nonnull @Nonempty final String sNamespacePrefix,
                @Nonnull final IMimeType aMimeType,
                @Nonnull @Nonempty final String sVersion)
  {
    m_sNamespaceURI = sNamespaceURI;
    m_sNamespacePrefix = sNamespacePrefix;
    m_aMimeType = aMimeType;
    m_sVersion = sVersion;
  }

  /**
   * @return The namespace URI for this SOAP version. Neither <code>null</code>
   *         nor empty.
   */
  @Nonnull
  @Nonempty
  public String getNamespaceURI ()
  {
    return m_sNamespaceURI;
  }

  /**
   * @return The default namespace prefix to be used. Neither <code>null</code>
   *         nor empty.
   */
  @Nonnull
  @Nonempty
  public String getNamespacePrefix ()
  {
    return m_sNamespacePrefix;
  }

  /**
   * @return The mime type of this SOAP version. Never <code>null</code>.
   */
  @Nonnull
  public IMimeType getMimeType ()
  {
    return m_aMimeType;
  }

  /**
   * Get the mime type of this SOAP version with the passed charset.
   *
   * @param aCharset
   *        The charset to be used. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public MimeType getMimeType (@Nonnull final Charset aCharset)
  {
    return new MimeType (m_aMimeType).addParameter (CMimeType.PARAMETER_NAME_CHARSET, aCharset.name ());
  }

  /**
   * @return The human readable version string. Neither <code>null</code> nor
   *         empty.
   */
  @Nonnull
  @Nonempty
  public String getVersion ()
  {
    return m_sVersion;
  }

  /**
   * Get the must understand value contained in SOAP instances of this version.
   *
   * @param bMustUnderstand
   *        must understand value
   * @return A non-<code>null</code> non-empty string.
   */
  @Nonnull
  @Nonempty
  public String getMustUnderstandValue (final boolean bMustUnderstand)
  {
    if (this == SOAP_11)
      return bMustUnderstand ? "1" : "0";
    return Boolean.toString (bMustUnderstand);
  }

  /**
   * @return The XML element local name of the SOAP header element.
   */
  @Nonnull
  public String getHeaderElementName ()
  {
    return "Header";
  }

  /**
   * @return The XML element local name of the SOAP body element.
   */
  @Nonnull
  public String getBodyElementName ()
  {
    return "Body";
  }

  /**
   * @return <code>true</code> if this SOAP version is the AS4 default version.
   * @see #AS4_DEFAULT
   */
  public boolean isAS4Default ()
  {
    return this == AS4_DEFAULT;
  }

  @Nullable
  public static ESoapVersion getFromVersionOrNull (@Nullable final String sVersion)
  {
    return getFromVersionOrDefault (sVersion, null);
  }

  @Nullable
  public static ESoapVersion getFromVersionOrDefault (@Nullable final String sVersion,
                                                      @Nullable final ESoapVersion eDefault)
  {
    if (StringHelper.hasNoText (sVersion))
      return eDefault;
    return ArrayHelper.findFirst (values (), x -> x.getVersion ().equals (sVersion), eDefault);
  }

  @Nullable
  public static ESoapVersion getFromNamespaceURIOrNull (@Nullable final String sNamespaceURI)
  {
    if (StringHelper.hasNoText (sNamespaceURI))
      return null;
    return ArrayHelper.findFirst (values (), x -> x.getNamespaceURI ().equals (sNamespaceURI));
  }

  @Nullable
  public static ESoapVersion getFromMimeTypeOrNull (@Nullable final IMimeType aMimeType)
  {
    if (aMimeType == null)
      return null;
    return ArrayHelper.findFirst (values (), x -> x.getMimeType ().equals (aMimeType));
  }
}
