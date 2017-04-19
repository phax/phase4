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
package com.helger.as4.servlet.soap;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.error.EEbmsError;
import com.helger.as4.marshaller.Ebms3ReaderBuilder;
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.mpc.IMPC;
import com.helger.as4.model.mpc.MPCManager;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.servlet.AS4MessageState;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.servlet.mgr.AS4ServerSettings;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3PartInfo;
import com.helger.as4lib.ebms3header.Ebms3PartyId;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.error.IError;
import com.helger.commons.error.SingleError;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.jaxb.validation.CollectingValidationEventHandler;
import com.helger.xml.XMLHelper;

/**
 * This class manages the EBMS Messaging SOAP header element
 *
 * @author Philip Helger
 * @author bayerlma
 */
public final class SOAPHeaderElementProcessorExtractEbms3Messaging implements ISOAPHeaderElementProcessor
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SOAPHeaderElementProcessorExtractEbms3Messaging.class);

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
        s_aLogger.warn ("MessageID and ReferenceToMessageID are the same!");

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
  private static String _getMPC (@Nonnull final Ebms3UserMessage aUserMessage, @Nonnull final PModeLeg aPModeLeg)
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
   * @param aErrorList
   *        to write erros to if they occur
   * @param aLocale
   *        Locale to be used for the error messages
   * @param aMPCMgr
   *        the MPC-Manager to search for the MPCID in the persisted data
   * @param aPModeLeg
   *        the leg to get the MPCID
   * @return Success if everything is all right, else Failure
   */
  @Nonnull
  private static ESuccess _checkMPC (@Nonnull final ErrorList aErrorList,
                                     @Nonnull final Locale aLocale,
                                     @Nonnull final MPCManager aMPCMgr,
                                     @Nonnull final PModeLeg aPModeLeg)
  {
    // Check if MPC is contained in PMode and if so, if it is valid
    if (aPModeLeg != null)
    {
      if (aPModeLeg.getBusinessInfo () != null)
      {
        final String sPModeMPC = aPModeLeg.getBusinessInfo ().getMPCID ();
        if (sPModeMPC != null)
          if (!aMPCMgr.containsWithID (sPModeMPC))
          {
            s_aLogger.warn ("Error processing the usermessage, PMode-MPC ID '" + sPModeMPC + "' is invalid!");

            aErrorList.add (EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getAsError (aLocale));
            return ESuccess.FAILURE;
          }
      }
    }
    else
    {
      s_aLogger.warn ("Error processing the usermessage, PMode does not contain a leg!");

      aErrorList.add (EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getAsError (aLocale));
      return ESuccess.FAILURE;
    }
    return ESuccess.SUCCESS;
  }

  /**
   * Checks if the Document has a SOAPBodyPayload.
   *
   * @param aPModeLeg
   *        to get the SOAPVersion
   * @param aSOAPDoc
   *        the document that should be checked if it contains a SOAPBodyPayload
   * @return true if it contains a SOAPBodyPayload else false
   */
  private static boolean _checkSOAPBodyHasPayload (@Nonnull final PModeLeg aPModeLeg, @Nonnull final Document aSOAPDoc)
  {
    if (aPModeLeg != null)
    {
      // Check if a SOAPBodyPayload exists
      final Element aBody = XMLHelper.getFirstChildElementOfName (aSOAPDoc.getFirstChild (),
                                                                  aPModeLeg.getProtocol ()
                                                                           .getSOAPVersion ()
                                                                           .getBodyElementName ());
      if (aBody != null && aBody.hasChildNodes ())
        return true;
    }
    return false;
  }

  @Nonnull
  public ESuccess processHeaderElement (@Nonnull final Document aSOAPDoc,
                                        @Nonnull final Element aElement,
                                        @Nonnull final ICommonsList <WSS4JAttachment> aAttachments,
                                        @Nonnull final AS4MessageState aState,
                                        @Nonnull final ErrorList aErrorList,
                                        @Nonnull final Locale aLocale)
  {
    final MPCManager aMPCMgr = MetaAS4Manager.getMPCMgr ();
    // Needed for the compression check: it is not allowed to have a
    // compressed attachment and a SOAPBodyPayload
    boolean bHasSoapBodyPayload = false;
    final ICommonsMap <String, EAS4CompressionMode> aCompressionAttachmentIDs = new CommonsHashMap <> ();

    // Parse EBMS3 Messaging object
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Ebms3Messaging aMessaging = Ebms3ReaderBuilder.ebms3Messaging ()
                                                        .setValidationEventHandler (aCVEH)
                                                        .read (aElement);

    // If the ebms3reader above fails aMessageing will be null => invalid/not
    // wellformed
    if (aMessaging == null)
    {
      // Errorcode/Id would be null => not conform with Ebms3ErrorMessage since
      // the message always needs a errorcode =>
      // Invalid Header == not wellformed/invalid xml
      for (final IError aError : aCVEH.getErrorList ())
      {
        aErrorList.add (SingleError.builder (aError)
                                   .setErrorID (EEbmsError.EBMS_INVALID_HEADER.getErrorCode ())
                                   .build ());
      }
      return ESuccess.FAILURE;
    }

    // 0 or 1 are allowed
    if (aMessaging.getUserMessageCount () > 1)
    {
      s_aLogger.warn ("Too many UserMessage objects contained: " + aMessaging.getUserMessageCount ());

      aErrorList.add (EEbmsError.EBMS_VALUE_INCONSISTENT.getAsError (aLocale));
      return ESuccess.FAILURE;
    }

    // Check if the usermessage has a PMode in the collaboration info
    final Ebms3UserMessage aUserMessage = CollectionHelper.getAtIndex (aMessaging.getUserMessage (), 0);
    if (aUserMessage == null)
    {
      // No UserMessage was found
      s_aLogger.warn ("No UserMessage object contained!");
      return ESuccess.FAILURE;
    }

    final List <Ebms3PartyId> aFromPartyIdList = aUserMessage.getPartyInfo ().getFrom ().getPartyId ();
    final List <Ebms3PartyId> aToPartyIdList = aUserMessage.getPartyInfo ().getTo ().getPartyId ();

    if (aFromPartyIdList.isEmpty () || aToPartyIdList.isEmpty ())
    {
      s_aLogger.warn ("No Party IDs contained in usermessage, should never occur.");
      aErrorList.add (EEbmsError.EBMS_VALUE_INCONSISTENT.getAsError (aLocale));
      return ESuccess.FAILURE;
    }

    if (aFromPartyIdList.size () > 1 || aToPartyIdList.size () > 1)
    {
      s_aLogger.warn ("More than one partyId is containted in From or To Recipient please check the message.");
      aErrorList.add (EEbmsError.EBMS_VALUE_INCONSISTENT.getAsError (aLocale));
      return ESuccess.FAILURE;
    }

    // Setting Initiator and Responder id, Required values or else xsd will
    // throw an error
    final String sInitiatorID = aFromPartyIdList.get (0).getValue ();
    final String sResponderID = aToPartyIdList.get (0).getValue ();

    IPMode aPMode = null;
    final Ebms3CollaborationInfo aCollaborationInfo = aUserMessage.getCollaborationInfo ();
    if (aCollaborationInfo != null)
    {
      // Find PMode
      String sPModeID = null;
      if (aCollaborationInfo.getAgreementRef () != null)
        sPModeID = aCollaborationInfo.getAgreementRef ().getPmode ();

      // Get responder address from properties file
      final String sResponderAddress = AS4ServerConfiguration.getResponderAddress ();

      aPMode = AS4ServerSettings.getPModeResolver ().getPModeOfID (sPModeID,
                                                                   aCollaborationInfo.getService ().getValue (),
                                                                   aCollaborationInfo.getAction (),
                                                                   sInitiatorID,
                                                                   sResponderID,
                                                                   sResponderAddress);

      if (aPMode == null)
      {
        s_aLogger.warn ("Failed to resolve PMode '" +
                        sPModeID +
                        "' using resolver " +
                        AS4ServerSettings.getPModeResolver ());

        aErrorList.add (EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getAsError (aLocale));
        return ESuccess.FAILURE;
      }
    }

    // to use the configuration for leg2
    PModeLeg aPModeLeg1 = null;
    PModeLeg aPModeLeg2 = null;
    IMPC aEffectiveMPC = null;

    if (aPMode != null)
    {
      aPModeLeg1 = aPMode.getLeg1 ();
      aPModeLeg2 = aPMode.getLeg2 ();

      // if the two - way is selected, check if it requires two legs and if both
      // are present
      if (aPMode.getMEPBinding ().getRequiredLegs () == 2)
      {
        if (aPModeLeg2 == null)
        {
          s_aLogger.warn ("Error processing the usermessage, PMode does not contain a enough legs!");

          aErrorList.add (EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getAsError (aLocale));
          return ESuccess.FAILURE;
        }
      }

      final boolean bUseLeg1 = _isUseLeg1 (aUserMessage);
      final PModeLeg aEffectiveLeg = bUseLeg1 ? aPModeLeg1 : aPModeLeg2;
      aState.setEffectivePModeLeg (bUseLeg1 ? 1 : 2, aEffectiveLeg);

      if (_checkMPC (aErrorList, aLocale, aMPCMgr, aEffectiveLeg).isFailure ())
        return ESuccess.FAILURE;

      bHasSoapBodyPayload = _checkSOAPBodyHasPayload (aEffectiveLeg, aSOAPDoc);
      final String sEffectiveMPCID = _getMPC (aUserMessage, aEffectiveLeg);

      // PMode is valid
      // Now Check if MPC valid
      aEffectiveMPC = aMPCMgr.getMPCOrDefaultOfID (sEffectiveMPCID);
      if (aEffectiveMPC == null)
      {
        s_aLogger.warn ("Error processing the usermessage, effective PMode-MPC ID '" +
                        sEffectiveMPCID +
                        "' is unknown!");

        aErrorList.add (EEbmsError.EBMS_VALUE_INCONSISTENT.getAsError (aLocale));
        return ESuccess.FAILURE;
      }
    }

    // Remember in state
    aState.setSoapBodyPayloadPresent (bHasSoapBodyPayload);

    final Ebms3PayloadInfo aEbms3PayloadInfo = aUserMessage.getPayloadInfo ();
    if (aEbms3PayloadInfo == null || aEbms3PayloadInfo.getPartInfo ().isEmpty ())
    {
      if (bHasSoapBodyPayload)
      {
        s_aLogger.warn ("No PartInfo is specified, so no SOAPBodyPayload is allowed.");

        aErrorList.add (EEbmsError.EBMS_VALUE_INCONSISTENT.getAsError (aLocale));
        return ESuccess.FAILURE;
      }

      // For the case that there is no Payload/Part - Info but still
      // attachments in the message
      if (aAttachments.isNotEmpty ())
      {
        s_aLogger.warn ("No PartInfo is specified, so no attachments are allowed.");

        aErrorList.add (EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.getAsError (aLocale));
        return ESuccess.FAILURE;
      }
    }
    else
    {
      // Check if there are more Attachments then specified
      if (aAttachments.size () > aEbms3PayloadInfo.getPartInfoCount ())
      {
        s_aLogger.warn ("Error processing the UserMessage, the amount of specified attachments does not correlate with the actual attachments in the UserMessage. Expected '" +
                        aEbms3PayloadInfo.getPartInfoCount () +
                        "'" +
                        " but was '" +
                        aAttachments.size () +
                        "'");

        aErrorList.add (EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.getAsError (aLocale));
        return ESuccess.FAILURE;
      }

      int nSpecifiedAttachments = 0;

      for (final Ebms3PartInfo aPart : aEbms3PayloadInfo.getPartInfo ())
      {
        // If href is null or empty there has to be a SOAP Payload
        if (StringHelper.hasNoText (aPart.getHref ()))
        {
          // Check if there is a BodyPayload as specified in the UserMessage
          if (!bHasSoapBodyPayload)
          {
            s_aLogger.warn ("Error processing the UserMessage, Expected a BodyPayload but there is none present. ");

            aErrorList.add (EEbmsError.EBMS_VALUE_INCONSISTENT.getAsError (aLocale));
            return ESuccess.FAILURE;
          }
        }
        else
        {
          // Attachment
          // To check attachments which are specified in the usermessage and
          // the real amount in the mime message
          nSpecifiedAttachments++;

          boolean bMimeTypePresent = false;
          boolean bCompressionTypePresent = false;

          if (aPart.getPartProperties () != null)
          {
            for (final Ebms3Property aEbms3Property : aPart.getPartProperties ().getProperty ())
            {
              if (aEbms3Property.getName ().equalsIgnoreCase ("mimetype"))
              {
                bMimeTypePresent = true;
              }
              if (aEbms3Property.getName ().equalsIgnoreCase ("compressiontype"))
              {
                // Only needed check here since AS4 does not support another
                // CompressionType
                // http://wiki.ds.unipi.gr/display/ESENS/PR+-+AS4
                final EAS4CompressionMode eCompressionMode = EAS4CompressionMode.getFromMimeTypeStringOrNull (aEbms3Property.getValue ());
                if (eCompressionMode == null)
                {
                  s_aLogger.warn ("Error processing the UserMessage, CompressionType " +
                                  aEbms3Property.getValue () +
                                  " is not supported. ");

                  aErrorList.add (EEbmsError.EBMS_VALUE_INCONSISTENT.getAsError (aLocale));
                  return ESuccess.FAILURE;
                }

                final String sAttachmentID = StringHelper.trimStart (aPart.getHref (), CreateUserMessage.PREFIX_CID);
                aCompressionAttachmentIDs.put (sAttachmentID, eCompressionMode);
                bCompressionTypePresent = true;
              }
            }
          }

          // if a compressiontype is present there has to be a mimetype present,
          // to specify what mimetype the attachment was before it got
          // compressed
          if (!bMimeTypePresent && bCompressionTypePresent)
          {
            s_aLogger.warn ("Error processing the UserMessage, MimeType for a compressed message not present. ");

            aErrorList.add (EEbmsError.EBMS_VALUE_INCONSISTENT.getAsError (aLocale));
            return ESuccess.FAILURE;
          }
        }
      }

      // If PartInfo(Usermessage - header) specified attachments and attached
      // attachment differ throw an error
      if (nSpecifiedAttachments != aAttachments.size ())
      {
        s_aLogger.warn ("Error processing the UserMessage, the amount of specified attachments does not correlate with the actual attachments in the UserMessage. Expected '" +
                        aEbms3PayloadInfo.getPartInfoCount () +
                        "'" +
                        " but was '" +
                        aAttachments.size () +
                        "'");

        aErrorList.add (EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.getAsError (aLocale));
        return ESuccess.FAILURE;
      }
    }

    // TODO if pullrequest the method for extracting the pmode needs to be
    // different since the pullrequest itself does not contain the pmode, it
    // is just reachable over the mpc where the usermessage is supposed to be
    // stored

    // Remember in state
    aState.setMessaging (aMessaging);
    aState.setPMode (aPMode);
    aState.setOriginalAttachments (aAttachments);
    aState.setCompressedAttachmentIDs (aCompressionAttachmentIDs);
    aState.setMPC (aEffectiveMPC);
    aState.setInitiatorID (sInitiatorID);
    aState.setResponderID (sResponderID);

    return ESuccess.SUCCESS;
  }
}
