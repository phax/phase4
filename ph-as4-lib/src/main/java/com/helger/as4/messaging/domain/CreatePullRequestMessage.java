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
package com.helger.as4.messaging.domain;

import javax.annotation.Nonnull;

import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3PullRequest;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;

public final class CreatePullRequestMessage
{
  private CreatePullRequestMessage ()
  {}

  @Nonnull
  public static AS4PullRequestMessage createPullRequestMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                                                @Nonnull final Ebms3MessageInfo aEbms3MessageInfo,
                                                                @Nonnull final String aMPC)
  {
    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();

    // Message Info
    aSignalMessage.setMessageInfo (aEbms3MessageInfo);

    // PullRequest
    final Ebms3PullRequest aEbms3PullRequest = new Ebms3PullRequest ();
    aEbms3PullRequest.setMpc (aMPC);
    aSignalMessage.setPullRequest (aEbms3PullRequest);

    final AS4PullRequestMessage ret = new AS4PullRequestMessage (eSOAPVersion, aSignalMessage);
    return ret;
  }
}
