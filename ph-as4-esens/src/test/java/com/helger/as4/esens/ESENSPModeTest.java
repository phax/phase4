/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.esens;

import org.junit.ClassRule;
import org.junit.Test;

import com.helger.as4.model.pmode.IPModeIDProvider;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;

public final class ESENSPModeTest
{
  @ClassRule
  public static final PhotonBasicWebTestRule s_aRule = new PhotonBasicWebTestRule ();

  @Test
  public void testESENSPMode ()
  {
    ESENSPMode.createESENSPMode ("TestInitiator",
                                 "TestResponder",
                                 "https://test.example.org",
                                 IPModeIDProvider.DEFAULT_DYNAMIC);
  }

  @Test
  public void testESENSPModeTwoWay ()
  {
    ESENSPMode.createESENSPModeTwoWay ("TestInitiator",
                                       "TestResponder",
                                       "https://test.example.org",
                                       IPModeIDProvider.DEFAULT_DYNAMIC);
  }
}
