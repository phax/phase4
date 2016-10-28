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
package com.helger.as4lib.message;

import java.util.Enumeration;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.http.HttpMessage;

import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.StringHelper;
import com.helger.datetime.util.PDTXMLConverter;
import com.helger.http.HTTPStringHelper;

/**
 * This class contains every method, static variables which are used by more
 * than one message creating classes in the package
 * com.helger.as4server.message.
 *
 * @author bayerlma
 */
@Immutable
public final class MessageHelperMethods
{

  private MessageHelperMethods ()
  {}

  /**
   * Create a new message info.
   *
   * @param sMessageId
   *        The message ID. Can be <code>null</code> in this case just a UUID
   *        gets generated. Else the MessageId gets added to the UID
   * @param sRefToMessageID
   *        Reference to message ID. May be <code>null</code>. Must be present
   *        on receipt etc.
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static Ebms3MessageInfo createEbms3MessageInfo (@Nullable final String sMessageId,
                                                         @Nullable final String sRefToMessageID)
  {
    final Ebms3MessageInfo aMessageInfo = new Ebms3MessageInfo ();
    final UUID aUUID = UUID.randomUUID ();
    if (StringHelper.hasNoText (sMessageId))
      aMessageInfo.setMessageId (aUUID.toString ());
    else
      aMessageInfo.setMessageId (aUUID.toString () + "@" + sMessageId);
    // TODO Change Timestamp or do we only want the present date when the
    // message gets sent/replied
    aMessageInfo.setTimestamp (PDTXMLConverter.getXMLCalendarNow ());
    aMessageInfo.setRefToMessageId (sRefToMessageID);
    return aMessageInfo;
  }

  public static void moveMIMEHeadersToHTTPHeader (@Nonnull final MimeMessage aMimeMsg,
                                                  @Nonnull final HttpMessage aHttpMsg) throws MessagingException
  {
    ValueEnforcer.notNull (aMimeMsg, "MimeMsg");
    ValueEnforcer.notNull (aHttpMsg, "HttpMsg");

    // Move all global mime headers to the POST request
    final Enumeration <?> e = aMimeMsg.getAllHeaders ();
    while (e.hasMoreElements ())
    {
      final Header h = (Header) e.nextElement ();
      // Make a single-line HTTP header value!
      aHttpMsg.addHeader (h.getName (), HTTPStringHelper.getUnifiedHTTPHeaderValue (h.getValue ()));
      aMimeMsg.removeHeader (h.getName ());
    }
  }
}
