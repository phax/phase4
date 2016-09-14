package com.helger.as4server.receive.soap;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.marshaller.Ebms3ReaderBuilder;
import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.model.pmode.IPMode;
import com.helger.as4lib.model.pmode.PModeLeg;
import com.helger.as4lib.model.pmode.PModeLegProtocol;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4server.receive.AS4MessageState;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.error.SingleError;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.state.ESuccess;
import com.helger.jaxb.validation.CollectingValidationEventHandler;

public final class SOAPHeaderElementProcessorExtractEbms3Messaging implements ISOAPHeaderElementProcessor
{
  @Nonnull
  public ESuccess processHeaderElement (@Nonnull final Document aSOAPDoc,
                                        @Nonnull final Element aElement,
                                        @Nonnull final AS4MessageState aState,
                                        @Nonnull final ErrorList aErrorList)
  {
    // Parse EBMS3 Messaging object
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Ebms3Messaging aMessaging = Ebms3ReaderBuilder.ebms3Messaging ()
                                                        .setValidationEventHandler (aCVEH)
                                                        .read (aElement);
    if (aMessaging == null)
    {
      aErrorList.addAll (aCVEH.getErrorList ());
      return ESuccess.FAILURE;
    }

    // 0 or 1 are allowed
    if (aMessaging.getUserMessageCount () > 1)
    {
      aErrorList.add (SingleError.builderError ()
                                 .setErrorText ("Too many UserMessage objects contained: " +
                                                aMessaging.getUserMessageCount ())
                                 .build ());
      return ESuccess.FAILURE;
    }

    // Check if the usermessage has a pmode in the collaboration info
    IPMode aPMode = null;
    final Ebms3UserMessage aUserMessage = CollectionHelper.getAtIndex (aMessaging.getUserMessage (), 0);
    if (aUserMessage != null)
    {
      String sPModeID = null;
      if (aUserMessage.getCollaborationInfo () != null &&
          aUserMessage.getCollaborationInfo ().getAgreementRef () != null)
      {
        // Find PMode
        sPModeID = aUserMessage.getCollaborationInfo ().getAgreementRef ().getPmode ();
        aPMode = MetaAS4Manager.getPModeMgr ().getPModeOfID (sPModeID);
      }
      if (aPMode == null)
      {
        aErrorList.add (SingleError.builderError ()
                                   .setErrorText ("Failed to resolve PMode '" + sPModeID + "'")
                                   .build ());
        return ESuccess.FAILURE;
      }
    }

    if (aPMode.getMEP () == null || aPMode.getMEPBinding () == null)
      throw new IllegalStateException ("PMode is incomplete: " + aPMode);

    // Check if pmode contains a protocol and if the message complies
    final PModeLeg aPModeLeg1 = aPMode.getLeg1 ();
    if (aPModeLeg1 == null)
    {
      aErrorList.add (SingleError.builderError ().setErrorText ("PMode is missing Leg 1").build ());
      return ESuccess.FAILURE;
    }

    // Check protocol
    {
      final PModeLegProtocol aProtocol = aPModeLeg1.getProtocol ();
      if (aProtocol == null || !"http".equals (aProtocol.getAddressProtocol ()))
      {
        aErrorList.add (SingleError.builderError ()
                                   .setErrorText ("PMode Leg uses unsupported protocol '" +
                                                  aProtocol.getAddressProtocol () +
                                                  "'")
                                   .build ());
        return ESuccess.FAILURE;
      }

      // Check SOAP - Version
      final ESOAPVersion ePModeSoapVersion = aProtocol.getSOAPVersion ();
      if (!aState.getSOAPVersion ().equals (ePModeSoapVersion))
      {
        aErrorList.add (SingleError.builderError ()
                                   .setErrorText ("Error processing the PMode, the SOAP Version (" +
                                                  ePModeSoapVersion +
                                                  ") is incorrect.")
                                   .build ());
        return ESuccess.FAILURE;
      }
    }

    // UserMessage does not need to get checked for null again since it got
    // checked above
    final Ebms3PartyInfo aPartyInfo = aUserMessage.getPartyInfo ();
    if (aPartyInfo != null)
    {
      // Initiator is optional for push
      if (aPMode.getInitiator () == null)
      {
        if (aPMode.getMEPBinding ().isPull ())
        {
          aErrorList.add (SingleError.builderError ().setErrorText ("Initiator is required for PULL message").build ());
          return ESuccess.FAILURE;
        }
      }
      else
      {
        if (aPartyInfo.getFrom () != null && aPartyInfo.getFrom ().getPartyId () != null)
        {
          // Check if PartyID is correct for Initiator
          final String sInitiatorID = aPMode.getInitiator ().getIDValue ();
          if (CollectionHelper.containsNone (aPartyInfo.getFrom ().getPartyId (),
                                             aID -> aID.getValue ().equals (sInitiatorID)))
          {
            aErrorList.add (SingleError.builderError ()
                                       .setErrorText ("Error processing the PMode, the Initiator/Sender PartyID is incorrect. Expected '" +
                                                      sInitiatorID +
                                                      "'")
                                       .build ());
            return ESuccess.FAILURE;
          }
        }
        else
        {
          aErrorList.add (SingleError.builderError ()
                                     .setErrorText ("Error processing the usermessage, initiator part is present. But from PartyInfo is invalid.")
                                     .build ());
          return ESuccess.FAILURE;
        }
      }

      // Response is optional for pull
      if (aPMode.getResponder () == null)
      {
        if (aPMode.getMEPBinding ().isPush ())
        {
          aErrorList.add (SingleError.builderError ().setErrorText ("Responder is required for PUSH message").build ());
          return ESuccess.FAILURE;
        }
      }
      else
      {
        if (aPartyInfo.getTo () != null && aPartyInfo.getTo ().getPartyId () != null)
        {
          // Check if PartyID is correct for Responder
          final String sResponderID = aPMode.getResponder ().getIDValue ();
          if (CollectionHelper.containsNone (aPartyInfo.getTo ().getPartyId (),
                                             aID -> aID.getValue ().equals (sResponderID)))
          {
            aErrorList.add (SingleError.builderError ()
                                       .setErrorText ("Error processing the PMode, the Responder PartyID is incorrect. Expected '" +
                                                      sResponderID +
                                                      "'")
                                       .build ());
            return ESuccess.FAILURE;
          }
        }
        else
        {
          aErrorList.add (SingleError.builderError ()
                                     .setErrorText ("Error processing the usermessage, to-PartyInfo is invalid.")
                                     .build ());
          return ESuccess.FAILURE;
        }
      }
    }

    // TODO if pullrequest the methode for extracting the pmode needs to be
    // different since the pullrequest itself does not contain the pmode, it is
    // just reachable over the mpc where the usermessage is supposed to be
    // stored

    // Remember in state
    aState.setMessaging (aMessaging);
    aState.setPMode (aPMode);

    return ESuccess.SUCCESS;
  }
}
