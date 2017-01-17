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
package com.helger.as4server.spi;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.helger.as4lib.attachment.WSS4JAttachment;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.xml.serialize.write.XMLWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test implementation of {@link IAS4ServletMessageProcessorSPI}
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class MockMessageProcessorSPI implements IAS4ServletMessageProcessorSPI
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (MockMessageProcessorSPI.class);

  @Nonnull
  @SuppressFBWarnings ("DMI_INVOKING_TOSTRING_ON_ARRAY")
  public AS4MessageProcessorResult processAS4Message (@Nullable final Ebms3UserMessage aUserMessage,
                                                      @Nullable final Node aPayload,
                                                      @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments)
  {
    s_aLogger.info ("Received AS4 message:");
    s_aLogger.info ("  UserMessage: " + aUserMessage);
    s_aLogger.info ("  Payload: " + (aPayload == null ? "null" : XMLWriter.getXMLString (aPayload)));
    if (aIncomingAttachments != null)
    {
      s_aLogger.info ("  Attachments: " + aIncomingAttachments.size ());
      for (final WSS4JAttachment x : aIncomingAttachments)
      {
        s_aLogger.info ("    Attachment Content Type: " + x.getMimeType ());
        if (x.getMimeType ().startsWith ("text"))
          s_aLogger.info ("    Attachment: " +
                          StreamHelper.getAllBytesAsString (x.getSourceStream (), StandardCharsets.ISO_8859_1));
      }
    }
    return new AS4MessageProcessorResult (ESuccess.SUCCESS);
  }
}
