/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.xml.validation.Schema;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.builder.IJAXBDocumentType;
import com.helger.jaxb.builder.JAXBDocumentType;
import com.helger.phase4.CAS4;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.ebms3header.NonRepudiationInformation;
import com.helger.phase4.soap11.Soap11Envelope;
import com.helger.phase4.soap12.Soap12Envelope;
import com.helger.xsds.xlink.CXLink;
import com.helger.xsds.xml.CXML_XSD;
import com.helger.xsds.xmldsig.CXMLDSig;

/**
 * Enumeration wizh all known EBMS document types.
 *
 * @author Philip Helger
 */
public enum EEbms3DocumentType implements IJAXBDocumentType
{
  MESSAGING (Ebms3Messaging.class,
             new CommonsArrayList <> (CXML_XSD.getXSDResource (),
                                      CXLink.getXSDResource (),
                                      CXMLDSig.getXSDResource (),
                                      CAS4.XSD_EBBP_SIGNALS,
                                      CAS4.XSD_EBMS_HEADER)),
  NON_REPUDIATION_INFORMATION (NonRepudiationInformation.class,
                               new CommonsArrayList <> (CXML_XSD.getXSDResource (),
                                                        CXLink.getXSDResource (),
                                                        CXMLDSig.getXSDResource (),
                                                        CAS4.XSD_EBBP_SIGNALS)),
  SOAP_11 (Soap11Envelope.class, new CommonsArrayList <> (CAS4.XSD_SOAP11)),
  SOAP_12 (Soap12Envelope.class, new CommonsArrayList <> (CXML_XSD.getXSDResource (), CAS4.XSD_SOAP12));

  private final JAXBDocumentType m_aDocType;

  EEbms3DocumentType (@Nonnull final Class <?> aClass, @Nonnull final List <? extends ClassPathResource> aXSDPaths)
  {
    m_aDocType = new JAXBDocumentType (aClass, aXSDPaths, null);
  }

  @Nonnull
  public Class <?> getImplementationClass ()
  {
    return m_aDocType.getImplementationClass ();
  }

  @Nonnull
  @Nonempty
  @ReturnsMutableCopy
  public ICommonsList <ClassPathResource> getAllXSDResources ()
  {
    return m_aDocType.getAllXSDResources ();
  }

  @Nonnull
  public String getNamespaceURI ()
  {
    return m_aDocType.getNamespaceURI ();
  }

  @Nonnull
  @Nonempty
  public String getLocalName ()
  {
    return m_aDocType.getLocalName ();
  }

  @Nonnull
  public Schema getSchema ()
  {
    return m_aDocType.getSchema ();
  }
}
