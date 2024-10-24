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
package com.helger.phase4.profile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;

/**
 * Base interface for an AS4 profile manager.
 *
 * @author Philip Helger
 * @since 0.10.4
 */
public interface IAS4ProfileManager extends IAS4ProfileRegistrar
{
  /**
   * @return A non-<code>null</code> but maybe empty list of all contained
   *         profiles.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <IAS4Profile> getAllProfiles ();

  /**
   * Find an existing profile with a certain ID.
   *
   * @param sID
   *        The ID to search. May be <code>null</code>.
   * @return <code>null</code> if no such profile exists
   */
  @Nullable
  IAS4Profile getProfileOfID (@Nullable String sID);
}
