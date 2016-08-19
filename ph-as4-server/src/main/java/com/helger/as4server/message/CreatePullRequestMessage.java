package com.helger.as4server.message;

import javax.annotation.Nonnull;

import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3PullRequest;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.messaging.AS4PullRequestMessage;
import com.helger.as4lib.soap.ESOAPVersion;

public class CreatePullRequestMessage
{
  public AS4PullRequestMessage createPullRequestMessage (@Nonnull final ESOAPVersion eSOAPVersion,
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

  public Ebms3MessageInfo createEbms3MessageInfo (@Nonnull final String sMessageId)
  {
    return MessageHelperMethods.createEbms3MessageInfo (sMessageId, null);
  }
}
