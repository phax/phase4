/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import java.time.LocalDateTime;
import java.util.function.Predicate;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.state.EContinue;

/**
 * Base interface for an AS4 duplication manager
 *
 * @author Philip Helger
 * @since 0.9.6
 */
public interface IAS4DuplicateManager
{
  boolean isEmpty ();

  @Nonnegative
  int size ();

  @Nullable
  IAS4DuplicateItem findFirst (@Nullable Predicate <? super IAS4DuplicateItem> aFilter);

  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <IAS4DuplicateItem> getAll ();

  /**
   * Check if the passed message ID was already handled.
   *
   * @param sMessageID
   *        Message ID to check. May be <code>null</code>.
   * @param sProfileID
   *        Active AS4 profile ID. May be used to define the PMode further. May
   *        be <code>null</code>.
   * @param sPModeID
   *        Active AS4 PMode ID. May be <code>null</code>.
   * @return {@link EContinue#CONTINUE} to continue
   */
  @Nonnull
  EContinue registerAndCheck (@Nullable String sMessageID, @Nullable String sProfileID, @Nullable String sPModeID);

  /**
   * Remove all entries in the cache.
   *
   * @return {@link EChange}
   */
  @Nonnull
  EChange clearCache ();

  /**
   * Delete all duplicate items that were created before the provided time.
   *
   * @param aRefDT
   *        The reference date time to compare to. May not be <code>null</code>.
   * @return A non-<code>null</code> list of all evicted message IDs.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <String> evictAllItemsBefore (@Nonnull LocalDateTime aRefDT);
}
