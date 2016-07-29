package com.helger.as4server.message;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;

import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3PullRequest;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.marshaller.Ebms3WriterBuilder;
import com.helger.as4lib.soap11.Soap11Body;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.as4lib.soap11.Soap11Header;

public class CreatePullRequestMessage
{
  public Document createPullRequestMessage (@Nonnull final Ebms3MessageInfo aEbms3MessageInfo,
                                            @Nonnull final String aMPC)
  {
    // Creating SOAP Envelope
    final Soap11Envelope aSoapEnv = new Soap11Envelope ();
    aSoapEnv.setHeader (new Soap11Header ());
    aSoapEnv.setBody (new Soap11Body ());

    // Creating Message
    final Ebms3Messaging aMessage = new Ebms3Messaging ();
    // TODO needs to be set to false because holodeck throws error if it is set
    // to true
    aMessage.setS11MustUnderstand (Boolean.FALSE);
    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();

    // Message Info
    aSignalMessage.setMessageInfo (aEbms3MessageInfo);

    // PullRequest
    final Ebms3PullRequest aEbms3PullRequest = new Ebms3PullRequest ();
    aEbms3PullRequest.setMpc (aMPC);
    aSignalMessage.setPullRequest (aEbms3PullRequest);

    aMessage.addSignalMessage (aSignalMessage);

    // Adding the signal message to the existing soap
    final Document aEbms3Message = Ebms3WriterBuilder.ebms3Messaging ().getAsDocument (aMessage);
    aSoapEnv.getHeader ().addAny (aEbms3Message.getDocumentElement ());

    return Ebms3WriterBuilder.soap11 ().getAsDocument (aSoapEnv);
  }

  public Ebms3MessageInfo createEbms3MessageInfo (@Nonnull final String sMessageId)
  {
    return MessageHelperMethods.createEbms3MessageInfo (sMessageId, null);
  }
}
