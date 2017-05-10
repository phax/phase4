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

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.servlet.spi.AS4MessageProcessorResult;
import com.helger.as4.servlet.spi.IAS4ServletMessageProcessorSPI;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.xml.serialize.write.XMLWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test implementation of {@link IAS4ServletMessageProcessorSPI}
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class MockMessageProcessorCheckingStreamsSPI implements IAS4ServletMessageProcessorSPI
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (MockMessageProcessorCheckingStreamsSPI.class);

  @Nonnull
  @SuppressFBWarnings ("DMI_INVOKING_TOSTRING_ON_ARRAY")
  public AS4MessageProcessorResult processAS4UserMessage (@Nonnull final Ebms3UserMessage aUserMessage,
                                                          @Nullable final Node aPayload,
                                                          @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments)
  {
    s_aLogger.info ("Received AS4 message:");
    if (false)
      s_aLogger.info ("  UserMessage: " + aUserMessage);
    if (false)
      s_aLogger.info ("  Payload: " + (aPayload == null ? "null" : XMLWriter.getNodeAsString (aPayload)));

    // To test returning with a failure works as intended
    if (aUserMessage.getCollaborationInfo ().getAction ().equals ("Failure"))
    {
      return AS4MessageProcessorResult.createFailure ("Failure");
    }

    if (aIncomingAttachments != null)
    {
      s_aLogger.info ("  Attachments: " + aIncomingAttachments.size ());
      for (final WSS4JAttachment x : aIncomingAttachments)
      {
        s_aLogger.info ("    Attachment Content Type: " + x.getMimeType ());
        if (x.getMimeType ().startsWith ("text") || x.getMimeType ().endsWith ("/xml"))
        {
          final InputStream aIS = x.getSourceStream ();
          s_aLogger.info ("    Attachment Stream Class: " + aIS.getClass ().getName ());
          s_aLogger.info ("    Attachment Content: " +
                          StreamHelper.getAllBytesAsString (x.getSourceStream (), x.getCharset ()));
        }
      }
    }
    return AS4MessageProcessorResult.createSuccess ();
  }

  @Nonnull
  public AS4MessageProcessorResult processAS4SignalMessage (@Nonnull final Ebms3SignalMessage aSignalMessage,
                                                            @Nonnull final IPMode aPmode)
  {
    return AS4MessageProcessorResult.createSuccess ();
  }
}
