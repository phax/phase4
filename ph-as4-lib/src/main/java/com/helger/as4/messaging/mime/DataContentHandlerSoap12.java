package com.helger.as4.messaging.mime;

import java.io.IOException;
import java.io.OutputStream;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataSource;
import javax.annotation.Nonnull;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.mime.CMimeType;
import com.sun.mail.handlers.text_plain;

/**
 * Special DataContentHandler for SOAP 1.2 messages with the special MIME type.
 *
 * @author helger
 */
public class DataContentHandlerSoap12 extends text_plain
{
  private static final ActivationDataFlavor [] FLAVORS = { new ActivationDataFlavor (StreamSource.class,
                                                                                     CMimeType.APPLICATION_SOAP_XML.getAsStringWithoutParameters (),
                                                                                     "SOAP") };

  @Override
  @Nonnull
  @ReturnsMutableObject ("design")
  protected ActivationDataFlavor [] getDataFlavors ()
  {
    return FLAVORS;
  }

  @Override
  @Nonnull
  protected Object getData (@Nonnull final ActivationDataFlavor aFlavor,
                            @Nonnull final DataSource ds) throws IOException
  {
    if (aFlavor.getRepresentationClass () == StreamSource.class)
      return new StreamSource (ds.getInputStream ());
    throw new IOException ("Unsupported flavor " + aFlavor + " on DS " + ds);
  }

  /**
   */
  @Override
  public void writeTo (@Nonnull final Object obj,
                       @Nonnull final String sMimeType,
                       @Nonnull final OutputStream os) throws IOException
  {
    try
    {
      final Transformer transformer = TransformerFactory.newInstance ().newTransformer ();
      final StreamResult result = new StreamResult (os);
      if (obj instanceof DataSource)
      {
        // Streaming transform applies only to
        // javax.xml.transform.StreamSource
        transformer.transform (new StreamSource (((DataSource) obj).getInputStream ()), result);
      }
      else
        if (obj instanceof Source)
        {
          transformer.transform ((Source) obj, result);
        }
        else
        {
          throw new IOException ("Invalid Object type = " +
                                 obj.getClass () +
                                 ". DataContentHandlerSoap12 can only convert DataSource or Source to XML.");
        }
    }
    catch (final TransformerException | RuntimeException ex)
    {
      throw new IOException ("Unable to run the JAXP transformer on a stream", ex);
    }
  }
}
