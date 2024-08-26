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
package com.helger.phase4.messaging.mime;

import java.nio.charset.Charset;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.transform.dom.DOMSource;

import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.util.AS4XMLHelper;

import jakarta.activation.CommandInfo;
import jakarta.activation.CommandMap;
import jakarta.activation.DataHandler;
import jakarta.activation.MailcapCommandMap;
import jakarta.mail.Header;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;

/**
 * Helper class for MIME message activities.<br>
 * Old name before v3: <code>MimeMessageCreator</code>
 *
 * @author Philip Helger
 */
public final class AS4MimeMessageHelper
{
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
      final StringBuilder aSB = new StringBuilder ();
      final MailcapCommandMap aCommandMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap ();
      for (final String sMimeType : aCommandMap.getMimeTypes ())
        for (final CommandInfo aCI : aCommandMap.getAllCommands (sMimeType))
          aSB.append (sMimeType)
             .append ("; ")
             .append (aCI.getCommandName ())
             .append ("; ")
             .append (aCI.getCommandClass ())
             .append ('\n');
      LoggerFactory.getLogger ("root").info (aSB.toString ());
    }

    // Register SOAP 1.2 content handler
    {
      final MailcapCommandMap aCommandMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap ();
      aCommandMap.addMailcap (ESoapVersion.SOAP_12.getMimeType ().getAsStringWithoutParameters () +
                              ";; x-java-content-handler=" +
                              DataContentHandlerSoap12.class.getName ());
      CommandMap.setDefaultCommandMap (aCommandMap);
    }
  }

  private AS4MimeMessageHelper ()
  {}

  @Nonnull
  public static AS4MimeMessage generateMimeMessage (@Nonnull final ESoapVersion eSoapVersion,
                                                    @Nonnull final Document aSoapEnvelope,
                                                    @Nullable final ICommonsList <WSS4JAttachment> aEncryptedAttachments) throws MessagingException
  {
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");
    ValueEnforcer.notNull (aSoapEnvelope, "SoapEnvelope");

    final Charset aCharset = AS4XMLHelper.XWS.getCharset ();
    final AS4SoapMimeMultipart aMimeMultipart = new AS4SoapMimeMultipart (eSoapVersion);
    final EContentTransferEncoding eCTE = EContentTransferEncoding.BINARY;
    final String sRootContentType = eSoapVersion.getMimeType (aCharset).getAsString ();

    {
      // Message Itself (repeatable)
      final MimeBodyPart aMessagePart = new MimeBodyPart ();
      aMessagePart.setDataHandler (new DataHandler (new DOMSource (aSoapEnvelope), sRootContentType));
      // Set AFTER DataHandler
      aMessagePart.setHeader (CHttpHeader.CONTENT_TRANSFER_ENCODING, eCTE.getID ());
      aMimeMultipart.addBodyPart (aMessagePart);
    }

    boolean bIsRepeatable = true;
    if (aEncryptedAttachments != null)
      for (final WSS4JAttachment aEncryptedAttachment : aEncryptedAttachments)
      {
        aEncryptedAttachment.addToMimeMultipart (aMimeMultipart);
        if (!aEncryptedAttachment.isRepeatable ())
          bIsRepeatable = false;
      }

    // Build main message
    final AS4MimeMessage aMsg = new AS4MimeMessage ((Session) null, bIsRepeatable);
    aMsg.setContent (aMimeMultipart);
    aMsg.saveChanges ();
    return aMsg;
  }

  /**
   * Take all headers from the MIME message and pass them to the provided
   * consumer. Afterwards remove all headers from the MIME message itself.
   *
   * @param aMimeMsg
   *        The message to use. May not be <code>null</code>.
   * @param aConsumer
   *        The consumer to be invoked. May not be <code>null</code>.
   * @param bUnifyValues
   *        <code>true</code> to unify the HTTP header values before passing
   *        them to the consumer.
   * @throws MessagingException
   *         In case of MIME message processing problems
   */
  public static void forEachHeaderAndRemoveAfterwards (@Nonnull final MimeMessage aMimeMsg,
                                                       @Nonnull final BiConsumer <String, String> aConsumer,
                                                       final boolean bUnifyValues) throws MessagingException
  {
    // Create a copy
    final ICommonsList <Header> aHeaders = CollectionHelper.newList (aMimeMsg.getAllHeaders ());

    // First round
    for (final Header aHeader : aHeaders)
    {
      // Make a single-line HTTP header value!
      aConsumer.accept (aHeader.getName (),
                        bUnifyValues ? HttpHeaderMap.getUnifiedValue (aHeader.getValue ()) : aHeader.getValue ());
    }

    // Remove all headers from MIME message
    // Do it after the copy loop, in case a header has more than one value!
    for (final Header aHeader : aHeaders)
      aMimeMsg.removeHeader (aHeader.getName ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public static HttpHeaderMap getAndRemoveAllHeaders (@Nonnull final MimeMessage aMimeMsg) throws MessagingException
  {
    final HttpHeaderMap ret = new HttpHeaderMap ();
    // Unification happens on the result header map
    forEachHeaderAndRemoveAfterwards (aMimeMsg, ret::addHeader, false);
    return ret;
  }
}
