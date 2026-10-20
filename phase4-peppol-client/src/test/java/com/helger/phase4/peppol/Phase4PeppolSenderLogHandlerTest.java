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
package com.helger.phase4.peppol;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.base.log.ILogHandler;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageBuilder;

/**
 * Test the log handler of {@link PeppolUserMessageBuilder}.
 *
 * @author Philip Helger
 */
public final class Phase4PeppolSenderLogHandlerTest
{
  @Test
  public void testLogHandler ()
  {
    final ICommonsList <String> aMessages = new CommonsArrayList <> ();
    final ILogHandler aLogHandler = ILogHandler.createForConsumer (aMessages::add);
    final PeppolUserMessageBuilder aBuilder = Phase4PeppolSender.builder ().logHandler (aLogHandler);
    assertSame (aLogHandler, aBuilder.logHandler ());

    // Nothing is set, so the handler must receive the name of a missing field
    assertFalse (aBuilder.isEveryRequiredFieldSet ());
    assertFalse (aMessages.isEmpty ());
    assertTrue (aMessages.get (0).contains ("is not set"));
  }
}
