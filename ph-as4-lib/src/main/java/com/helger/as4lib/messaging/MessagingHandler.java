/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.messaging;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.marshaller.Ebms3WriterBuilder;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.soap11.Soap11Body;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.as4lib.soap11.Soap11Header;
import com.helger.as4lib.soap12.Soap12Body;
import com.helger.as4lib.soap12.Soap12Envelope;
import com.helger.as4lib.soap12.Soap12Header;
import com.helger.commons.ValueEnforcer;

public class MessagingHandler
{
  public MessagingHandler ()
  {}

  public void handleSignalMessage (@Nonnull final Ebms3SignalMessage aSignalMessage)
  {
    ValueEnforcer.notNull (aSignalMessage, "SignalMessage");
    // TODO
  }

  public void handleUserMessage (@Nonnull final Ebms3UserMessage aUserMessage)
  {
    ValueEnforcer.notNull (aUserMessage, "UserMessage");
    // TODO
  }

  public void handle (@Nonnull final Ebms3Messaging aMessaging)
  {
    ValueEnforcer.notNull (aMessaging, "Messaging");

    if (aMessaging.hasSignalMessageEntries ())
      for (final Ebms3SignalMessage aSignalMessage : aMessaging.getSignalMessage ())
        handleSignalMessage (aSignalMessage);
    if (aMessaging.hasUserMessageEntries ())
      for (final Ebms3UserMessage aUserMessage : aMessaging.getUserMessage ())
        handleUserMessage (aUserMessage);
  }

  @Nonnull
  public static Document createSOAPEnvelopeAsDocument (@Nonnull final ESOAPVersion eSOAPVersion,
                                                       @Nonnull final Ebms3Messaging aEbms3Messaging,
                                                       @Nullable final Element aPayload)
  {
    final Document aEbms3Document = Ebms3WriterBuilder.ebms3Messaging ().getAsDocument (aEbms3Messaging);

    switch (eSOAPVersion)
    {
      case SOAP_11:
      {
        // Creating SOAP 11 Envelope
        final Soap11Envelope aSoapEnv = new Soap11Envelope ();
        aSoapEnv.setHeader (new Soap11Header ());
        aSoapEnv.setBody (new Soap11Body ());
        aSoapEnv.getHeader ().addAny (aEbms3Document.getDocumentElement ());
        if (aPayload != null)
          aSoapEnv.getBody ().addAny (aPayload);
        return Ebms3WriterBuilder.soap11 ().getAsDocument (aSoapEnv);
      }
      case SOAP_12:
      {
        // Creating SOAP 12 Envelope
        final Soap12Envelope aSoapEnv = new Soap12Envelope ();
        aSoapEnv.setHeader (new Soap12Header ());
        aSoapEnv.setBody (new Soap12Body ());
        aSoapEnv.getHeader ().addAny (aEbms3Document.getDocumentElement ());
        if (aPayload != null)
          aSoapEnv.getBody ().addAny (aPayload);
        return Ebms3WriterBuilder.soap12 ().getAsDocument (aSoapEnv);
      }
      default:
        throw new IllegalArgumentException ("Unsupported SOAP version!");
    }
  }
}
