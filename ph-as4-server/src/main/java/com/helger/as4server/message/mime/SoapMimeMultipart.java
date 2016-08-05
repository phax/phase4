package com.helger.as4server.message.mime;

import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;

public final class SoapMimeMultipart extends MimeMultipart
{
  public SoapMimeMultipart () throws ParseException
  {
    super ("related");
    // type parameter is essential for Axis to work!
    final ContentType cType = new ContentType (contentType);
    cType.setParameter ("type", "application/soap+xml");
    contentType = cType.toString ();
  }
}