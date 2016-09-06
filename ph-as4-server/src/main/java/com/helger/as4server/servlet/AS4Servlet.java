package com.helger.as4server.servlet;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;
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

import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.message.AS4ReceiptMessage;
import com.helger.as4lib.message.CreateReceiptMessage;
import com.helger.as4lib.model.pmode.DefaultPMode;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.model.pmode.PModeLeg;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.as4server.receive.AS4MessageState;
import com.helger.as4server.receive.soap.ISOAPHeaderElementProcessor;
import com.helger.as4server.receive.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.commons.charset.CCharset;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.errorlist.IErrorBase;
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

public class AS4Servlet extends HttpServlet
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4Servlet.class);
  private static final IMimeType MT_MULTIPART_RELATED = EMimeContentType.MULTIPART.buildMimeType ("related");

  // TODO Replace with PMode Manager
  private final PMode aTestPMode = DefaultPMode.getDefaultPmode ();

  private void _handleSOAPMessage (@Nonnull final Document aSOAPDocument,
                                   @Nonnull final ESOAPVersion eSOAPVersion,
                                   @Nonnull final AS4Response aUR) throws Exception
  {
    // Find SOAP header
    final Node aHeaderNode = XMLHelper.getFirstChildElementOfName (aSOAPDocument.getDocumentElement (),
                                                                   eSOAPVersion.getNamespaceURI (),
                                                                   eSOAPVersion.getHeaderElementName ());
    if (aHeaderNode == null)
    {
      aUR.setBadRequest ("SOAP document is missing a Header element");
      return;
    }

    // Extract all header elements
    final ICommonsList <AS4SOAPHeader> aHeaders = new CommonsArrayList<> ();
    for (final Element aHeaderChild : new ChildElementIterator (aHeaderNode))
    {
      final QName aQName = AS4XMLHelper.getQName (aHeaderChild);
      final String sMustUnderstand = aHeaderChild.getAttributeNS (eSOAPVersion.getNamespaceURI (), "mustUnderstand");
      final boolean bIsMustUnderstand = eSOAPVersion.getMustUnderstandValue (true).equals (sMustUnderstand);
      aHeaders.add (new AS4SOAPHeader (aHeaderChild, aQName, bIsMustUnderstand));
    }

    final AS4MessageState aState = new AS4MessageState (eSOAPVersion);
    // handle all headers in the order of the registered handlers!
    for (final Map.Entry <QName, ISOAPHeaderElementProcessor> aEntry : SOAPHeaderElementProcessorRegistry.getAllElementProcessors ()
                                                                                                         .entrySet ())
    {
      final QName aQName = aEntry.getKey ();
      final AS4SOAPHeader aHeader = aHeaders.findFirst (x -> aQName.equals (x.getQName ()));
      if (aHeader != null)
      {
        final ISOAPHeaderElementProcessor aProcessor = aEntry.getValue ();
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Processing SOAP header element " + aQName.toString () + " with processor " + aProcessor);

        // Process element
        final ICommonsList <IErrorBase <?>> aErrorList = new CommonsArrayList<> ();
        if (aProcessor.processHeaderElement (aHeader.getNode (), aState, aErrorList).isSuccess ())
          aHeader.setProcessed (true);
        else
        {
          // upon failure, the element stays unprocessed
          s_aLogger.warn ("Failed to process SOAP header element " +
                          aQName.toString () +
                          " with processor " +
                          aProcessor);
        }
      }
      // else: no header element for current processor
    }

    // Now check if all must understand headers were processed
    for (final AS4SOAPHeader aHeader : aHeaders)
      if (aHeader.isMustUnderstand () && !aHeader.isProcessed ())
      {
        aUR.setBadRequest ("Error processing required SOAP header element " + aHeader.getQName ().toString ());
        return;
      }

    final Ebms3Messaging aMessaging = aState.getMessaging ();
    if (aMessaging == null)
    {
      aUR.setBadRequest ("No Ebms3 Messaging header was found");
      return;
    }

    // PMode Check
    if (!checkIfPModeConform (aMessaging, eSOAPVersion, aUR))
      return;

    // Every message should only contain 1 UserMessage and n (0..n)
    // SignalMessages
    if (aMessaging.getUserMessageCount () != 1)
    {
      aUR.setBadRequest ("Unexpected number of Ebms3 UserMessages found: " + aMessaging.getUserMessageCount ());
      return;
    }

    // TODO wss security signing check is missing!

    final Ebms3UserMessage aEbms3UserMessage = aMessaging.getUserMessageAtIndex (0);
    final CreateReceiptMessage aReceiptMessage = new CreateReceiptMessage ();
    // TODO Change to dynamic Message ID
    final Ebms3MessageInfo aEbms3MessageInfo = aReceiptMessage.createEbms3MessageInfo ("UUID-3@receiver.example.com",
                                                                                       null);
    final AS4ReceiptMessage aDoc = aReceiptMessage.createReceiptMessage (eSOAPVersion,
                                                                         aEbms3MessageInfo,
                                                                         aEbms3UserMessage,
                                                                         null)
                                                  .setMustUnderstand (false);

    // We've got our response
    aUR.setContentAndCharset (AS4XMLHelper.serializeXML (aDoc.getAsSOAPDocument ()), CCharset.CHARSET_UTF_8_OBJ)
       .setMimeType (eSOAPVersion.getMimeType ());
  }

  private boolean checkIfPModeConform (@Nonnull final Ebms3Messaging aMessaging,
                                       @Nonnull final ESOAPVersion eSOAPVersion,
                                       @Nonnull final AS4Response aUR)
  {
    final Ebms3UserMessage aEbms3UserMessage = aMessaging.getUserMessageAtIndex (0);

    // TODO Go through PModeManager to check for PModes | if PModeManger
    // contains aMessage.Pmode
    if (!aEbms3UserMessage.getCollaborationInfo ().getAgreementRef ().getPmode ().equals (aTestPMode.getID ()))
    {
      aUR.setBadRequest ("Error processing the PMode " +
                         aMessaging.getUserMessageAtIndex (0).getCollaborationInfo ().getAgreementRef ().getPmode () +
                         " can not be found in the PMode - Manager.");
      return false;
    }

    // Check if pmode contains a protocol and if the message complies
    final PModeLeg aPModeLeg = aTestPMode.getLeg1 ();
    if (aPModeLeg == null)
    {
      aUR.setBadRequest ("PMode is missing Leg 1");
      return false;
    }
    if (aPModeLeg.getProtocol () == null)
    {
      aUR.setBadRequest ("PMode Leg 1 is missing protocol section");
      return false;
    }
    final ESOAPVersion ePModeSoapVersion = aPModeLeg.getProtocol ().getSOAPVersion ();
    if (!eSOAPVersion.equals (ePModeSoapVersion))
    {
      aUR.setBadRequest ("Error processing the PMode, the SOAP - Version (" + ePModeSoapVersion + ") is incorrect.");
      return false;
    }

    // Check if PartyID is correct
    if (!aEbms3UserMessage.getPartyInfo ().getFrom ().getPartyId ().contains (aTestPMode.getInitiator ().getIDValue ()))
    {
      aUR.setBadRequest ("Error processing the PMode, the Initiator/Sender PartyID is incorrect.");
      return false;
    }
    if (!aEbms3UserMessage.getPartyInfo ().getTo ().getPartyId ().contains (aTestPMode.getResponder ().getIDValue ()))
    {
      aUR.setBadRequest ("Error processing the PMode, the Responder PartyID is incorrect.");
      return false;
    }

    // WSS Security checks TODO check with EWSSVersion
    // TODO CHECK algorithms

    return true;
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
