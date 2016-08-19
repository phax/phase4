package com.helger.as4lib.message;

import javax.annotation.Nonnull;

import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.ValueEnforcer;

/**
 * AS4 pull request message
 *
 * @author Philip Helger
 */
public class AS4PullRequestMessage extends AbstractAS4Message <AS4PullRequestMessage>
{
  public AS4PullRequestMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                @Nonnull final Ebms3SignalMessage aSignalMessage)
  {
    super (eSOAPVersion);
    ValueEnforcer.notNull (aSignalMessage, "SignalMessage");
    m_aMessaging.addSignalMessage (aSignalMessage);
  }
}
