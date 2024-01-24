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
package com.helger.phase4.wss;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.string.StringHelper;

/**
 * Specifies the different WS Security versions available
 *
 * @author bayerlma
 */
public enum EWSSVersion
{
  @Deprecated (forRemoval = false)
  WSS_10("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "wsse", "1.0"),
  @Deprecated (forRemoval = false)
  WSS_11("http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd", "wsse11", "1.1"),
  WSS_111 ("http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd", "wsse11", "1.1.1");

  private final String m_sNamespaceURI;
  private final String m_sNamespacePrefix;
  private final String m_sVersion;

  EWSSVersion (@Nonnull @Nonempty final String sNamespaceURI,
               @Nonnull @Nonempty final String sNamespacePrefix,
               @Nonnull @Nonempty final String sVersion)
  {
    m_sNamespaceURI = sNamespaceURI;
    m_sNamespacePrefix = sNamespacePrefix;
    m_sVersion = sVersion;
  }

  /**
   * @return The namespace URI of the specification. Neither <code>null</code>
   *         nor empty.
   */
  @Nonnull
  @Nonempty
  public String getNamespaceURI ()
  {
    return m_sNamespaceURI;
  }

  /**
   * @return The intended namespace prefix to be used. Neither <code>null</code>
   *         nor empty.
   */
  @Nonnull
  @Nonempty
  public String getNamespacePrefix ()
  {
    return m_sNamespacePrefix;
  }

  /**
   * @return The specification version number. Neither <code>null</code> nor
   *         empty.
   */
  @Nonnull
  @Nonempty
  public String getVersion ()
  {
    return m_sVersion;
  }

  @Nullable
  public static EWSSVersion getFromVersionOrNull (@Nullable final String sVersion)
  {
    return getFromVersionOrDefault (sVersion, null);
  }

  @Nullable
  public static EWSSVersion getFromVersionOrDefault (@Nullable final String sVersion,
                                                     @Nullable final EWSSVersion eDefault)
  {
    if (StringHelper.hasNoText (sVersion))
      return eDefault;
    return EnumHelper.findFirst (EWSSVersion.class, x -> x.getVersion ().equals (sVersion), eDefault);
  }
}
