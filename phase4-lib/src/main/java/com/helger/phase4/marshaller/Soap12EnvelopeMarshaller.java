/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phase4.marshaller;

import java.util.List;

import javax.xml.namespace.QName;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.phase4.CAS4;
import com.helger.phase4.model.ESoapVersion;
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
  public static final QName ROOT_ELEMENT_QNAME = new QName (ESoapVersion.SOAP_12.getNamespaceURI (),
                                                            "Envelope",
                                                            ESoapVersion.SOAP_12.getNamespacePrefix ());

  public Soap12EnvelopeMarshaller ()
  {
    // Information is taken from the @XmlType of
    // Soap12Envelope and from @XmlSchema of package-info
    super (Soap12Envelope.class, XSDS, createSimpleJAXBElement (ROOT_ELEMENT_QNAME, Soap12Envelope.class));
    setNamespaceContext (Soap12NamespaceHandler.getInstance ());
  }
}
