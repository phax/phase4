package com.helger.as4lib.wss;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.string.StringHelper;

public enum EWSSVersion
{
  WSS_10 ("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "wsse", "1.0"),
  WSS_11 ("http://docs.oasis-open.org/wss/2005/xx/oasis-2005xx-wss-wssecurity-secext-1.1.xsd", "wsse11", "1.1");

  private final String m_sNamespaceURI;
  private final String m_sNamespacePrefix;
  private final String m_sVersion;

  private EWSSVersion (@Nonnull @Nonempty final String sNamespaceURI,
                       @Nonnull @Nonempty final String sNamespacePrefix,
                       @Nonnull @Nonempty final String sVersion)
  {
    m_sNamespaceURI = sNamespaceURI;
    m_sNamespacePrefix = sNamespacePrefix;
    m_sVersion = sVersion;
  }

  @Nonnull
  @Nonempty
  public String getNamespaceURI ()
  {
    return m_sNamespaceURI;
  }

  @Nonnull
  @Nonempty
  public String getNamespacePrefix ()
  {
    return m_sNamespacePrefix;
  }

  @Nonnull
  @Nonempty
  public String getVersion ()
  {
    return m_sVersion;
  }

  @Nonnull
  public String getHeaderElementName ()
  {
    return "Header";
  }

  @Nonnull
  public String getBodyElementName ()
  {
    return "Body";
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
