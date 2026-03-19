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
package com.helger.phase4.model.message;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.base.string.StringHelper;

/**
 * Test class for class {@link EAS4MessageType}.
 *
 * @author Philip Helger
 */
public final class EAS4MessageTypeTest
{
  @Test
  public void testBasic ()
  {
    for (final EAS4MessageType e : EAS4MessageType.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getID ()));
      assertTrue (StringHelper.isNotEmpty (e.getDisplayName ()));
      assertSame (e, EAS4MessageType.getFromIDOrNull (e.getID ()));
      // Each type is either a user message or a signal message
      assertTrue (e.isUserMessage () != e.isSignalMessage ());
    }
  }

  @Test
  public void testUserMessage ()
  {
    assertTrue (EAS4MessageType.USER_MESSAGE.isUserMessage ());
    assertFalse (EAS4MessageType.USER_MESSAGE.isSignalMessage ());
    assertFalse (EAS4MessageType.USER_MESSAGE.isReceiptOrError ());
  }

  @Test
  public void testSignalMessages ()
  {
    for (final EAS4MessageType e : new EAS4MessageType [] { EAS4MessageType.ERROR_MESSAGE,
                                                             EAS4MessageType.PULL_REQUEST,
                                                             EAS4MessageType.RECEIPT })
    {
      assertFalse (e.isUserMessage ());
      assertTrue (e.isSignalMessage ());
    }
  }

  @Test
  public void testReceiptOrError ()
  {
    assertTrue (EAS4MessageType.RECEIPT.isReceiptOrError ());
    assertTrue (EAS4MessageType.ERROR_MESSAGE.isReceiptOrError ());
    assertFalse (EAS4MessageType.PULL_REQUEST.isReceiptOrError ());
    assertFalse (EAS4MessageType.USER_MESSAGE.isReceiptOrError ());
  }

  @Test
  public void testUnknown ()
  {
    assertNull (EAS4MessageType.getFromIDOrNull ("does-not-exist"));
    assertNull (EAS4MessageType.getFromIDOrNull (null));
  }

  @Test
  public void testDisplayNames ()
  {
    assertNotNull (EAS4MessageType.USER_MESSAGE.getDisplayName ());
    assertNotNull (EAS4MessageType.RECEIPT.getDisplayName ());
    assertNotNull (EAS4MessageType.ERROR_MESSAGE.getDisplayName ());
    assertNotNull (EAS4MessageType.PULL_REQUEST.getDisplayName ());
  }
}
