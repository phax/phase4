/*
 * Copyright (C) 2019-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.nemhandel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.profile.peppol.PeppolPMode;

/**
 * PMode creation code. Nemhandel eDelivery is based on the Peppol AS4 profile 2.0.2 and the
 * Nemhandel specifications do not define any deviating PMode values. Therefore the Peppol PMode is
 * reused.
 *
 * @author Philip Helger
 */
@Immutable
public final class NemhandelPMode
{
  private NemhandelPMode ()
  {}

  /**
   * One-Way Version of the Nemhandel pmode uses one-way push
   *
   * @param sInitiatorID
   *        Initiator ID. May neither be <code>null</code> nor empty.
   * @param sResponderID
   *        Responder ID. May neither be <code>null</code> nor empty.
   * @param sAddress
   *        Endpoint address URL. May be <code>null</code>.
   * @param aPModeIDProvider
   *        PMode ID provider. May not be <code>null</code>.
   * @param bPersist
   *        <code>true</code> to persist the PMode in the PModeManager, <code>false</code> to have
   *        it only in memory.
   * @return New PMode and never <code>null</code>.
   * @see PeppolPMode#createPeppolPMode(String, String, String, IPModeIDProvider, boolean)
   */
  @NonNull
  public static PMode createNemhandelPMode (@NonNull @Nonempty final String sInitiatorID,
                                            @NonNull @Nonempty final String sResponderID,
                                            @Nullable final String sAddress,
                                            @NonNull final IPModeIDProvider aPModeIDProvider,
                                            final boolean bPersist)
  {
    return PeppolPMode.createPeppolPMode (sInitiatorID, sResponderID, sAddress, aPModeIDProvider, bPersist);
  }
}
