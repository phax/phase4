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

import javax.annotation.Nonnull;

import com.helger.phase4.model.ESoapVersion;

import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.ParseException;

/**
 * Special {@link MimeMultipart} that modifies the Content-Type to add the
 * "type" parameter with the SOAP versions MIME type.<br>
 * Old name before v3: <code>AS4SoapMimeMultipart</code>.
 *
 * @author Philip Helger
 */
public class AS4SoapMimeMultipart extends MimeMultipart
{
  private static final String RELATED = "related";
  private static final String CT_PARAM_TYPE = "type";

  public AS4SoapMimeMultipart (@Nonnull final ESoapVersion eSoapVersion) throws ParseException
  {
    super (RELATED);

    // type parameter is essential for Axis to work!
    // But no charset! RFC 2387, section 3.4 has a special definition
    final ContentType aContentType = new ContentType (contentType);
    aContentType.setParameter (CT_PARAM_TYPE, eSoapVersion.getMimeType ().getAsString ());
    // No "charset" parameter here (see #263)
    contentType = aContentType.toString ();
  }
}
