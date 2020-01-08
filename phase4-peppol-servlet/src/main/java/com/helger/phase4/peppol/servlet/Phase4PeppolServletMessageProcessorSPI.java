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
package com.helger.phase4.peppol.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.attachment.AS4DecompressException;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.IAS4Attachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.error.EEbmsError;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.servlet.IAS4MessageState;
import com.helger.phase4.servlet.spi.AS4MessageProcessorResult;
import com.helger.phase4.servlet.spi.AS4SignalMessageProcessorResult;
import com.helger.phase4.servlet.spi.IAS4ServletMessageProcessorSPI;
import com.helger.sbdh.builder.SBDHReader;
import com.helger.xml.serialize.write.XMLWriter;

/**
 * This is the SPI implementation to handle incoming AS4 requests from
 * phase4-servlet. Users of this package must implement
 * {@link IPhase4PeppolIncomingSBDHandlerSPI} instead.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class Phase4PeppolServletMessageProcessorSPI implements IAS4ServletMessageProcessorSPI
{
  public static final class ReadAttachment
  {
    private String m_sID;
    private String m_sMimeType;
    private String m_sUncompressedMimeType;
    private Charset m_aCharset;
    private EAS4CompressionMode m_eCompressionMode;
    private byte [] m_aPayloadBytes;
    private StandardBusinessDocument m_aSBDH;

    private ReadAttachment ()
    {}

    @Nullable
    public String getID ()
    {
      return m_sID;
    }

    @Nullable
    public String getMimeType ()
    {
      return m_sMimeType;
    }

    @Nullable
    public String getUncompressedMimeType ()
    {
      return m_sUncompressedMimeType;
    }

    @Nullable
    public Charset getCharset ()
    {
      return m_aCharset;
    }

    @Nullable
    public EAS4CompressionMode getCompressionMode ()
    {
      return m_eCompressionMode;
    }

    @Nonnull
    @ReturnsMutableObject
    public byte [] payloadBytes ()
    {
      return m_aPayloadBytes;
    }

    @Nonnull
    @ReturnsMutableObject
    public StandardBusinessDocument standardBusinessDocument ()
    {
      return m_aSBDH;
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4PeppolServletMessageProcessorSPI.class);

  private ICommonsList <IPhase4PeppolIncomingSBDHandlerSPI> m_aHandlers;

  public Phase4PeppolServletMessageProcessorSPI ()
  {
    m_aHandlers = ServiceLoaderHelper.getAllSPIImplementations (IPhase4PeppolIncomingSBDHandlerSPI.class);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IPhase4PeppolIncomingSBDHandlerSPI> getAllHandler ()
  {
    return m_aHandlers.getClone ();
  }

  public void setAllHandler (@Nonnull final Iterable <? extends IPhase4PeppolIncomingSBDHandlerSPI> aHandlers)
  {
    ValueEnforcer.notNull (aHandlers, "Handlers");
    m_aHandlers = new CommonsArrayList <> (aHandlers);
  }

  @Nonnull
  public AS4MessageProcessorResult processAS4UserMessage (@Nonnull final HttpHeaderMap aHttpHeaders,
                                                          @Nonnull final Ebms3UserMessage aUserMessage,
                                                          @Nonnull final IPMode aSrcPMode,
                                                          @Nullable final Node aPayload,
                                                          @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                          @Nonnull final IAS4MessageState aState,
                                                          @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Invoking processAS4UserMessage");

    final String sMessageID = aUserMessage.getMessageInfo ().getMessageId ();
    final String sService = aUserMessage.getCollaborationInfo ().getServiceValue ();
    final String sAction = aUserMessage.getCollaborationInfo ().getAction ();
    final String sConversationID = aUserMessage.getCollaborationInfo ().getConversationId ();

    // Debug log
    if (LOGGER.isDebugEnabled ())
    {
      if (aSrcPMode == null)
        LOGGER.debug ("  No Source PMode present");
      else
        LOGGER.debug ("  Source PMode = " + aSrcPMode.getID ());
      LOGGER.debug ("  Message ID = '" + sMessageID + "'");
      LOGGER.debug ("  Service = '" + sService + "'");
      LOGGER.debug ("  Action = '" + sAction + "'");
      LOGGER.debug ("  ConversationId = '" + sConversationID + "'");

      // Log source properties
      LOGGER.debug ("  MessageProperties:");
      for (final Ebms3Property p : aUserMessage.getMessageProperties ().getProperty ())
        LOGGER.debug ("    [" + p.getName () + "] = [" + p.getValue () + "]");

      if (aPayload == null)
        LOGGER.debug ("  No Payload present");
      else
        LOGGER.debug ("  Payload = " + XMLWriter.getNodeAsString (aPayload));
    }

    // Read all attachments
    final ICommonsList <ReadAttachment> aReadAttachments = new CommonsArrayList <> ();
    if (aIncomingAttachments != null)
    {
      int nAttachmentIndex = 0;
      for (final IAS4Attachment aIncomingAttachment : aIncomingAttachments)
      {
        final ReadAttachment a = new ReadAttachment ();
        a.m_sID = aIncomingAttachment.getId ();
        a.m_sMimeType = aIncomingAttachment.getMimeType ();
        a.m_sUncompressedMimeType = aIncomingAttachment.getUncompressedMimeType ();
        a.m_aCharset = aIncomingAttachment.getCharset ();
        a.m_eCompressionMode = aIncomingAttachment.getCompressionMode ();
        try (final InputStream aSIS = aIncomingAttachment.getSourceStream ())
        {
          final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
          if (StreamHelper.copyInputStreamToOutputStreamAndCloseOS (aSIS, aBAOS).isSuccess ())
          {
            a.m_aPayloadBytes = aBAOS.getBufferOrCopy ();
          }
        }
        catch (final IOException | AS4DecompressException ex)
        {
          // Fall through
        }
        if (a.m_aPayloadBytes == null)
        {
          final Ebms3Error aEbmsError = EEbmsError.EBMS_DECOMPRESSION_FAILURE.getAsEbms3Error (aState.getLocale (),
                                                                                               aState.getRefToMessageID ());
          aProcessingErrorMessages.add (aEbmsError);
          return AS4MessageProcessorResult.createFailure ("Processing errors occurred");
        }

        // Read data as SBDH
        a.m_aSBDH = SBDHReader.standardBusinessDocument ().read (a.m_aPayloadBytes);
        if (a.m_aSBDH == null)
        {
          final Ebms3Error aEbmsError = EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.getAsEbms3Error (aState.getLocale (),
                                                                                                aState.getRefToMessageID ());
          aProcessingErrorMessages.add (aEbmsError);
          return AS4MessageProcessorResult.createFailure ("Failed to interprete payload as SBDH");
        }

        aReadAttachments.add (a);

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Attachment " +
                        nAttachmentIndex +
                        " with ID [" +
                        a.m_sID +
                        "] uses [" +
                        a.m_sMimeType +
                        (a.m_sUncompressedMimeType == null ? null : " - uncompressed " + a.m_sUncompressedMimeType) +
                        "] and [" +
                        StringHelper.getToString (a.m_aCharset, "no charset") +
                        "] and length is " +
                        (a.m_aPayloadBytes == null ? "<error>" : Integer.toString (a.m_aPayloadBytes.length)) +
                        " bytes" +
                        (a.m_eCompressionMode == null ? "" : " of compressed payload"));
        nAttachmentIndex++;
      }
    }

    if (aReadAttachments.size () != 1)
    {
      // In Peppol there must be exactly one payload
      final Ebms3Error aEbmsError = EEbmsError.EBMS_OTHER.getAsEbms3Error (aState.getLocale (),
                                                                           aState.getRefToMessageID ());
      aProcessingErrorMessages.add (aEbmsError);
      return AS4MessageProcessorResult.createFailure ("In Peppol exactly one payload attachment is expected. This request has " +
                                                      aReadAttachments.size () +
                                                      " attachments");
    }

    final ReadAttachment aReadAttachment = aReadAttachments.getFirst ();
    if (m_aHandlers.isEmpty ())
      LOGGER.warn ("No handler is present - the message is unhandled and discarded");
    else
      for (final IPhase4PeppolIncomingSBDHandlerSPI aHandler : m_aHandlers)
      {
        try
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Invoking Peppol handler " + aHandler);
          aHandler.handleIncomingSBD (aHttpHeaders.getClone (),
                                      aReadAttachment.payloadBytes (),
                                      aReadAttachment.standardBusinessDocument ());
        }
        catch (final Exception ex)
        {
          LOGGER.error ("Error invoking Peppol handler " + aHandler, ex);
        }
      }

    return AS4MessageProcessorResult.createSuccess ();
  }

  @Nonnull
  public AS4SignalMessageProcessorResult processAS4SignalMessage (@Nonnull final HttpHeaderMap aHttpHeaders,
                                                                  @Nonnull final Ebms3SignalMessage aSignalMessage,
                                                                  @Nullable final IPMode aPMode,
                                                                  @Nonnull final IAS4MessageState aState,
                                                                  @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
  {
    LOGGER.error ("Invoking processAS4SignalMessage is not supported");
    throw new UnsupportedOperationException ();
  }
}
