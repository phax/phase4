package com.helger.as4lib.soap;

import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.string.StringHelper;

public enum ESOAPVersion
{
  SOAP_11 ("http://schemas.xmlsoap.org/soap/envelope/", "S11", CMimeType.TEXT_XML, "1.1"),
  SOAP_12 ("http://www.w3.org/2003/05/soap-envelope", "S12", CMimeType.APPLICATION_SOAP_XML, "1.2");

  /** According to spec 2.1. Feature Set */
  public static final ESOAPVersion AS4_DEFAULT = ESOAPVersion.SOAP_12;

  private final String m_sNamespaceURI;
  private final String m_sNamespacePrefix;
  private final IMimeType m_aMimeType;
  private final String m_sVersion;

  private ESOAPVersion (@Nonnull @Nonempty final String sNamespaceURI,
                        @Nonnull @Nonempty final String sNamespacePrefix,
                        @Nonnull final IMimeType aMimeType,
                        @Nonnull @Nonempty final String sVersion)
  {
    m_sNamespaceURI = sNamespaceURI;
    m_sNamespacePrefix = sNamespacePrefix;
    m_aMimeType = aMimeType;
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
  public IMimeType getMimeType ()
  {
    return m_aMimeType;
  }

  @Nonnull
  public MimeType getMimeType (@Nonnull final Charset aCharset)
  {
    return new MimeType (m_aMimeType).addParameter (CMimeType.PARAMETER_NAME_CHARSET, aCharset.name ());
  }

  @Nonnull
  @Nonempty
  public String getVersion ()
  {
    return m_sVersion;
  }

  @Nonnull
  @Nonempty
  public String getMustUnderstandValue (final boolean bMustUnderstand)
  {
    if (this == SOAP_11)
      return bMustUnderstand ? "1" : "0";
    return Boolean.toString (bMustUnderstand);
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
  public static ESOAPVersion getFromVersionOrNull (@Nullable final String sVersion)
  {
    return getFromVersionOrDefault (sVersion, null);
  }

  @Nullable
  public static ESOAPVersion getFromVersionOrDefault (@Nullable final String sVersion,
                                                      @Nullable final ESOAPVersion eDefault)
  {
    if (StringHelper.hasNoText (sVersion))
      return eDefault;
    return EnumHelper.findFirst (ESOAPVersion.class, x -> x.getVersion ().equals (sVersion), eDefault);
  }
}
