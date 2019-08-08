/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
