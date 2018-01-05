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
package com.helger.as4.server;

import org.junit.Before;

import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.settings.Settings;

/**
 * The test classes for the usermessage, are split up for a better overview.
 * Since all these classes need the same setup and a helper method, this class
 * was created. Also with the help of Parameterized.class, each test will be
 * done for both SOAP Versions.
 *
 * @author bayerlma
 */
public abstract class AbstractClientSetUp
{
  protected static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger (AbstractClientSetUp.class);
  protected static final String DEFAULT_PARTY_ID = "APP_MOCK_DUMMY_001";

  protected Settings m_aSettings;

  @Before
  public void setUp ()
  {
    AS4ServerConfiguration.internalReinitForTestOnly ();

    m_aSettings = AS4ServerConfiguration.getMutableSettings ();

    // Create the mock PModes
    MockPModeGenerator.ensureMockPModesArePresent ();
  }
}
