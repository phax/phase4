/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.IsSPIInterface;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.model.pmode.IPMode;

/**
 * SPI interface to resolve the P-Mode of an incoming standalone Receipt or Error signal message.
 * Such messages contain no P-Mode relevant information themselves, so without an implementation of
 * this SPI no P-Mode can be determined for them. That in turn means a <b>signed</b> standalone
 * Receipt or Error cannot be processed, because the signature verification requires a P-Mode.<br>
 * This is the signal message counterpart of {@link IAS4IncomingPullRequestProcessorSPI}.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@IsSPIInterface
public interface IAS4IncomingSignalMessagePModeProviderSPI
{
  /**
   * Find the P-Mode to be used for the provided incoming signal message. A typical implementation
   * resolves it from the <code>RefToMessageId</code> of a previously sent message.
   *
   * @param aSignalMessage
   *        The incoming Receipt or Error signal message. Never <code>null</code>.
   * @return <code>null</code> if this implementation cannot determine a P-Mode for the provided
   *         signal message. The first non-<code>null</code> result of all registered
   *         implementations is used.
   */
  @Nullable
  IPMode findPMode (@NonNull Ebms3SignalMessage aSignalMessage);
}
