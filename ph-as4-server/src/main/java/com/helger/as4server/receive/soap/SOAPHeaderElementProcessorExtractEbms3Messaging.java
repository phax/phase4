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
package com.helger.as4server.receive.soap;

import java.util.Locale;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.attachment.WSS4JAttachment;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3PartInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.marshaller.Ebms3ReaderBuilder;
import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.model.mpc.IMPC;
import com.helger.as4lib.model.mpc.MPCManager;
import com.helger.as4lib.model.pmode.config.IPModeConfig;
import com.helger.as4lib.model.pmode.leg.PModeLeg;
import com.helger.as4server.receive.AS4MessageState;
import com.helger.as4server.settings.AS4ServerSettings;
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
 */
public final class SOAPHeaderElementProcessorExtractEbms3Messaging implements ISOAPHeaderElementProcessor
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SOAPHeaderElementProcessorExtractEbms3Messaging.class);

  @Nonnull
  public ESuccess processHeaderElement (@Nonnull final Document aSOAPDoc,
                                        @Nonnull final Element aElement,
                                        @Nonnull final ICommonsList <WSS4JAttachment> aAttachments,
                                        @Nonnull final AS4MessageState aState,
                                        @Nonnull final ErrorList aErrorList,
                                        @Nonnull final Locale aLocale)
  {
    final MPCManager aMPCMgr = MetaAS4Manager.getMPCMgr ();

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

    // Check if the usermessage has a pmodeconfig in the collaboration info
    final Ebms3UserMessage aUserMessage = CollectionHelper.getAtIndex (aMessaging.getUserMessage (), 0);
    if (aUserMessage == null)
    {
      // No UserMessage was found
      s_aLogger.warn ("No UserMessage object contained!");
      return ESuccess.FAILURE;
    }

    IPModeConfig aPModeConfig = null;
    if (aUserMessage.getCollaborationInfo () != null && aUserMessage.getCollaborationInfo ().getAgreementRef () != null)
    {
      // Find PMode
      final String sPModeConfigID = aUserMessage.getCollaborationInfo ().getAgreementRef ().getPmode ();
      aPModeConfig = AS4ServerSettings.getPModeConfigResolver ().getPModeConfigOfID (sPModeConfigID);
      if (aPModeConfig == null)
      {
        s_aLogger.warn ("Failed to resolve PMode '" +
                        sPModeConfigID +
                        "' using resolver " +
                        AS4ServerSettings.getPModeConfigResolver ());

        aErrorList.add (EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getAsError (aLocale));
        return ESuccess.FAILURE;
      }
    }

    // TODO check for right leg in two way situation
    PModeLeg aPModeLeg1 = null;
    IMPC aEffectiveMPC = null;

    if (aPModeConfig != null)
    {
      // Check if MPC is contained in PMode and if so, if it is valid
      aPModeLeg1 = aPModeConfig.getLeg1 ();
      if (aPModeLeg1 != null)
      {
        if (aPModeLeg1.getBusinessInfo () != null)
        {
          final String sPModeMPC = aPModeLeg1.getBusinessInfo ().getMPCID ();
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

      // PMode is valid
      // Now Check if MPC valid
      String sEffectiveMPCID = aUserMessage.getMpc ();
      if (sEffectiveMPCID == null)
      {
        if (aPModeLeg1.getBusinessInfo () != null)
          sEffectiveMPCID = aPModeLeg1.getBusinessInfo ().getMPCID ();
      }
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

    // Needed for the compression check: it is not allowed to have a
    // compressed attachment and a SOAPBodyPayload
    boolean bHasSoapBodyPayload = false;

    final ICommonsMap <String, EAS4CompressionMode> aCompressionAttachmentIDs = new CommonsHashMap<> ();

    if (aPModeLeg1 != null)
    {
      // Check if a SOAPBodyPayload exists
      final Element aBody = XMLHelper.getFirstChildElementOfName (aSOAPDoc.getFirstChild (),
                                                                  aPModeLeg1.getProtocol ()
                                                                            .getSOAPVersion ()
                                                                            .getBodyElementName ());
      if (aBody != null && aBody.hasChildNodes ())
        bHasSoapBodyPayload = true;
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
            s_aLogger.warn ("Error processing the UserMessage, Expected a BodyPayload but there is one present. ");

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

          for (final Ebms3Property aEbms3Property : aPart.getPartProperties ().getProperty ())
          {
            if (aEbms3Property.getName ().toLowerCase ().equals ("compressiontype"))
            {
              if (bHasSoapBodyPayload)
              {
                s_aLogger.warn ("Error processing the UserMessage, it contains compressed attachment in consequence you can not have anything in the SOAPBodyPayload.");

                aErrorList.add (EEbmsError.EBMS_VALUE_INCONSISTENT.getAsError (aLocale));
                return ESuccess.FAILURE;
              }

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

              final String sAttachmentID = StringHelper.trimStart (aPart.getHref (), "cid:");
              aCompressionAttachmentIDs.put (sAttachmentID, eCompressionMode);
            }
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

    // TODO if pullrequest the methode for extracting the pmode needs to be
    // different since the pullrequest itself does not contain the pmode, it
    // is
    // just reachable over the mpc where the usermessage is supposed to be
    // stored

    // Remember in state
    aState.setMessaging (aMessaging);
    aState.setPModeConfig (aPModeConfig);
    aState.setOriginalAttachments (aAttachments);
    aState.setCompressedAttachmentIDs (aCompressionAttachmentIDs);
    aState.setMPC (aEffectiveMPC);
    // Setting Initiator and Responder id, Required values or else xsd will
    // throw an error
    aState.setInitiatorID (aUserMessage.getPartyInfo ().getFrom ().getPartyIdAtIndex (0).getValue ());
    aState.setResponderID (aUserMessage.getPartyInfo ().getTo ().getPartyIdAtIndex (0).getValue ());

    return ESuccess.SUCCESS;
  }
}
