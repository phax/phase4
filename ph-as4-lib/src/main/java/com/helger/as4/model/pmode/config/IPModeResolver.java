/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.model.pmode.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.model.pmode.IPMode;

/**
 * Resolve PMode from ID
 *
 * @author Philip Helger
 */
@FunctionalInterface
public interface IPModeResolver
{
  /**
   * Get the PMode config of the passed ID.
   *
   * @param sPModeID
   *        The direct PMode ID to be resolved. May be <code>null</code>.
   * @param sService
   *        The service as specified in the EBMS CollaborationInformation. May
   *        not be <code>null</code>.
   * @param sAction
   *        The action as specified in the EBMS CollaborationInformation. May
   *        not be <code>null</code>.
   * @return <code>null</code> if resolution failed.
   */
  @Nullable
  IPMode getPModeOfID (@Nullable String sPModeID, @Nonnull String sService, @Nonnull String sAction);
}
