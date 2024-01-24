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

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.phase4.CAS4;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.ebms3header.ObjectFactory;
import com.helger.xsds.xlink.CXLink;
import com.helger.xsds.xml.CXML_XSD;
import com.helger.xsds.xmldsig.CXMLDSig;

/**
 * Marshaller for {@link Ebms3Messaging} objects.
 *
 * @author Philip Helger
 * @since 2.0.0
 */
public class Ebms3MessagingMarshaller extends GenericJAXBMarshaller <Ebms3Messaging>
{
  public static final List <ClassPathResource> XSDS = new CommonsArrayList <> (CXML_XSD.getXSDResource (),
                                                                               CXLink.getXSDResource (),
                                                                               CXMLDSig.getXSDResource (),
                                                                               CAS4.XSD_EBBP_SIGNALS,
                                                                               CAS4.XSD_EBMS_HEADER).getAsUnmodifiable ();

  public Ebms3MessagingMarshaller ()
  {
    super (Ebms3Messaging.class, XSDS, new ObjectFactory ()::createMessaging);
    setNamespaceContext (Ebms3NamespaceHandler.getInstance ());
  }
}
