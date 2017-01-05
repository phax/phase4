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
  public void validateUserMessage (final Ebms3UserMessage aUserMessage) throws Ebms3Exception
  {
    if (StringHelper.hasNoText (aUserMessage.getMessageInfo ().getMessageId ()))
      throw new Ebms3Exception (EEbmsError.EBMS_INVALID_HEADER, "MessageInfo contains no MessageId", null);

    final String sRefToMessageId = aUserMessage.getMessageInfo ().getRefToMessageId ();

    if (StringHelper.hasNoText (aUserMessage.getMpc ()))
      throw new Ebms3Exception (EEbmsError.EBMS_VALUE_NOT_RECOGNIZED, "MPC value is not recognizable", sRefToMessageId);

    if (aUserMessage.getMessageInfo ().getTimestamp ().isValid ())
      throw new Ebms3Exception (EEbmsError.EBMS_INVALID_HEADER, "Timestamp is not valid", sRefToMessageId);

    if (aUserMessage.getPartyInfo ().getFrom ().getPartyId ().isEmpty ())
      throw new Ebms3Exception (EEbmsError.EBMS_INVALID_HEADER, "PartyId is missing", sRefToMessageId);

    if (aUserMessage.getCollaborationInfo ().getConversationId ().isEmpty ())
      throw new Ebms3Exception (EEbmsError.EBMS_INVALID_HEADER,
                                "ConversationId from CollaborationInfo is missing",
                                sRefToMessageId);
  }
}
