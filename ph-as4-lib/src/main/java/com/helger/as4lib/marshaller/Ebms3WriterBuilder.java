package com.helger.as4lib.marshaller;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as4lib.ebms3header.Ebms3Messaging;
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
    aNSCtx.addMapping ("eb", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/");
    aNSCtx.addMapping ("S11", "http://schemas.xmlsoap.org/soap/envelope/");
    aNSCtx.addMapping ("S12", "http://www.w3.org/2003/05/soap-envelope");
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
