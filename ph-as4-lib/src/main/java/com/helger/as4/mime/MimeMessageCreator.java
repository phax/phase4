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
package com.helger.as4.mime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.w3c.dom.Document;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.xml.AS4XMLHelper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.charset.CCharset;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.mail.cte.EContentTransferEncoding;

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
                                          @Nullable final ICommonsList <WSS4JAttachment> aEncryptedAttachments) throws Exception
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

    if (aEncryptedAttachments != null)
      for (final WSS4JAttachment aEncryptedAttachment : aEncryptedAttachments)
      {
        aEncryptedAttachment.addToMimeMultipart (aMimeMultipart);
      }

    // Build main message
    final MimeMessage aMsg = new MimeMessage ((Session) null);
    aMsg.setContent (aMimeMultipart);
    aMsg.saveChanges ();

    return aMsg;
  }
}
