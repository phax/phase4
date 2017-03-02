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
package com.helger.as4.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.as4.CAS4;
import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.attachment.IIncomingAttachmentFactory;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.attachment.WSS4JAttachment.IHasAttachmentSourceStream;
import com.helger.as4.error.EEbmsError;
import com.helger.as4.error.EEbmsErrorSeverity;
import com.helger.as4.messaging.domain.AS4ErrorMessage;
import com.helger.as4.messaging.domain.AS4ReceiptMessage;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.CreateErrorMessage;
import com.helger.as4.messaging.domain.CreateReceiptMessage;
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.messaging.domain.EAS4MessageType;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.encrypt.EncryptionCreator;
import com.helger.as4.messaging.mime.MimeMessageCreator;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.MEPHelper;
import com.helger.as4.model.pmode.EPModeSendReceiptReplyPattern;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.PModeManager;
import com.helger.as4.model.pmode.PModeParty;
import com.helger.as4.model.pmode.config.IPModeConfig;
import com.helger.as4.model.pmode.config.PModeConfigManager;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.as4.model.pmode.leg.PModeLegSecurity;
import com.helger.as4.partner.Partner;
import com.helger.as4.partner.PartnerManager;
import com.helger.as4.profile.IAS4Profile;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.servlet.mgr.AS4ServerSettings;
import com.helger.as4.servlet.mgr.AS4ServletMessageProcessorManager;
import com.helger.as4.servlet.soap.AS4SingleSOAPHeader;
import com.helger.as4.servlet.soap.ISOAPHeaderElementProcessor;
import com.helger.as4.servlet.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.as4.servlet.spi.AS4MessageProcessorResult;
import com.helger.as4.servlet.spi.IAS4ServletMessageProcessorSPI;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.as4.util.StringMap;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3Description;
import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3From;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartInfo;
import com.helger.as4lib.ebms3header.Ebms3PartyId;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3PullRequest;
import com.helger.as4lib.ebms3header.Ebms3To;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.error.IError;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.string.StringHelper;
import com.helger.http.EHTTPMethod;
import com.helger.http.EHTTPVersion;
import com.helger.http.HTTPStringHelper;
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

/**
 * AS4 receiving servlet.<br>
 * Use a configuration like the following in your <code>WEB-INF/web.xm</code>
 * file:
 *
 * <pre>
&lt;servlet&gt;
  &lt;servlet-name&gt;AS4Servlet&lt;/servlet-name&gt;
  &lt;servlet-class&gt;com.helger.as4.servlet.AS4Servlet&lt;/servlet-class&gt;
&lt;/servlet&gt;
&lt;servlet-mapping&gt;
  &lt;servlet-name&gt;AS4Servlet&lt;/servlet-name&gt;
  &lt;url-pattern&gt;/as4&lt;/url-pattern&gt;
&lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author Martin Bayerl
 * @author Philip Helger
 */
public final class AS4Servlet extends AbstractUnifiedResponseServlet
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4Servlet.class);
  private static final IMimeType MT_MULTIPART_RELATED = EMimeContentType.MULTIPART.buildMimeType ("related");

  public AS4Servlet ()
  {}

  @Override
  protected Set <EHTTPMethod> getAllowedHTTPMethods ()
  {
    return ALLOWED_METHDOS_POST;
  }

  private static void _decompressAttachments (@Nonnull final Ebms3UserMessage aUserMessage,
                                              @Nonnull final IAS4MessageState aState,
                                              @Nonnull final ICommonsList <WSS4JAttachment> aIncomingDecryptedAttachments)
  {
    for (final WSS4JAttachment aIncomingAttachment : aIncomingDecryptedAttachments.getClone ())
    {
      final EAS4CompressionMode eCompressionMode = aState.getAttachmentCompressionMode (aIncomingAttachment.getId ());
      if (eCompressionMode != null)
      {
        final IHasAttachmentSourceStream aOldISP = aIncomingAttachment.getInputStreamProvider ();
        aIncomingAttachment.setSourceStreamProvider ( () -> eCompressionMode.getDecompressStream (aOldISP.getInputStream ()));

        final String sAttachmentContentID = StringHelper.trimStart (aIncomingAttachment.getId (), "attachment=");
        final Ebms3PartInfo aPart = CollectionHelper.findFirst (aUserMessage.getPayloadInfo ().getPartInfo (),
                                                                x -> x.getHref ().contains (sAttachmentContentID));
        if (aPart != null)
        {
          final Ebms3Property aProperty = CollectionHelper.findFirst (aPart.getPartProperties ().getProperty (),
                                                                      x -> x.getName ()
                                                                            .equals (CreateUserMessage.PART_PROPERTY_MIME_TYPE));
          if (aProperty != null)
          {
            aIncomingAttachment.overwriteMimeType (aProperty.getValue ());
          }
        }
      }
    }
  }

  private void _processSOAPHeaderElements (@Nonnull final Document aSOAPDocument,
                                           @Nonnull final ESOAPVersion eSOAPVersion,
                                           @Nonnull final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                           @Nonnull final Locale aDisplayLocale,
                                           @Nonnull final AS4MessageState aState,
                                           @Nonnull final ICommonsList <Ebms3Error> aErrorMessages) throws BadRequestException
  {
    final ICommonsList <AS4SingleSOAPHeader> aHeaders = new CommonsArrayList<> ();
    {
      // Find SOAP header
      final Node aHeaderNode = XMLHelper.getFirstChildElementOfName (aSOAPDocument.getDocumentElement (),
                                                                     eSOAPVersion.getNamespaceURI (),
                                                                     eSOAPVersion.getHeaderElementName ());
      if (aHeaderNode == null)
        throw new BadRequestException ("SOAP document is missing a Header element");

      // Extract all header elements including their mustUnderstand value
      for (final Element aHeaderChild : new ChildElementIterator (aHeaderNode))
      {
        final QName aQName = XMLHelper.getQName (aHeaderChild);
        final String sMustUnderstand = aHeaderChild.getAttributeNS (eSOAPVersion.getNamespaceURI (), "mustUnderstand");
        final boolean bIsMustUnderstand = eSOAPVersion.getMustUnderstandValue (true).equals (sMustUnderstand);
        aHeaders.add (new AS4SingleSOAPHeader (aHeaderChild, aQName, bIsMustUnderstand));
      }
    }

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
                                           aIncomingAttachments,
                                           aState,
                                           aErrorList,
                                           aDisplayLocale)
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

        for (final IError aError : aErrorList)
        {
          String sRefToMessageID = "";
          if (aState.getMessaging () != null)
            if (aState.getMessaging ().getUserMessageCount () > 0)
              sRefToMessageID = aState.getMessaging ().getUserMessageAtIndex (0).getMessageInfo ().getMessageId ();

          final EEbmsError ePredefinedError = EEbmsError.getFromErrorCodeOrNull (aError.getErrorID ());
          if (ePredefinedError != null)
            aErrorMessages.add (ePredefinedError.getAsEbms3Error (aDisplayLocale, sRefToMessageID));
          else
          {
            final Ebms3Error aEbms3Error = new Ebms3Error ();
            aEbms3Error.setErrorDetail (aError.getErrorText (aDisplayLocale));
            aEbms3Error.setErrorCode (aError.getErrorID ());
            aEbms3Error.setSeverity (aError.getErrorLevel ().getID ());
            aEbms3Error.setOrigin (aError.getErrorFieldName ());
            aEbms3Error.setRefToMessageInError (aState.getMessaging ()
                                                      .getUserMessageAtIndex (0)
                                                      .getMessageInfo ()
                                                      .getMessageId ());
            aErrorMessages.add (aEbms3Error);
          }
        }

        // Stop processing of other headers
        break;
      }
    }

    // If an error message is present, send it back gracefully
    if (aErrorMessages.isEmpty ())
    {
      // Now check if all must understand headers were processed
      // Are all must-understand headers processed?
      for (final AS4SingleSOAPHeader aHeader : aHeaders)
        if (aHeader.isMustUnderstand () && !aHeader.isProcessed ())
          throw new BadRequestException ("Error processing required SOAP header element " +
                                         aHeader.getQName ().toString ());
    }
  }

  private static void _invokeSPIs (@Nonnull final Ebms3UserMessage aUserMessage,
                                   @Nullable final Node aPayloadNode,
                                   @Nullable final ICommonsList <WSS4JAttachment> aDecryptedAttachments,
                                   @Nonnull final ICommonsList <Ebms3Error> aErrorMessages,
                                   @Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments,
                                   @Nonnull final Locale aLocale)
  {
    // Invoke all SPIs
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
          // Add response attachments, payloads
          aResult.addAllAttachmentsTo (aResponseAttachments);
          if (s_aLogger.isDebugEnabled ())
            s_aLogger.debug ("Successfully invoked AS4 message processor " + aProcessor);
        }
        else
        {
          s_aLogger.warn ("Invoked AS4 message processor SPI " + aProcessor + " returned a failure");

          final Ebms3Error aError = new Ebms3Error ();
          aError.setSeverity (EEbmsErrorSeverity.FAILURE.getSeverity ());
          aError.setErrorCode (EEbmsError.EBMS_OTHER.getErrorCode ());
          aError.setRefToMessageInError (aUserMessage.getMessageInfo ().getMessageId ());
          final Ebms3Description aDesc = new Ebms3Description ();
          aDesc.setValue (aResult.getErrorMessage ());
          aDesc.setLang (aLocale.getLanguage ());
          aError.setDescription (aDesc);
          aErrorMessages.add (aError);

          // Stop processing
          break;
        }
      }
      catch (final Throwable t)
      {
        throw new BadRequestException ("Error processing incoming AS4 message with processor " + aProcessor, t);
      }
  }

  private void _handleSOAPMessage (@Nonnull final AS4ResourceManager aResMgr,
                                   @Nonnull final Document aSOAPDocument,
                                   @Nonnull final ESOAPVersion eSOAPVersion,
                                   @Nonnull final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                   @Nonnull final AS4Response aAS4Response,
                                   @Nonnull final Locale aLocale) throws TransformerFactoryConfigurationError,
                                                                  TransformerException,
                                                                  WSSecurityException,
                                                                  MessagingException
  {
    ValueEnforcer.notNull (aSOAPDocument, "SOAPDocument");
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    ValueEnforcer.notNull (aIncomingAttachments, "IncomingAttachments");
    ValueEnforcer.notNull (aAS4Response, "AS4Response");
    ValueEnforcer.notNull (aLocale, "Locale");

    if (s_aLogger.isDebugEnabled ())
    {
      s_aLogger.debug ("Received the following SOAP " + eSOAPVersion.getVersion () + " document:");
      s_aLogger.debug (AS4XMLHelper.serializeXML (aSOAPDocument));
      s_aLogger.debug ("Including the following attachments:");
      s_aLogger.debug (aIncomingAttachments.toString ());
    }

    // Collect all runtime errors
    final ICommonsList <Ebms3Error> aErrorMessages = new CommonsArrayList<> ();

    // All further operations should only operate on the interface
    IAS4MessageState aState;
    {
      // This is where all data from the SOAP headers is stored to
      final AS4MessageState aStateImpl = new AS4MessageState (eSOAPVersion, aResMgr);

      // Handle all headers - the only place where the AS4MessageState values
      _processSOAPHeaderElements (aSOAPDocument,
                                  eSOAPVersion,
                                  aIncomingAttachments,
                                  aLocale,
                                  aStateImpl,
                                  aErrorMessages);

      aState = aStateImpl;
    }

    final IPModeConfig aPModeConfig = aState.getPModeConfig ();
    Ebms3UserMessage aUserMessage = null;
    Ebms3PullRequest aPullRequest = null;
    Node aPayloadNode = null;
    ICommonsList <WSS4JAttachment> aDecryptedAttachments = null;
    // Storing for two-way response messages
    final ICommonsList <WSS4JAttachment> aResponseAttachments = new CommonsArrayList<> ();

    if (aErrorMessages.isEmpty ())
    {
      if (aPModeConfig == null)
        throw new BadRequestException ("No AS4 P-Mode configuration found!");

      // Every message can only contain 1 User message or 1 pull message
      // aUserMessage can be null on incoming Pull-Message!
      aUserMessage = aState.getMessaging ().getUserMessageAtIndex (0);
      aPullRequest = aState.getMessaging ().getSignalMessageCount () > 0 ? aState.getMessaging ()
                                                                                 .getSignalMessageAtIndex (0)
                                                                                 .getPullRequest ()
                                                                         : null;
      if (aUserMessage == null && aPullRequest == null)
        throw new BadRequestException ("UserMessage or PullRequest must be present!");
      if (aUserMessage != null && aPullRequest != null)
        throw new BadRequestException ("Only UserMessage or PullRequest may be present!");

      // Only do profile checks if a profile is set
      final String sProfileName = AS4ServerConfiguration.getAS4ProfileName ();
      if (StringHelper.hasText (sProfileName))
      {
        final IAS4Profile aProfile = MetaAS4Manager.getProfileMgr ().getProfileOfID (sProfileName);
        if (aProfile == null)
        {
          throw new BadRequestException ("The AS4 profile " + sProfileName + " does not exist.");
        }

        // Profile Checks gets set when started with Server
        final ErrorList aErrorList = new ErrorList ();
        aProfile.getValidator ().validatePModeConfig (aPModeConfig, aErrorList);
        aProfile.getValidator ().validateUserMessage (aUserMessage, aErrorList);
        if (aErrorList.isNotEmpty ())
        {
          throw new BadRequestException ("Error validating incoming AS4 message with the profile " +
                                         aProfile.getDisplayName () +
                                         "\n Following errors are present: " +
                                         aErrorList.getAllErrors ().getAllTexts (aLocale));
        }
      }

      // Ensure the decrypted attachments are used
      aDecryptedAttachments = aState.hasDecryptedAttachments () ? aState.getDecryptedAttachments ()
                                                                : aState.getOriginalAttachments ();

      if (aUserMessage != null)
      {
        // Decompress attachments (if compressed)
        // Result is directly in the decrypted attachments list!
        _decompressAttachments (aUserMessage, aState, aDecryptedAttachments);
      }

      final boolean bUseDecryptedSOAP = aState.hasDecryptedSOAPDocument ();
      final Document aRealSOAPDoc = bUseDecryptedSOAP ? aState.getDecryptedSOAPDocument () : aSOAPDocument;
      assert aRealSOAPDoc != null;

      // Find SOAP body
      final Node aBodyNode = XMLHelper.getFirstChildElementOfName (aRealSOAPDoc.getDocumentElement (),
                                                                   eSOAPVersion.getNamespaceURI (),
                                                                   eSOAPVersion.getBodyElementName ());
      if (aBodyNode == null)
        throw new BadRequestException ((bUseDecryptedSOAP ? "Decrypted" : "Original") +
                                       " SOAP document is missing a Body element");
      aPayloadNode = aBodyNode.getFirstChild ();

      if (aUserMessage != null)
      {
        // Check if originalSender and finalRecipient are present
        // Since these two properties are mandatory
        if (aUserMessage.getMessageProperties () == null)
          throw new BadRequestException ("No Message Properties present but OriginalSender and finalRecipient have to be present");

        final List <Ebms3Property> aProps = aUserMessage.getMessageProperties ().getProperty ();
        if (aProps.isEmpty ())
          throw new BadRequestException ("Message Property element present but no properties");

        _checkPropertiesOrignalSenderAndFinalRecipient (aProps);
      }

      // Additional Matrix on what should happen in certain scenarios
      // Check if Partner+Partner combination is already present
      // P+P neu + PConfig da = anlegen
      // P+P neu + PConfig neu = Fehler
      // P+P neu + PConfig Id fehlt = default
      // P+P da + PConfig neu = fehler
      // P+P da + PConfig da = nix tun
      // P+P da + PConfig id fehlt = default

      final String sConfigID = aPModeConfig.getID ();

      final PModeConfigManager aPModeConfigMgr = MetaAS4Manager.getPModeConfigMgr ();
      final PartnerManager aPartnerMgr = MetaAS4Manager.getPartnerMgr ();

      if (aPModeConfigMgr.containsWithID (sConfigID))
      {
        if (aPartnerMgr.containsWithID (aState.getInitiatorID ()) &&
            aPartnerMgr.containsWithID (aState.getResponderID ()))
        {
          _ensurePModeIsPresent (aState, sConfigID, aUserMessage.getPartyInfo ());
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

          _ensurePModeIsPresent (aState, sConfigID, aUserMessage.getPartyInfo ());
        }
      }

      if (_isNotPingMessage (aPModeConfig))
      {
        final String sMessageID = aUserMessage.getMessageInfo ().getMessageId ();
        final boolean bIsDuplicate = MetaAS4Manager.getIncomingDuplicateMgr ().registerAndCheck (sMessageID).isBreak ();
        if (bIsDuplicate)
        {
          s_aLogger.info ("Not invoking SPIs, because message was already handled!");
          final Ebms3Description aDesc = new Ebms3Description ();
          aDesc.setLang (aLocale.getLanguage ());
          aDesc.setValue ("Another message with the same ID was already received!");
          aErrorMessages.add (EEbmsError.EBMS_OTHER.getAsEbms3Error (aLocale, sMessageID, null, aDesc));
        }
        else
        {
          _invokeSPIs (aUserMessage,
                       aPayloadNode,
                       aDecryptedAttachments,
                       aErrorMessages,
                       aResponseAttachments,
                       aLocale);
        }
      }
    }

    // Generate ErrorMessage if errors in the process are present and the
    // partners declared in their pmode config they want an error response
    if (aErrorMessages.isNotEmpty ())

    {
      if (_isSendErrorAsResponse (aPModeConfig))
      {
        final AS4ErrorMessage aErrorMsg = CreateErrorMessage.createErrorMessage (eSOAPVersion,
                                                                                 MessageHelperMethods.createEbms3MessageInfo (),
                                                                                 aErrorMessages);

        aAS4Response.setContentAndCharset (AS4XMLHelper.serializeXML (aErrorMsg.getAsSOAPDocument ()),
                                           StandardCharsets.UTF_8)
                    .setMimeType (eSOAPVersion.getMimeType ());
      }
      else
        s_aLogger.warn ("Not sending back the error, because sending error response is prohibited in PMode");
    }
    else
    {
      final PModeLeg aEffectiveLeg = aState.getEffectivePModeLeg ();

      final boolean bSendReceiptAsResponse = _isSendReceiptAsResponse (aEffectiveLeg);

      if (aPModeConfig.getMEP ().isOneWay ())
      {
        // If no Error is present check if partners declared if they want a
        // response and if this response should contain non-repudiation
        // information if applicable
        if (bSendReceiptAsResponse)
        {
          final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
          final AS4ReceiptMessage aReceiptMessage = CreateReceiptMessage.createReceiptMessage (eSOAPVersion,
                                                                                               aEbms3MessageInfo,
                                                                                               aUserMessage,
                                                                                               aSOAPDocument,
                                                                                               _isSendNonRepudiationInformation (aPModeConfig))
                                                                        .setMustUnderstand (true);

          // We've got our response
          Document aResponseDoc = aReceiptMessage.getAsSOAPDocument ();

          final PModeLeg aLeg1 = aPModeConfig.getLeg1 ();

          aResponseDoc = _signResponse (aResMgr,
                                        aResponseAttachments,
                                        aLeg1.getSecurity (),
                                        aResponseDoc,
                                        aLeg1.getProtocol ().getSOAPVersion ());

          aAS4Response.setContentAndCharset (AS4XMLHelper.serializeXML (aResponseDoc), StandardCharsets.UTF_8)
                      .setMimeType (eSOAPVersion.getMimeType ());
        }
        else
          s_aLogger.info ("Not sending back the receipt response, because sending receipt response is prohibited in PMode");
      }
      else
        if (aPModeConfig.getMEPBinding ().equals (EMEPBinding.PUSH_PUSH))
        {
          // TODO twowaypushpush
          // Need to save messageid for reftomessageid
          // Basically oneway with messageid save
          aUserMessage.getMessageInfo ().getMessageId ();

        }
        else
        {
          // TWO - WAY
          // Except for two way push push, every response as usermessage is
          // always on leg 2
          final PModeLeg aLeg2 = aPModeConfig.getLeg2 ();
          if (aLeg2 != null)
          {
            if (MEPHelper.isValidResponseType (aPModeConfig.getMEP (),
                                               aPModeConfig.getMEPBinding (),
                                               EAS4MessageType.USER_MESSAGE,
                                               false))
            {

              final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (MessageHelperMethods.createRandomMessageID (),
                                                                                                      aUserMessage.getMessageInfo ()
                                                                                                                  .getMessageId ());
              final Ebms3PayloadInfo aEbms3PayloadInfo = CreateUserMessage.createEbms3PayloadInfo (null,
                                                                                                   aResponseAttachments);

              // Invert from and to role from original user message
              final Ebms3PartyInfo aEbms3PartyInfo = CreateUserMessage.createEbms3PartyInfo (aUserMessage.getPartyInfo ()
                                                                                                         .getTo ()
                                                                                                         .getRole (),
                                                                                             aUserMessage.getPartyInfo ()
                                                                                                         .getTo ()
                                                                                                         .getPartyIdAtIndex (0)
                                                                                                         .getValue (),
                                                                                             aUserMessage.getPartyInfo ()
                                                                                                         .getFrom ()
                                                                                                         .getRole (),
                                                                                             aUserMessage.getPartyInfo ()
                                                                                                         .getFrom ()
                                                                                                         .getPartyIdAtIndex (0)
                                                                                                         .getValue ());

              // Should be exactly the same as incoming message
              final Ebms3CollaborationInfo aEbms3CollaborationInfo = aUserMessage.getCollaborationInfo ();

              // Need to switch C1 and C4 around from the original usermessage
              final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
              Ebms3Property aFinalRecipient = null;
              Ebms3Property aOriginalSender = null;
              for (final Ebms3Property aProp : aUserMessage.getMessageProperties ().getProperty ())
              {
                if (aProp.getName ().equals (CAS4.FINAL_RECIPIENT))
                {
                  aOriginalSender = aProp;
                }
                else
                  if (aProp.getName ().equals (CAS4.ORIGINAL_SENDER))
                  {
                    aFinalRecipient = aProp;
                  }
              }

              aFinalRecipient.setName (CAS4.ORIGINAL_SENDER);
              aOriginalSender.setName (CAS4.FINAL_RECIPIENT);

              aEbms3MessageProperties.addProperty (aFinalRecipient);
              aEbms3MessageProperties.addProperty (aOriginalSender);

              final AS4UserMessage aResponeUserMesage = CreateUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                                             aEbms3PayloadInfo,
                                                                                             aEbms3CollaborationInfo,
                                                                                             aEbms3PartyInfo,
                                                                                             aEbms3MessageProperties,
                                                                                             eSOAPVersion);
              // We've got our response
              Document aResponseDoc = aResponeUserMesage.getAsSOAPDocument ();

              if (aLeg2.getSecurity () != null)
              {
                aResponseDoc = _signResponse (aResMgr,
                                              aResponseAttachments,
                                              aLeg2.getSecurity (),
                                              aResponseDoc,
                                              aLeg2.getProtocol ().getSOAPVersion ());
              }

              if (aResponseAttachments.isNotEmpty ())
              {

                final MimeMessage aMimeMsg = _generateMimeMessageForResponse (aResMgr,
                                                                              aResponseAttachments,
                                                                              aLeg2,
                                                                              aResponseDoc);

                // Move all mime headers to the HTTP request
                final Enumeration <?> aEnum = aMimeMsg.getAllHeaders ();
                while (aEnum.hasMoreElements ())
                {
                  final Header h = (Header) aEnum.nextElement ();
                  // Make a single-line HTTP header value!
                  aAS4Response.addCustomResponseHeader (h.getName (),
                                                        HTTPStringHelper.getUnifiedHTTPHeaderValue (h.getValue ()));

                  // Remove from MIME message!
                  aMimeMsg.removeHeader (h.getName ());
                }

                // send mime with unified response
                aAS4Response.setContent ( () -> {
                  try
                  {
                    return aMimeMsg.getInputStream ();
                  }
                  catch (IOException | MessagingException ex)
                  {
                    throw new IllegalStateException ("Failed to get MIME input stream", ex);
                  }
                }).setMimeType (MT_MULTIPART_RELATED);
              }
              else
              {
                aAS4Response.setContentAndCharset (AS4XMLHelper.serializeXML (aResponseDoc), StandardCharsets.UTF_8)
                            .setMimeType (eSOAPVersion.getMimeType ());
              }
            }
          }
        }
    }
  }

  /**
   * If the PModeLegSecurity has set a Sign and Digest Algorithm the message
   * will be signed, else the message will be returned as it is.
   *
   * @param aResMgr
   * @param aResponseAttachments
   * @param aSecurity
   * @param aDocToBeSigned
   * @param eSOAPVersion
   * @return
   * @throws WSSecurityException
   */
  private Document _signResponse (@Nonnull final AS4ResourceManager aResMgr,
                                  @Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments,
                                  @Nonnull final PModeLegSecurity aSecurity,
                                  @Nonnull final Document aDocToBeSigned,
                                  @Nonnull final ESOAPVersion eSOAPVersion) throws WSSecurityException
  {
    if (aSecurity.getX509SignatureAlgorithm () != null && aSecurity.getX509SignatureHashFunction () != null)
    {
      final SignedMessageCreator aCreator = new SignedMessageCreator ();
      final boolean bMustUnderstand = true;
      return aCreator.createSignedMessage (aDocToBeSigned,
                                           eSOAPVersion,
                                           aResponseAttachments,
                                           aResMgr,
                                           bMustUnderstand,
                                           aSecurity.getX509SignatureAlgorithm (),
                                           aSecurity.getX509SignatureHashFunction ());
    }
    return aDocToBeSigned;
  }

  /**
   * Returns the MimeMessage with encrypted attachment or without depending on
   * what is configured in the pmodeconfig within Leg2.
   *
   * @param aResMgr
   *        ResourceManager needed for the encryption process, needs to be
   *        passed down
   * @param aResponseAttachments
   *        The Attachments that should be encrypted
   * @param aLeg2
   *        Leg2 to get necessary information, EncryptionAlgorithm, SOAPVersion
   * @param aResponseDoc
   *        the document that contains the user message
   * @return a MimeMessage to be sent
   * @throws MessagingException
   * @throws TransformerException
   * @throws TransformerFactoryConfigurationError
   * @throws WSSecurityException
   */
  @Nonnull
  private MimeMessage _generateMimeMessageForResponse (@Nonnull final AS4ResourceManager aResMgr,
                                                       @Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments,
                                                       @Nonnull final PModeLeg aLeg2,
                                                       @Nonnull final Document aResponseDoc) throws WSSecurityException,
                                                                                             TransformerFactoryConfigurationError,
                                                                                             TransformerException,
                                                                                             MessagingException
  {
    MimeMessage aMimeMsg = null;
    if (aLeg2.getSecurity () != null)
    {
      if (aLeg2.getSecurity ().getX509EncryptionAlgorithm () != null)
      {
        final EncryptionCreator aEncryptCreator = new EncryptionCreator ();
        aMimeMsg = aEncryptCreator.encryptMimeMessage (aLeg2.getProtocol ().getSOAPVersion (),
                                                       aResponseDoc,
                                                       true,
                                                       aResponseAttachments,
                                                       aResMgr,
                                                       aLeg2.getSecurity ().getX509EncryptionAlgorithm ());

      }
      else
      {
        aMimeMsg = new MimeMessageCreator (aLeg2.getProtocol ()
                                                .getSOAPVersion ()).generateMimeMessage (aResponseDoc,
                                                                                         aResponseAttachments);
      }
    }
    if (aMimeMsg == null)
      throw new IllegalStateException ("Unexpected");
    return aMimeMsg;
  }

  /**
   * EBMS core specification 4.2 details these default values. In eSENS they get
   * used to implement a ping service, we took this over even outside of eSENS.
   * If you use these default values you can try to "ping" the server, the
   * method just checks if the pmode got these exact values set. If true, no SPI
   * processing is done.
   *
   * @param aPModeConfig
   *        to check
   * @return true if the default values to ping are not used else false
   */
  private static boolean _isNotPingMessage (@Nonnull final IPModeConfig aPModeConfig)
  {
    final PModeLegBusinessInformation aBInfo = aPModeConfig.getLeg1 ().getBusinessInfo ();

    if (aBInfo != null &&
        CAS4.DEFAULT_ACTION_URL.equals (aBInfo.getAction ()) &&
        CAS4.DEFAULT_SERVICE_URL.equals (aBInfo.getService ()))
    {
      return false;
    }
    return true;
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
  private static boolean _isSendErrorAsResponse (@Nullable final IPModeConfig aPModeConfig)
  {
    if (aPModeConfig != null)
      if (aPModeConfig.getLeg1 () != null)
        if (aPModeConfig.getLeg1 ().getErrorHandling () != null)
          if (aPModeConfig.getLeg1 ().getErrorHandling ().isReportAsResponseDefined ())
          {
            // Note: this is enabled in Default PMode
            return aPModeConfig.getLeg1 ().getErrorHandling ().isReportAsResponse ();
          }
    // Default behavior
    return true;
  }

  /**
   * Checks if a ReceiptReplyPattern is set to Response or not.
   *
   * @param aPLeg
   *        to PMode leg to use. May not be <code>null</code>.
   * @return Returns the value if set, else DEFAULT <code>TRUE</code>.
   */
  private static boolean _isSendReceiptAsResponse (@Nonnull final PModeLeg aLeg)
  {
    if (aLeg != null)
      if (aLeg.getSecurity () != null)
      {
        // Note: this is enabled in Default PMode
        return EPModeSendReceiptReplyPattern.RESPONSE.equals (aLeg.getSecurity ().getSendReceiptReplyPattern ());
      }
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
   * @throws BadRequestException
   *         on error
   */
  private static void _checkPropertiesOrignalSenderAndFinalRecipient (@Nonnull final List <Ebms3Property> aPropertyList) throws BadRequestException
  {
    String sOriginalSenderC1 = null;
    String sFinalRecipientC4 = null;

    for (final Ebms3Property sProperty : aPropertyList)
    {
      if (sProperty.getName ().equals (CAS4.ORIGINAL_SENDER))
        sOriginalSenderC1 = sProperty.getValue ();
      else
        if (sProperty.getName ().equals (CAS4.FINAL_RECIPIENT))
          sFinalRecipientC4 = sProperty.getValue ();
    }

    if (StringHelper.hasNoText (sOriginalSenderC1))
      throw new BadRequestException (CAS4.ORIGINAL_SENDER + " property is empty or not existant but mandatory");
    if (StringHelper.hasNoText (sFinalRecipientC4))
      throw new BadRequestException (CAS4.FINAL_RECIPIENT + " property is empty or not existant but mandatory");
  }

  /**
   * Creates a PMode if it does not exist already.
   *
   * @param aState
   *        needed to get Responder and Initiator
   * @param sConfigID
   *        needed to get the PModeConfig for the PMode
   * @param aPartyInfo
   *        Party information from user message, needed to get full information
   *        of the Initiator and Responder
   */
  private static void _ensurePModeIsPresent (@Nonnull final IAS4MessageState aState,
                                             @Nonnull final String sConfigID,
                                             @Nonnull final Ebms3PartyInfo aPartyInfo)
  {
    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    if (aPModeMgr.containsNone (doesPartnerAndPartnerExist (aState.getInitiatorID (),
                                                            aState.getResponderID (),
                                                            sConfigID)))
    {
      final Ebms3From aFrom = aPartyInfo.getFrom ();
      final Ebms3PartyId aFromID = aFrom.getPartyIdAtIndex (0);
      final Ebms3To aTo = aPartyInfo.getTo ();
      final Ebms3PartyId aToID = aTo.getPartyIdAtIndex (0);
      final IPModeConfig aPModeConfig = MetaAS4Manager.getPModeConfigMgr ().getPModeConfigOfID (sConfigID);
      final PMode aPMode = new PMode (new PModeParty (aFromID.getType (),
                                                      aFromID.getValue (),
                                                      aFrom.getRole (),
                                                      null,
                                                      null),
                                      new PModeParty (aToID.getType (), aToID.getValue (), aTo.getRole (), null, null),
                                      aPModeConfig);
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

  @Nonnull
  private InputStream _getRequestIS (@Nonnull final HttpServletRequest aHttpServletRequest) throws IOException
  {
    final InputStream aIS = aHttpServletRequest.getInputStream ();
    return aIS;
  }

  @Override
  protected void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final AS4Response aHttpResponse = (AS4Response) aUnifiedResponse;
    final HttpServletRequest aHttpServletRequest = aRequestScope.getRequest ();

    // TODO make locale dynamic
    final Locale aLocale = Locale.US;

    // XXX By default login in admin user; why do we need a logged-in user?
    final ELoginResult e = LoggedInUserManager.getInstance ().loginUser (CSecurity.USER_ADMINISTRATOR_LOGIN,
                                                                         CSecurity.USER_ADMINISTRATOR_PASSWORD);
    assert e.isSuccess () : "Login failed: " + e.toString ();

    try (final AS4ResourceManager aResMgr = new AS4ResourceManager ())
    {
      // Determine content type
      final String sContentType = aHttpServletRequest.getContentType ();
      if (StringHelper.hasNoText (sContentType))
        throw new BadRequestException ("Content-Type header is missing");

      final MimeType aContentType = MimeTypeParser.parseMimeType (sContentType);
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Received Content-Type: " + aContentType);
      if (aContentType == null)
        throw new BadRequestException ("Failed to parse Content-Type '" + sContentType + "'");

      Document aSOAPDocument = null;
      ESOAPVersion eSOAPVersion = null;
      final ICommonsList <WSS4JAttachment> aIncomingAttachments = new CommonsArrayList<> ();

      final IMimeType aPlainContentType = aContentType.getCopyWithoutParameters ();
      if (aPlainContentType.equals (MT_MULTIPART_RELATED))
      {
        // MIME message
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Received MIME message");

        final String sBoundary = aContentType.getParameterValueWithName ("boundary");
        if (StringHelper.hasNoText (sBoundary))
        {
          throw new BadRequestException ("Content-Type '" +
                                         aHttpServletRequest.getContentType () +
                                         "' misses boundary parameter");
        }

        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("MIME Boundary = " + sBoundary);

        // PARSING MIME Message via MultiPartStream
        final MultipartStream aMulti = new MultipartStream (_getRequestIS (aHttpServletRequest),
                                                            sBoundary.getBytes (StandardCharsets.ISO_8859_1),
                                                            (MultipartProgressNotifier) null);
        final IIncomingAttachmentFactory aIAF = AS4ServerSettings.getIncomingAttachmentFactory ();

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
            eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (), x -> aPlainPartMT.equals (x.getMimeType ()));

            // Read SOAP document
            aSOAPDocument = DOMReader.readXMLDOM (aBodyPart.getInputStream ());
          }
          else
          {
            // MIME Attachment (index is gt 0)
            final WSS4JAttachment aAttachment = aIAF.createAttachment (aBodyPart, aResMgr);
            aIncomingAttachments.add (aAttachment);
          }
          nIndex++;
        }
      }
      else
      {
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Received plain message with Content-Type " + aContentType.getAsString ());

        // Expect plain SOAP - read whole request to DOM
        // Note: this may require a huge amount of memory for large requests
        aSOAPDocument = DOMReader.readXMLDOM (_getRequestIS (aHttpServletRequest));

        // Determine SOAP version from content type
        eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (), x -> aPlainContentType.equals (x.getMimeType ()));
      }

      if (aSOAPDocument == null)
      {
        // We don't have a SOAP document
        throw new BadRequestException (eSOAPVersion == null ? "Failed to parse incoming message!"
                                                            : "Failed to parse incoming SOAP " +
                                                              eSOAPVersion.getVersion () +
                                                              " document!");
      }

      if (eSOAPVersion == null)
      {
        // Determine SOAP version from namespace URI of read document as the
        // last fallback
        final String sNamespaceURI = XMLHelper.getNamespaceURI (aSOAPDocument);
        eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (), x -> x.getNamespaceURI ().equals (sNamespaceURI));
        if (eSOAPVersion == null)
          throw new BadRequestException ("Failed to determine SOAP version from XML document!");
      }

      // SOAP document and SOAP version are determined
      _handleSOAPMessage (aResMgr, aSOAPDocument, eSOAPVersion, aIncomingAttachments, aHttpResponse, aLocale);
    }
    catch (final BadRequestException ex)
    {
      aHttpResponse.setResponseError (HttpServletResponse.SC_BAD_REQUEST, ex.getMessage (), ex.getCause ());
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
