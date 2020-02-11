package com.helger.phase4.messaging;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.servlet.http.Cookie;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.string.StringHelper;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;

/**
 * Messaging helper methods.
 *
 * @author Philip Helger
 * @since 0.9.10
 */
@Immutable
public final class AS4MessagingHelper
{
  private AS4MessagingHelper ()
  {}

  @Nonnull
  @Nonempty
  public static IJsonObject getIncomingMetadataAsJson (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata)
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
        if (StringHelper.hasText (aCookie.getDomain ()))
          aCookieObj.add ("Domain", aCookie.getDomain ());
        if (StringHelper.hasText (aCookie.getPath ()))
          aCookieObj.add ("Path", aCookie.getPath ());
        aCookieObj.add ("Secure", aCookie.getSecure ());
        aCookieObj.add ("HttpOnly", aCookie.isHttpOnly ());
        aCookieObj.add ("Name", aCookie.getName ());
        aCookieObj.add ("Value", aCookie.getValue ());
        aCookieObj.add ("Version", aCookie.getVersion ());
        aCookieObj.add ("MaxAge", aCookie.getMaxAge ());
        if (StringHelper.hasText (aCookie.getComment ()))
          aCookieObj.add ("Comment", aCookie.getComment ());
        aArray.add (aCookieObj);
      }
      if (aArray.isNotEmpty ())
        aMap.add ("Cookies", aArray);
    }
    return aMap;
  }
}
