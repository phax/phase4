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
package com.helger.phase4.messaging.http;

import javax.annotation.Nonnull;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.w3c.dom.Node;

import com.helger.commons.mime.IMimeType;
import com.helger.phase4.util.AS4XMLHelper;

/**
 * Special HttpClient HTTP POST entity that contains a DOM Node as a serialized
 * String. This entity is repeatable.
 *
 * @author Philip Helger
 */
public class HttpXMLEntity extends StringEntity
{
  public HttpXMLEntity (@Nonnull final Node aNode, @Nonnull final IMimeType aMimeType)
  {
    // ContentType Required for AS4.NET
    super (AS4XMLHelper.serializeXML (aNode),
           ContentType.parse (aMimeType.getAsString ()).withCharset (AS4XMLHelper.XWS.getCharset ()));
  }
}
