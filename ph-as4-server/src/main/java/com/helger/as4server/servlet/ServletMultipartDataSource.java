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