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
package com.helger.phase4.client;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import com.helger.commons.state.EContinue;

/**
 * Callback to be informed on http retries
 *
 * @author Philip Helger
 * @since 0.9.14
 */
public interface IAS4RetryCallback
{
  /**
   * Invoked when it is clear that a retry will happen, but before the waiting
   * starts
   *
   * @param sMessageID
   *        The AS4 message ID. May not be <code>null</code>.
   * @param sURL
   *        The destination URL to which the transmission fails. May not be
   *        <code>null</code>.
   * @param nTry
   *        The current try that will be retried later, 0-based.
   * @param nMaxTries
   *        The maximum number of tries that will happen. 1-based. So e.g. 2
   *        means that there will be 1 retry: one original try and one retry. If
   *        the number is e.g. 11: one original try and 10 retries.
   * @param nRetryIntervalMS
   *        The milliseconds to be waited before the next retry.
   * @param ex
   *        The exception that occurred during sending. Usually an IOException.
   *        Never <code>null</code>.
   * @return {@link EContinue#CONTINUE} to continue with the procedure as
   *         foreseen, {@link EContinue#BREAK} to interrupt resending. May not
   *         be <code>null</code>.
   */
  @Nonnull
  EContinue onBeforeRetry (@Nonnull String sMessageID,
                           @Nonnull String sURL,
                           @Nonnegative int nTry,
                           @Nonnegative int nMaxTries,
                           long nRetryIntervalMS,
                           @Nonnull Exception ex);
}
