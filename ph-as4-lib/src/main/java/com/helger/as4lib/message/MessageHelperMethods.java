package com.helger.as4lib.message;

import java.io.IOException;
import java.util.Enumeration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpMessage;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.datetime.util.PDTXMLConverter;

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

  public static Document getSoapEnvelope11ForTest (@Nonnull final String sPath) throws SAXException,
                                                                                IOException,
                                                                                ParserConfigurationException
  {
    final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ();
    domFactory.setNamespaceAware (true); // never forget this!
    final DocumentBuilder builder = domFactory.newDocumentBuilder ();
    return builder.parse (new ClassPathResource (sPath).getInputStream ());
  }

  // TODO Change Timestamp or do we only want the present date when the message
  // gets sent/replied
  public static Ebms3MessageInfo createEbms3MessageInfo (@Nonnull final String sMessageId,
                                                         @Nullable final String sRefToMessageID)
  {
    final Ebms3MessageInfo aMessageInfo = new Ebms3MessageInfo ();
    aMessageInfo.setMessageId (sMessageId);
    aMessageInfo.setTimestamp (PDTXMLConverter.getXMLCalendarNow ());
    aMessageInfo.setRefToMessageId (sRefToMessageID);
    return aMessageInfo;
  }

  public static void moveMIMEHeadersToHTTPHeader (@Nonnull final MimeMessage aMimeMsg,
                                                  @Nonnull final HttpMessage aHttpMsg) throws MessagingException
  {
    // Move all global mime headers to the POST request
    final Enumeration <?> e = aMimeMsg.getAllHeaders ();
    while (e.hasMoreElements ())
    {
      final Header h = (Header) e.nextElement ();
      aHttpMsg.addHeader (h.getName (), h.getValue ());
      aMimeMsg.removeHeader (h.getName ());
    }
  }
}
