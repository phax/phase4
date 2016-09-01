package com.helger.as4server.servlet;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.MimeBodyPart;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.message.AS4ReceiptMessage;
import com.helger.as4lib.message.AS4UserMessage;
import com.helger.as4lib.message.CreateReceiptMessage;
import com.helger.as4lib.message.CreateUserMessage;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.as4server.receive.AS4MessageState;
import com.helger.as4server.receive.soap.ISOAPHeaderElementProcessor;
import com.helger.as4server.receive.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.commons.charset.CCharset;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.string.StringHelper;
import com.helger.web.multipart.MultipartProgressNotifier;
import com.helger.web.multipart.MultipartStream;
import com.helger.web.multipart.MultipartStream.MultipartItemInputStream;
import com.helger.xml.ChildElementIterator;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.write.XMLWriter;

public class AS4Servlet extends HttpServlet
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4Servlet.class);
  private static final IMimeType MT_MULTIPART_RELATED = EMimeContentType.MULTIPART.buildMimeType ("related");

  public static Document testUserMessageSoapNotSigned (@Nonnull final ESOAPVersion eSOAPVersion,
                                                       @Nullable final Node aPayload,
                                                       @Nullable final Iterable <? extends IAS4Attachment> aAttachments)
  {
    final CreateUserMessage aUserMessage = new CreateUserMessage ();

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList<> ();
    final Ebms3Property aEbms3PropertyProcess = new Ebms3Property ();
    aEbms3PropertyProcess.setName ("ProcessInst");
    aEbms3PropertyProcess.setValue ("PurchaseOrder:123456");
    final Ebms3Property aEbms3PropertyContext = new Ebms3Property ();
    aEbms3PropertyContext.setName ("ContextID");
    aEbms3PropertyContext.setValue ("987654321");
    aEbms3Properties.add (aEbms3PropertyContext);
    aEbms3Properties.add (aEbms3PropertyProcess);

    final Ebms3MessageInfo aEbms3MessageInfo = aUserMessage.createEbms3MessageInfo ("UUID-2@receiver.example.com");
    final Ebms3PayloadInfo aEbms3PayloadInfo = aUserMessage.createEbms3PayloadInfo (aPayload, aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = aUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                      "MyServiceTypes",
                                                                                                      "QuoteToCollect",
                                                                                                      "4321",
                                                                                                      "pm-esens-generic-resp",
                                                                                                      "http://agreements.holodeckb2b.org/examples/agreement0");
    final Ebms3PartyInfo aEbms3PartyInfo = aUserMessage.createEbms3PartyInfo ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sender",
                                                                              "APP_1000000101",
                                                                              "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder",
                                                                              "APP_1000000101");
    final Ebms3MessageProperties aEbms3MessageProperties = aUserMessage.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = aUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                aEbms3PayloadInfo,
                                                                aEbms3CollaborationInfo,
                                                                aEbms3PartyInfo,
                                                                aEbms3MessageProperties,
                                                                eSOAPVersion)
                                            .setMustUnderstand (false);
    System.out.println (XMLWriter.getXMLString (aDoc.getAsSOAPDocument (aPayload)));
    return aDoc.getAsSOAPDocument (aPayload);
  }

  private void _handleSOAPMessage (@Nonnull final Document aSOAPDocument,
                                   @Nonnull final ESOAPVersion eSOAPVersion,
                                   @Nonnull final AS4Response aUR) throws Exception
  {
    // Find SOAP header
    final Node aHeader = XMLHelper.getFirstChildElementOfName (aSOAPDocument.getDocumentElement (), "Header");
    if (aHeader == null)
    {
      aUR.setBadRequest ("SOAP document is missing a Header element");
      return;
    }

    final AS4MessageState aState = new AS4MessageState (eSOAPVersion);
    for (final Element aHeaderChild : new ChildElementIterator (aHeader))
    {
      final QName aQName = AS4XMLHelper.getQName (aHeaderChild);
      final String sMustUnderstand = aHeaderChild.getAttributeNS (eSOAPVersion.getNamespaceURI (), "mustUnderstand");
      final boolean bIsMustUnderstand = eSOAPVersion.getMustUnderstandValue (true).equals (sMustUnderstand);

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Processing SOAP header element " +
                         aQName.toString () +
                         " with mustUnderstand=" +
                         bIsMustUnderstand);

      final ISOAPHeaderElementProcessor aProcessor = SOAPHeaderElementProcessorRegistry.getHeaderElementProcessor (aQName);
      if (aProcessor == null)
      {
        if (bIsMustUnderstand)
        {
          aUR.setBadRequest ("No handler for required SOAP header element " + aQName.toString () + " found");
          return;
        }
        aState.addUnhandledHeader (aQName);
      }
      else
      {
        if (aProcessor.processHeaderElement (aHeaderChild, aState).isFailure ())
        {
          if (bIsMustUnderstand)
          {
            aUR.setBadRequest ("Error processing required SOAP header element " + aQName.toString ());
            return;
          }
          aState.addFailedHeader (aQName);
        }
        else
        {
          // Handled
          aState.addHandledHeader (aQName);
        }
      }
    }

    final Ebms3Messaging aMessaging = aState.getMessaging ();
    if (aMessaging == null)
    {
      aUR.setBadRequest ("No Ebms3 Messaging header was found");
      return;
    }

    AS4Servlet.testUserMessageSoapNotSigned (eSOAPVersion, null, null);

    for (final Ebms3UserMessage aEbms3UserMessage : aMessaging.getUserMessage ())
    {
      final CreateReceiptMessage aReceiptMessage = new CreateReceiptMessage ();
      final Ebms3MessageInfo aEbms3MessageInfo = aReceiptMessage.createEbms3MessageInfo ("UUID-3@receiver.example.com",
                                                                                         null);
      final AS4ReceiptMessage aDoc = aReceiptMessage.createReceiptMessage (eSOAPVersion,
                                                                           aEbms3MessageInfo,
                                                                           aEbms3UserMessage,
                                                                           null)
                                                    .setMustUnderstand (false);

      System.out.println (AS4XMLHelper.serializeXML (aDoc.getAsSOAPDocument ()));
    }

    aUR.setContentAndCharset ("<h1>Got " + eSOAPVersion + "</h1>\n", CCharset.CHARSET_UTF_8_OBJ)
       .setMimeType (CMimeType.TEXT_HTML);
  }

  @Override
  protected void doPost (@Nonnull final HttpServletRequest aHttpServletRequest,
                         @Nonnull final HttpServletResponse aHttpServletResponse) throws ServletException, IOException
  {
    // Never cache the responses
    final AS4Response aUR = new AS4Response (aHttpServletRequest);

    try
    {
      // Determine content type
      final MimeType aMT = MimeTypeParser.parseMimeType (aHttpServletRequest.getContentType ());
      s_aLogger.info ("Content-Type: " + aMT);
      if (aMT == null)
      {
        aUR.setBadRequest ("Failed to parse Content-Type '" + aHttpServletRequest.getContentType () + "'");
      }
      else
      {
        Document aSOAPDocument = null;
        ESOAPVersion eSOAPVersion = null;

        final IMimeType aPlainMT = aMT.getCopyWithoutParameters ();
        if (aPlainMT.equals (MT_MULTIPART_RELATED))
        {
          // MIME message
          if (s_aLogger.isDebugEnabled ())
            s_aLogger.debug ("Received MIME message");

          final String sBoundary = aMT.getParameterValueWithName ("boundary");
          if (StringHelper.hasNoText (sBoundary))
          {
            aUR.setBadRequest ("Content-Type '" +
                               aHttpServletRequest.getContentType () +
                               "' misses boundary parameter");
          }
          else
          {
            if (s_aLogger.isDebugEnabled ())
              s_aLogger.debug ("MIME Boundary = " + sBoundary);

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

              final MimeBodyPart p = new MimeBodyPart (aItemIS2);
              if (nIndex == 0)
              {
                // SOAP document
                final IMimeType aPlainPartMT = MimeTypeParser.parseMimeType (p.getContentType ())
                                                             .getCopyWithoutParameters ();

                // Determine SOAP version from MIME part content type
                eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (),
                                                      x -> aPlainPartMT.equals (x.getMimeType ()));

                // Read SOAP document
                aSOAPDocument = DOMReader.readXMLDOM (p.getInputStream ());
              }
              else
              {
                // TODO MIME Attachment - ignore for now
              }
              nIndex++;
            }
          }
        }
        else
        {
          if (s_aLogger.isDebugEnabled ())
            s_aLogger.debug ("Received plain message with Content-Type " + aMT.getAsString ());

          // Expect plain SOAP - read whole request to DOM
          // Note: this may require a huge amount of memory for large requests
          aSOAPDocument = DOMReader.readXMLDOM (aHttpServletRequest.getInputStream ());

          // Determine SOAP version from content type
          eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (), x -> aPlainMT.equals (x.getMimeType ()));
        }

        if (aSOAPDocument == null)
        {
          aUR.setBadRequest ("Failed to parse " + eSOAPVersion + " document!");
        }
        else
        {
          if (eSOAPVersion == null)
          {
            // Determine from namespace URI of read document
            final String sNamespaceURI = XMLHelper.getNamespaceURI (aSOAPDocument);
            eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (),
                                                  x -> x.getNamespaceURI ().equals (sNamespaceURI));
          }

          if (eSOAPVersion == null)
          {
            aUR.setBadRequest ("Failed to determine SOAP version from XML document!");
          }
          else
          {
            // SOAP document and SOAP version are determined
            _handleSOAPMessage (aSOAPDocument, eSOAPVersion, aUR);
          }
        }
      }
    }
    catch (final Throwable t)
    {
      aUR.setResponseError (HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error processing AS4 request", t);
    }

    aUR.applyToResponse (aHttpServletResponse);
  }

  @Override
  public void doGet (@Nonnull final HttpServletRequest aHttpServletRequest,
                     @Nonnull final HttpServletResponse aHttpServletResponse) throws ServletException, IOException
  {
    // XXX debug only
    doPost (aHttpServletRequest, aHttpServletResponse);
  }
}
