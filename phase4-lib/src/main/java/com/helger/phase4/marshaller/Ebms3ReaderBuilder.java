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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.jaxb.builder.JAXBReaderBuilder;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.soap11.Soap11Envelope;
import com.helger.phase4.soap12.Soap12Envelope;

/**
 * A reader builder for Ebms documents.
 *
 * @author Philip Helger
 * @param <JAXBTYPE>
 *        The Ebms implementation class to be read
 */
@NotThreadSafe
public class Ebms3ReaderBuilder <JAXBTYPE> extends JAXBReaderBuilder <JAXBTYPE, Ebms3ReaderBuilder <JAXBTYPE>>
{
  public Ebms3ReaderBuilder (@Nonnull final EEbms3DocumentType eDocType, @Nonnull final Class <JAXBTYPE> aImplClass)
  {
    super (eDocType, aImplClass);
  }

  @Nonnull
  public static Ebms3ReaderBuilder <Ebms3Messaging> ebms3Messaging ()
  {
    return new Ebms3ReaderBuilder <> (EEbms3DocumentType.MESSAGING, Ebms3Messaging.class);
  }

  @Nonnull
  public static Ebms3ReaderBuilder <Soap11Envelope> soap11 ()
  {
    return new Ebms3ReaderBuilder <> (EEbms3DocumentType.SOAP_11, Soap11Envelope.class);
  }

  @Nonnull
  public static Ebms3ReaderBuilder <Soap12Envelope> soap12 ()
  {
    return new Ebms3ReaderBuilder <> (EEbms3DocumentType.SOAP_12, Soap12Envelope.class);
  }
}
