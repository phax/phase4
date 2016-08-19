package com.helger.as4lib.messaging;

import javax.annotation.Nonnull;

import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.ValueEnforcer;

public class AS4ErrorMessage extends AbstractAS4Message <AS4ErrorMessage>
{
  public AS4ErrorMessage (@Nonnull final ESOAPVersion eSOAPVersion, @Nonnull final Ebms3SignalMessage aSignalMessage)
  {
    super (eSOAPVersion);
    ValueEnforcer.notNull (aSignalMessage, "SignalMessage");
    m_aMessaging.addSignalMessage (aSignalMessage);
  }
}
