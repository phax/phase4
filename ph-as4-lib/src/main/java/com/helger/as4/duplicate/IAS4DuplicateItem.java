package com.helger.as4.duplicate;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;

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
  LocalDateTime getDateTime ();

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
   * @return The AS4 PMode ID in use. May be <code>null</code>.
   */
  @Nullable
  String getPModeID ();
}
