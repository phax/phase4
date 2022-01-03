/*
 * Copyright (C) 2015-2022 Philip Helger (www.helger.com)
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

  /**
   * @return <code>true</code> if an explicit default profile is present,
   *         <code>false</code> if not.
   */
  boolean hasDefaultProfile ();

  /**
   * @return The default profile. If none is set, and exactly one profile is
   *         present, it is used. <code>null</code> if no default is present and
   *         more than one profile is registered
   */
  @Nullable
  IAS4Profile getDefaultProfileOrNull ();

  /**
   * @return The default profile. If none is set, and exactly one profile is
   *         present, it is used. If no default profile is present and more than
   *         one profile is present an Exception is thrown.
   * @throws IllegalStateException
   *         If no default is present and more than one profile is registered
   */
  @Nonnull
  IAS4Profile getDefaultProfile ();

  /**
   * Set the default profile to be used.
   *
   * @param sDefaultProfileID
   *        The ID of the default profile. May be <code>null</code>.
   * @return <code>null</code> if no such profile is registered, the resolve
   *         profile otherwise.
   */
  @Nullable
  default IAS4Profile setDefaultProfileID (@Nullable final String sDefaultProfileID)
  {
    final IAS4Profile aDefault = getProfileOfID (sDefaultProfileID);
    setDefaultProfile (aDefault);
    return aDefault;
  }
}
