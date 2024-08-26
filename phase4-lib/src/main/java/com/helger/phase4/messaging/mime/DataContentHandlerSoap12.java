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
package com.helger.phase4.messaging.mime;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.angus.mail.handlers.text_plain;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.phase4.model.ESoapVersion;

import jakarta.activation.ActivationDataFlavor;
import jakarta.activation.DataSource;

/**
 * Special DataContentHandler for SOAP 1.2 messages with the special MIME type.
 *
 * @author Philip Helger
 */
public class DataContentHandlerSoap12 extends text_plain
{
  private static final ActivationDataFlavor [] FLAVORS = { new ActivationDataFlavor (StreamSource.class,
                                                                                     ESoapVersion.SOAP_12.getMimeType ()
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
  protected Object getData (@Nonnull final ActivationDataFlavor aFlavor, @Nonnull final DataSource aDataSource)
                                                                                                                throws IOException
  {
    if (aFlavor.getRepresentationClass () == StreamSource.class)
      return new StreamSource (aDataSource.getInputStream ());

    throw new IOException ("Unsupported flavor " + aFlavor + " on DS " + aDataSource);
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
      final Transformer aTransformer = TransformerFactory.newInstance ().newTransformer ();
      final StreamResult aStreamResult = new StreamResult (aOS);
      if (aObj instanceof DataSource)
      {
        // Streaming transform applies only to
        // javax.xml.transform.StreamSource
        aTransformer.transform (new StreamSource (((DataSource) aObj).getInputStream ()), aStreamResult);
      }
      else
        if (aObj instanceof Source)
        {
          aTransformer.transform ((Source) aObj, aStreamResult);
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
