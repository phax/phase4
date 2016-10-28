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
package com.helger.as4lib.mime;

import javax.activation.DataHandler;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.w3c.dom.Document;

import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.charset.CCharset;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.mime.CMimeType;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.mail.datasource.InputStreamDataSource;

public final class MimeMessageCreator
{
  private static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";

  private final ESOAPVersion m_eSOAPVersion;

  public MimeMessageCreator (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
  }

  @Nonnull
  public MimeMessage generateMimeMessage (@Nonnull final Document aSOAPEnvelope,
                                          @Nullable final Iterable <? extends IAS4Attachment> aAttachments,
                                          @Nullable final ICommonsList <Attachment> aEncryptedAttachments) throws Exception
  {
    final SoapMimeMultipart aMimeMultipart = new SoapMimeMultipart (m_eSOAPVersion);
    final EContentTransferEncoding eCTE = EContentTransferEncoding.BINARY;

    {
      // Message Itself
      final MimeBodyPart aMessagePart = new MimeBodyPart ();
      final String aDoc = AS4XMLHelper.serializeXML (aSOAPEnvelope);
      aMessagePart.setContent (aDoc, m_eSOAPVersion.getMimeType (CCharset.CHARSET_UTF_8_OBJ).getAsString ());
      aMessagePart.setHeader (CONTENT_TRANSFER_ENCODING, eCTE.getID ());
      aMimeMultipart.addBodyPart (aMessagePart);
    }

    // Add all attachments (if any)
    if (aAttachments != null)
      for (final IAS4Attachment aAttachment : aAttachments)
        aAttachment.addToMimeMultipart (aMimeMultipart);

    if (aEncryptedAttachments != null)
      for (final Attachment aEncryptedAttachment : aEncryptedAttachments)
      {
        final MimeBodyPart aMimeBodyPart = new MimeBodyPart ();
        // Important: don't add the other attachment headers to the mime body
        // part, otherwise decryption is likely to fail!

        // Content-ID is required
        aMimeBodyPart.setHeader (AttachmentUtils.MIME_HEADER_CONTENT_ID, aEncryptedAttachment.getId ());
        // Use application/octet-stream manually
        aMimeBodyPart.setHeader (AttachmentUtils.MIME_HEADER_CONTENT_TYPE,
                                 CMimeType.APPLICATION_OCTET_STREAM.getAsString ());
        aMimeBodyPart.setDataHandler (new DataHandler (new InputStreamDataSource (aEncryptedAttachment.getSourceStream (),
                                                                                  aEncryptedAttachment.getId ()).getEncodingAware (eCTE)));
        aMimeMultipart.addBodyPart (aMimeBodyPart);
      }

    // Build main message
    final MimeMessage aMsg = new MimeMessage ((Session) null);
    aMsg.setContent (aMimeMultipart);
    aMsg.saveChanges ();
    return aMsg;
  }
}
