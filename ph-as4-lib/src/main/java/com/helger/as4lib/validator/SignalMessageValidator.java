package com.helger.as4lib.validator;

import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.exception.Ebms3Exception;

/**
 * Not sure if needed since xsd checks all of the following
 * 
 * @author bayerlma
 */
public class SignalMessageValidator
{
  public void validateSignalMessage (final Ebms3SignalMessage signalMessage) throws Ebms3Exception
  {
    if (signalMessage.getMessageInfo ().getMessageId ().isEmpty ())
    {
      throw new Ebms3Exception (EEbmsError.EBMS_INVALID_HEADER, "MessageInfo messageId is missing", null);
    }

    final String sRefToMessageId = signalMessage.getMessageInfo ().getRefToMessageId ();
    if (!signalMessage.getError ().isEmpty () ||
        !signalMessage.getPullRequest ().getMpc ().isEmpty () ||
        signalMessage.getReceipt ().hasAnyEntries ())
    {

    }
    else
    {
      throw new Ebms3Exception (EEbmsError.EBMS_INVALID_HEADER,
                                "No Messages are inside the SignalMessage",
                                sRefToMessageId);
    }
  }
}
