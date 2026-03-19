/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.model.pmode.leg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.base.string.StringHelper;

/**
 * Test class for class {@link EPModeSendReceiptReplyPattern}.
 *
 * @author Philip Helger
 */
public final class EPModeSendReceiptReplyPatternTest
{
  @Test
  public void testBasic ()
  {
    for (final EPModeSendReceiptReplyPattern e : EPModeSendReceiptReplyPattern.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getID ()));
      assertSame (e, EPModeSendReceiptReplyPattern.getFromIDOrNull (e.getID ()));
    }
  }

  @Test
  public void testValues ()
  {
    assertEquals (2, EPModeSendReceiptReplyPattern.values ().length);
    assertEquals ("response", EPModeSendReceiptReplyPattern.RESPONSE.getID ());
    assertEquals ("callback", EPModeSendReceiptReplyPattern.CALLBACK.getID ());
  }

  @Test
  public void testUnknown ()
  {
    assertNull (EPModeSendReceiptReplyPattern.getFromIDOrNull ("does-not-exist"));
    assertNull (EPModeSendReceiptReplyPattern.getFromIDOrNull (null));
    assertNull (EPModeSendReceiptReplyPattern.getFromIDOrNull (""));
  }
}
