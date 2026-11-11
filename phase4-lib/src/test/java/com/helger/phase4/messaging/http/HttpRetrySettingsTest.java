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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;

import org.junit.Test;

import com.helger.unittest.support.TestHelper;

/**
 * Test class for class {@link HttpRetrySettings}.
 *
 * @author Philip Helger
 */
public final class HttpRetrySettingsTest
{
  @Test
  public void testDefaults ()
  {
    final HttpRetrySettings x = new HttpRetrySettings ();
    assertFalse (x.isRetryEnabled ());
    assertTrue (x.isReplaySafe ());
    assertEquals (HttpRetrySettings.DEFAULT_MAX_RETRIES_UNKNOWN_HOST, x.getMaxRetriesUnknownHost ());
    assertEquals (BigDecimal.ZERO, x.getJitterFactor ());
    assertEquals (HttpRetrySettings.DEFAULT_MAX_DURATION_BEFORE_RETRY, x.getMaxDurationBeforeRetry ());
    assertEquals (HttpRetrySettings.DEFAULT_MAX_RETRY_AFTER, x.getMaxRetryAfter ());
  }

  @Test
  public void testAssignFromAndEquals ()
  {
    final HttpRetrySettings x = new HttpRetrySettings ().setMaxRetries (3)
                                                        .setReplaySafe (false)
                                                        .setMaxRetriesUnknownHost (2)
                                                        .setJitterFactor (new BigDecimal ("0.2"))
                                                        .setMaxDurationBeforeRetry (Duration.ofMinutes (1))
                                                        .setMaxRetryAfter (Duration.ofSeconds (30));
    final HttpRetrySettings y = new HttpRetrySettings ();
    y.assignFrom (x);
    TestHelper.testDefaultImplementationWithEqualContentObject (x, y);
    TestHelper.testDefaultImplementationWithDifferentContentObject (x, new HttpRetrySettings ());

    final HttpRetrySettings z = new HttpRetrySettings ();
    z.assignFrom (x);
    z.setReplaySafe (true);
    TestHelper.testDefaultImplementationWithDifferentContentObject (x, z);
  }

  @Test
  public void testGetWithJitter ()
  {
    final Duration aDuration = Duration.ofSeconds (10);
    assertSame (aDuration, HttpRetrySettings.getWithJitter (aDuration, BigDecimal.ZERO));
    assertSame (Duration.ZERO, HttpRetrySettings.getWithJitter (Duration.ZERO, new BigDecimal ("0.5")));

    for (int i = 0; i < 100; ++i)
    {
      final long nMillis = HttpRetrySettings.getWithJitter (aDuration, new BigDecimal ("0.2")).toMillis ();
      assertTrue (Long.toString (nMillis), nMillis >= 8_000 && nMillis <= 12_000);
    }
  }
}
