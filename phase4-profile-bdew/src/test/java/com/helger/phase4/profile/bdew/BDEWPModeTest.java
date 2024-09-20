/*
 * Copyright (C) 2023-2024 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
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
package com.helger.phase4.profile.bdew;

import static org.junit.Assert.assertNotNull;

import org.junit.ClassRule;
import org.junit.Test;

import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.PMode;
import com.helger.photon.app.mock.PhotonAppWebTestRule;

/**
 * Test class for class {@link BDEWPMode}.
 *
 * @author Gregor Scholtysik
 */
public final class BDEWPModeTest
{
  @ClassRule
  public static final PhotonAppWebTestRule RULE = new PhotonAppWebTestRule ();

  @Test
  public void testBDEWPMode ()
  {
    final PMode aPMode = BDEWPMode.createBDEWPMode ("TestInitiator",
                                                    BDEWPMode.BDEW_PARTY_ID_TYPE_BDEW,
                                                    "TestResponder",
                                                    BDEWPMode.BDEW_PARTY_ID_TYPE_BDEW,
                                                    "https://test.example.org",
                                                    IPModeIDProvider.DEFAULT_DYNAMIC,
                                                    false);
    assertNotNull (aPMode);
  }
}
