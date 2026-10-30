/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

import com.helger.base.string.StringHelper;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.error.EEbmsErrorCategory;
import com.helger.phase4.model.error.EEbmsErrorSeverity;

/**
 * Test class for {@link EEbmsMultiHopError}.<br>
 * Covers R12 and D11 - the four multi-hop error codes of ebMS3 Part 2 appendix H.
 *
 * @option
 * @author Philip Helger
 */
public final class EEbmsMultiHopErrorTest
{
  /**
   * R12 - all four codes are recognised.
   */
  @Test
  public void testAllCodesAreResolvable ()
  {
    assertSame (EEbmsMultiHopError.EBMS_ROUTING_FAILURE, EEbmsMultiHopError.getFromErrorCodeOrNull ("EBMS:0020"));
    assertSame (EEbmsMultiHopError.EBMS_MPC_CAPACITY_EXCEEDED,
                EEbmsMultiHopError.getFromErrorCodeOrNull ("EBMS:0021"));
    assertSame (EEbmsMultiHopError.EBMS_MESSAGE_PERSISTENCE_TIMEOUT,
                EEbmsMultiHopError.getFromErrorCodeOrNull ("EBMS:0022"));
    assertSame (EEbmsMultiHopError.EBMS_MESSAGE_EXPIRED, EEbmsMultiHopError.getFromErrorCodeOrNull ("EBMS:0023"));

    assertNull (EEbmsMultiHopError.getFromErrorCodeOrNull ("EBMS:0001"));
    assertNull (EEbmsMultiHopError.getFromErrorCodeOrNull (null));
  }

  /**
   * D11 - 0020, 0021 and 0022 are failures, 0023 is a warning.
   */
  @Test
  public void testSeverities ()
  {
    assertSame (EEbmsErrorSeverity.FAILURE, EEbmsMultiHopError.EBMS_ROUTING_FAILURE.getSeverity ());
    assertSame (EEbmsErrorSeverity.FAILURE, EEbmsMultiHopError.EBMS_MPC_CAPACITY_EXCEEDED.getSeverity ());
    assertSame (EEbmsErrorSeverity.FAILURE, EEbmsMultiHopError.EBMS_MESSAGE_PERSISTENCE_TIMEOUT.getSeverity ());
    assertSame (EEbmsErrorSeverity.WARNING, EEbmsMultiHopError.EBMS_MESSAGE_EXPIRED.getSeverity ());
  }

  /**
   * D11 - the category is Communication for all of them.
   */
  @Test
  public void testCategories ()
  {
    for (final EEbmsMultiHopError e : EEbmsMultiHopError.values ())
      assertSame (EEbmsErrorCategory.COMMUNICATION, e.getCategory ());
  }

  /**
   * The codes must not collide with the built-in phase4 errors.
   */
  @Test
  public void testNoCollisionWithCoreErrors ()
  {
    for (final EEbmsMultiHopError e : EEbmsMultiHopError.values ())
      assertNull ("The multi-hop error code " + e.getErrorCode () + " collides with a core phase4 error",
                  EEbmsError.getFromErrorCodeOrNull (e.getErrorCode ()));
  }

  /**
   * Every constant must be usable to build an Ebms3Error.
   */
  @Test
  public void testErrorBuilder ()
  {
    for (final EEbmsMultiHopError e : EEbmsMultiHopError.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getShortDescription ()));
      assertNotNull (e.getDescription ());
      assertTrue (StringHelper.isNotEmpty (e.getDescription ().getDisplayText (Locale.US)));

      final var aEbms3Error = e.errorBuilder (Locale.US).build ();
      assertNotNull (aEbms3Error);
      assertEquals (e.getErrorCode (), aEbms3Error.getErrorCode ());
    }
  }
}
