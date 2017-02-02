package com.helger.as4.servlet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.state.EContinue;

public class AS4DuplicateChecker
{
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
    // TODO
    return EContinue.CONTINUE;
  }
}
