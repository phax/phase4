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
package com.helger.phase4.multihop.incoming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.Duration;

import org.junit.Test;

import com.helger.phase4.multihop.AS4MultiHopConfig;

/**
 * Test class for {@link MultiHopSentMessageStore}.<br>
 * Covers D12 - the bounded, expiring in-memory store.
 *
 * @author Philip Helger
 */
public final class MultiHopSentMessageStoreTest
{
  @Test
  public void testRememberAndFind ()
  {
    final MultiHopSentMessageStore aStore = new MultiHopSentMessageStore (new AS4MultiHopConfig ());
    aStore.rememberSentMessage ("msg-1", "pmode-1");

    assertEquals ("pmode-1", aStore.getPModeIDOfSentMessage ("msg-1"));
    assertNull (aStore.getPModeIDOfSentMessage ("msg-unknown"));
    assertNull (aStore.getPModeIDOfSentMessage (null));
    assertEquals (1, aStore.size ());
  }

  /**
   * D12 - the oldest entries are dropped once the configured maximum size is exceeded.
   */
  @Test
  public void testCapacityLimit ()
  {
    final AS4MultiHopConfig aCfg = new AS4MultiHopConfig ().setSentMessageStoreMaxSize (3);
    final MultiHopSentMessageStore aStore = new MultiHopSentMessageStore (aCfg);

    for (int i = 1; i <= 5; ++i)
      aStore.rememberSentMessage ("msg-" + i, "pmode-" + i);

    assertEquals (3, aStore.size ());

    // The two oldest are gone
    assertNull (aStore.getPModeIDOfSentMessage ("msg-1"));
    assertNull (aStore.getPModeIDOfSentMessage ("msg-2"));

    // The three newest survived
    assertEquals ("pmode-3", aStore.getPModeIDOfSentMessage ("msg-3"));
    assertEquals ("pmode-4", aStore.getPModeIDOfSentMessage ("msg-4"));
    assertEquals ("pmode-5", aStore.getPModeIDOfSentMessage ("msg-5"));
  }

  /**
   * D12 - entries older than the configured maximum age are not handed out any more.
   */
  @Test
  public void testExpiry () throws Exception
  {
    final AS4MultiHopConfig aCfg = new AS4MultiHopConfig ().setSentMessageStoreMaxAge (Duration.ofMillis (50));
    final MultiHopSentMessageStore aStore = new MultiHopSentMessageStore (aCfg);

    aStore.rememberSentMessage ("msg-old", "pmode-old");
    assertNotNull (aStore.getPModeIDOfSentMessage ("msg-old"));

    Thread.sleep (120);

    assertNull ("An expired entry must not be handed out", aStore.getPModeIDOfSentMessage ("msg-old"));

    // Adding a new entry also purges the expired one
    aStore.rememberSentMessage ("msg-new", "pmode-new");
    assertEquals (1, aStore.size ());
    assertEquals ("pmode-new", aStore.getPModeIDOfSentMessage ("msg-new"));
  }

  /**
   * Re-remembering the same message ID updates the PMode and refreshes the position.
   */
  @Test
  public void testReRemember ()
  {
    final AS4MultiHopConfig aCfg = new AS4MultiHopConfig ().setSentMessageStoreMaxSize (2);
    final MultiHopSentMessageStore aStore = new MultiHopSentMessageStore (aCfg);

    aStore.rememberSentMessage ("msg-1", "pmode-1");
    aStore.rememberSentMessage ("msg-2", "pmode-2");
    // Refresh msg-1, so that msg-2 becomes the oldest
    aStore.rememberSentMessage ("msg-1", "pmode-1b");
    aStore.rememberSentMessage ("msg-3", "pmode-3");

    assertEquals (2, aStore.size ());
    assertEquals ("pmode-1b", aStore.getPModeIDOfSentMessage ("msg-1"));
    assertNull (aStore.getPModeIDOfSentMessage ("msg-2"));
    assertEquals ("pmode-3", aStore.getPModeIDOfSentMessage ("msg-3"));
  }

  @Test
  public void testClear ()
  {
    final MultiHopSentMessageStore aStore = new MultiHopSentMessageStore (new AS4MultiHopConfig ());
    aStore.rememberSentMessage ("msg-1", "pmode-1");
    assertEquals (1, aStore.size ());

    aStore.clear ();
    assertEquals (0, aStore.size ());
    assertNull (aStore.getPModeIDOfSentMessage ("msg-1"));
  }

  @Test
  public void testDefaultInstance ()
  {
    assertNotNull (MultiHopSentMessageStore.getDefaultInstance ());
    assertEquals (MultiHopSentMessageStore.getDefaultInstance (), MultiHopSentMessageStore.getDefaultInstance ());
  }
}
