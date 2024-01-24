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
package com.helger.phase4.server;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.helger.phase4.ScopedAS4Configuration;
import com.helger.scope.ScopeHelper;

/**
 * The test classes for the usermessage, are split up for a better overview.
 * Since all these classes need the same setup and a helper method, this class
 * was created. Also with the help of Parameterized.class, each test will be
 * done for both SOAP Versions.
 *
 * @author bayerlma
 * @author Philip Helger
 */
public abstract class AbstractAS4TestSetUp
{
  private static ScopedAS4Configuration s_aSC;

  @BeforeClass
  public static void startTest () throws Exception
  {
    s_aSC = ScopedAS4Configuration.createTestConfig ();
  }

  @AfterClass
  public static void endTest () throws Exception
  {
    if (s_aSC != null)
      s_aSC.close ();
  }

  @Before
  public void setUp ()
  {
    if (false)
      ScopeHelper.setLifeCycleDebuggingEnabled (true);

    // Create the mock PModes
    MockPModeGenerator.ensureMockPModesArePresent ();
  }
}
