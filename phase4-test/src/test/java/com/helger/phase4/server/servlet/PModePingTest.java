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
package com.helger.phase4.server.servlet;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.model.ESoapVersion;

public final class PModePingTest extends AbstractUserMessageTestSetUpExt
{
  // Can only check success, since we cannot check if SPIs got called or not
  @Test
  public void testUsePModePingSuccessful () throws Exception
  {
    final Document aDoc = modifyUserMessage (null, null, null, createDefaultProperties (), null, null, null);

    final String ret = sendPlainMessage (new HttpXMLEntity (aDoc, ESoapVersion.AS4_DEFAULT.getMimeType ()), true, null);
    assertNotNull (ret);
  }
}
