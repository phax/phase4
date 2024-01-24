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
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.jaxb.builder.JAXBWriterBuilder;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.ebms3header.NonRepudiationInformation;
import com.helger.phase4.soap11.Soap11Envelope;
import com.helger.phase4.soap12.Soap12Envelope;

/**
 * A reader builder for Ebms documents.
 *
 * @author Philip Helger
 * @param <JAXBTYPE>
 *        The Ebms implementation class to be written
 */
@NotThreadSafe
@Deprecated (since = "2.0.0", forRemoval = true)
public class Ebms3WriterBuilder <JAXBTYPE> extends JAXBWriterBuilder <JAXBTYPE, Ebms3WriterBuilder <JAXBTYPE>>
{
  public Ebms3WriterBuilder (@Nonnull final EEbms3DocumentType eDocType)
  {
    super (eDocType);
    setNamespaceContext (Ebms3NamespaceHandler.getInstance ());
  }

  @Nonnull
  public static Ebms3WriterBuilder <Ebms3Messaging> ebms3Messaging ()
  {
    return new Ebms3WriterBuilder <> (EEbms3DocumentType.MESSAGING);
  }

  @Nonnull
  public static Ebms3WriterBuilder <NonRepudiationInformation> nonRepudiationInformation ()
  {
    return new Ebms3WriterBuilder <> (EEbms3DocumentType.NON_REPUDIATION_INFORMATION);
  }

  @Nonnull
  public static Ebms3WriterBuilder <Soap11Envelope> soap11 ()
  {
    return new Ebms3WriterBuilder <> (EEbms3DocumentType.SOAP_11);
  }

  @Nonnull
  public static Ebms3WriterBuilder <Soap12Envelope> soap12 ()
  {
    return new Ebms3WriterBuilder <> (EEbms3DocumentType.SOAP_12);
  }
}
