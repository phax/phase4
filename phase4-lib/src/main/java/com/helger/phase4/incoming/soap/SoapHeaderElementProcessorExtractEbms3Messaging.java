/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.incoming.soap;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.charset.CharsetHelper;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.error.IError;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.ebms3header.Ebms3PartInfo;
import com.helger.phase4.ebms3header.Ebms3PartyId;
import com.helger.phase4.ebms3header.Ebms3PayloadInfo;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3PullRequest;
import com.helger.phase4.ebms3header.Ebms3Receipt;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.AS4IncomingMessageState;
import com.helger.phase4.incoming.IAS4IncomingReceiverConfiguration;
import com.helger.phase4.incoming.mgr.AS4IncomingPullRequestProcessorManager;
import com.helger.phase4.incoming.spi.IAS4IncomingPullRequestProcessorSPI;
import com.helger.phase4.marshaller.Ebms3MessagingMarshaller;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.model.mpc.IMPC;
import com.helger.phase4.model.mpc.IMPCManager;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.resolve.IAS4PModeResolver;
import com.helger.xml.XMLHelper;

/**
 * This class manages the EBMS Messaging SOAP header element
 *
 * @author Philip Helger
 * @author bayerlma
 * @author Gregor Scholtysik
 */
public class SoapHeaderElementProcessorExtractEbms3Messaging implements ISoapHeaderElementProcessor
{
  /** The QName for which this processor should be invoked */
  public static final QName QNAME_MESSAGING = new QName ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/",
                                                         "Messaging");

  private static final Logger LOGGER = LoggerFactory.getLogger (SoapHeaderElementProcessorExtractEbms3Messaging.class);

  private final IAS4PModeResolver m_aPModeResolver;
  private final Consumer <? super IPMode> m_aPModeConsumer;
  private final IAS4IncomingReceiverConfiguration m_aIncomingReceiverConfiguration;

  /**
   * Ctor
   *
   * @param aPModeResolver
   *        The PMode resolver to be used. May not be <code>null</code>.
   * @param aPModeConsumer
   *        An optional consumer that is invoked every time a PMode was
   *        successfully resolved. May be <code>null</code>.
   * @param aIRC
   *        The incoming receiver configuration. May not be <code>null</code>.
   *        Since v3.0.0.
   */
  public SoapHeaderElementProcessorExtractEbms3Messaging (@Nonnull final IAS4PModeResolver aPModeResolver,
                                                          @Nullable final Consumer <? super IPMode> aPModeConsumer,
                                                          @Nonnull final IAS4IncomingReceiverConfiguration aIRC)
  {
    ValueEnforcer.notNull (aPModeResolver, "PModeResolver");
    m_aPModeResolver = aPModeResolver;
    m_aPModeConsumer = aPModeConsumer;
    m_aIncomingReceiverConfiguration = aIRC;
  }

  /**
   * Checks if Leg1 should be used or not.
   *
   * @param aUserMessage
   *        needed to get the message ids
   * @return true if leg1 should be used else false
   */
  private static boolean _isUseLeg1 (@Nonnull final Ebms3UserMessage aUserMessage)
  {
    final String sThisMessageID = aUserMessage.getMessageInfo ().getMessageId ();
    final String sRefToMessageID = aUserMessage.getMessageInfo ().getRefToMessageId ();

    if (StringHelper.hasText (sRefToMessageID))
      if (sThisMessageID.equals (sRefToMessageID))
        LOGGER.warn ("MessageID and ReferenceToMessageID are the same (" + sThisMessageID + ")!");

    // If the message has a non-empty reference to a previous message, and this
    // reference differs from this message's ID, than leg 2 should be used
    return StringHelper.hasNoText (sRefToMessageID) || sRefToMessageID.equals (sThisMessageID);
  }

  /**
   * Determines the effective MPCID.
   *
   * @param aUserMessage
   *        to get the MPC that is used
   * @param aPModeLeg
   *        to get the MPC that will be used if the message does not define one
   * @return the MPCID
   */
  @Nullable
  private static String _getMPCIDOfUserMsg (@Nonnull final Ebms3UserMessage aUserMessage,
                                            @Nonnull final PModeLeg aPModeLeg)
  {
    String sEffectiveMPCID = aUserMessage.getMpc ();
    if (sEffectiveMPCID == null)
    {
      if (aPModeLeg.getBusinessInfo () != null)
        sEffectiveMPCID = aPModeLeg.getBusinessInfo ().getMPCID ();
    }
    return sEffectiveMPCID;
  }

  /**
   * Checks if the MPC that is contained in the PMode is valid.
   *
   * @param aPModeLeg
   *        the leg to get the MPCID
   * @param aMPCMgr
   *        the MPC-Manager to search for the MPCID in the persisted data
   * @param aLocale
   *        Locale to be used for the error messages
   * @param aProcessingErrorMessagesTarget
   *        to write errors to if they occur
   * @return Success if everything is all right, else Failure
   */
  @Nonnull
  private static ESuccess _checkMPCOfPMode (@Nonnull final PModeLeg aPModeLeg,
                                            @Nonnull final IMPCManager aMPCMgr,
                                            @Nonnull final Locale aLocale,
                                            @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessagesTarget)
  {
    // Check if MPC is contained in PMode and if so, if it is valid
    if (aPModeLeg.getBusinessInfo () != null)
    {
      final String sPModeMPC = aPModeLeg.getBusinessInfo ().getMPCID ();
      if (sPModeMPC != null && !aMPCMgr.containsWithID (sPModeMPC))
      {
        final String sDetails = "Error processing the usermessage, PMode-MPC ID '" + sPModeMPC + "' is invalid!";
        LOGGER.error (sDetails);
        aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.errorBuilder (aLocale)
                                                                                    .errorDetail (sDetails)
                                                                                    .build ());
        return ESuccess.FAILURE;
      }
    }
    return ESuccess.SUCCESS;
  }

  /**
   * Checks if the Document has a SOAPBodyPayload.
   *
   * @param aPModeLeg
   *        to get the SOAPVersion
   * @param aSoapDoc
   *        the document that should be checked if it contains a SOAPBodyPayload
   * @return true if it contains a SOAPBodyPayload else false
   */
  private static boolean _checkSoapBodyHasPayload (@Nonnull final PModeLeg aPModeLeg, @Nonnull final Document aSoapDoc)
  {
    // Check if a SOAP-Body payload exists
    final Element aBody = XMLHelper.getFirstChildElementOfName (aSoapDoc.getFirstChild (),
                                                                aPModeLeg.getProtocol ()
                                                                         .getSoapVersion ()
                                                                         .getBodyElementName ());
    return aBody != null && aBody.hasChildNodes ();
  }

  private void _notifyPModeResolved (@Nonnull final IPMode aPMode)
  {
    if (m_aPModeConsumer != null)
      m_aPModeConsumer.accept (aPMode);
  }

  @Nonnull
  public ESuccess processHeaderElement (@Nonnull final Document aSoapDoc,
                                        @Nonnull final Element aElement,
                                        @Nonnull final ICommonsList <WSS4JAttachment> aAttachments,
                                        @Nonnull final AS4IncomingMessageState aIncomingState,
                                        @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessagesTarget)
  {
    final IMPCManager aMPCMgr = MetaAS4Manager.getMPCMgr ();
    IPMode aPMode = null;
    final ICommonsMap <String, EAS4CompressionMode> aCompressionAttachmentIDs = new CommonsHashMap <> ();
    IMPC aEffectiveMPC = null;
    String sInitiatorID = null;
    String sResponderID = null;
    final Locale aLocale = aIncomingState.getLocale ();

    // Parse EBMS3 Messaging object
    final ErrorList aErrorList = new ErrorList ();
    final Ebms3Messaging aMessaging = new Ebms3MessagingMarshaller ().setCollectErrors (aErrorList).read (aElement);

    // If the ebms3reader above fails aMessaging will be null => invalid/not
    // wellformed
    if (aMessaging == null || aErrorList.containsAtLeastOneError ())
    {
      // Errorcode/Id would be null => not conform with Ebms3ErrorMessage since
      // the message always needs a errorcode =>
      // Invalid Header == not wellformed/invalid xml
      for (final IError aError : aErrorList)
      {
        final String sDetails = "Header error: " + aError.getAsString (aLocale);
        LOGGER.error (sDetails);
        // Clone the error and add an error ID
        aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_INVALID_HEADER.errorBuilder (aLocale)
                                                                          .errorDetail (sDetails,
                                                                                        aError.getLinkedException ())
                                                                          .build ());
      }
      return ESuccess.FAILURE;
    }

    // Remember in state
    aIncomingState.setMessaging (aMessaging);

    // 0 or 1 are allowed
    final int nUserMessages = aMessaging.getUserMessageCount ();
    if (nUserMessages > 1)
    {
      final String sDetails = "Too many UserMessage objects (" + nUserMessages + ") contained.";
      LOGGER.error (sDetails);
      aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                            .errorDetail (sDetails)
                                                                            .build ());
      return ESuccess.FAILURE;
    }

    // 0 or 1 are allowed
    final int nSignalMessages = aMessaging.getSignalMessageCount ();
    if (nSignalMessages > 1)
    {
      final String sDetails = "Too many SignalMessage objects (" + nSignalMessages + ") contained.";
      LOGGER.error (sDetails);
      aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                            .errorDetail (sDetails)
                                                                            .build ());
      return ESuccess.FAILURE;
    }

    if (nUserMessages + nSignalMessages == 0)
    {
      // No Message was found
      final String sDetails = "Neither UserMessage nor SignalMessage object contained.";
      LOGGER.error (sDetails);
      aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                            .errorDetail (sDetails)
                                                                            .build ());
      return ESuccess.FAILURE;
    }

    // Check if the usermessage has a PMode in the collaboration info
    final Ebms3UserMessage aUserMessage = CollectionHelper.getAtIndex (aMessaging.getUserMessage (), 0);
    if (aUserMessage != null)
    {
      final Ebms3MessageInfo aMsgInfo = aUserMessage.getMessageInfo ();
      if (aMsgInfo != null)
      {
        // Set this is as early as possible, so that eventually occurring error
        // messages can use the "RefToMessageId" element properly
        aIncomingState.setMessageID (aMsgInfo.getMessageId ());
        aIncomingState.setRefToMessageID (aMsgInfo.getRefToMessageId ());
        aIncomingState.setMessageTimestamp (aMsgInfo.getTimestamp ());
      }

      // PartyInfo is mandatory in UserMessage
      // From is mandatory in PartyInfo
      final List <Ebms3PartyId> aFromPartyIdList = aUserMessage.getPartyInfo ().getFrom ().getPartyId ();
      if (aFromPartyIdList.size () > 1)
      {
        final String sDetails = "More than one PartyId (" +
                                aFromPartyIdList.size () +
                                ") is contained in From-Recipient please check the message.";
        LOGGER.error (sDetails);
        aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                              .errorDetail (sDetails)
                                                                              .build ());
        return ESuccess.FAILURE;
      }

      // To is mandatory in PartyInfo
      final List <Ebms3PartyId> aToPartyIdList = aUserMessage.getPartyInfo ().getTo ().getPartyId ();
      if (aToPartyIdList.size () > 1)
      {
        final String sDetails = "More than one PartyId (" +
                                aToPartyIdList.size () +
                                ") is contained in To-Recipient please check the message.";
        LOGGER.error (sDetails);
        aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                              .errorDetail (sDetails)
                                                                              .build ());
        return ESuccess.FAILURE;
      }

      // Setting Initiator and Responder id, Required values or else xsd will
      // throw an error
      sInitiatorID = aFromPartyIdList.get (0).getValue ();
      sResponderID = aToPartyIdList.get (0).getValue ();

      final Ebms3CollaborationInfo aCollaborationInfo = aUserMessage.getCollaborationInfo ();
      if (aCollaborationInfo != null)
      {
        // Find PMode
        String sPModeID = null;
        final String sService = aCollaborationInfo.getService ().getValue ();
        final String sAction = aCollaborationInfo.getAction ();
        String sAgreementRef = null;
        if (aCollaborationInfo.getAgreementRef () != null)
        {
          sPModeID = aCollaborationInfo.getAgreementRef ().getPmode ();
          sAgreementRef = aCollaborationInfo.getAgreementRef ().getValue ();
        }

        // Get responder address
        final String sAddress = m_aIncomingReceiverConfiguration.getReceiverEndpointAddress ();

        aPMode = m_aPModeResolver.findPMode (sPModeID,
                                             sService,
                                             sAction,
                                             sInitiatorID,
                                             sResponderID,
                                             sAgreementRef,
                                             sAddress);

        // Should be screened by the XSD conversion already
        if (aPMode == null)
        {
          final String sDetails = "Failed to resolve PMode '" +
                                  sPModeID +
                                  "' / '" +
                                  sService +
                                  "' / '" +
                                  sAction +
                                  "' / '" +
                                  sInitiatorID +
                                  "' / '" +
                                  sResponderID +
                                  "' / '" +
                                  sAgreementRef +
                                  "' / '" +
                                  sAddress +
                                  "' using resolver " +
                                  m_aPModeResolver;
          LOGGER.error (sDetails);
          aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.errorBuilder (aLocale)
                                                                                      .errorDetail (sDetails)
                                                                                      .build ());
          return ESuccess.FAILURE;
        }

        _notifyPModeResolved (aPMode);
      }

      // to use the configuration for leg2
      PModeLeg aPModeLeg1 = null;
      PModeLeg aPModeLeg2 = null;

      // Needed for the compression check: it is not allowed to have a
      // compressed attachment and a SOAPBodyPayload
      boolean bHasSoapBodyPayload = false;

      // PMode may be null if no CollaborationInfo is present
      if (aPMode != null)
      {
        aPModeLeg1 = aPMode.getLeg1 ();
        aPModeLeg2 = aPMode.getLeg2 ();

        // if the two - way is selected, check if it requires two legs and if
        // both are present
        if (aPMode.getMEPBinding ().getRequiredLegs () == 2 && aPModeLeg2 == null)
        {
          final String sDetails = "Error processing the UserMessage, PMode does not contain leg 2.";
          LOGGER.error (sDetails);
          aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.errorBuilder (aLocale)
                                                                                      .errorDetail (sDetails)
                                                                                      .build ());
          return ESuccess.FAILURE;
        }

        final boolean bUseLeg1 = _isUseLeg1 (aUserMessage);
        final PModeLeg aEffectiveLeg = bUseLeg1 ? aPModeLeg1 : aPModeLeg2;
        final int nLegNum = bUseLeg1 ? 1 : 2;
        if (aEffectiveLeg == null)
        {
          final String sDetails = "Error processing the UserMessage, PMode does not contain effective leg " +
                                  nLegNum +
                                  ".";
          LOGGER.error (sDetails);
          aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.errorBuilder (aLocale)
                                                                                      .errorDetail (sDetails)
                                                                                      .build ());
          return ESuccess.FAILURE;
        }

        aIncomingState.setEffectivePModeLeg (nLegNum, aEffectiveLeg);
        if (_checkMPCOfPMode (aEffectiveLeg, aMPCMgr, aLocale, aProcessingErrorMessagesTarget).isFailure ())
          return ESuccess.FAILURE;

        bHasSoapBodyPayload = _checkSoapBodyHasPayload (aEffectiveLeg, aSoapDoc);
        final String sEffectiveMPCID = _getMPCIDOfUserMsg (aUserMessage, aEffectiveLeg);

        // PMode is valid
        // Now Check if MPC valid
        aEffectiveMPC = aMPCMgr.getMPCOrDefaultOfID (sEffectiveMPCID);
        if (aEffectiveMPC == null)
        {
          final String sDetails = "Error processing the UserMessage, effective MPC ID '" +
                                  sEffectiveMPCID +
                                  "' is unknown!";
          LOGGER.error (sDetails);
          aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                                .errorDetail (sDetails)
                                                                                .build ());
          return ESuccess.FAILURE;
        }
      }

      // Remember in state
      aIncomingState.setSoapBodyPayloadPresent (bHasSoapBodyPayload);

      final Ebms3PayloadInfo aEbms3PayloadInfo = aUserMessage.getPayloadInfo ();
      if (aEbms3PayloadInfo == null || aEbms3PayloadInfo.getPartInfo ().isEmpty ())
      {
        if (bHasSoapBodyPayload)
        {
          final String sDetails = "No PayloadInfo/PartInfo is specified, so no SOAP body payload is allowed.";
          LOGGER.error (sDetails);
          aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                                .errorDetail (sDetails)
                                                                                .build ());
          return ESuccess.FAILURE;
        }

        // For the case that there is no Payload/Part - Info but still
        // attachments in the message
        if (aAttachments.isNotEmpty ())
        {
          final String sDetails = "No PayloadInfo/PartInfo is specified, so no attachments are allowed.";
          LOGGER.error (sDetails);
          aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.errorBuilder (aLocale)
                                                                                    .errorDetail (sDetails)
                                                                                    .build ());
          return ESuccess.FAILURE;
        }
      }
      else
      {
        // Check if there are more Attachments then specified
        if (aAttachments.size () > aEbms3PayloadInfo.getPartInfoCount ())
        {
          final String sDetails = "Error processing the UserMessage, the amount of specified attachments does not correlate with the actual attachments in the UserMessage. Expected " +
                                  aEbms3PayloadInfo.getPartInfoCount () +
                                  " but having " +
                                  aAttachments.size () +
                                  " attachments.";
          LOGGER.error (sDetails);
          aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.errorBuilder (aLocale)
                                                                                    .errorDetail (sDetails)
                                                                                    .build ());
          return ESuccess.FAILURE;
        }

        int nSpecifiedAttachments = 0;

        for (final Ebms3PartInfo aPartInfo : aEbms3PayloadInfo.getPartInfo ())
        {
          // If href is null or empty there has to be a SOAP Payload
          if (StringHelper.hasNoText (aPartInfo.getHref ()))
          {
            // Check if there is a BodyPayload as specified in the UserMessage
            if (!bHasSoapBodyPayload)
            {
              final String sDetails = "Error processing the UserMessage. Expected a SOAP body payload but there is none present.";
              LOGGER.error (sDetails);
              aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                                    .errorDetail (sDetails)
                                                                                    .build ());
              return ESuccess.FAILURE;
            }
          }
          else
          {
            // Attachment
            // To check attachments which are specified in the usermessage and
            // the real amount in the mime message
            nSpecifiedAttachments++;

            final String sAttachmentID = StringHelper.trimStart (aPartInfo.getHref (), MessageHelperMethods.PREFIX_CID);
            final WSS4JAttachment aIncomingAttachment = aAttachments.findFirst (x -> EqualsHelper.equals (x.getId (),
                                                                                                          sAttachmentID));
            if (aIncomingAttachment == null)
            {
              LOGGER.warn ("Failed to resolve MIME attachment '" +
                           sAttachmentID +
                           "' in list of " +
                           aAttachments.getAllMapped (WSS4JAttachment::getId));
            }

            boolean bMimeTypePresent = false;
            boolean bCompressionTypePresent = false;

            if (aPartInfo.getPartProperties () != null)
              for (final Ebms3Property aEbms3Property : aPartInfo.getPartProperties ().getProperty ())
              {
                final String sPropertyName = aEbms3Property.getName ();
                final String sPropertyValue = aEbms3Property.getValue ();

                if (sPropertyName.equalsIgnoreCase (MessageHelperMethods.PART_PROPERTY_MIME_TYPE))
                {
                  bMimeTypePresent = StringHelper.hasText (sPropertyValue);
                }
                else
                  if (sPropertyName.equalsIgnoreCase (MessageHelperMethods.PART_PROPERTY_COMPRESSION_TYPE))
                  {
                    // Only needed check here since AS4 does not support another
                    // CompressionType
                    // http://wiki.ds.unipi.gr/display/ESENS/PR+-+AS4
                    final EAS4CompressionMode eCompressionMode = EAS4CompressionMode.getFromMimeTypeStringOrNull (sPropertyValue);
                    if (eCompressionMode == null)
                    {
                      final String sDetails = "Error processing the UserMessage, CompressionType '" +
                                              sPropertyValue +
                                              "' of attachment '" +
                                              sAttachmentID +
                                              "' is not supported.";
                      LOGGER.error (sDetails);
                      aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                                            .errorDetail (sDetails)
                                                                                            .build ());
                      return ESuccess.FAILURE;
                    }

                    aCompressionAttachmentIDs.put (sAttachmentID, eCompressionMode);
                    bCompressionTypePresent = true;
                  }
                  else
                    if (sPropertyName.equalsIgnoreCase (MessageHelperMethods.PART_PROPERTY_CHARACTER_SET))
                    {
                      if (StringHelper.hasText (sPropertyValue))
                      {
                        final Charset aCharset = CharsetHelper.getCharsetFromNameOrNull (sPropertyValue);
                        if (aCharset == null)
                        {
                          final String sDetails = "Value '" +
                                                  sPropertyValue +
                                                  "' of property '" +
                                                  MessageHelperMethods.PART_PROPERTY_CHARACTER_SET +
                                                  "' of attachment '" +
                                                  sAttachmentID +
                                                  "' is not supported";
                          LOGGER.error (sDetails);
                          aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                                                .errorDetail (sDetails)
                                                                                                .build ());
                          return ESuccess.FAILURE;
                        }
                        else
                          if (aIncomingAttachment != null)
                            aIncomingAttachment.setCharset (aCharset);
                      }
                    }
                // else we don't care about the property
              }

            // if a compressiontype is present there has to be a mimetype
            // present, to specify what mimetype the attachment was before it
            // got compressed
            if (bCompressionTypePresent && !bMimeTypePresent)
            {
              final String sDetails = "Error processing the UserMessage, MimeType for a compressed attachment ('" +
                                      sAttachmentID +
                                      "') is not present.";
              LOGGER.error (sDetails);
              aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                                    .errorDetail (sDetails)
                                                                                    .build ());
              return ESuccess.FAILURE;
            }
          }
        }

        // If Usermessage/PartInfo specified attachments and MIME attachment
        // differ throw an error
        // This may also be an indicator for "external payloads"
        if (nSpecifiedAttachments != aAttachments.size ())
        {
          final String sDetails = "Error processing the UserMessage: the amount of specified attachments does not correlate with the actual attachments in the UserMessage. Expected " +
                                  aEbms3PayloadInfo.getPartInfoCount () +
                                  " but having " +
                                  aAttachments.size () +
                                  " attachments. This is an indicator, that an external attached was provided.";
          LOGGER.error (sDetails);
          aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.errorBuilder (aLocale)
                                                                                    .errorDetail (sDetails)
                                                                                    .build ());
          return ESuccess.FAILURE;
        }
      }
    }
    else
    {
      // Must be a SignalMessage
      // all vars stay null
      final Ebms3SignalMessage aSignalMessage = aMessaging.getSignalMessageAtIndex (0);

      final Ebms3MessageInfo aMsgInfo = aSignalMessage.getMessageInfo ();
      if (aMsgInfo != null)
      {
        // Set this is as early as possible, so that eventually occurring error
        // messages can use the "RefToMessageId" element properly
        aIncomingState.setMessageID (aMsgInfo.getMessageId ());
        aIncomingState.setRefToMessageID (aMsgInfo.getRefToMessageId ());
        aIncomingState.setMessageTimestamp (aMsgInfo.getTimestamp ());
      }

      final Ebms3PullRequest aEbms3PullRequest = aSignalMessage.getPullRequest ();
      final Ebms3Receipt aEbms3Receipt = aSignalMessage.getReceipt ();
      if (aEbms3PullRequest != null)
      {
        final String sMPC = aEbms3PullRequest.getMpc ();
        final IMPC aMPC = aMPCMgr.getMPCOfID (sMPC);
        if (aMPC == null)
        {
          // Return value not recognized when MPC is not currently saved
          final String sDetails = "Failed to resolve the PullRequest MPC '" + sMPC + "'";
          LOGGER.error (sDetails);
          aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_NOT_RECOGNIZED.errorBuilder (aLocale)
                                                                                  .errorDetail (sDetails)
                                                                                  .build ());
          return ESuccess.FAILURE;
        }

        // Create SPI which returns a PMode
        for (final IAS4IncomingPullRequestProcessorSPI aProcessor : AS4IncomingPullRequestProcessorManager.getAllProcessors ())
        {
          aPMode = aProcessor.findPMode (aSignalMessage);
          if (aPMode != null)
          {
            LOGGER.info ("Found PMode '" +
                         aPMode.getID () +
                         "' for MPC '" +
                         sMPC +
                         "' in SignalMessage " +
                         aSignalMessage);

            _notifyPModeResolved (aPMode);
            break;
          }
        }

        if (aPMode == null)
        {
          final String sDetails = "Failed to resolve PMode for PullRequest with MPC '" + sMPC + "'";
          LOGGER.error (sDetails);
          aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_NOT_RECOGNIZED.errorBuilder (aLocale)
                                                                                  .errorDetail (sDetails)
                                                                                  .build ());
          return ESuccess.FAILURE;
        }
      }
      else
        if (aEbms3Receipt != null)
        {
          final String sRefToMessageID = aSignalMessage.getMessageInfo ().getRefToMessageId ();
          if (StringHelper.hasNoText (sRefToMessageID))
          {
            final String sDetails = "The Receipt does not contain a RefToMessageId";
            LOGGER.error (sDetails);
            aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_INVALID_RECEIPT.errorBuilder (aLocale)
                                                                               .errorDetail (sDetails)
                                                                               .build ());
            return ESuccess.FAILURE;
          }
        }
        else
        {
          // Error Message
          if (!aSignalMessage.getError ().isEmpty ())
          {
            for (final Ebms3Error aError : aSignalMessage.getError ())
            {
              /*
               * Ebms 3 spec 6.2.6: This OPTIONAL attribute indicates the
               * MessageId of the message in error, for which this error is
               * raised.
               */
              if (false)
                if (StringHelper.hasNoText (aError.getRefToMessageInError ()))
                {
                  final String sDetails = "The Error does not contain a RefToMessageInError";
                  LOGGER.error (sDetails);
                  aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_INCONSISTENT.errorBuilder (aLocale)
                                                                                        .errorDetail (sDetails)
                                                                                        .build ());
                  return ESuccess.FAILURE;
                }
            }
          }
        }
    }

    // Remember in state
    aIncomingState.setPMode (aPMode);
    aIncomingState.setOriginalSoapDocument (aSoapDoc);
    aIncomingState.setOriginalAttachments (aAttachments);
    aIncomingState.setCompressedAttachmentIDs (aCompressionAttachmentIDs);
    aIncomingState.setMPC (aEffectiveMPC);
    aIncomingState.setInitiatorID (sInitiatorID);
    aIncomingState.setResponderID (sResponderID);

    return ESuccess.SUCCESS;
  }
}
