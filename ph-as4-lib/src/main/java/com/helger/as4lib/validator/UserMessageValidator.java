package com.helger.as4lib.validator;

import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.exception.Ebms3Exception;
import com.helger.commons.string.StringHelper;

/**
 * Not sure if needed since xsd checks all of the following
 *
 * @author bayerlma
 */
public class UserMessageValidator
{
  // TODO check if MPC is real, implement Ebms3Exception
  public void validateUserMessage (final Ebms3UserMessage aUserMessage) throws Ebms3Exception
  {
    if (StringHelper.hasNoText (aUserMessage.getMessageInfo ().getMessageId ()))
      throw new Ebms3Exception (EEbmsError.EBMS_INVALID_HEADER, "MessageInfo contains no MessageId", null);

    final String sRefToMessageId = aUserMessage.getMessageInfo ().getRefToMessageId ();
    // TODO Check if Default is set here? or should we set default after this
    // check?
    if (StringHelper.hasNoText (aUserMessage.getMpc ()))
      throw new Ebms3Exception (EEbmsError.EBMS_VALUE_NOT_RECOGNIZED, "MPC value is not recognizable", sRefToMessageId);

    if (aUserMessage.getMessageInfo ().getTimestamp ().isValid ())
      throw new Ebms3Exception (EEbmsError.EBMS_INVALID_HEADER, "Timestamp is not valid", sRefToMessageId);

    // TODO check if its a feasible way to control the PartyInfo
    if (aUserMessage.getPartyInfo ().getFrom ().getPartyId ().isEmpty ())
      throw new Ebms3Exception (EEbmsError.EBMS_INVALID_HEADER, "PartyId is missing", sRefToMessageId);

    if (aUserMessage.getCollaborationInfo ().getConversationId ().isEmpty ())
      throw new Ebms3Exception (EEbmsError.EBMS_INVALID_HEADER,
                                "ConversationId from CollaborationInfo is missing",
                                sRefToMessageId);
  }
}
