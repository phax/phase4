package com.helger.as4server.servlet;

import java.io.IOException;

import javax.activation.DataHandler;
import javax.annotation.Nonnull;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.charset.CCharset;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.string.StringHelper;
import com.helger.web.multipart.MultipartProgressNotifier;
import com.helger.web.multipart.MultipartStream;
import com.helger.web.multipart.MultipartStream.MultipartItemInputStream;
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
      final String sBoundary = aMT.getParameterValueWithName ("boundary");
      if (StringHelper.hasNoText (sBoundary))
      {
        s_aLogger.error ("Content-Type '" + aHttpServletRequest.getContentType () + "' misses boundary");
        aHttpServletResponse.sendError (HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

      s_aLogger.info ("Boundary = " + sBoundary);

      try
      {
        final MimeMessage aMsg = new MimeMessage (null, aHttpServletRequest.getInputStream ());
        aMsg.writeTo (System.out);

        // WHY string shouldnt it be multipart since attachment gets sent
        s_aLogger.info (aMsg.getContent ().getClass ().getName ());
        if (aMsg.getContent () instanceof MimeMultipart)
        {

          final Multipart aMultipart = (Multipart) aMsg.getContent ();

          s_aLogger.info ("BodyPart - MultiPartCount: " + aMultipart.getCount ());

          for (int i = 0; i < aMultipart.getCount (); i++)
          {
            final BodyPart aBodyPart = aMultipart.getBodyPart (i);
            final String sDisposition = aBodyPart.getDisposition ();
            if (sDisposition != null && sDisposition.equalsIgnoreCase ("ATTACHMENT"))
            {
              final DataHandler handler = aBodyPart.getDataHandler ();
              s_aLogger.info ("file name : " + handler.getName ());
            }
          }
        }
      }
      catch (final MessagingException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace ();
      }

      final MultipartStream aMulti = new MultipartStream (aHttpServletRequest.getInputStream (),
                                                          sBoundary.getBytes (CCharset.CHARSET_ISO_8859_1_OBJ),
                                                          (MultipartProgressNotifier) null);
      int nIndex = 0;
      aMulti.skipPreamble ();
      s_aLogger.info ("Found part " + (nIndex++));
      final MultipartItemInputStream aItemIS = aMulti.createInputStream ();
      // StreamHelper.getAllBytes (aItemIS);
      System.out.println (StreamHelper.getAllBytesAsString (aItemIS, CCharset.CHARSET_ISO_8859_1_OBJ));

      while (true)
      {
        final boolean bNextPart = aMulti.readBoundary ();
        if (!bNextPart)
          break;
        s_aLogger.info ("Found part " + (nIndex++));
        final MultipartItemInputStream aItemIS2 = aMulti.createInputStream ();
        // StreamHelper.getAllBytes (aItemIS);
        System.out.println (StreamHelper.getAllBytesAsString (aItemIS2, CCharset.CHARSET_ISO_8859_1_OBJ));
      }
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
