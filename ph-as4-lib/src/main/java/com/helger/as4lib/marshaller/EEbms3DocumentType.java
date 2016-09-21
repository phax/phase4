package com.helger.as4lib.marshaller;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.validation.Schema;

import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.NonRepudiationInformation;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.as4lib.soap12.Soap12Envelope;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.jaxb.builder.IJAXBDocumentType;
import com.helger.jaxb.builder.JAXBDocumentType;

public enum EEbms3DocumentType implements IJAXBDocumentType
{
  MESSAGING (Ebms3Messaging.class, new CommonsArrayList<> (new ClassPathResource (CAS4.XSD_EBMS_HEADER),
                                                           new ClassPathResource (CAS4.XSD_EBBP_SIGNALS))),
  NON_REPUDIATION_INFORMATION (NonRepudiationInformation.class, new CommonsArrayList<> (new ClassPathResource (CAS4.XSD_EBBP_SIGNALS))),
  SOAP_11 (Soap11Envelope.class, new CommonsArrayList<> (new ClassPathResource (CAS4.XSD_SOAP11))),
  SOAP_12 (Soap12Envelope.class, new CommonsArrayList<> (new ClassPathResource (CAS4.XSD_SOAP12)));

  private final JAXBDocumentType m_aDocType;

  private EEbms3DocumentType (@Nonnull final Class <?> aClass,
                              @Nonnull final List <? extends IReadableResource> aXSDPaths)
  {
    this (aClass, aXSDPaths, null);
  }

  private EEbms3DocumentType (@Nonnull final Class <?> aClass,
                              @Nonnull final List <? extends IReadableResource> aXSDPaths,
                              @Nullable final Function <String, String> aTypeToElementNameMapper)
  {
    m_aDocType = new JAXBDocumentType (aClass,
                                       CollectionHelper.newListMapped (aXSDPaths, IReadableResource::getPath),
                                       aTypeToElementNameMapper);
  }

  @Nonnull
  public Class <?> getImplementationClass ()
  {
    return m_aDocType.getImplementationClass ();
  }

  @Nonnull
  @Nonempty
  @ReturnsMutableCopy
  public ICommonsList <String> getAllXSDPaths ()
  {
    return m_aDocType.getAllXSDPaths ();
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
  public Schema getSchema (@Nullable final ClassLoader aClassLoader)
  {
    return m_aDocType.getSchema (aClassLoader);
  }
}
