package com.helger.as4server.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

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
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.write.XMLWriter;

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

    try
    {
      Document aSOAPDocument = null;
      ESOAPVersion eSOAPVersion = null;

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

        // PARSING MIME Message via Datasource and Request
        if (false)
          try
          {
            final MimeMultipart aMultipart = new MimeMultipart (new ServletMultipartDataSource (aHttpServletRequest));

            for (int i = 0; i < aMultipart.getCount (); i++)
            {
              final BodyPart aBodyPart = aMultipart.getBodyPart (i);

              if (aBodyPart.getContent () instanceof InputStream)
              {
                s_aLogger.info (StreamHelper.getAllBytesAsString ((InputStream) aBodyPart.getContent (),
                                                                  Charset.defaultCharset ()));
                s_aLogger.info ("Bodypart " + i);
                if (aBodyPart.getDataHandler () == null)
                {
                  s_aLogger.info ("should not be null expect for first bodypart " + i);
                }
                else
                {
                  s_aLogger.info ("Data Handler exists for multipart " + i);
                }
              }

            }
            final MimeMessage aMsg = new MimeMessage ((Session) null);
            aMsg.setContent (aMultipart);
            if (aMsg.getDataHandler () == null)
            {
              s_aLogger.info ("should not be null");
            }
            else
            {
              s_aLogger.info ("Data Handler exists for multipart");
            }

          }
          catch (final MessagingException e1)
          {
            e1.printStackTrace ();
          }

        // PARSING MIME Message via MultiPartStream

        final MultipartStream aMulti = new MultipartStream (aHttpServletRequest.getInputStream (),
                                                            sBoundary.getBytes (CCharset.CHARSET_ISO_8859_1_OBJ),
                                                            (MultipartProgressNotifier) null);
        int nIndex = 0;
        while (true)
        {
          final boolean bNextPart = nIndex == 0 ? aMulti.skipPreamble () : aMulti.readBoundary ();
          if (!bNextPart)
            break;
          s_aLogger.info ("Found part " + nIndex);
          final MultipartItemInputStream aItemIS2 = aMulti.createInputStream ();

          try
          {
            final MimeBodyPart p = new MimeBodyPart (aItemIS2);
            if (nIndex == 0)
            {
              // SOAP document
              // TODO handle
              final IMimeType aPlainPartMT = MimeTypeParser.parseMimeType (p.getContentType ())
                                                           .getCopyWithoutParameters ();
              if (aPlainPartMT.equals (ESOAPVersion.SOAP_11.getMimeType ()))
                eSOAPVersion = ESOAPVersion.SOAP_11;
              else
                if (aPlainPartMT.equals (ESOAPVersion.SOAP_12.getMimeType ()))
                  eSOAPVersion = ESOAPVersion.SOAP_12;
                else
                {
                  s_aLogger.error ("Got unsupported MimeBodyPart Content-Type '" + p.getContentType () + "'");
                  aHttpServletResponse.sendError (HttpServletResponse.SC_BAD_REQUEST);
                  return;
                }

              aSOAPDocument = DOMReader.readXMLDOM (p.getInputStream ());
            }
            else
            {
              // Attachment - ignore for now
            }
          }
          catch (final MessagingException e)
          {
            // TODO Auto-generated catch block
            e.printStackTrace ();
          }
          nIndex++;
        }
      }
      else
        if (aPlainMT.equals (ESOAPVersion.SOAP_11.getMimeType ()))
        {
          // SOAP 1.1
          eSOAPVersion = ESOAPVersion.SOAP_11;
          aSOAPDocument = DOMReader.readXMLDOM (aHttpServletRequest.getInputStream ());
        }
        else
          if (aPlainMT.equals (ESOAPVersion.SOAP_12.getMimeType ()))
          {
            // SOAP 1.2
            eSOAPVersion = ESOAPVersion.SOAP_12;
            aSOAPDocument = DOMReader.readXMLDOM (aHttpServletRequest.getInputStream ());
          }
          else
          {
            s_aLogger.error ("Got unsupported Content-Type '" + aHttpServletRequest.getContentType () + "'");
            aHttpServletResponse.sendError (HttpServletResponse.SC_BAD_REQUEST);
            return;
          }

      if (aSOAPDocument == null)
      {
        s_aLogger.error ("Failed to parse " + eSOAPVersion + " document!");
        aHttpServletResponse.sendError (HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

      System.out.println (XMLWriter.getXMLString (aSOAPDocument));

      new UnifiedResponse (aHttpServletRequest).setContentAndCharset ("<h1> hi </h1>\n" +
                                                                      "Content-Type: " +
                                                                      aHttpServletRequest.getContentType (),
                                                                      CCharset.CHARSET_UTF_8_OBJ)
                                               .setMimeType (CMimeType.TEXT_HTML)
                                               .disableCaching ()
                                               .applyToResponse (aHttpServletResponse);
    }
    catch (final Throwable t)
    {
      throw new ServletException ("Internal error", t);
    }
  }

  @Override
  public void doGet (@Nonnull final HttpServletRequest aHttpServletRequest,
                     @Nonnull final HttpServletResponse aHttpServletResponse) throws ServletException, IOException
  {
    doPost (aHttpServletRequest, aHttpServletResponse);
  }
}
