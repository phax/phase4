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
package com.helger.phase4.sender;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for class {@link EAS4UserMessageSendResult}.
 *
 * @author Philip Helger
 */
public final class EAS4UserMessageSendResultTest
{
  @Test
  public void testRetryFeasible ()
  {
    // A received AS4 Error Message (e.g. EBMS:4001 duplicate) must never lead
    // to a retry, because retrying would only produce the same error again
    assertFalse (EAS4UserMessageSendResult.AS4_ERROR_MESSAGE_RECEIVED.isRetryFeasible ());

    // Programming errors and definitive transport errors are not retryable
    assertFalse (EAS4UserMessageSendResult.INVALID_PARAMETERS.isRetryFeasible ());
    assertFalse (EAS4UserMessageSendResult.TRANSPORT_ERROR_NO_RETRY.isRetryFeasible ());

    // Success is obviously no reason to retry
    assertFalse (EAS4UserMessageSendResult.SUCCESS.isRetryFeasible ());

    // These are the only results for which a retry is recommended
    assertTrue (EAS4UserMessageSendResult.TRANSPORT_ERROR.isRetryFeasible ());
    assertTrue (EAS4UserMessageSendResult.NO_SIGNAL_MESSAGE_RECEIVED.isRetryFeasible ());
    assertTrue (EAS4UserMessageSendResult.INVALID_SIGNAL_MESSAGE_RECEIVED.isRetryFeasible ());
  }

  @Test
  public void testSuccess ()
  {
    for (final EAS4UserMessageSendResult e : EAS4UserMessageSendResult.values ())
      if (e == EAS4UserMessageSendResult.SUCCESS)
        assertTrue (e.isSuccess ());
      else
        assertFalse (e.isSuccess ());
  }
}
