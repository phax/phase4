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
package com.helger.as4.messaging.mime;

import java.nio.charset.Charset;

import javax.activation.CommandInfo;
import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.mime.CMimeType;
import com.helger.mail.cte.EContentTransferEncoding;

public final class MimeMessageCreator
{
  private static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";

  static
  {
    /**
     * Java 1.8.0_77 on Win7 64 Bit JDK contains as default:
     *
     * <pre>
    text/html; content-handler; com.sun.mail.handlers.text_html
    text/html; view; com.sun.activation.viewers.TextViewer
    text/html; edit; com.sun.activation.viewers.TextEditor
    message/rfc822; content-handler; com.sun.mail.handlers.message_rfc822
    multipart/*; content-handler; com.sun.mail.handlers.multipart_mixed
    text/xml; content-handler; com.sun.mail.handlers.text_xml
    text/xml; view; com.sun.activation.viewers.TextViewer
    text/xml; edit; com.sun.activation.viewers.TextEditor
    text/plain; content-handler; com.sun.mail.handlers.text_plain
    text/plain; view; com.sun.activation.viewers.TextViewer
    text/plain; edit; com.sun.activation.viewers.TextEditor
    text/*; view; com.sun.activation.viewers.TextViewer
    text/*; edit; com.sun.activation.viewers.TextEditor
    image/jpeg; view; com.sun.activation.viewers.ImageViewer
    image/gif; view; com.sun.activation.viewers.ImageViewer
     * </pre>
     */
    if (false)
    {
      final MailcapCommandMap aCommandMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap ();
      for (final String m : aCommandMap.getMimeTypes ())
        for (final CommandInfo i : aCommandMap.getAllCommands (m))
          System.out.println (m + "; " + i.getCommandName () + "; " + i.getCommandClass ());
    }

    {
      final MailcapCommandMap aCommandMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap ();
      aCommandMap.addMailcap (CMimeType.APPLICATION_SOAP_XML.getAsStringWithoutParameters () +
                              ";; x-java-content-handler=" +
                              DataContentHandlerSoap12.class.getName ());
      CommandMap.setDefaultCommandMap (aCommandMap);
    }
  }

  private final ESOAPVersion m_eSOAPVersion;

  public MimeMessageCreator (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
  }

  @Nonnull
  public MimeMessage generateMimeMessage (@Nonnull final Document aSOAPEnvelope,
                                          @Nullable final ICommonsList <WSS4JAttachment> aEncryptedAttachments) throws MessagingException
  {
    final Charset aCharset = AS4XMLHelper.XWS.getCharset ();
    final SoapMimeMultipart aMimeMultipart = new SoapMimeMultipart (m_eSOAPVersion, aCharset);
    final EContentTransferEncoding eCTE = EContentTransferEncoding.BINARY;
    final String sContentType = m_eSOAPVersion.getMimeType (aCharset).getAsString ();

    {
      // Message Itself
      final MimeBodyPart aMessagePart = new MimeBodyPart ();
      aMessagePart.setContent (new DOMSource (aSOAPEnvelope), sContentType);
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
