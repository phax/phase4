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
  public static final QName ROOT_ELEMENT_QNAME = new QName (ESoapVersion.SOAP_11.getNamespaceURI (),
                                                            "Envelope",
                                                            ESoapVersion.SOAP_11.getNamespacePrefix ());

  public Soap11EnvelopeMarshaller ()
  {
    // Information is taken from the @XmlType of
    // Soap11Envelope and from @XmlSchema of package-info
    super (Soap11Envelope.class, XSDS, createSimpleJAXBElement (ROOT_ELEMENT_QNAME, Soap11Envelope.class));
    setNamespaceContext (Soap11NamespaceHandler.getInstance ());
  }
}
