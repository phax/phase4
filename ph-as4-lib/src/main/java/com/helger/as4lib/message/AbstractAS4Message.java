/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.marshaller.Ebms3WriterBuilder;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.soap11.Soap11Body;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.as4lib.soap11.Soap11Header;
import com.helger.as4lib.soap12.Soap12Body;
import com.helger.as4lib.soap12.Soap12Envelope;
import com.helger.as4lib.soap12.Soap12Header;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.traits.IGenericImplTrait;

public abstract class AbstractAS4Message <IMPLTYPE extends AbstractAS4Message <IMPLTYPE>>
                                         implements IAS4Message, IGenericImplTrait <IMPLTYPE>
{
  private final ESOAPVersion m_eSOAPVersion;
  protected final Ebms3Messaging m_aMessaging = new Ebms3Messaging ();

  public AbstractAS4Message (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
  }

  @Nonnull
  public final ESOAPVersion getSOAPVersion ()
  {
    return m_eSOAPVersion;
  }

  @Nonnull
  public final IMPLTYPE setMustUnderstand (final boolean bMustUnderstand)
  {
    switch (m_eSOAPVersion)
    {
      case SOAP_11:
        m_aMessaging.setS11MustUnderstand (Boolean.valueOf (bMustUnderstand));
        break;
      case SOAP_12:
        m_aMessaging.setS12MustUnderstand (Boolean.valueOf (bMustUnderstand));
        break;
      default:
        throw new IllegalStateException ("Unsupported SOAP version");
    }
    return thisAsT ();
  }

  @Nonnull
  public final Document getAsSOAPDocument (@Nullable final Node aPayload)
  {
    final Document aEbms3Document = Ebms3WriterBuilder.ebms3Messaging ().getAsDocument (m_aMessaging);
    if (aEbms3Document == null)
      throw new IllegalStateException ("Failed to write EBMS3 Messaging to XML");

    final Node aRealPayload = aPayload instanceof Document ? ((Document) aPayload).getDocumentElement () : aPayload;

    switch (m_eSOAPVersion)
    {
      case SOAP_11:
      {
        // Creating SOAP 11 Envelope
        final Soap11Envelope aSoapEnv = new Soap11Envelope ();
        aSoapEnv.setHeader (new Soap11Header ());
        aSoapEnv.setBody (new Soap11Body ());
        aSoapEnv.getHeader ().addAny (aEbms3Document.getDocumentElement ());
        if (aRealPayload != null)
          aSoapEnv.getBody ().addAny (aRealPayload);
        return Ebms3WriterBuilder.soap11 ().getAsDocument (aSoapEnv);
      }
      case SOAP_12:
      {
        // Creating SOAP 12 Envelope
        final Soap12Envelope aSoapEnv = new Soap12Envelope ();
        aSoapEnv.setHeader (new Soap12Header ());
        aSoapEnv.setBody (new Soap12Body ());
        aSoapEnv.getHeader ().addAny (aEbms3Document.getDocumentElement ());
        if (aRealPayload != null)
          aSoapEnv.getBody ().addAny (aRealPayload);
        return Ebms3WriterBuilder.soap12 ().getAsDocument (aSoapEnv);
      }
      default:
        throw new IllegalStateException ("Unsupported SOAP version!");
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("SOAPVersion", m_eSOAPVersion).toString ();
  }
}
