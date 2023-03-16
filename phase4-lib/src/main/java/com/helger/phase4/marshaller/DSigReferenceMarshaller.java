package com.helger.phase4.marshaller;

import java.util.List;

import javax.xml.namespace.QName;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.xsds.xmldsig.CXMLDSig;
import com.helger.xsds.xmldsig.ReferenceType;

/**
 * Marshaller for {@link ReferenceType} objects.
 *
 * @author Philip Helger
 * @since 2.0.0
 */
public class DSigReferenceMarshaller extends GenericJAXBMarshaller <ReferenceType>
{
  public static final List <ClassPathResource> XSDS = new CommonsArrayList <> (CXMLDSig.getXSDResource ()).getAsUnmodifiable ();

  public DSigReferenceMarshaller ()
  {
    super (ReferenceType.class,
           XSDS,
           createSimpleJAXBElement (new QName (CXMLDSig.NAMESPACE_URI, "ReferenceType", CXMLDSig.DEFAULT_PREFIX),
                                    ReferenceType.class));
  }
}
