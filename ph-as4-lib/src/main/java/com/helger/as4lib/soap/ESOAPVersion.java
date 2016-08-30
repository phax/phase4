package com.helger.as4lib.soap;

import java.nio.charset.Charset;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeType;

public enum ESOAPVersion
{
  SOAP_11 ("http://schemas.xmlsoap.org/soap/envelope/", "S11", CMimeType.TEXT_XML),
  SOAP_12 ("http://www.w3.org/2003/05/soap-envelope", "S12", CMimeType.APPLICATION_SOAP_XML);

  private final String m_sNamespaceURI;
  private final String m_sNamespacePrefix;
  private final IMimeType m_aMimeType;

  private ESOAPVersion (@Nonnull @Nonempty final String sNamespaceURI,
                        @Nonnull @Nonempty final String sNamespacePrefix,
                        @Nonnull final IMimeType aMimeType)
  {
    m_sNamespaceURI = sNamespaceURI;
    m_sNamespacePrefix = sNamespacePrefix;
    m_aMimeType = aMimeType;
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
  @Nonempty
  public String getMustUnderstandValue (final boolean bMustUnderstand)
  {
    if (this == SOAP_11)
      return bMustUnderstand ? "1" : "0";
    return Boolean.toString (bMustUnderstand);
  }

  @Nonnull
  public MimeType getMimeType (@Nonnull final Charset aCharset)
  {
    return new MimeType (m_aMimeType).addParameter (CMimeType.PARAMETER_NAME_CHARSET, aCharset.name ());
  }
}
