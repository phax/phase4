package com.helger.as4server.message;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;

import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3PullRequest;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.messaging.MessagingHandler;
import com.helger.as4lib.soap.ESOAPVersion;

public class CreatePullRequestMessage
{
  public Document createPullRequestMessage (@Nonnull final ESOAPVersion eSOAPVersion,
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

    // Creating Message
    final Ebms3Messaging aMessage = new Ebms3Messaging ();
    // TODO Needs to beset to 0 (equals false) since holodeck currently throws
    // a exception he does not understand mustUnderstand
    if (eSOAPVersion.equals (ESOAPVersion.SOAP_11))
      aMessage.setS11MustUnderstand (Boolean.FALSE);
    else
      aMessage.setS12MustUnderstand (Boolean.FALSE);
    aMessage.addSignalMessage (aSignalMessage);

    // Adding the signal message to the existing soap
    return MessagingHandler.createSOAPEnvelopeAsDocument (eSOAPVersion, aMessage, null);
  }

  public Ebms3MessageInfo createEbms3MessageInfo (@Nonnull final String sMessageId)
  {
    return MessageHelperMethods.createEbms3MessageInfo (sMessageId, null);
  }
}
