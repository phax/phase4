package com.helger.as4lib.message;

import java.util.Enumeration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.http.HttpMessage;

import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.commons.ValueEnforcer;
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
   *        The message ID. Should never be <code>null</code> for production
   *        message but <code>null</code> is allowed for testing purposes.
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
    aMessageInfo.setMessageId (sMessageId);
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
