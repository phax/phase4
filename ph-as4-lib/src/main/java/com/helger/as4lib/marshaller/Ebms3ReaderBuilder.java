package com.helger.as4lib.marshaller;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.as4lib.soap12.Soap12Envelope;
import com.helger.jaxb.builder.JAXBReaderBuilder;

/**
 * A reader builder for Ebms documents.
 *
 * @author Philip Helger
 * @param <JAXBTYPE>
 *        The Ebms implementation class to be read
 */
@NotThreadSafe
public class Ebms3ReaderBuilder <JAXBTYPE> extends JAXBReaderBuilder <JAXBTYPE, Ebms3ReaderBuilder <JAXBTYPE>>
{
  public Ebms3ReaderBuilder (@Nonnull final EEbms3DocumentType eDocType, @Nonnull final Class <JAXBTYPE> aImplClass)
  {
    super (eDocType, aImplClass);
  }

  @Nonnull
  public static Ebms3ReaderBuilder <Ebms3Messaging> ebms3Messaging ()
  {
    return new Ebms3ReaderBuilder<> (EEbms3DocumentType.MESSAGING, Ebms3Messaging.class);
  }

  @Nonnull
  public static Ebms3ReaderBuilder <Soap11Envelope> soap11 ()
  {
    return new Ebms3ReaderBuilder<> (EEbms3DocumentType.SOAP_11, Soap11Envelope.class);
  }

  @Nonnull
  public static Ebms3ReaderBuilder <Soap12Envelope> soap12 ()
  {
    return new Ebms3ReaderBuilder<> (EEbms3DocumentType.SOAP_12, Soap12Envelope.class);
  }
}
