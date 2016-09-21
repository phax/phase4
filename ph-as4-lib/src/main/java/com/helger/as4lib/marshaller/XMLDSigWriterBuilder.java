package com.helger.as4lib.marshaller;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.xml.security.binding.xmldsig.ReferenceType;

import com.helger.as4lib.constants.CAS4;
import com.helger.jaxb.builder.JAXBWriterBuilder;
import com.helger.xml.namespace.MapBasedNamespaceContext;

/**
 * A reader builder for XMLDSig documents.
 *
 * @author Philip Helger
 * @param <JAXBTYPE>
 *        The XMLDSig implementation class to be written
 */
@NotThreadSafe
public class XMLDSigWriterBuilder <JAXBTYPE> extends JAXBWriterBuilder <JAXBTYPE, XMLDSigWriterBuilder <JAXBTYPE>>
{
  public XMLDSigWriterBuilder (@Nonnull final XMLDSigDocumentType eDocType)
  {
    super (eDocType);
    final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext ();
    aNSCtx.addMapping ("ds", CAS4.DS_NS);
    setNamespaceContext (aNSCtx);
  }

  @Nonnull
  public static XMLDSigWriterBuilder <ReferenceType> dsigReference ()
  {
    return new XMLDSigWriterBuilder<> (XMLDSigDocumentType.REFERENCE);
  }
}
