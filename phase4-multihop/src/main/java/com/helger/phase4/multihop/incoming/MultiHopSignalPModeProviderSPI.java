/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.incoming;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.string.StringHelper;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.incoming.spi.IAS4IncomingSignalMessagePModeProviderSPI;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.pmode.IPMode;

/**
 * Resolves the PMode of an incoming routed Receipt or Error by looking up its
 * <code>RefToMessageId</code> in the {@link MultiHopSentMessageStore}. Without this, a
 * <b>signed</b> routed signal cannot be processed by the servlet at all.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@IsSPIImplementation
public final class MultiHopSignalPModeProviderSPI implements IAS4IncomingSignalMessagePModeProviderSPI
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MultiHopSignalPModeProviderSPI.class);

  @UsedViaReflection
  public MultiHopSignalPModeProviderSPI ()
  {}

  @Nullable
  public IPMode findPMode (@NonNull final Ebms3SignalMessage aSignalMessage)
  {
    if (aSignalMessage.getMessageInfo () == null)
      return null;

    final String sRefToMessageID = aSignalMessage.getMessageInfo ().getRefToMessageId ();
    if (StringHelper.isEmpty (sRefToMessageID))
      return null;

    final String sPModeID = MultiHopSentMessageStore.getDefaultInstance ()
                                                    .getPModeIDOfSentMessage (sRefToMessageID);
    if (sPModeID == null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("The multi-hop sent message store does not know the message ID '" +
                      sRefToMessageID +
                      "' - no PMode can be provided");
      return null;
    }

    final IPMode aPMode = MetaAS4Manager.getPModeMgr ().getPModeOfID (sPModeID);
    if (aPMode == null)
      LOGGER.warn ("The multi-hop sent message store resolved the message ID '" +
                   sRefToMessageID +
                   "' to the PMode ID '" +
                   sPModeID +
                   "', but no such PMode is registered");
    return aPMode;
  }
}
