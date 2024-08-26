/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.incoming.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.IsSPIInterface;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.model.pmode.IPMode;

/**
 * Implement this SPI interface to handle incoming pull request appropriately
 * and give the servlet the right PMode back.<br/>
 * Name before v3:
 * <code>com.helger.phase4.servlet.spi.IAS4ServletPullRequestProcessorSPI</code>
 *
 * @author bayerlma
 * @author Philip Helger
 */
@IsSPIInterface
public interface IAS4IncomingPullRequestProcessorSPI
{
  /**
   * Process incoming AS4 signal message and determine the PMode to be used.
   *
   * @param aSignalMessage
   *        The received signal message. May not be <code>null</code>. Contains
   *        the pull request AND the message info!
   * @return The resolved PMode. May be <code>null</code>.
   */
  @Nullable
  IPMode findPMode (@Nonnull Ebms3SignalMessage aSignalMessage);
}
