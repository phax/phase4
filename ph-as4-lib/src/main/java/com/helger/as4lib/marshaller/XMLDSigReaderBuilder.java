package com.helger.as4lib.marshaller;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.jaxb.builder.JAXBReaderBuilder;
import com.helger.xsds.xmldsig.ReferenceType;

/**
 * A reader builder for XMLDSig documents.
 *
 * @author Philip Helger
 * @param <JAXBTYPE>
 *        The XMLDSig implementation class to be read
 */
@NotThreadSafe
public class XMLDSigReaderBuilder <JAXBTYPE> extends JAXBReaderBuilder <JAXBTYPE, XMLDSigReaderBuilder <JAXBTYPE>>
{
  public XMLDSigReaderBuilder (@Nonnull final XMLDSigDocumentType eDocType, @Nonnull final Class <JAXBTYPE> aImplClass)
  {
    super (eDocType, aImplClass);
  }

  @Nonnull
  public static XMLDSigReaderBuilder <ReferenceType> dsigReference ()
  {
    return new XMLDSigReaderBuilder<> (XMLDSigDocumentType.REFERENCE, ReferenceType.class);
  }
}
