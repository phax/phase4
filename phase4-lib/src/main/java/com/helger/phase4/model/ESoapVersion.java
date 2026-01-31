/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.array.ArrayHelper;
import com.helger.base.string.StringHelper;
import com.helger.mime.CMimeType;
import com.helger.mime.IMimeType;
import com.helger.mime.MimeType;

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

  ESoapVersion (@NonNull @Nonempty final String sNamespaceURI,
                @NonNull @Nonempty final String sNamespacePrefix,
                @NonNull final IMimeType aMimeType,
                @NonNull @Nonempty final String sVersion)
  {
    m_sNamespaceURI = sNamespaceURI;
    m_sNamespacePrefix = sNamespacePrefix;
    m_aMimeType = aMimeType;
    m_sVersion = sVersion;
  }

  /**
   * @return The namespace URI for this SOAP version. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getNamespaceURI ()
  {
    return m_sNamespaceURI;
  }

  /**
   * @return The default namespace prefix to be used. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getNamespacePrefix ()
  {
    return m_sNamespacePrefix;
  }

  /**
   * @return The mime type of this SOAP version. Never <code>null</code>.
   */
  @NonNull
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
  @NonNull
  @ReturnsMutableCopy
  public MimeType getMimeType (@NonNull final Charset aCharset)
  {
    return new MimeType (m_aMimeType).addParameter (CMimeType.PARAMETER_NAME_CHARSET, aCharset.name ());
  }

  /**
   * @return The human readable version string. Neither <code>null</code> nor empty.
   */
  @NonNull
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
  @NonNull
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
  @NonNull
  public String getHeaderElementName ()
  {
    return "Header";
  }

  /**
   * @return The XML element local name of the SOAP body element.
   */
  @NonNull
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
    if (StringHelper.isEmpty (sVersion))
      return eDefault;
    return ArrayHelper.findFirst (values (), x -> x.getVersion ().equals (sVersion), eDefault);
  }

  @Nullable
  public static ESoapVersion getFromNamespaceURIOrNull (@Nullable final String sNamespaceURI)
  {
    if (StringHelper.isEmpty (sNamespaceURI))
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
