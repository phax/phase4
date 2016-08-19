package com.helger.as4lib.message;

import javax.annotation.Nonnull;

import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.ValueEnforcer;

/**
 * AS4 user message
 *
 * @author Philip Helger
 */
public class AS4UserMessage extends AbstractAS4Message <AS4UserMessage>
{
  public AS4UserMessage (@Nonnull final ESOAPVersion eSOAPVersion, @Nonnull final Ebms3UserMessage aUserMessage)
  {
    super (eSOAPVersion);
    ValueEnforcer.notNull (aUserMessage, "UserMessage");
    m_aMessaging.addUserMessage (aUserMessage);
  }
}
