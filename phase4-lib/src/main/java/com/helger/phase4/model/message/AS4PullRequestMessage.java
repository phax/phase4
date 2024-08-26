/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.model.message;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3PullRequest;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.model.ESoapVersion;

/**
 * AS4 pull request message
 *
 * @author Philip Helger
 */
public class AS4PullRequestMessage extends AbstractAS4Message <AS4PullRequestMessage>
{
  private final Ebms3SignalMessage m_aSignalMessage;

  public AS4PullRequestMessage (@Nonnull final ESoapVersion eSoapVersion,
                                @Nonnull final Ebms3SignalMessage aSignalMessage)
  {
    super (eSoapVersion, EAS4MessageType.PULL_REQUEST);

    ValueEnforcer.notNull (aSignalMessage, "SignalMessage");
    m_aMessaging.addSignalMessage (aSignalMessage);

    m_aSignalMessage = aSignalMessage;
  }

  /**
   * @return The {@link Ebms3SignalMessage} passed in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  public final Ebms3SignalMessage getEbms3SignalMessage ()
  {
    return m_aSignalMessage;
  }

  @Nonnull
  public static AS4PullRequestMessage create (@Nonnull final ESoapVersion eSoapVersion,
                                              @Nonnull final Ebms3MessageInfo aEbms3MessageInfo,
                                              @Nullable final String sMPC,
                                              @Nullable final List <Object> aAny)
  {
    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();

    // Message Info
    aSignalMessage.setMessageInfo (aEbms3MessageInfo);

    // PullRequest
    if (StringHelper.hasText (sMPC))
    {
      final Ebms3PullRequest aEbms3PullRequest = new Ebms3PullRequest ();
      aEbms3PullRequest.setMpc (sMPC);
      aSignalMessage.setPullRequest (aEbms3PullRequest);
    }

    aSignalMessage.setAny (aAny);

    return new AS4PullRequestMessage (eSoapVersion, aSignalMessage);
  }
}
