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

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.resolve.IAS4PModeResolver;
import com.helger.phase4.multihop.CAS4MultiHop;

/**
 * An {@link IAS4PModeResolver} decorator that also tries the <code>.resp</code> and
 * <code>.init</code> PMode unit suffixes.
 * <p>
 * ebMS3 Part 2 section 2.7.2 splits a PMode into units, and
 * <code>eb:AgreementRef/@pmode</code> carries the ID <b>without</b> the suffix (R11). A receiving
 * endpoint that stores its PModes per unit therefore has to add the suffix back when resolving
 * (D7).
 * </p>
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public class MultiHopPModeResolver implements IAS4PModeResolver
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MultiHopPModeResolver.class);

  private final IAS4PModeResolver m_aDelegate;

  /**
   * Constructor.
   *
   * @param aDelegate
   *        The resolver to decorate. May not be <code>null</code>.
   */
  public MultiHopPModeResolver (@NonNull final IAS4PModeResolver aDelegate)
  {
    ValueEnforcer.notNull (aDelegate, "Delegate");
    m_aDelegate = aDelegate;
  }

  /**
   * @return The decorated resolver. Never <code>null</code>.
   */
  @NonNull
  public final IAS4PModeResolver getDelegate ()
  {
    return m_aDelegate;
  }

  @Nullable
  public IPMode findPMode (@Nullable final String sPModeID,
                           @NonNull final String sService,
                           @NonNull final String sAction,
                           @NonNull @Nonempty final String sInitiatorID,
                           @NonNull @Nonempty final String sResponderID,
                           @Nullable final String sAgreementRef,
                           @Nullable final String sAddress)
  {
    // First try the ID as provided - this also covers the "no PMode ID at all" case,
    // where the delegate falls back to Service and Action
    IPMode ret = m_aDelegate.findPMode (sPModeID, sService, sAction, sInitiatorID, sResponderID, sAgreementRef, sAddress);
    if (ret != null || StringHelper.isEmpty (sPModeID))
      return ret;

    // D7 - try the PMode unit suffixes, responding side first
    for (final String sSuffix : new String [] { CAS4MultiHop.PMODE_SUFFIX_RESP, CAS4MultiHop.PMODE_SUFFIX_INIT })
    {
      final String sCandidate = sPModeID + sSuffix;
      ret = m_aDelegate.findPMode (sCandidate, sService, sAction, sInitiatorID, sResponderID, sAgreementRef, sAddress);
      if (ret != null)
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Resolved the PMode ID '" + sPModeID + "' via the multi-hop unit ID '" + sCandidate + "'");
        return ret;
      }
    }

    return null;
  }
}
