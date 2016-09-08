package com.helger.as4server.receive.soap;

import java.util.List;

import javax.annotation.Nonnull;

import org.w3c.dom.Element;

import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.marshaller.Ebms3ReaderBuilder;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.model.pmode.PModeManager;
import com.helger.as4server.receive.AS4MessageState;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.errorlist.IErrorBase;
import com.helger.commons.errorlist.SingleError;
import com.helger.commons.state.ESuccess;
import com.helger.jaxb.validation.CollectingValidationEventHandler;

public final class SOAPHeaderElementProcessorExtractEbms3Messaging implements ISOAPHeaderElementProcessor
{
  @Nonnull
  public ESuccess processHeaderElement (@Nonnull final Element aElement,
                                        @Nonnull final AS4MessageState aState,
                                        @Nonnull final List <? super IErrorBase <?>> aErrorList)
  {
    // Parse EBMS3 Messaging object
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Ebms3Messaging aMessaging = Ebms3ReaderBuilder.ebms3Messaging ()
                                                        .setValidationEventHandler (aCVEH)
                                                        .read (aElement);
    if (aMessaging == null)
    {
      aCVEH.getResourceErrors ().getAllFailures ().forEach (aErrorList::add);
      return ESuccess.FAILURE;
    }

    // 0 or 1 are allowed
    if (aMessaging.getUserMessageCount () > 1)
    {
      aErrorList.add (SingleError.createError ("Too many UserMessage objects contained: " +
                                               aMessaging.getUserMessageCount ()));
      return ESuccess.FAILURE;
    }

    // Check if the usermessage has a pmode in the collaboration info
    PMode aPMode = null;
    final Ebms3UserMessage aUserMessage = CollectionHelper.getAtIndex (aMessaging.getUserMessage (), 0);
    if (aUserMessage != null)
    {
      String sPModeID = null;
      if (aUserMessage.getCollaborationInfo () != null &&
          aUserMessage.getCollaborationInfo ().getAgreementRef () != null)
      {
        // Find PMode
        sPModeID = aUserMessage.getCollaborationInfo ().getAgreementRef ().getPmode ();
        aPMode = PModeManager.getPModeOfID (sPModeID);
      }
      if (aPMode == null)
      {
        aErrorList.add (SingleError.createError ("Failed to resolve PMode '" + sPModeID + "'"));
        return ESuccess.FAILURE;
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
