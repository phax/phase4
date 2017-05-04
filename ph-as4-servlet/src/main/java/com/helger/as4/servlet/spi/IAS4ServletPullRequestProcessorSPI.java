package com.helger.as4.servlet.spi;

import javax.annotation.Nonnull;

import com.helger.as4.model.pmode.PMode;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.commons.annotation.IsSPIInterface;

/**
 * Implement this SPI interface to handle incoming pull request appropriately
 * and give the servlet the right pmode back.
 *
 * @author bayerlma
 */
@IsSPIInterface
public interface IAS4ServletPullRequestProcessorSPI
{
  /**
   * Process incoming AS4 user message
   *
   * @param aSignalMessage
   *        The received signal message. May not be <code>null</code>. Contains
   *        the pull request AND the message info!
   * @return A non-<code>null</code> result object.
   */
  @Nonnull
  PMode processAS4UserMessage (@Nonnull Ebms3SignalMessage aSignalMessage);
}
