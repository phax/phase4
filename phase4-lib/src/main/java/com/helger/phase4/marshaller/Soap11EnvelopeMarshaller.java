package com.helger.phase4.marshaller;

import java.util.List;

import javax.xml.namespace.QName;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.phase4.CAS4;
import com.helger.phase4.soap.ESoapVersion;
import com.helger.phase4.soap11.Soap11Envelope;

/**
 * Marshaller for {@link Soap11Envelope} objects.
 *
 * @author Philip Helger
 * @since 2.0.0
 */
public class Soap11EnvelopeMarshaller extends GenericJAXBMarshaller <Soap11Envelope>
{
  public static final List <ClassPathResource> XSDS = new CommonsArrayList <> (CAS4.XSD_SOAP11).getAsUnmodifiable ();

  public Soap11EnvelopeMarshaller ()
  {
    // Information is taken from the @XmlType of
    // Soap11Envelope and from @XmlSchema of package-info
    super (Soap11Envelope.class,
           XSDS,
           createSimpleJAXBElement (new QName (ESoapVersion.SOAP_11.getNamespaceURI (),
                                               "Envelope",
                                               ESoapVersion.SOAP_11.getNamespacePrefix ()),
                                    Soap11Envelope.class));
    setNamespaceContext (Soap11NamespaceHandler.getInstance ());
  }
}
