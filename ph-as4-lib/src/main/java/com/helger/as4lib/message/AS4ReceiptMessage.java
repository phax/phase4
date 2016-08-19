package com.helger.as4lib.message;

import javax.annotation.Nonnull;

import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.ValueEnforcer;

/**
 * AS4 receipt message
 * 
 * @author Philip Helger
 */
public class AS4ReceiptMessage extends AbstractAS4Message <AS4ReceiptMessage>
{
  public AS4ReceiptMessage (@Nonnull final ESOAPVersion eSOAPVersion, @Nonnull final Ebms3SignalMessage aSignalMessage)
  {
    super (eSOAPVersion);
    ValueEnforcer.notNull (aSignalMessage, "SignalMessage");
    m_aMessaging.addSignalMessage (aSignalMessage);
  }
}
