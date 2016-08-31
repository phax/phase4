package com.helger.as4server.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;

import javax.activation.DataSource;
import javax.servlet.ServletRequest;

public class ServletMultipartDataSource implements DataSource
{
  private final String contentType;
  private final InputStream inputStream;

  public ServletMultipartDataSource (final ServletRequest request) throws IOException
  {
    inputStream = new SequenceInputStream (new ByteArrayInputStream ("\n".getBytes ()), request.getInputStream ());
    contentType = request.getContentType ();
  }

  public InputStream getInputStream () throws IOException
  {
    return inputStream;
  }

  public OutputStream getOutputStream () throws IOException
  {
    return null;
  }

  public String getContentType ()
  {
    return contentType;
  }

  public String getName ()
  {
    return "ServletMultipartDataSource";
  }
}

// PARSING MIME Message via Datasource and Request
// if (false)
// try
// {
// final MimeMultipart aMultipart = new MimeMultipart (new
// ServletMultipartDataSource (aHttpServletRequest));
//
// for (int i = 0; i < aMultipart.getCount (); i++)
// {
// final BodyPart aBodyPart = aMultipart.getBodyPart (i);
//
// if (aBodyPart.getContent () instanceof InputStream)
// {
// s_aLogger.info (StreamHelper.getAllBytesAsString ((InputStream)
// aBodyPart.getContent (),
// Charset.defaultCharset ()));
// s_aLogger.info ("Bodypart " + i);
// if (aBodyPart.getDataHandler () == null)
// {
// s_aLogger.info ("should not be null expect for first bodypart " + i);
// }
// else
// {
// s_aLogger.info ("Data Handler exists for multipart " + i);
// }
// }
//
// }
// final MimeMessage aMsg = new MimeMessage ((Session) null);
// aMsg.setContent (aMultipart);
// if (aMsg.getDataHandler () == null)
// {
// s_aLogger.info ("should not be null");
// }
// else
// {
// s_aLogger.info ("Data Handler exists for multipart");
// }
//
// }
// catch (final MessagingException e1)
// {
// e1.printStackTrace ();
// }
