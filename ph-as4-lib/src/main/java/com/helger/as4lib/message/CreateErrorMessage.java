/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.message;

import javax.annotation.Nonnull;

import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.collection.ext.ICommonsList;

public class CreateErrorMessage
{
  @Nonnull
  public AS4ErrorMessage createErrorMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                             @Nonnull final Ebms3MessageInfo aEbms3MessageInfo,
                                             @Nonnull final ICommonsList <Ebms3Error> aErrorMessages)
  {
    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();

    // Message Info
    aSignalMessage.setMessageInfo (aEbms3MessageInfo);

    // Error Message
    aSignalMessage.setError (aErrorMessages);

    final AS4ErrorMessage ret = new AS4ErrorMessage (eSOAPVersion, aSignalMessage);
    return ret;
  }

  public Ebms3MessageInfo createEbms3MessageInfo (@Nonnull final String sMessageId)
  {
    return MessageHelperMethods.createEbms3MessageInfo (sMessageId);
  }
}
