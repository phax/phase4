/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.phase4.messaging.mime;

import java.io.IOException;
import java.io.OutputStream;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataSource;
import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.phase4.soap.ESOAPVersion;
import com.sun.mail.handlers.text_plain;

/**
 * Special DataContentHandler for SOAP 1.2 messages with the special MIME type.
 *
 * @author Philip Helger
 */
public class DataContentHandlerSoap12 extends text_plain
{
  private static final ActivationDataFlavor [] FLAVORS = { new ActivationDataFlavor (StreamSource.class,
                                                                                     ESOAPVersion.SOAP_12.getMimeType ()
                                                                                                         .getAsStringWithoutParameters (),
                                                                                     "SOAP") };

  @Override
  @Nonnull
  @ReturnsMutableObject ("design")
  protected ActivationDataFlavor [] getDataFlavors ()
  {
    return FLAVORS;
  }

  @Override
  @Nonnull
  protected Object getData (@Nonnull final ActivationDataFlavor aFlavor,
                            @Nonnull final DataSource ds) throws IOException
  {
    if (aFlavor.getRepresentationClass () == StreamSource.class)
      return new StreamSource (ds.getInputStream ());
    throw new IOException ("Unsupported flavor " + aFlavor + " on DS " + ds);
  }

  /**
   */
  @Override
  public void writeTo (@Nonnull final Object aObj,
                       @Nonnull final String sMimeType,
                       @Nonnull @WillNotClose final OutputStream aOS) throws IOException
  {
    try
    {
      final Transformer transformer = TransformerFactory.newInstance ().newTransformer ();
      final StreamResult result = new StreamResult (aOS);
      if (aObj instanceof DataSource)
      {
        // Streaming transform applies only to
        // javax.xml.transform.StreamSource
        transformer.transform (new StreamSource (((DataSource) aObj).getInputStream ()), result);
      }
      else
        if (aObj instanceof Source)
        {
          transformer.transform ((Source) aObj, result);
        }
        else
        {
          throw new IOException ("Invalid Object type = " +
                                 aObj.getClass () +
                                 ". DataContentHandlerSoap12 can only convert DataSource or Source to XML.");
        }
    }
    catch (final TransformerException | RuntimeException ex)
    {
      throw new IOException ("Unable to run the JAXP transformer on a stream", ex);
    }
  }
}
