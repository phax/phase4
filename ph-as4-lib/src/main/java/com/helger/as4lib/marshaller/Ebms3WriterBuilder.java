package com.helger.as4lib.marshaller;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.as4lib.soap12.Soap12Envelope;
import com.helger.jaxb.builder.JAXBWriterBuilder;
import com.helger.xml.namespace.MapBasedNamespaceContext;

/**
 * A reader builder for Ebms documents.
 *
 * @author Philip Helger
 * @param <JAXBTYPE>
 *        The Ebms implementation class to be written
 */
@NotThreadSafe
public class Ebms3WriterBuilder <JAXBTYPE> extends JAXBWriterBuilder <JAXBTYPE, Ebms3WriterBuilder <JAXBTYPE>>
{
  public Ebms3WriterBuilder (@Nonnull final EEbms3DocumentType eDocType)
  {
    super (eDocType);
    final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext ();
    aNSCtx.addMapping ("eb", CAS4.EBMS_NS);
    for (final ESOAPVersion e : ESOAPVersion.values ())
      aNSCtx.addMapping (e.getNamespacePrefix (), e.getNamespaceURI ());
    setNamespaceContext (aNSCtx);
  }

  @Nonnull
  public static Ebms3WriterBuilder <Ebms3Messaging> ebms3Messaging ()
  {
    return new Ebms3WriterBuilder<> (EEbms3DocumentType.MESSAGING);
  }

  @Nonnull
  public static Ebms3WriterBuilder <Soap11Envelope> soap11 ()
  {
    return new Ebms3WriterBuilder<> (EEbms3DocumentType.SOAP_11);
  }

  @Nonnull
  public static Ebms3WriterBuilder <Soap12Envelope> soap12 ()
  {
    return new Ebms3WriterBuilder<> (EEbms3DocumentType.SOAP_12);
  }
}
