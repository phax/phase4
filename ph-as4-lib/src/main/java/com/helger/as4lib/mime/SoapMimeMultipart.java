package com.helger.as4lib.mime;

import javax.annotation.Nonnull;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;

import com.helger.as4lib.soap.ESOAPVersion;

/**
 * Special {@link MimeMultipart} that modifies the Content-Type to add the
 * "type" parameter with the SOAP versions MIME type.
 *
 * @author Philip Helger
 */
public class SoapMimeMultipart extends MimeMultipart
{
  public SoapMimeMultipart (@Nonnull final ESOAPVersion eSOAPVersion) throws ParseException
  {
    super ("related");
    // type parameter is essential for Axis to work!
    final ContentType cType = new ContentType (contentType);
    cType.setParameter ("type", eSOAPVersion.getMimeType ().getAsString ());
    contentType = cType.toString ();
  }
}
