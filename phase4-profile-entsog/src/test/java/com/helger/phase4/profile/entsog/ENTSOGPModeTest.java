/*
 * Copyright (C) 2015-2021 Pavel Rotek
 * pavel[dot]rotek[at]gmail[dot]com
 *
 * Copyright (C) 2021-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.entsog;

import static org.junit.Assert.assertNotNull;

import org.junit.ClassRule;
import org.junit.Test;

import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.PMode;
import com.helger.photon.app.mock.PhotonAppWebTestRule;

/**
 * Test class for class {@link ENTSOGPMode}.
 *
 * @author Pavel Rotek
 */
public final class ENTSOGPModeTest
{
  @ClassRule
  public static final PhotonAppWebTestRule RULE = new PhotonAppWebTestRule ();

  @Test
  public void testENTSOGPMode ()
  {
    final PMode aPMode = ENTSOGPMode.createENTSOGPMode ("TestInitiator",
                                                        "TestResponder",
                                                        "https://test.example.org",
                                                        IPModeIDProvider.DEFAULT_DYNAMIC,
                                                        false);
    assertNotNull (aPMode);
  }
}
