/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.incoming;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;

import jakarta.servlet.http.Cookie;

/**
 * Messaging helper methods.<br>
 * Old name before v3: <code>AS4MessagingHelper</code>
 *
 * @author Philip Helger
 * @since 0.9.10
 */
@Immutable
public final class AS4IncomingHelper
{
  private AS4IncomingHelper ()
  {}

  /**
   * Convert an {@link IAS4IncomingMessageMetadata} structure to a JSON representation.
   *
   * @param aMessageMetadata
   *        The message metadata to convert. May not be <code>null</code>.
   * @return A non-<code>null</code> JSON object.
   */
  @NonNull
  @Nonempty
  public static IJsonObject getIncomingMetadataAsJson (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata)
  {
    final IJsonObject aMap = new JsonObject ();
    aMap.add ("IncomingUniqueID", aMessageMetadata.getIncomingUniqueID ());
    aMap.add ("IncomingDT", PDTWebDateHelper.getAsStringXSD (aMessageMetadata.getIncomingDT ()));
    aMap.add ("Mode", aMessageMetadata.getMode ().getID ());
    if (aMessageMetadata.hasRemoteAddr ())
      aMap.add ("RemoteAddr", aMessageMetadata.getRemoteAddr ());
    if (aMessageMetadata.hasRemoteHost ())
      aMap.add ("RemoteHost", aMessageMetadata.getRemoteHost ());
    if (aMessageMetadata.hasRemotePort ())
      aMap.add ("RemotePort", aMessageMetadata.getRemotePort ());
    if (aMessageMetadata.hasRemoteUser ())
      aMap.add ("RemoteUser", aMessageMetadata.getRemoteUser ());
    {
      final IJsonArray aArray = new JsonArray ();
      for (final Cookie aCookie : aMessageMetadata.cookies ())
      {
        final IJsonObject aCookieObj = new JsonObject ();
        if (StringHelper.isNotEmpty (aCookie.getDomain ()))
          aCookieObj.add ("Domain", aCookie.getDomain ());
        if (StringHelper.isNotEmpty (aCookie.getPath ()))
          aCookieObj.add ("Path", aCookie.getPath ());
        aCookieObj.add ("Secure", aCookie.getSecure ());
        aCookieObj.add ("HttpOnly", aCookie.isHttpOnly ());
        aCookieObj.add ("Name", aCookie.getName ());
        aCookieObj.add ("Value", aCookie.getValue ());
        aCookieObj.add ("MaxAge", aCookie.getMaxAge ());
        aArray.add (aCookieObj);
      }
      if (aArray.isNotEmpty ())
        aMap.add ("Cookies", aArray);
    }
    {
      final IJsonArray aArray = new JsonArray ();
      for (final var aHeader : aMessageMetadata.getAllHttpHeaders ())
      {
        final ICommonsList <String> aHeaderValues = aHeader.getValue ();

        final IJsonObject aObj = new JsonObject ();
        if (aHeaderValues.size () != 1)
          aObj.add (aHeader.getKey (), new JsonArray ().addAll (aHeaderValues));
        else
          aObj.add (aHeader.getKey (), aHeaderValues.getFirstOrNull ());
        aArray.add (aObj);
      }
      if (aArray.isNotEmpty ())
        aMap.add ("HttpHeaders", aArray);
    }
    return aMap;
  }
}
