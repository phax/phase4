/*
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;

import com.helger.phase4.soap.ESoapVersion;

/**
 * Special {@link MimeMultipart} that modifies the Content-Type to add the
 * "type" parameter with the SOAP versions MIME type.
 *
 * @author Philip Helger
 */
public class SoapMimeMultipart extends MimeMultipart
{
  public SoapMimeMultipart (@Nonnull final ESoapVersion eSoapVersion, @Nonnull final Charset aCharset) throws ParseException
  {
    super ("related");

    // type parameter is essential for Axis to work!
    // But no charset! RFC 2387, section 3.4 has a special definition
    final ContentType aContentType = new ContentType (contentType);
    aContentType.setParameter ("type", eSoapVersion.getMimeType ().getAsString ());
    aContentType.setParameter ("charset", aCharset.name ());
    contentType = aContentType.toString ();
  }
}
