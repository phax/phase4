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

import javax.annotation.Nonnull;

import com.helger.jaxb.builder.JAXBReaderBuilder;
import com.helger.xsds.xmldsig.ReferenceType;

/**
 * A reader builder for XMLDSig documents.
 *
 * @author Philip Helger
 * @param <JAXBTYPE>
 *        The XMLDSig implementation class to be read
 */
@Deprecated (since = "2.0.0", forRemoval = true)
public class XMLDSigReaderBuilder <JAXBTYPE> extends JAXBReaderBuilder <JAXBTYPE, XMLDSigReaderBuilder <JAXBTYPE>>
{
  public XMLDSigReaderBuilder (@Nonnull final EXMLDSigDocumentType eDocType, @Nonnull final Class <JAXBTYPE> aImplClass)
  {
    super (eDocType, aImplClass);
  }

  @Nonnull
  public static XMLDSigReaderBuilder <ReferenceType> dsigReference ()
  {
    return new XMLDSigReaderBuilder <> (EXMLDSigDocumentType.REFERENCE, ReferenceType.class);
  }
}
