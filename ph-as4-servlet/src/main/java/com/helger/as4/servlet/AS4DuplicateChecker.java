package com.helger.as4.servlet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.collection.ext.CommonsHashSet;
import com.helger.commons.collection.ext.ICommonsSet;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.state.EContinue;

/**
 * This is the duplicate checker for receiving. <br>
 * TODO Duplicate check implementation is dumb because it uses an unlimited set
 * and does not persist the data. Suggestions for improvement: 1) limit number
 * of entries - ideally with a time constraint. 2) persist the data so that a
 * restart of the server does not alter working of duplicate checking. 3) Create
 * a job that regularly (e.g. every 10 minutes) removes all entries that are
 * older than a certain threshold.
 *
 * @author Philip Helger
 */
public final class AS4DuplicateChecker
{
  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  private static final ICommonsSet <String> s_aSet = new CommonsHashSet<> ();

  private AS4DuplicateChecker ()
  {}

  /**
   * Check if the passed message ID was already handled.
   *
   * @param sMessageID
   *        Message ID to check. May be <code>null</code>.
   * @return {@link EContinue#CONTINUE} to continue
   */
  @Nonnull
  public static EContinue registerAndCheck (@Nullable final String sMessageID)
  {
    final boolean bAdded = s_aRWLock.writeLocked ( () -> s_aSet.add (sMessageID));
    return bAdded ? EContinue.CONTINUE : EContinue.BREAK;
  }

  /**
   * Remove all entries in the cache.
   */
  public static void clearCache ()
  {
    s_aRWLock.writeLocked ( () -> s_aSet.clear ());
  }
}
