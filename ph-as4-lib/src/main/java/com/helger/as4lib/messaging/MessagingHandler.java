/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.messaging;

import javax.annotation.Nonnull;

import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.ValueEnforcer;

public class MessagingHandler
{
  public MessagingHandler ()
  {}

  public void handleSignalMessage (@Nonnull final Ebms3SignalMessage aSignalMessage)
  {
    ValueEnforcer.notNull (aSignalMessage, "SignalMessage");
    // TODO
  }

  public void handleUserMessage (@Nonnull final Ebms3UserMessage aUserMessage)
  {
    ValueEnforcer.notNull (aUserMessage, "UserMessage");
    // TODO
  }

  public void handle (@Nonnull final Ebms3Messaging aMessaging)
  {
    ValueEnforcer.notNull (aMessaging, "Messaging");

    if (aMessaging.hasSignalMessageEntries ())
      for (final Ebms3SignalMessage aSignalMessage : aMessaging.getSignalMessage ())
        handleSignalMessage (aSignalMessage);
    if (aMessaging.hasUserMessageEntries ())
      for (final Ebms3UserMessage aUserMessage : aMessaging.getUserMessage ())
        handleUserMessage (aUserMessage);
  }
}
