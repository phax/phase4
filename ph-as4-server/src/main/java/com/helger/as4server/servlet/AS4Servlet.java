/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as4server.servlet;

import static org.junit.Assert.assertTrue;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.mail.internet.MimeBodyPart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.attachment.WSS4JAttachment;
import com.helger.as4lib.attachment.incoming.AS4IncomingWSS4JAttachment;
import com.helger.as4lib.attachment.incoming.IAS4IncomingAttachment;
import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.message.AS4ErrorMessage;
import com.helger.as4lib.message.AS4ReceiptMessage;
import com.helger.as4lib.message.CreateErrorMessage;
import com.helger.as4lib.message.CreateReceiptMessage;
import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.model.pmode.DefaultPMode;
import com.helger.as4lib.model.pmode.IPMode;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.model.pmode.PModeConfigManager;
import com.helger.as4lib.model.pmode.PModeParty;
import com.helger.as4lib.partner.Partner;
import com.helger.as4lib.partner.PartnerManager;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.util.AS4ResourceManager;
import com.helger.as4lib.util.IOHelper;
import com.helger.as4lib.util.StringMap;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.as4server.attachment.IIncomingAttachmentFactory;
import com.helger.as4server.mgr.MetaManager;
import com.helger.as4server.receive.AS4MessageState;
import com.helger.as4server.receive.soap.ISOAPHeaderElementProcessor;
import com.helger.as4server.receive.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.as4server.settings.AS4ServerSettings;
import com.helger.as4server.spi.AS4MessageProcessorResult;
import com.helger.as4server.spi.IAS4ServletMessageProcessorSPI;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.charset.CCharset;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.string.StringHelper;
import com.helger.http.EHTTPMethod;
import com.helger.http.EHTTPVersion;
import com.helger.photon.core.servlet.AbstractUnifiedResponseServlet;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.login.ELoginResult;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.web.multipart.MultipartProgressNotifier;
import com.helger.web.multipart.MultipartStream;
import com.helger.web.multipart.MultipartStream.MultipartItemInputStream;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.web.servlet.response.UnifiedResponse;
import com.helger.xml.ChildElementIterator;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.read.DOMReader;

public final class AS4Servlet extends AbstractUnifiedResponseServlet
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4Servlet.class);
  private static final IMimeType MT_MULTIPART_RELATED = EMimeContentType.MULTIPART.buildMimeType ("related");

  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static final ICommonsList <IAS4ServletMessageProcessorSPI> s_aProcessors = new CommonsArrayList<> ();

  // C1
  private String sOriginalSender = null;
  // C4
  private String sFinalRecipient = null;

  /**
   * Reload all SPI implementations of {@link IAS4ServletMessageProcessorSPI}.
   */
  public static void reinitProcessors ()
  {
    final ICommonsList <IAS4ServletMessageProcessorSPI> aProcessorSPIs = ServiceLoaderHelper.getAllSPIImplementations (IAS4ServletMessageProcessorSPI.class);
    if (aProcessorSPIs.isEmpty ())
      s_aLogger.warn ("No AS4 message processor is registered. All incoming messages will be discarded!");
    else
      s_aLogger.info ("Found " + aProcessorSPIs.size () + " AS4 message processors");

    s_aRWLock.writeLocked ( () -> s_aProcessors.setAll (aProcessorSPIs));
  }

  static
  {
    // Init once at the beginning
    reinitProcessors ();
  }

  /**
   * @return A list of all registered receiver handlers. Never <code>null</code>
   *         but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <IAS4ServletMessageProcessorSPI> getAllProcessors ()
  {
    return s_aRWLock.readLocked ( () -> s_aProcessors.getClone ());
  }

  public AS4Servlet ()
  {}

  private void _handleSOAPMessage (@Nonnull final Document aSOAPDocument,
                                   @Nonnull final ESOAPVersion eSOAPVersion,
                                   @Nonnull final ICommonsList <IAS4IncomingAttachment> aIncomingAttachments,
                                   @Nonnull final AS4Response aAS4Response,
                                   @Nonnull final Locale aLocale) throws Exception
  {
    ValueEnforcer.notNull (aSOAPDocument, "SOAPDocument");
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    ValueEnforcer.notNull (aIncomingAttachments, "IncomingAttachments");
    ValueEnforcer.notNull (aAS4Response, "AS4Response");
    ValueEnforcer.notNull (aLocale, "Locale");

    // TODO remove if or entire statement so much output
    if (GlobalDebug.isDebugMode () && false)
    {
      s_aLogger.info ("Received the following SOAP " + eSOAPVersion.getVersion () + " document:");
      s_aLogger.info (AS4XMLHelper.serializeXML (aSOAPDocument));
      s_aLogger.info ("Including the following attachments:");
      s_aLogger.info (aIncomingAttachments.toString ());
    }

    // Find SOAP header
    final Node aHeaderNode = XMLHelper.getFirstChildElementOfName (aSOAPDocument.getDocumentElement (),
                                                                   eSOAPVersion.getNamespaceURI (),
                                                                   eSOAPVersion.getHeaderElementName ());
    if (aHeaderNode == null)
    {
      aAS4Response.setBadRequest ("SOAP document is missing a Header element");
      return;
    }

    // Find SOAP body
    Node aBodyNode = XMLHelper.getFirstChildElementOfName (aSOAPDocument.getDocumentElement (),
                                                           eSOAPVersion.getNamespaceURI (),
                                                           eSOAPVersion.getBodyElementName ());
    if (aBodyNode == null)
    {
      aAS4Response.setBadRequest ("SOAP document is missing a Body element");
      return;
    }

    // Extract all header elements including their mustUnderstand value
    final ICommonsList <AS4SOAPHeader> aHeaders = new CommonsArrayList<> ();
    for (final Element aHeaderChild : new ChildElementIterator (aHeaderNode))
    {
      final QName aQName = XMLHelper.getQName (aHeaderChild);
      final String sMustUnderstand = aHeaderChild.getAttributeNS (eSOAPVersion.getNamespaceURI (), "mustUnderstand");
      final boolean bIsMustUnderstand = eSOAPVersion.getMustUnderstandValue (true).equals (sMustUnderstand);
      aHeaders.add (new AS4SOAPHeader (aHeaderChild, aQName, bIsMustUnderstand));
    }

    final ICommonsList <Ebms3Error> aErrorMessages = new CommonsArrayList<> ();

    try (final AS4ResourceManager aResMgr = new AS4ResourceManager ())
    {
      // Convert all attachments to WSS4J attachments
      // Need to check, since not every message will have attachments
      final ICommonsList <WSS4JAttachment> aWSS4JAttachments = new CommonsArrayList<> (aIncomingAttachments,
                                                                                       x -> x.getAsWSS4JAttachment (aResMgr));

      // This is where all data from the SOAP headers is stored to
      final AS4MessageState aState = new AS4MessageState (eSOAPVersion, aResMgr);

      // handle all headers in the order of the registered handlers!
      for (final Map.Entry <QName, ISOAPHeaderElementProcessor> aEntry : SOAPHeaderElementProcessorRegistry.getInstance ()
                                                                                                           .getAllElementProcessors ()
                                                                                                           .entrySet ())
      {
        final QName aQName = aEntry.getKey ();

        // Check if this message contains a header for the current handler
        final AS4SOAPHeader aHeader = aHeaders.findFirst (x -> aQName.equals (x.getQName ()));
        if (aHeader == null)
        {
          // no header element for current processor
          if (s_aLogger.isDebugEnabled ())
            s_aLogger.debug ("Message contains no SOAP header element with QName " + aQName.toString ());
          continue;
        }

        final ISOAPHeaderElementProcessor aProcessor = aEntry.getValue ();
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Processing SOAP header element " + aQName.toString () + " with processor " + aProcessor);

        // Process element
        final ErrorList aErrorList = new ErrorList ();
        if (aProcessor.processHeaderElement (aSOAPDocument,
                                             aHeader.getNode (),
                                             aWSS4JAttachments,
                                             aState,
                                             aErrorList,
                                             aLocale)
                      .isSuccess ())
        {
          // Mark header as processed (for mustUnderstand check)
          aHeader.setProcessed (true);
        }
        else
        {
          // upon failure, the element stays unprocessed and sends back a signal
          // message with the errors
          s_aLogger.warn ("Failed to process SOAP header element " +
                          aQName.toString () +
                          " with processor " +
                          aProcessor +
                          "; error details: " +
                          aErrorList);

          // TODO Use AS4 Esens profile if appropriate
          aErrorList.forEach (error -> {
            final EEbmsError ePredefinedError = EEbmsError.getFromErrorCodeOrNull (error.getErrorID ());
            if (ePredefinedError != null)
              aErrorMessages.add (ePredefinedError.getAsEbms3Error (aLocale));
            else
            {
              final Ebms3Error aEbms3Error = new Ebms3Error ();
              aEbms3Error.setErrorDetail (error.getErrorText (aLocale));
              aEbms3Error.setErrorCode (error.getErrorID ());
              aEbms3Error.setSeverity (error.getErrorLevel ().getID ());
              aEbms3Error.setOrigin (error.getErrorFieldName ());
              aErrorMessages.add (aEbms3Error);
            }
          });

          // Stop processing of other headers
          break;
        }
      }

      // Now check if all must understand headers were processed
      Ebms3Messaging aMessaging = null;
      if (aErrorMessages.isEmpty ())
      {
        for (final AS4SOAPHeader aHeader : aHeaders)
          if (aHeader.isMustUnderstand () && !aHeader.isProcessed ())
          {
            aAS4Response.setBadRequest ("Error processing required SOAP header element " +
                                        aHeader.getQName ().toString ());
            return;
          }

        aMessaging = aState.getMessaging ();
        if (aMessaging == null)
        {
          aAS4Response.setBadRequest ("No Ebms3 Messaging header was found");
          return;
        }

        // Every message should only contain 1 UserMessage and n (0..n)
        // SignalMessages
        if (aMessaging.getUserMessageCount () != 1)
        {
          aAS4Response.setBadRequest ("Unexpected number of Ebms3 UserMessages found: " +
                                      aMessaging.getUserMessageCount ());
          return;
        }
        final Ebms3UserMessage aUserMessage = aMessaging.getUserMessageAtIndex (0);

        // Decompressing the attachments
        final ICommonsList <IAS4IncomingAttachment> aDecryptedAttachments = new CommonsArrayList<> (aState.hasDecryptedAttachments () ? aState.getDecryptedAttachments ()
                                                                                                                                      : aState.getOriginalAttachments (),
                                                                                                    x -> new AS4IncomingWSS4JAttachment (x));

        // Decompress attachments (if compress)
        final IIncomingAttachmentFactory aIAF = MetaManager.getIncomingAttachmentFactory ();
        for (final IAS4IncomingAttachment aIncomingAttachment : aDecryptedAttachments.getClone ())
        {
          final EAS4CompressionMode eCompressionMode = aState.getAttachmentCompressionMode (aIncomingAttachment.getContentID ());
          if (eCompressionMode != null)
          {
            // Remove the old one
            aDecryptedAttachments.remove (aIncomingAttachment);

            // Add the new one with decompressing InputStream
            final IAS4IncomingAttachment aDecompressedAttachment = aIAF.createAttachment (eCompressionMode.getDecompressStream (aIncomingAttachment.getInputStream ()));
            aDecryptedAttachments.add (aDecompressedAttachment);
          }
        }

        // Do something with the message
        final Document aDecryptedSOAPDoc = aState.getDecryptedSOAPDocument ();
        if (aDecryptedSOAPDoc != null)
        {
          // Re-evaluate body node from decrypted SOAP document
          aBodyNode = XMLHelper.getFirstChildElementOfName (aDecryptedSOAPDoc.getDocumentElement (),
                                                            eSOAPVersion.getNamespaceURI (),
                                                            eSOAPVersion.getBodyElementName ());
          if (aBodyNode == null)
          {
            aAS4Response.setBadRequest ("Decrypted SOAP document is missing a Body element");
            return;
          }
        }
        final Node aPayloadNode = aBodyNode.getFirstChild ();

        // Check if originalSender and finalRecipient are present also saves
        // them into variables
        final String sCheckResult = _checkAndSaveProperties (aUserMessage.getMessageProperties ().getProperty ());
        if (!StringHelper.hasNoText (sCheckResult))
        {
          aAS4Response.setBadRequest (sCheckResult);
          return;
        }

        // Step 1 check if PModeConfig exists
        // TODO check with test might call exception when null here
        final String sConfigID = aState.getPModeConfig ().getID ();

        final PModeConfigManager aPModeConfigMgr = MetaAS4Manager.getPModeConfigMgr ();
        final PartnerManager aPartnerMgr = MetaAS4Manager.getPartnerMgr ();

        if (StringHelper.hasNoText (sConfigID))
        {
          // Use default PMode if no PModeConfig ID is present
          DefaultPMode.getDefaultPModeConfig ();
          // TODO make default pmode interchangeable
          // TODO Note to myself, how do you get until here (look at validation)
          // and give the following steps the default configuration

        }
        else
          if (aPModeConfigMgr.containsWithID (sConfigID))
          {
            if (aState.getResponderID () == null)
            {
              s_aLogger.info ("Default Responder used");
              aState.setResponderID (AS4ServerSettings.getDefaultResponderID ());
            }

            if (aState.getInitiatorID () == null)
            {
              aAS4Response.setBadRequest ("No Initiator specifed, currently mandatory since only one way supported");
              return;
            }

            if (aPartnerMgr.containsWithID (aState.getInitiatorID ()) &&
                aPartnerMgr.containsWithID (aState.getResponderID ()))
            {
              // Step 2: Check if P+P already exists, P+P should be C1-C4 but
              // initiator and responder id itself are C2 and C3
              _createPModeIfNotPresent (aState, sConfigID, aUserMessage);
            }
            else
            {
              // TODO needs null checks maybe for the ids
              // if (!aPartnerMgr.containsWithID (aState.getInitiatorID ()) &&
              // !aPartnerMgr.containsWithID (aState.getResponderID ()))
              // {
              // _createOrUpdatePartner (aState.getUsedCertificate (),
              // aState.getInitiatorID ());
              // }
              // else
              if (!aPartnerMgr.containsWithID (aState.getInitiatorID ()))
              {
                _createOrUpdatePartner (aState.getUsedCertificate (), aState.getInitiatorID ());
              }
              else
                if (!aPartnerMgr.containsWithID (aState.getResponderID ()))
                {
                  s_aLogger.info ("Responder is not the default or an already registered one");
                }

              _createPModeIfNotPresent (aState, sConfigID, aUserMessage);
            }
          }
          else
          {
            // Return bad request since pmodeconfigs can not be added
            // dynamically
            aAS4Response.setBadRequest ("PModeConfig could not be found with ID: " + sConfigID);
            return;
          }

        // Check if Partner+Partner combination is already present
        // P+P neu + PConfig da = anlegen
        // P+P neu + PConfig neu = Fehler
        // P+P neu + PConfig Id fehlt = default
        // P+P da + PConfig neu = fehler
        // P+P da + PConfig da = nix tun
        // P+P da + PConfig id fehlt = default

        for (final IAS4ServletMessageProcessorSPI aProcessor : getAllProcessors ())
          try
          {
            if (s_aLogger.isDebugEnabled ())
              s_aLogger.debug ("Invoking AS4 message processor " + aProcessor);

            final AS4MessageProcessorResult aResult = aProcessor.processAS4Message (aUserMessage,
                                                                                    aPayloadNode,
                                                                                    aDecryptedAttachments);
            if (aResult == null)
              throw new IllegalStateException ("No result object present!");
            if (aResult.isSuccess ())
            {
              if (s_aLogger.isDebugEnabled ())
                s_aLogger.debug ("Successfully invoked AS4 message processor " + aProcessor);
            }
            else
            {
              s_aLogger.warn ("Invoked AS4 message processor " + aProcessor + " returned a failure");
              // TODO add error message
            }
          }
          catch (final Throwable t)
          {
            s_aLogger.error ("Error processing incoming AS4 message with processor " + aProcessor, t);
            // TODO add error message
          }
      }

      // TODO build response according to handler result
      if (aErrorMessages.isNotEmpty ())
      {
        final CreateErrorMessage aCreateErrorMessage = new CreateErrorMessage ();
        final AS4ErrorMessage aErrorMsg = aCreateErrorMessage.createErrorMessage (eSOAPVersion,
                                                                                  aCreateErrorMessage.createEbms3MessageInfo (CAS4.LIB_NAME),
                                                                                  aErrorMessages);

        aAS4Response.setContentAndCharset (AS4XMLHelper.serializeXML (aErrorMsg.getAsSOAPDocument ()),
                                           CCharset.CHARSET_UTF_8_OBJ)
                    .setMimeType (eSOAPVersion.getMimeType ());
      }
      else
      {
        final Ebms3UserMessage aEbms3UserMessage = aMessaging.getUserMessageAtIndex (0);
        final CreateReceiptMessage aCreateReceiptMessage = new CreateReceiptMessage ();
        final Ebms3MessageInfo aEbms3MessageInfo = aCreateReceiptMessage.createEbms3MessageInfo (CAS4.LIB_NAME, null);
        final AS4ReceiptMessage aReceiptMessage = aCreateReceiptMessage.createReceiptMessage (eSOAPVersion,
                                                                                              aEbms3MessageInfo,
                                                                                              aEbms3UserMessage,
                                                                                              aSOAPDocument)
                                                                       .setMustUnderstand (true);

        // We've got our response
        final Document aResponseDoc = aReceiptMessage.getAsSOAPDocument ();
        aAS4Response.setContentAndCharset (AS4XMLHelper.serializeXML (aResponseDoc), CCharset.CHARSET_UTF_8_OBJ)
                    .setMimeType (eSOAPVersion.getMimeType ());
      }
    }
  }

  private void _createOrUpdatePartner (@Nonnull final X509Certificate usedCertificate, @Nonnull final String sID)
  {
    final StringMap aStringMap = new StringMap ();
    aStringMap.setAttribute (Partner.ATTR_PARTNER_NAME, sID);
    if (usedCertificate != null)
      aStringMap.setAttribute (Partner.ATTR_CERT, IOHelper.getPEMEncodedCertificate (usedCertificate));
    final PartnerManager aPartnerMgr = MetaAS4Manager.getPartnerMgr ();
    aPartnerMgr.createOrUpdatePartner (sID, aStringMap);
  }

  @Nullable
  private String _checkAndSaveProperties (@Nonnull final List <Ebms3Property> aPropertyList)
  {
    if (aPropertyList.isEmpty ())
    {
      return "No C1 and C4 are specified in the properties(originalSender and finalRecipient)!";
    }

    for (final Ebms3Property sProperty : aPropertyList)
    {
      if (sProperty.getName ().equals ("originalSender"))
      {
        sOriginalSender = sProperty.getValue ();
      }
      else
        if (sProperty.getName ().equals ("finalRecipient"))
        {
          sFinalRecipient = sProperty.getValue ();
        }
    }
    if (StringHelper.hasNoText (sOriginalSender))
    {
      return "originalSender property is empty or not existant but mandatory";
    }
    if (StringHelper.hasNoText (sFinalRecipient))
    {
      return "finalRecipient property is empty or not existant but mandatory";
    }
    return null;
  }

  private void _createPModeIfNotPresent (@Nonnull final AS4MessageState aState,
                                         @Nonnull final String sConfigID,
                                         @Nonnull final Ebms3UserMessage aUserMessage)
  {
    if (MetaAS4Manager.getPModeMgr ()
                      .getAll (doesPartnerAndPartnerExist (aState.getInitiatorID (),
                                                           aState.getResponderID (),
                                                           sConfigID))
                      .isEmpty ())
    {
      // TODO might need to add type also => check with tests
      final PMode aPMode = new PMode (new PModeParty (null,
                                                      aUserMessage.getPartyInfo ()
                                                                  .getFrom ()
                                                                  .getPartyId ()
                                                                  .get (0)
                                                                  .getValue (),
                                                      aUserMessage.getPartyInfo ().getFrom ().getRole (),
                                                      null,
                                                      null),
                                      new PModeParty (null,
                                                      aUserMessage.getPartyInfo ()
                                                                  .getTo ()
                                                                  .getPartyId ()
                                                                  .get (0)
                                                                  .getValue (),
                                                      aUserMessage.getPartyInfo ().getTo ().getRole (),
                                                      null,
                                                      null),
                                      MetaAS4Manager.getPModeConfigMgr ().getPModeConfigOfID (sConfigID));
      MetaAS4Manager.getPModeMgr ().createPMode (aPMode);
    }
    // If the PMode already exists we do not need to do anything
  }

  public static Predicate <IPMode> doesPartnerAndPartnerExist (@Nonnull final String sInitiatorID,
                                                               @Nonnull final String sResponderID,
                                                               @Nonnull final String sPModeConfigID)
  {
    return p -> p.getInitiator ().getID ().equals (sInitiatorID) &&
                p.getResponder ().getID ().equals (sResponderID) &&
                p.getConfigID ().equals (sPModeConfigID);
  }

  @Override
  @Nonnull
  protected AS4Response createUnifiedResponse (@Nonnull final EHTTPVersion eHTTPVersion,
                                               @Nonnull final EHTTPMethod eHTTPMethod,
                                               @Nonnull final HttpServletRequest aHttpRequest)
  {
    return new AS4Response (eHTTPVersion, eHTTPMethod, aHttpRequest);
  }

  @Override
  protected void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final AS4Response aHttpResponse = (AS4Response) aUnifiedResponse;
    final HttpServletRequest aHttpServletRequest = aRequestScope.getRequest ();

    // XXX By default login in admin user
    final ELoginResult e = LoggedInUserManager.getInstance ().loginUser (CSecurity.USER_ADMINISTRATOR_LOGIN,
                                                                         CSecurity.USER_ADMINISTRATOR_PASSWORD);
    assertTrue (e.toString (), e.isSuccess ());

    try
    {
      // Determine content type
      final MimeType aMT = MimeTypeParser.parseMimeType (aHttpServletRequest.getContentType ());
      s_aLogger.info ("Content-Type: " + aMT);
      if (aMT == null)
      {
        aHttpResponse.setBadRequest ("Failed to parse Content-Type '" + aHttpServletRequest.getContentType () + "'");
        return;
      }

      Document aSOAPDocument = null;
      ESOAPVersion eSOAPVersion = null;
      final ICommonsList <IAS4IncomingAttachment> aIncomingAttachments = new CommonsArrayList<> ();

      final IMimeType aPlainMT = aMT.getCopyWithoutParameters ();
      if (aPlainMT.equals (MT_MULTIPART_RELATED))
      {
        // MIME message
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Received MIME message");

        final String sBoundary = aMT.getParameterValueWithName ("boundary");
        if (StringHelper.hasNoText (sBoundary))
        {
          aHttpResponse.setBadRequest ("Content-Type '" +
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

            final MimeBodyPart aBodyPart = new MimeBodyPart (aItemIS2);
            if (nIndex == 0)
            {
              // SOAP document
              final IMimeType aPlainPartMT = MimeTypeParser.parseMimeType (aBodyPart.getContentType ())
                                                           .getCopyWithoutParameters ();

              // Determine SOAP version from MIME part content type
              eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (),
                                                    x -> aPlainPartMT.equals (x.getMimeType ()));

              // Read SOAP document
              aSOAPDocument = DOMReader.readXMLDOM (aBodyPart.getInputStream ());
            }
            else
            {
              // MIME Attachment
              final IAS4IncomingAttachment aAttachment = MetaManager.getIncomingAttachmentFactory ()
                                                                    .createAttachment (aBodyPart);
              aIncomingAttachments.add (aAttachment);
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
        if (eSOAPVersion == null)
          aHttpResponse.setBadRequest ("Failed to parse incoming document!");
        else
          aHttpResponse.setBadRequest ("Failed to parse incoming SOAP " + eSOAPVersion.getVersion () + " document!");
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
          aHttpResponse.setBadRequest ("Failed to determine SOAP version from XML document!");
        }
        else
        {
          // SOAP document and SOAP version are determined
          // TODO make locale dynamic
          _handleSOAPMessage (aSOAPDocument, eSOAPVersion, aIncomingAttachments, aHttpResponse, Locale.US);
        }
      }
    }
    catch (final Throwable t)
    {
      aHttpResponse.setResponseError (HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                      "Internal error processing AS4 request",
                                      t);
    }
    finally
    {
      LoggedInUserManager.getInstance ().logoutCurrentUser ();
    }
  }
}
