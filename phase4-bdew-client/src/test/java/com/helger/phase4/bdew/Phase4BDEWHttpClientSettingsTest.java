/*
 * Copyright (C) 2023-2025 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
 *
 * Copyright (C) 2023-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.bdew;

import java.security.GeneralSecurityException;

import org.junit.Test;

/**
 * Test class for class {@link Phase4BDEWHttpClientSettings}
 *
 * @author Gregor Scholtysik
 */
public class Phase4BDEWHttpClientSettingsTest
{
  @Test
  public void testBasic () throws GeneralSecurityException
  {
    new Phase4BDEWHttpClientSettings ();
  }
}
