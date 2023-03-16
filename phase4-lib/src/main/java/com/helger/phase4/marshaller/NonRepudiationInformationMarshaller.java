package com.helger.phase4.marshaller;

import java.util.List;

import javax.xml.namespace.QName;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.phase4.CAS4;
import com.helger.phase4.ebms3header.NonRepudiationInformation;
import com.helger.xsds.xlink.CXLink;
import com.helger.xsds.xml.CXML_XSD;
import com.helger.xsds.xmldsig.CXMLDSig;

/**
 * Marshaller for {@link NonRepudiationInformation} objects.
 *
 * @author Philip Helger
 * @since 2.0.0
 */
public class NonRepudiationInformationMarshaller extends GenericJAXBMarshaller <NonRepudiationInformation>
{
  public static final List <ClassPathResource> XSDS = new CommonsArrayList <> (CXML_XSD.getXSDResource (),
                                                                               CXLink.getXSDResource (),
                                                                               CXMLDSig.getXSDResource (),
                                                                               CAS4.XSD_EBBP_SIGNALS).getAsUnmodifiable ();

  public NonRepudiationInformationMarshaller ()
  {
    // Information is taken from the @XmlRootElement of
    // NonRepudiationInformation
    super (NonRepudiationInformation.class,
           XSDS,
           createSimpleJAXBElement (new QName ("http://docs.oasis-open.org/ebxml-bp/ebbp-signals-2.0",
                                               "NonRepudiationInformation"),
                                    NonRepudiationInformation.class));
    setNamespaceContext (Ebms3NamespaceHandler.getInstance ());
  }
}
