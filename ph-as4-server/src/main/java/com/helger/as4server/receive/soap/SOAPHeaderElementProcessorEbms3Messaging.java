package com.helger.as4server.receive.soap;

import javax.annotation.Nonnull;

import org.w3c.dom.Element;

import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.marshaller.Ebms3ReaderBuilder;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.as4server.receive.AS4MessageState;
import com.helger.commons.state.ESuccess;
import com.helger.jaxb.validation.CollectingValidationEventHandler;

public class SOAPHeaderElementProcessorEbms3Messaging implements ISOAPHeaderElementProcessor
{
  @Nonnull
  public ESuccess processHeaderElement (@Nonnull final Element aElement, @Nonnull final AS4MessageState aState)
  {
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Ebms3Messaging aMessaging = Ebms3ReaderBuilder.ebms3Messaging ()
                                                        .setValidationEventHandler (aCVEH)
                                                        .read (aElement);
    if (aMessaging == null)
    {
      aState.addProcessingErrors (AS4XMLHelper.getQName (aElement), aCVEH.getResourceErrors ().getAllFailures ());
      return ESuccess.FAILURE;
    }

    // Remember in state
    aState.setMessaging (aMessaging);
    return ESuccess.SUCCESS;
  }
}
