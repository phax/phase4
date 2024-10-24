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
package com.helger.phase4.model.pmode.resolve;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.phase4.model.pmode.IPMode;

/**
 * Resolve PMode from ID
 *
 * @author Philip Helger
 */
@FunctionalInterface
public interface IAS4PModeResolver
{
  /**
   * Get the PMode from the passed parameters.
   *
   * @param sPModeID
   *        The direct PMode ID to be resolved. May be <code>null</code>.
   * @param sService
   *        The service as specified in the EBMS CollaborationInformation. May
   *        not be <code>null</code>.
   * @param sAction
   *        The action as specified in the EBMS CollaborationInformation. May
   *        not be <code>null</code>.
   * @param sInitiatorID
   *        Initiator ID from user message. May neither be <code>null</code> nor
   *        empty.
   * @param sResponderID
   *        Responder ID from user message. May neither be <code>null</code> nor
   *        empty.
   * @param sAgreementRef
   *        The agreement reference from the user message. May be
   *        <code>null</code>.
   * @param sAddress
   *        Endpoint address. May be <code>null</code>.
   * @return <code>null</code> if resolution failed.
   */
  @Nullable
  IPMode findPMode (@Nullable String sPModeID,
                    @Nonnull String sService,
                    @Nonnull String sAction,
                    @Nonnull @Nonempty String sInitiatorID,
                    @Nonnull @Nonempty String sResponderID,
                    @Nullable String sAgreementRef,
                    @Nullable String sAddress);
}
