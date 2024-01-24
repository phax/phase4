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
package com.helger.phase4.duplicate;

import java.io.Serializable;
import java.time.OffsetDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.string.StringHelper;

/**
 * Base interface for a single duplication check item.
 *
 * @author Philip Helger
 */
public interface IAS4DuplicateItem extends IHasID <String>, Serializable
{
  /**
   * @return The date time when the entry was created. Never <code>null</code>.
   */
  @Nonnull
  OffsetDateTime getDateTime ();

  /**
   * @return The message ID. Neither <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  String getMessageID ();

  /**
   * @return The AS4 profile ID in use. May be <code>null</code>.
   */
  @Nullable
  String getProfileID ();

  /**
   * @return <code>true</code> if an AS4 profile ID is present,
   *         <code>false</code> if not.
   * @since 0.8.3
   */
  default boolean hasProfileID ()
  {
    return StringHelper.hasText (getProfileID ());
  }

  /**
   * @return The AS4 PMode ID in use. May be <code>null</code>.
   */
  @Nullable
  String getPModeID ();

  /**
   * @return <code>true</code> if an AS4 PMode ID is present, <code>false</code>
   *         if not.
   * @since 0.8.3
   */
  default boolean hasPModeID ()
  {
    return StringHelper.hasText (getPModeID ());
  }
}
