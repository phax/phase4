/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.MimeBodyPart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.wss4j.common.util.AttachmentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.attachment.WSS4JAttachment;
import com.helger.as4lib.attachment.incoming.AS4IncomingWSS4JAttachment;
import com.helger.as4lib.attachment.incoming.AbstractAS4IncomingAttachment;
import com.helger.as4lib.attachment.incoming.IAS4IncomingAttachment;
import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3PartInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.message.AS4ErrorMessage;
import com.helger.as4lib.message.AS4ReceiptMessage;
import com.helger.as4lib.message.CreateErrorMessage;
import com.helger.as4lib.message.CreateReceiptMessage;
import com.helger.as4lib.message.CreateUserMessage;
import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.model.pmode.EPModeSendReceiptReplyPattern;
import com.helger.as4lib.model.pmode.IPMode;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.model.pmode.PModeManager;
import com.helger.as4lib.model.pmode.PModeParty;
import com.helger.as4lib.model.pmode.config.IPModeConfig;
import com.helger.as4lib.model.pmode.config.PModeConfigManager;
import com.helger.as4lib.model.profile.IAS4Profile;
import com.helger.as4lib.partner.Partner;
import com.helger.as4lib.partner.PartnerManager;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.util.AS4ResourceManager;
import com.helger.as4lib.util.StringMap;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.as4server.attachment.IIncomingAttachmentFactory;
import com.helger.as4server.mgr.MetaManager;
import com.helger.as4server.receive.AS4MessageState;
import com.helger.as4server.receive.soap.ISOAPHeaderElementProcessor;
import com.helger.as4server.receive.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.as4server.settings.AS4ServerConfiguration;
import com.helger.as4server.spi.AS4MessageProcessorResult;
import com.helger.as4server.spi.AS4ServletMessageProcessorManager;
import com.helger.as4server.spi.IAS4ServletMessageProcessorSPI;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.charset.CCharset;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.error.list.ErrorList;
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
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.multipart.MultipartProgressNotifier;
import com.helger.web.multipart.MultipartStream;
import com.helger.web.multipart.MultipartStream.MultipartItemInputStream;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.ChildElementIterator;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.read.DOMReader;

public final class AS4Servlet extends AbstractUnifiedResponseServlet
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4Servlet.class);
  private static final IMimeType MT_MULTIPART_RELATED = EMimeContentType.MULTIPART.buildMimeType ("related");

  public AS4Servlet ()
  {}

  private void _handleSOAPMessage (@Nonnull final AS4ResourceManager aResMgr,
                                   @Nonnull final Document aSOAPDocument,
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

    // TODO remove if or entire statement, so much output
    if (s_aLogger.isDebugEnabled ())
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
    final ICommonsList <AS4SingleSOAPHeader> aHeaders = new CommonsArrayList <> ();
    for (final Element aHeaderChild : new ChildElementIterator (aHeaderNode))
    {
      final QName aQName = XMLHelper.getQName (aHeaderChild);
      final String sMustUnderstand = aHeaderChild.getAttributeNS (eSOAPVersion.getNamespaceURI (), "mustUnderstand");
      final boolean bIsMustUnderstand = eSOAPVersion.getMustUnderstandValue (true).equals (sMustUnderstand);
      aHeaders.add (new AS4SingleSOAPHeader (aHeaderChild, aQName, bIsMustUnderstand));
    }

    final ICommonsList <Ebms3Error> aErrorMessages = new CommonsArrayList <> ();

    // Convert all attachments to WSS4J attachments
    // Need to check, since not every message will have attachments
    final ICommonsList <WSS4JAttachment> aWSS4JAttachments = new CommonsArrayList <> (aIncomingAttachments,
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
      final AS4SingleSOAPHeader aHeader = aHeaders.findFirst (x -> aQName.equals (x.getQName ()));
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
    Ebms3UserMessage aUserMessage = null;

    if (aErrorMessages.isEmpty ())
    {
      for (final AS4SingleSOAPHeader aHeader : aHeaders)
        if (aHeader.isMustUnderstand () && !aHeader.isProcessed ())
        {
          aAS4Response.setBadRequest ("Error processing required SOAP header element " +
                                      aHeader.getQName ().toString ());
          return;
        }

      aMessaging = aState.getMessaging ();

      // Every message can only contain 1 Usermessage but 0..n signalmessages
      aUserMessage = aMessaging.getUserMessageAtIndex (0);

      // Decompressing the attachments
      final ICommonsList <IAS4IncomingAttachment> aDecryptedAttachments = new CommonsArrayList <> (aState.hasDecryptedAttachments () ? aState.getDecryptedAttachments ()
                                                                                                                                     : aState.getOriginalAttachments (),
                                                                                                   x -> new AS4IncomingWSS4JAttachment (x));

      // Decompress attachments (if compressed)
      final IIncomingAttachmentFactory aIAF = MetaManager.getIncomingAttachmentFactory ();
      for (final IAS4IncomingAttachment aIncomingAttachment : aDecryptedAttachments.getClone ())
      {
        final EAS4CompressionMode eCompressionMode = aState.getAttachmentCompressionMode (aIncomingAttachment.getContentID ());
        if (eCompressionMode != null)
        {
          // Remove the old one
          aDecryptedAttachments.remove (aIncomingAttachment);

          // Add the new one with decompressing InputStream
          final IAS4IncomingAttachment aDecompressedAttachment = aIAF.createAttachment (aResMgr,
                                                                                        eCompressionMode.getDecompressStream (aIncomingAttachment.getInputStream ()),
                                                                                        aIncomingAttachment.getAllAttributes ());

          final String sAttachmentContentID = StringHelper.trimStart (aIncomingAttachment.getContentID (),
                                                                      "attachment=");
          final Ebms3PartInfo aPart = CollectionHelper.findFirst (aUserMessage.getPayloadInfo ().getPartInfo (),
                                                                  x -> x.getHref ().contains (sAttachmentContentID));
          if (aPart != null)
          {
            final Ebms3Property aProperty = CollectionHelper.findFirst (aPart.getPartProperties ().getProperty (),
                                                                        x -> x.getName ()
                                                                              .equals (CreateUserMessage.PART_PROPERTY_MIME_TYPE));
            if (aProperty != null)
            {
              ((AbstractAS4IncomingAttachment) aDecompressedAttachment).setAttribute (AttachmentUtils.MIME_HEADER_CONTENT_TYPE,
                                                                                      aProperty.getValue ());
            }
          }

          aDecryptedAttachments.add (aDecompressedAttachment);
        }
      }

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

      // Check if originalSender and finalRecipient are present
      // Since these two properties are mandatory
      if (aUserMessage.getMessageProperties () != null)
      {
        if (aUserMessage.getMessageProperties ().getProperty () != null)
        {
          final String sCheckResult = _checkPropertiesOrignalSenderAndFinalRecipient (aUserMessage.getMessageProperties ()
                                                                                                  .getProperty ());
          if (!StringHelper.hasNoText (sCheckResult))
          {
            aAS4Response.setBadRequest (sCheckResult);
            return;
          }
        }
        else
        {
          aAS4Response.setBadRequest ("Message Property element present but no properties");
          return;
        }
      }
      else
      {
        aAS4Response.setBadRequest ("No Message Properties present but OriginalSender and finalRecipient have to be present");
        return;
      }

      // Additional Matrix on what should happen in certain scenarios
      // Check if Partner+Partner combination is already present
      // P+P neu + PConfig da = anlegen
      // P+P neu + PConfig neu = Fehler
      // P+P neu + PConfig Id fehlt = default
      // P+P da + PConfig neu = fehler
      // P+P da + PConfig da = nix tun
      // P+P da + PConfig id fehlt = default

      final String sConfigID = aState.getPModeConfig ().getID ();

      final PModeConfigManager aPModeConfigMgr = MetaAS4Manager.getPModeConfigMgr ();
      final PartnerManager aPartnerMgr = MetaAS4Manager.getPartnerMgr ();

      if (aPModeConfigMgr.containsWithID (sConfigID))
      {
        if (aPartnerMgr.containsWithID (aState.getInitiatorID ()) &&
            aPartnerMgr.containsWithID (aState.getResponderID ()))
        {
          _ensurePModeIsPresent (aState, sConfigID, aUserMessage);
        }
        else
        {
          if (!aPartnerMgr.containsWithID (aState.getInitiatorID ()))
          {
            _createOrUpdatePartner (aState.getUsedCertificate (), aState.getInitiatorID ());
          }
          else
            if (!aPartnerMgr.containsWithID (aState.getResponderID ()))
            {
              s_aLogger.warn ("Responder is not the default or an already registered one");
              _createOrUpdatePartner (null, aState.getResponderID ());
            }

          _ensurePModeIsPresent (aState, sConfigID, aUserMessage);
        }
      }

      for (final IAS4ServletMessageProcessorSPI aProcessor : AS4ServletMessageProcessorManager.getAllProcessors ())
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
            aAS4Response.setBadRequest ("Invoked AS4 message processor " + aProcessor + " returned a failure");
            return;
          }
        }
        catch (final Throwable t)
        {
          s_aLogger.error ("Error processing incoming AS4 message with processor " + aProcessor, t);
          aAS4Response.setBadRequest ("Error processing incoming AS4 message with processor " +
                                      aProcessor +
                                      ", Exception: " +
                                      t.getLocalizedMessage ());
          return;
        }
    }
    // PModeConfig
    final IPModeConfig aPModeConfig = aState.getPModeConfig ();

    // Only do profile checks if a profile is set
    if (AS4ServerConfiguration.getAS4Profile () != null)
    {
      final IAS4Profile aProfile = MetaAS4Manager.getProfileMgr ()
                                                 .getProfileOfID (AS4ServerConfiguration.getAS4Profile ());

      if (aProfile != null)
      {
        // Profile Checks gets set when started with Server
        final ErrorList aErrorList = new ErrorList ();
        aProfile.getValidator ().validatePModeConfig (aPModeConfig, aErrorList);
        aProfile.getValidator ().validateUserMessage (aUserMessage, aErrorList);
        if (aErrorList.isNotEmpty ())
        {
          s_aLogger.error ("Error validating incoming AS4 message with the profile " + aProfile.getDisplayName ());
          aAS4Response.setBadRequest ("Error validating incoming AS4 message with the profile " +
                                      aProfile.getDisplayName () +
                                      "\n Following errors are present: " +
                                      aErrorList.getAllErrors ().getAllTexts (aLocale));
          return;
        }
      }
      else
      {
        aAS4Response.setBadRequest ("The profile " + AS4ServerConfiguration.getAS4Profile () + " does not exist.");
        return;
      }
    }

    // Generate ErrorMessage if errors in the process are present and the
    // partners declared in their pmodeconfig they want an errorresponse
    if (aErrorMessages.isNotEmpty ())
    {
      if (_isSendErrorResponse (aPModeConfig))
      {
        final CreateErrorMessage aCreateErrorMessage = new CreateErrorMessage ();
        final AS4ErrorMessage aErrorMsg = aCreateErrorMessage.createErrorMessage (eSOAPVersion,
                                                                                  aCreateErrorMessage.createEbms3MessageInfo (CAS4.LIB_NAME),
                                                                                  aErrorMessages);

        aAS4Response.setContentAndCharset (AS4XMLHelper.serializeXML (aErrorMsg.getAsSOAPDocument ()),
                                           CCharset.CHARSET_UTF_8_OBJ)
                    .setMimeType (eSOAPVersion.getMimeType ());
      }
    }
    else
    {
      // If no Error is present check if partners declared if they want an
      // response and if this response should contain
      // nonrepudiationinformation if applicable
      if (_isSendResponse (aPModeConfig))
      {
        final Ebms3UserMessage aEbms3UserMessage = aMessaging.getUserMessageAtIndex (0);
        final CreateReceiptMessage aCreateReceiptMessage = new CreateReceiptMessage ();
        final Ebms3MessageInfo aEbms3MessageInfo = aCreateReceiptMessage.createEbms3MessageInfo (CAS4.LIB_NAME);
        final AS4ReceiptMessage aReceiptMessage = aCreateReceiptMessage.createReceiptMessage (eSOAPVersion,
                                                                                              aEbms3MessageInfo,
                                                                                              aEbms3UserMessage,
                                                                                              aSOAPDocument,
                                                                                              _isSendNonRepudiationInformation (aPModeConfig))
                                                                       .setMustUnderstand (true);

        // We've got our response
        final Document aResponseDoc = aReceiptMessage.getAsSOAPDocument ();
        aAS4Response.setContentAndCharset (AS4XMLHelper.serializeXML (aResponseDoc), CCharset.CHARSET_UTF_8_OBJ)
                    .setMimeType (eSOAPVersion.getMimeType ());
      }
    }
  }

  /**
   * Checks if in the given PModeConfig the isSendReceiptNonRepudiation is set
   * or not.
   *
   * @param aPModeConfig
   *        to check the attribute
   * @return Returns the value if set, else DEFAULT <code>false</code>.
   */
  private static boolean _isSendNonRepudiationInformation (@Nullable final IPModeConfig aPModeConfig)
  {
    if (aPModeConfig != null)
      if (aPModeConfig.getLeg1 () != null)
        if (aPModeConfig.getLeg1 ().getSecurity () != null)
          if (aPModeConfig.getLeg1 ().getSecurity ().isSendReceiptNonRepudiationDefined ())
            return aPModeConfig.getLeg1 ().getSecurity ().isSendReceiptNonRepudiation ();
    // Default behavior
    return false;
  }

  /**
   * Checks if in the given PModeConfig isReportAsResponse is set.
   *
   * @param aPModeConfig
   *        to check the attribute
   * @return Returns the value if set, else DEFAULT <code>TRUE</code>.
   */
  private static boolean _isSendErrorResponse (@Nullable final IPModeConfig aPModeConfig)
  {
    if (aPModeConfig != null)
      if (aPModeConfig.getLeg1 () != null)
        if (aPModeConfig.getLeg1 ().getErrorHandling () != null)
          if (aPModeConfig.getLeg1 ().getErrorHandling ().isReportAsResponseDefined ())
            return aPModeConfig.getLeg1 ().getErrorHandling ().isReportAsResponse ();
    // Default behavior
    return true;
  }

  /**
   * Checks if a ReceiptReplyPattern is set to Response or not.
   *
   * @param aPModeConfig
   *        to check the attribute
   * @return Returns the value if set, else DEFAULT <code>TRUE</code>.
   */
  private static boolean _isSendResponse (@Nullable final IPModeConfig aPModeConfig)
  {
    if (aPModeConfig != null)
      if (aPModeConfig.getLeg1 () != null)
        if (aPModeConfig.getLeg1 ().getSecurity () != null)
          return EPModeSendReceiptReplyPattern.RESPONSE.equals (aPModeConfig.getLeg1 ()
                                                                            .getSecurity ()
                                                                            .getSendReceiptReplyPattern ());

    // Default behaviour if the value is not set or no security is existing
    return true;
  }

  /**
   * Creates or Updates are Partner. Overwrites with the values in the parameter
   * or creates a new Partner if not present in the PartnerManager already.
   *
   * @param aUsedCertificate
   *        Certificate that should be used
   * @param sID
   *        ID of the Partner
   */
  private static void _createOrUpdatePartner (@Nullable final X509Certificate aUsedCertificate,
                                              @Nonnull final String sID)
  {
    final StringMap aStringMap = new StringMap ();
    aStringMap.setAttribute (Partner.ATTR_PARTNER_NAME, sID);
    if (aUsedCertificate != null)
      aStringMap.setAttribute (Partner.ATTR_CERT, CertificateHelper.getPEMEncodedCertificate (aUsedCertificate));
    final PartnerManager aPartnerMgr = MetaAS4Manager.getPartnerMgr ();
    aPartnerMgr.createOrUpdatePartner (sID, aStringMap);
  }

  /**
   * Checks the mandatory properties OriginalSender and FinalRecipient if those
   * two are set.
   *
   * @param aPropertyList
   *        the property list that should be checked for the two specific ones
   * @return <code>null</code> if both properties are present, else returns the
   *         error message that should be returned to the user.
   */
  @Nullable
  private static String _checkPropertiesOrignalSenderAndFinalRecipient (@Nonnull final List <Ebms3Property> aPropertyList)
  {
    String sOriginalSenderC1 = null;
    String sFinalRecipientC4 = null;

    for (final Ebms3Property sProperty : aPropertyList)
    {
      if (sProperty.getName ().equals ("originalSender"))
        sOriginalSenderC1 = sProperty.getValue ();
      else
        if (sProperty.getName ().equals ("finalRecipient"))
          sFinalRecipientC4 = sProperty.getValue ();
    }
    if (StringHelper.hasNoText (sOriginalSenderC1))
    {
      return "originalSender property is empty or not existant but mandatory";
    }
    if (StringHelper.hasNoText (sFinalRecipientC4))
    {
      return "finalRecipient property is empty or not existant but mandatory";
    }
    return null;
  }

  /**
   * Creates a PMode if it does not exist already.
   *
   * @param aState
   *        needed to get Responder and Initiator
   * @param sConfigID
   *        needed to get the PModeConfig for the PMode
   * @param aUserMessage
   *        needed to get full information of the Initiator and Responder
   */
  private static void _ensurePModeIsPresent (@Nonnull final AS4MessageState aState,
                                             @Nonnull final String sConfigID,
                                             @Nonnull final Ebms3UserMessage aUserMessage)
  {
    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    if (aPModeMgr.containsNone (doesPartnerAndPartnerExist (aState.getInitiatorID (),
                                                            aState.getResponderID (),
                                                            sConfigID)))
    {
      final PMode aPMode = new PMode (new PModeParty (aUserMessage.getPartyInfo ()
                                                                  .getFrom ()
                                                                  .getPartyIdAtIndex (0)
                                                                  .getType (),
                                                      aUserMessage.getPartyInfo ()
                                                                  .getFrom ()
                                                                  .getPartyIdAtIndex (0)
                                                                  .getValue (),
                                                      aUserMessage.getPartyInfo ().getFrom ().getRole (),
                                                      null,
                                                      null),
                                      new PModeParty (aUserMessage.getPartyInfo ()
                                                                  .getTo ()
                                                                  .getPartyIdAtIndex (0)
                                                                  .getType (),
                                                      aUserMessage.getPartyInfo ()
                                                                  .getTo ()
                                                                  .getPartyIdAtIndex (0)
                                                                  .getValue (),
                                                      aUserMessage.getPartyInfo ().getTo ().getRole (),
                                                      null,
                                                      null),
                                      MetaAS4Manager.getPModeConfigMgr ().getPModeConfigOfID (sConfigID));
      aPModeMgr.createOrUpdatePMode (aPMode);
    }
    // If the PMode already exists we do not need to do anything
  }

  /**
   * This Predicate helps to find if a Partner (Initiator), Partner (Responder)
   * with a specific PModeConfig already exists.
   *
   * @param sInitiatorID
   *        Initiator to check
   * @param sResponderID
   *        Responder to check
   * @param sPModeConfigID
   *        PModeConfig to check
   * @return aPMode if it already exists with all 3 components
   */
  @Nullable
  public static Predicate <IPMode> doesPartnerAndPartnerExist (@Nullable final String sInitiatorID,
                                                               @Nullable final String sResponderID,
                                                               @Nullable final String sPModeConfigID)
  {
    return x -> EqualsHelper.equals (x.getInitiatorID (), sInitiatorID) &&
                EqualsHelper.equals (x.getResponderID (), sResponderID) &&
                x.getConfigID ().equals (sPModeConfigID);
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
    // XXX why do we need a logged-in user?
    final ELoginResult e = LoggedInUserManager.getInstance ().loginUser (CSecurity.USER_ADMINISTRATOR_LOGIN,
                                                                         CSecurity.USER_ADMINISTRATOR_PASSWORD);
    assert e.isSuccess () : "Login failed: " + e.toString ();

    try (final AS4ResourceManager aResMgr = new AS4ResourceManager ())
    {
      // Determine content type
      final MimeType aMT = MimeTypeParser.parseMimeType (aHttpServletRequest.getContentType ());
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Received Content-Type: " + aMT);
      if (aMT == null)
      {
        aHttpResponse.setBadRequest ("Failed to parse Content-Type '" + aHttpServletRequest.getContentType () + "'");
        return;
      }

      Document aSOAPDocument = null;
      ESOAPVersion eSOAPVersion = null;
      final ICommonsList <IAS4IncomingAttachment> aIncomingAttachments = new CommonsArrayList <> ();

      final IMimeType aPlainContentType = aMT.getCopyWithoutParameters ();
      if (aPlainContentType.equals (MT_MULTIPART_RELATED))
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
            final boolean bHasNextPart = nIndex == 0 ? aMulti.skipPreamble () : aMulti.readBoundary ();
            if (!bHasNextPart)
              break;

            if (s_aLogger.isDebugEnabled ())
              s_aLogger.debug ("Found MIME part " + nIndex);
            final MultipartItemInputStream aItemIS2 = aMulti.createInputStream ();

            final MimeBodyPart aBodyPart = new MimeBodyPart (aItemIS2);
            if (nIndex == 0)
            {
              // First MIME part -> SOAP document
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
              // MIME Attachment (index is gt 0)
              final IAS4IncomingAttachment aAttachment = MetaManager.getIncomingAttachmentFactory ()
                                                                    .createAttachment (aResMgr, aBodyPart);
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
        eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (), x -> aPlainContentType.equals (x.getMimeType ()));
      }

      if (aSOAPDocument == null)
      {
        // We don't have a SOAP document
        if (eSOAPVersion == null)
          aHttpResponse.setBadRequest ("Failed to parse incoming message!");
        else
          aHttpResponse.setBadRequest ("Failed to parse incoming SOAP " + eSOAPVersion.getVersion () + " document!");
      }
      else
      {
        if (eSOAPVersion == null)
        {
          // Determine from namespace URI of read document as the last fallback
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
          _handleSOAPMessage (aResMgr, aSOAPDocument, eSOAPVersion, aIncomingAttachments, aHttpResponse, Locale.US);
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
