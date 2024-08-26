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
package com.helger.phase4.server.spi;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3PayloadInfo;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3PullRequest;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.incoming.spi.AS4MessageProcessorResult;
import com.helger.phase4.incoming.spi.AS4SignalMessageProcessorResult;
import com.helger.phase4.incoming.spi.IAS4IncomingMessageProcessorSPI;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.server.MockPModeGenerator;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.write.XMLWriter;

/**
 * Test implementation of {@link IAS4IncomingMessageProcessorSPI}
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class MockMessageProcessorCheckingStreamsSPI implements IAS4IncomingMessageProcessorSPI
{
  public static final String ACTION_FAILURE = "Failure";
  private static final String DEFAULT_AGREEMENT = "urn:as4:agreements:so-that-we-have-a-non-empty-value";

  private static final Logger LOGGER = LoggerFactory.getLogger (MockMessageProcessorCheckingStreamsSPI.class);

  @Nonnull
  public AS4MessageProcessorResult processAS4UserMessage (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                          @Nonnull final HttpHeaderMap aHttpHeaders,
                                                          @Nonnull final Ebms3UserMessage aUserMessage,
                                                          @Nonnull final IPMode aPMode,
                                                          @Nullable final Node aPayload,
                                                          @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                          @Nonnull final IAS4IncomingMessageState aState,
                                                          @Nonnull final ICommonsList <Ebms3Error> aEbmsErrorMessagesTarget)
  {
    // Needed for AS4_TA13 because we want to force a decompression failure and
    // for that to happen the stream has to be read
    {
      LOGGER.info ("Received AS4 message:");
      LOGGER.info ("  UserMessage: " + aUserMessage);
      LOGGER.info ("  Payload: " +
                   (aPayload == null ? "null" : true ? "present" : XMLWriter.getNodeAsString (aPayload)));

      if (aIncomingAttachments != null)
      {
        LOGGER.info ("  Attachments: " + aIncomingAttachments.size ());
        for (final WSS4JAttachment x : aIncomingAttachments)
        {
          LOGGER.info ("    Attachment Content Type: " + x.getMimeType ());
          if (x.getMimeType ().startsWith ("text") || x.getMimeType ().endsWith ("/xml"))
          {
            try (final InputStream aIS = x.getSourceStream ())
            {
              LOGGER.info ("    Attachment Stream Class: " + aIS.getClass ().getName ());
              final String sContent = StreamHelper.getAllBytesAsString (x.getSourceStream (), x.getCharset ());
              LOGGER.info ("    Attachment Content: " + (sContent == null ? "null" : sContent.length () + " chars"));
            }
            catch (final IllegalStateException | IOException ex)
            {
              LOGGER.warn ("    Attachment Content: CANNOT BE READ", ex);
            }
          }
        }
      }
    }

    // To test returning with a failure works as intended
    if (aUserMessage.getCollaborationInfo ().getAction ().equals (ACTION_FAILURE))
    {
      aEbmsErrorMessagesTarget.add (EEbmsError.EBMS_OTHER.errorBuilder (aState.getLocale ())
                                                         .refToMessageInError (aState.getMessageID ())
                                                         .errorDetail (ACTION_FAILURE)
                                                         .build ());
      return AS4MessageProcessorResult.createFailure ();
    }
    return AS4MessageProcessorResult.createSuccess ();
  }

  @Nonnull
  public AS4SignalMessageProcessorResult processAS4SignalMessage (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                                  @Nonnull final HttpHeaderMap aHttpHeaders,
                                                                  @Nonnull final Ebms3SignalMessage aSignalMessage,
                                                                  @Nonnull final IPMode aPmode,
                                                                  @Nonnull final IAS4IncomingMessageState aState,
                                                                  @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
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

    final Ebms3PullRequest aPR = aSignalMessage.getPullRequest ();
    if (aPR == null || !"TWO-SPI".equals (aPR.getMpc ()))
    {
      // Some other PR - just acknowledge
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    // Demo special handling
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = AS4TestConstants.getEBMSProperties ();

    final Ebms3MessageInfo aMessageInfo = aSignalMessage.getMessageInfo ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (aMessageInfo.getMessageId ());
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (aPayload != null, null);

    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    final Ebms3PartyInfo aEbms3PartyInfo;
    aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo ("PullPMode",
                                                                                 DEFAULT_AGREEMENT,
                                                                                 null,
                                                                                 AS4TestConstants.TEST_SERVICE_TYPE,
                                                                                 MockPModeGenerator.SOAP11_SERVICE,
                                                                                 AS4TestConstants.TEST_ACTION,
                                                                                 AS4TestConstants.TEST_CONVERSATION_ID);
    aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_INITIATOR_URL,
                                                                 "pullinitiator",
                                                                 CAS4.DEFAULT_RESPONDER_URL,
                                                                 "pullresponder");

    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (aEbms3Properties);

    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setCollaborationInfo (aEbms3CollaborationInfo);
    aUserMessage.setMessageInfo (aEbms3MessageInfo);
    aUserMessage.setMessageProperties (aEbms3MessageProperties);
    aUserMessage.setPartyInfo (aEbms3PartyInfo);
    aUserMessage.setPayloadInfo (aEbms3PayloadInfo);
    aUserMessage.setMpc (aSignalMessage.getPullRequest ().getMpc ());

    return AS4SignalMessageProcessorResult.createSuccess (null, null, aUserMessage);
  }

  public void processAS4ResponseMessage (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                         @Nonnull final IAS4IncomingMessageState aState,
                                         @Nonnull @Nonempty final String sResponseMessageID,
                                         @Nullable final byte [] aResponseBytes,
                                         final boolean bResponsePayloadIsAvailable)
  {}
}
