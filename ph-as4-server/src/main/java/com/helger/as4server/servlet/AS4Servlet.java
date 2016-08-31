package com.helger.as4server.servlet;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.marshaller.Ebms3ReaderBuilder;
import com.helger.as4lib.marshaller.Ebms3WriterBuilder;
import com.helger.as4lib.message.CreateReceiptMessage;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.commons.charset.CCharset;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.string.StringHelper;
import com.helger.jaxb.validation.CollectingValidationEventHandler;
import com.helger.web.multipart.MultipartProgressNotifier;
import com.helger.web.multipart.MultipartStream;
import com.helger.web.multipart.MultipartStream.MultipartItemInputStream;
import com.helger.web.servlet.response.UnifiedResponse;
import com.helger.xml.serialize.read.DOMReader;

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
    s_aLogger.info ("Content-Type: " + aMT);
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

      if (aMT.getContentType ().equals (CMimeType.TEXT_PLAIN.getContentType ()) &&
          aMT.getContentSubType ().equals (CMimeType.TEXT_PLAIN.getContentSubType ()))
      {
        handlePlainSoapMessage (eSOAPVersion, aHttpServletRequest);
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
              {
                // aSOAPDocument = DOMReader.readXMLDOM (p.getInputStream ());
                final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
                final Soap11Envelope aEnv = Ebms3ReaderBuilder.soap11 ()
                                                              .setValidationEventHandler (aCVEH)
                                                              .read (p.getInputStream ());
                final String sReRead = Ebms3WriterBuilder.soap11 ().getAsString (aEnv);
                s_aLogger.info ("Just to recheck what was read: " + sReRead);
                final Ebms3Messaging aMessage = Ebms3ReaderBuilder.ebms3Messaging ()
                                                                  .setValidationEventHandler (aCVEH)
                                                                  .read ((Element) aEnv.getHeader ().getAnyAtIndex (0));

              }

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

      // TODO System.out.println (XMLWriter.getXMLString (aSOAPDocument));

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

  private void handlePlainSoapMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                       final HttpServletRequest aHttpServletRequest) throws IOException
  {

    // aSOAPDocument = DOMReader.readXMLDOM (p.getInputStream ());
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Soap11Envelope aEnv = Ebms3ReaderBuilder.soap11 ()
                                                  .setValidationEventHandler (aCVEH)
                                                  .read (aHttpServletRequest.getInputStream ());

    final String sReRead = Ebms3WriterBuilder.soap11 ().getAsString (aEnv);
    s_aLogger.info ("Just to recheck what was read: " + sReRead);

    final Ebms3Messaging aMessage = Ebms3ReaderBuilder.ebms3Messaging ()
                                                      .setValidationEventHandler (aCVEH)
                                                      .read ((Element) aEnv.getHeader ().getAnyAtIndex (0));

    for (final Ebms3UserMessage aEbms3UserMessage : aMessage.getUserMessage ())
    {
      final CreateReceiptMessage aReceiptMessage = new CreateReceiptMessage ();
      final Ebms3MessageInfo aEbms3MessageInfo = aReceiptMessage.createEbms3MessageInfo ("UUID-2@receiver.example.com",
                                                                                         null);
      // final AS4ReceiptMessage aDoc = aReceiptMessage.createReceiptMessage
      // (eSOAPVersion,
      // aEbms3MessageInfo,
      // aEbms3UserMessage);
      //
      // return aDoc.getAsSOAPDocument (aPayload);
      //
    }

  }
}
