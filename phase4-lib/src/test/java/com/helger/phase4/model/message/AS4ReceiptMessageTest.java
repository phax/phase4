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
package com.helger.phase4.model.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.marshaller.Ebms3MessagingMarshaller;
import com.helger.phase4.marshaller.Ebms3NamespaceHandler;
import com.helger.phase4.marshaller.Soap12EnvelopeMarshaller;
import com.helger.phase4.marshaller.Soap12NamespaceHandler;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.soap12.Soap12Envelope;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * Test class for class {@link AS4ReceiptMessage}.
 *
 * @author Philip Helger
 */
public final class AS4ReceiptMessageTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4ReceiptMessageTest.class);

  @Test
  public void testReadWriteWithUserMessage ()
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setPartyInfo (MessageHelperMethods.createEbms3PartyInfo ("fromRole",
                                                                          "fromParty",
                                                                          "toRole",
                                                                          "toParty"));
    aUserMessage.setCollaborationInfo (MessageHelperMethods.createEbms3CollaborationInfo (null,
                                                                                          "urn:any",
                                                                                          null,
                                                                                          "svcType",
                                                                                          "svc",
                                                                                          "action",
                                                                                          MessageHelperMethods.createRandomConversationID ()));
    aUserMessage.setMessageProperties (MessageHelperMethods.createEbms3MessageProperties ());
    aUserMessage.setPayloadInfo (MessageHelperMethods.createEbms3PayloadInfo (false, new CommonsArrayList <> ()));
    aUserMessage.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());
    aUserMessage.setMpc (null);

    final AS4ReceiptMessage aMsg = AS4ReceiptMessage.create (ESoapVersion.AS4_DEFAULT,
                                                             MessageHelperMethods.createRandomMessageID (),
                                                             aUserMessage,
                                                             null,
                                                             false,
                                                             MessageHelperMethods.createRandomMessageID ());
    final Ebms3SignalMessage aSrcSignal = aMsg.getEbms3SignalMessage ();
    assertNotNull (aMsg);

    final Document aSoapDoc = aMsg.getAsSoapDocument ();
    assertNotNull (aSoapDoc);

    final byte [] aSoapBytes = XMLWriter.getNodeAsBytes (aSoapDoc);
    assertNotNull (aSoapBytes);

    if (false)
      LOGGER.info (XMLWriter.getNodeAsString (aSoapDoc,
                                              new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN)
                                                                      .setNamespaceContext (Ebms3NamespaceHandler.getInstance ()
                                                                                                                 .getClone ()
                                                                                                                 .addMappings (Soap12NamespaceHandler.getInstance ()))));

    final Soap12Envelope aSoapEnv = new Soap12EnvelopeMarshaller ().read (aSoapDoc);
    assertNotNull (aSoapEnv);

    final Element e = (Element) aSoapEnv.getHeader ().getAnyAtIndex (0);
    final Ebms3Messaging aEbms3 = new Ebms3MessagingMarshaller ().read (e);
    assertNotNull (aEbms3);

    // Does not work, because of "any" content
    if (false)
      assertEquals (aSrcSignal, aEbms3.getSignalMessageAtIndex (0));
  }

  @Test
  public void testReadWriteWithoutUserMessage ()
  {
    final AS4ReceiptMessage aMsg = AS4ReceiptMessage.create (ESoapVersion.AS4_DEFAULT,
                                                             MessageHelperMethods.createRandomMessageID (),
                                                             null,
                                                             null,
                                                             false,
                                                             MessageHelperMethods.createRandomMessageID ());
    final Ebms3SignalMessage aSrcSignal = aMsg.getEbms3SignalMessage ();
    assertNotNull (aMsg);

    final Document aSoapDoc = aMsg.getAsSoapDocument ();
    assertNotNull (aSoapDoc);

    final byte [] aSoapBytes = XMLWriter.getNodeAsBytes (aSoapDoc);
    assertNotNull (aSoapBytes);

    if (true)
      LOGGER.info (XMLWriter.getNodeAsString (aSoapDoc,
                                              new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN)
                                                                      .setNamespaceContext (Ebms3NamespaceHandler.getInstance ()
                                                                                                                 .getClone ()
                                                                                                                 .addMappings (Soap12NamespaceHandler.getInstance ()))));

    final Soap12Envelope aSoapEnv = new Soap12EnvelopeMarshaller ().read (aSoapDoc);
    assertNotNull (aSoapEnv);

    final Element e = (Element) aSoapEnv.getHeader ().getAnyAtIndex (0);
    final Ebms3Messaging aEbms3 = new Ebms3MessagingMarshaller ().read (e);
    assertNotNull (aEbms3);

    // Does not work, because of "any" content
    if (false)
      assertEquals (aSrcSignal, aEbms3.getSignalMessageAtIndex (0));
  }
}
