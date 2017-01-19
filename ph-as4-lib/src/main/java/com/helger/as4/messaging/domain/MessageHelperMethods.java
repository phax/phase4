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
package com.helger.as4.messaging.domain;

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
 * than one message creating classes in this package.
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
   * @param sMessageIDSuffix
   *        The message ID suffix. If present, it is appended to the generated
   *        UUID, otherwise just the UUID is used.
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static Ebms3MessageInfo createEbms3MessageInfo (@Nullable final String sMessageIDSuffix)
  {
    final Ebms3MessageInfo aMessageInfo = new Ebms3MessageInfo ();

    final UUID aUUID = UUID.randomUUID ();
    aMessageInfo.setMessageId (StringHelper.getConcatenatedOnDemand (aUUID.toString (), '@', sMessageIDSuffix));

    // TODO Change Timestamp or do we only want the present date when the
    // message gets sent/replied
    aMessageInfo.setTimestamp (PDTXMLConverter.getXMLCalendarNow ());
    return aMessageInfo;
  }

  public static void moveMIMEHeadersToHTTPHeader (@Nonnull final MimeMessage aMimeMsg,
                                                  @Nonnull final HttpMessage aHttpMsg) throws MessagingException
  {
    ValueEnforcer.notNull (aMimeMsg, "MimeMsg");
    ValueEnforcer.notNull (aHttpMsg, "HttpMsg");

    // Move all mime headers to the HTTP request
    final Enumeration <?> aEnum = aMimeMsg.getAllHeaders ();
    while (aEnum.hasMoreElements ())
    {
      final Header h = (Header) aEnum.nextElement ();
      // Make a single-line HTTP header value!
      aHttpMsg.addHeader (h.getName (), HTTPStringHelper.getUnifiedHTTPHeaderValue (h.getValue ()));

      // Remove from MIME message!
      aMimeMsg.removeHeader (h.getName ());
    }
  }
}
