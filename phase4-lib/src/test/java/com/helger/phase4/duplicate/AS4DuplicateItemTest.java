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
package com.helger.phase4.duplicate;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.helger.xml.mock.XMLTestHelper;

/**
 * Test class for class {@link AS4DuplicateItem}.
 *
 * @author Philip Helger
 */
public final class AS4DuplicateItemTest
{
  @Test
  public void testBasic ()
  {
    final AS4DuplicateItem x = new AS4DuplicateItem ("x", "profile", "pmode");
    assertEquals ("x", x.getID ());
    assertEquals ("x", x.getMessageID ());
    assertEquals ("profile", x.getProfileID ());
    assertEquals ("pmode", x.getPModeID ());
    XMLTestHelper.testMicroTypeConversion (x);
  }
}
