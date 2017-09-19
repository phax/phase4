/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.server.spi;

import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.helger.as4.AS4TestConstants;
import com.helger.as4.CAS4;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.domain.UserMessageCreator;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.server.MockPModeGenerator;
import com.helger.as4.servlet.IAS4MessageState;
import com.helger.as4.servlet.spi.AS4MessageProcessorResult;
import com.helger.as4.servlet.spi.AS4SignalMessageProcessorResult;
import com.helger.as4.servlet.spi.IAS4ServletMessageProcessorSPI;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.write.XMLWriter;

/**
 * Test implementation of {@link IAS4ServletMessageProcessorSPI}
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class MockMessageProcessorCheckingStreamsSPI implements IAS4ServletMessageProcessorSPI
{
  public static final String ACTION_FAILURE = "Failure";

  private static final Logger s_aLogger = LoggerFactory.getLogger (MockMessageProcessorCheckingStreamsSPI.class);

  @Nonnull
  public AS4MessageProcessorResult processAS4UserMessage (@Nonnull final Ebms3UserMessage aUserMessage,
                                                          @Nonnull final IPMode aPMode,
                                                          @Nullable final Node aPayload,
                                                          @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                          @Nonnull final IAS4MessageState aState)
  {
    // Needed for AS4_TA13 because we want to force a decompression failure and
    // for that to happen the stream has to be read
    {
      s_aLogger.info ("Received AS4 message:");
      s_aLogger.info ("  UserMessage: " + aUserMessage);
      s_aLogger.info ("  Payload: " +
                      (aPayload == null ? "null" : true ? "present" : XMLWriter.getNodeAsString (aPayload)));

      if (aIncomingAttachments != null)
      {
        s_aLogger.info ("  Attachments: " + aIncomingAttachments.size ());
        for (final WSS4JAttachment x : aIncomingAttachments)
        {
          s_aLogger.info ("    Attachment Content Type: " + x.getMimeType ());
          if (x.getMimeType ().startsWith ("text") || x.getMimeType ().endsWith ("/xml"))
          {
            try
            {
              final InputStream aIS = x.getSourceStream ();
              s_aLogger.info ("    Attachment Stream Class: " + aIS.getClass ().getName ());
              final String sContent = StreamHelper.getAllBytesAsString (x.getSourceStream (), x.getCharset ());
              s_aLogger.info ("    Attachment Content: " + sContent.length () + " chars");
            }
            catch (final IllegalStateException ex)
            {
              s_aLogger.warn ("    Attachment Content: CANNOT BE READ", ex);
            }
          }
        }
      }
    }

    // To test returning with a failure works as intended
    if (aUserMessage.getCollaborationInfo ().getAction ().equals (ACTION_FAILURE))
    {
      return AS4MessageProcessorResult.createFailure (ACTION_FAILURE);
    }
    return AS4MessageProcessorResult.createSuccess ();
  }

  @Nonnull
  public AS4SignalMessageProcessorResult processAS4SignalMessage (@Nonnull final Ebms3SignalMessage aSignalMessage,
                                                                  @Nonnull final IPMode aPmode,
                                                                  @Nonnull final IAS4MessageState aState)
  {
    if (aSignalMessage.getReceipt () != null)
    {
      // Receipt - just acknowledge
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    if (!aSignalMessage.getError ().isEmpty ())
    {
      // Error - just acknowledge
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    if (aSignalMessage.getPullRequest ().getMpc ().equals ("TWO-SPI"))
    {
      try
      {
        final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));

        // Add properties
        final ICommonsList <Ebms3Property> aEbms3Properties = MockEbmsHelper.getEBMSProperties ();

        final Ebms3MessageInfo aMessageInfo = aSignalMessage.getMessageInfo ();

        final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (aMessageInfo.getMessageId ());
        final Ebms3PayloadInfo aEbms3PayloadInfo = UserMessageCreator.createEbms3PayloadInfo (aPayload, null);

        final Ebms3CollaborationInfo aEbms3CollaborationInfo;
        final Ebms3PartyInfo aEbms3PartyInfo;
        aEbms3CollaborationInfo = UserMessageCreator.createEbms3CollaborationInfo (AS4TestConstants.TEST_ACTION,
                                                                                   AS4TestConstants.TEST_SERVICE_TYPE,
                                                                                   MockPModeGenerator.SOAP11_SERVICE,
                                                                                   AS4TestConstants.TEST_CONVERSATION_ID,
                                                                                   "PullPMode",
                                                                                   MockEbmsHelper.DEFAULT_AGREEMENT);
        aEbms3PartyInfo = UserMessageCreator.createEbms3PartyInfo (CAS4.DEFAULT_SENDER_URL,
                                                                   "pullinitiator",
                                                                   CAS4.DEFAULT_RESPONDER_URL,
                                                                   "pullresponder");

        final Ebms3MessageProperties aEbms3MessageProperties = UserMessageCreator.createEbms3MessageProperties (aEbms3Properties);

        final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
        aUserMessage.setCollaborationInfo (aEbms3CollaborationInfo);
        aUserMessage.setMessageInfo (aEbms3MessageInfo);
        aUserMessage.setMessageProperties (aEbms3MessageProperties);
        aUserMessage.setPartyInfo (aEbms3PartyInfo);
        aUserMessage.setPayloadInfo (aEbms3PayloadInfo);
        aUserMessage.setMpc (aSignalMessage.getPullRequest ().getMpc ());

        return AS4SignalMessageProcessorResult.createSuccess (null, null, aUserMessage);
      }
      catch (final SAXException ex)
      {
        s_aLogger.error ("Internal error", ex);
      }
    }
    return AS4SignalMessageProcessorResult.createSuccess ();
  }
}
