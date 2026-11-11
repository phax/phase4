/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.messaging.http;

/**
 * The classification of a failed HTTP transmission in respect to retrying it. The guiding question
 * is, whether the request could have reached the server.
 *
 * @author Philip Helger
 * @see HttpRetryClassifier
 * @since 4.9.0
 */
public enum EHttpRetryClassification
{
  /**
   * The request was never sent, or the server explicitly asked to try again later. A retry is
   * feasible for any HTTP method.
   */
  RETRY,
  /**
   * The request may have reached the server. A retry is only feasible, if replaying the request is
   * safe, e.g. because the receiver detects duplicates.
   */
  RETRY_IF_REPLAY_SAFE,
  /**
   * The error is deterministic or the operation was interrupted. A retry is never feasible.
   */
  NO_RETRY;

  /**
   * Check if a retry is feasible for this classification.
   *
   * @param bReplaySafe
   *        <code>true</code> if replaying the request is safe, <code>false</code> if not.
   * @return <code>true</code> if a retry is feasible, <code>false</code> if not.
   */
  public boolean isRetryFeasible (final boolean bReplaySafe)
  {
    return this == RETRY || (this == RETRY_IF_REPLAY_SAFE && bReplaySafe);
  }
}
