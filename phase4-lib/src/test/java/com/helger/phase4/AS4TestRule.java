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
package com.helger.phase4;

import com.helger.photon.app.mock.PhotonAppWebTestRule;

/**
 * A special JUnit "rule" for the AS4 tests.
 *
 * @author Philip Helger
 */
public class AS4TestRule extends PhotonAppWebTestRule
{
  @Override
  public void before ()
  {
    super.before ();
    // your code here
  }

  @Override
  public void after ()
  {
    // your code here
    super.after ();
  }
}
