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
