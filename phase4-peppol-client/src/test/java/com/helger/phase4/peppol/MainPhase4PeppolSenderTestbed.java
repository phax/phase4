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
package com.helger.phase4.peppol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.peppol.sml.ESML;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.marshaller.Ebms3SignalMessageMarshaller;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.servlet.mock.MockServletContext;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Send to Peppol Testbed
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderTestbed
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderTestbed.class);

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    try
    {
      // TODO use correct XML to send as String (no SBDH)
      final String sXML = "TODO";
      final Element aPayloadElement = DOMReader.readXMLDOM (sXML).getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ("Failed to read XML file to be send");

      // TODO use correct participant ID
      final IParticipantIdentifier aSenderID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("yours");
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("theirs");
      final EAS4UserMessageSendResult eResult;
      eResult = Phase4PeppolSender.builder ()
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                  .senderParticipantID (aSenderID)
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID ("POP000306")
                                  .countryC1 ("AT")
                                  .payload (aPayloadElement)
                                  .smpClient (new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                                                                     aReceiverID,
                                                                     ESML.DIGIT_TEST))
                                  .disableValidation ()
                                  .signalMsgConsumer ( (aSignalMsg, aMessageMetadata, aState) -> {
                                    LOGGER.info (new Ebms3SignalMessageMarshaller ().setFormattedOutput (true)
                                                                                    .getAsString (aSignalMsg));
                                  })
                                  .sendMessageAndCheckForReceipt ();
      LOGGER.info ("Peppol send result: " + eResult);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending Peppol message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
