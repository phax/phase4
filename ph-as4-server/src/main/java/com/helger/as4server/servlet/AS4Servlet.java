package com.helger.as4server.servlet;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.charset.CCharset;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.web.servlet.response.UnifiedResponse;

public class AS4Servlet extends HttpServlet
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4Servlet.class);
  private static final IMimeType MT_MULTIPART_RELATED = EMimeContentType.MULTIPART.buildMimeType ("related");

  @Override
  protected void doPost (@Nonnull final HttpServletRequest aHttpServletRequest,
                         @Nonnull final HttpServletResponse aHttpServletResponse) throws ServletException, IOException
  {
    // Determine content type
    final MimeType aMT = MimeTypeParser.parseMimeType (aHttpServletRequest.getContentType ());
    if (aMT == null)
    {
      s_aLogger.error ("Failed to parse Content-Type '" + aHttpServletRequest.getContentType () + "'");
      aHttpServletResponse.sendError (HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    final IMimeType aPlainMT = aMT.getCopyWithoutParameters ();
    if (aPlainMT.equals (MT_MULTIPART_RELATED))
    {
      // MIME message
    }
    else
      if (aPlainMT.equals (ESOAPVersion.SOAP_11.getMimeType ()))
      {
        // SOAP 1.1
      }
      else
        if (aPlainMT.equals (ESOAPVersion.SOAP_12.getMimeType ()))
        {
          // SOAP 1.2
        }
        else
        {
          s_aLogger.error ("Got unsupported Content-Type '" + aHttpServletRequest.getContentType () + "'");
          aHttpServletResponse.sendError (HttpServletResponse.SC_BAD_REQUEST);
          return;
        }

    new UnifiedResponse (aHttpServletRequest).setContentAndCharset ("<h1> hi </h1>\n" +
                                                                    "Content-Type: " +
                                                                    aHttpServletRequest.getContentType (),
                                                                    CCharset.CHARSET_UTF_8_OBJ)
                                             .setMimeType (CMimeType.TEXT_HTML)
                                             .disableCaching ()
                                             .applyToResponse (aHttpServletResponse);
  }

  @Override
  public void doGet (@Nonnull final HttpServletRequest aHttpServletRequest,
                     @Nonnull final HttpServletResponse aHttpServletResponse) throws ServletException, IOException
  {
    doPost (aHttpServletRequest, aHttpServletResponse);
  }
}
