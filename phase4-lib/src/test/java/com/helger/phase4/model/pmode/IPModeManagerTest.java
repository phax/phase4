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
package com.helger.phase4.model.pmode;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.phase4.AS4TestRule;
import com.helger.phase4.mgr.MetaAS4Manager;

/**
 * Test class for class {@link IPModeManager}.
 *
 * @author bayerlma
 * @author Philip Helger
 */
public final class IPModeManagerTest
{
  @Rule
  public final TestRule m_aTestRule = new AS4TestRule ();

  @Test
  public void testBasic ()
  {
    final IPModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    assertNotNull (aPModeMgr);
  }
}
