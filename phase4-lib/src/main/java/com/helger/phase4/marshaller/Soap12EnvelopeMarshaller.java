package com.helger.phase4.marshaller;

import java.util.List;

import javax.xml.namespace.QName;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.phase4.CAS4;
import com.helger.phase4.soap.ESoapVersion;
import com.helger.phase4.soap12.Soap12Envelope;
import com.helger.xsds.xml.CXML_XSD;

/**
 * Marshaller for {@link Soap12Envelope} objects.
 *
 * @author Philip Helger
 * @since 2.0.0
 */
public class Soap12EnvelopeMarshaller extends GenericJAXBMarshaller <Soap12Envelope>
{
  public static final List <ClassPathResource> XSDS = new CommonsArrayList <> (CXML_XSD.getXSDResource (),
                                                                               CAS4.XSD_SOAP12).getAsUnmodifiable ();

  public Soap12EnvelopeMarshaller ()
  {
    // Information is taken from the @XmlType of
    // Soap12Envelope and from @XmlSchema of package-info
    super (Soap12Envelope.class,
           XSDS,
           createSimpleJAXBElement (new QName (ESoapVersion.SOAP_12.getNamespaceURI (),
                                               "Envelope",
                                               ESoapVersion.SOAP_12.getNamespacePrefix ()),
                                    Soap12Envelope.class));
    setNamespaceContext (Soap12NamespaceHandler.getInstance ());
  }
}
