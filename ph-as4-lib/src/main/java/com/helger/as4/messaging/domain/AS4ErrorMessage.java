/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.messaging.domain;

import javax.annotation.Nonnull;

import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.StringHelper;

/**
 * AS4 error message
 *
 * @author Philip Helger
 */
public class AS4ErrorMessage extends AbstractAS4Message <AS4ErrorMessage>
{
  public AS4ErrorMessage (@Nonnull final ESOAPVersion eSOAPVersion, @Nonnull final Ebms3SignalMessage aSignalMessage)
  {
    super (eSOAPVersion, EAS4MessageType.ERROR_MESSAGE);
    ValueEnforcer.notNull (aSignalMessage, "SignalMessage");
    m_aMessaging.addSignalMessage (aSignalMessage);

    for (final Ebms3Error aError : aSignalMessage.getError ())
      if (aError.getDescription () != null && StringHelper.hasNoText (aError.getDescription ().getValue ()))
        throw new IllegalArgumentException ("Error description may not be empty - will lead to invalid XML!");
  }
}
