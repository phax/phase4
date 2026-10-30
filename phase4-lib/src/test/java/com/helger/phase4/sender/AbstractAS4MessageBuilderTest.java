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
package com.helger.phase4.sender;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.phase4.AS4TestRule;
import com.helger.phase4.model.pmode.resolve.AS4DefaultPModeResolver;
import com.helger.phase4.sender.AS4Sender.BuilderPullRequest;

/**
 * Test class for class {@link AbstractAS4MessageBuilder}.
 *
 * @author Philip Helger
 */
public final class AbstractAS4MessageBuilderTest
{
  @Rule
  public final TestRule m_aRule = new AS4TestRule ();

  @NonNull
  private static BuilderPullRequest _createBuilder ()
  {
    return AS4Sender.builderPullRequest ()
                    .as4ProfileID ("test")
                    .pmodeResolver (new AS4DefaultPModeResolver (null))
                    .mpc ("mpc")
                    .endpointURL ("http://localhost/as4");
  }

  @Test
  public void testValidIDs ()
  {
    assertTrue (_createBuilder ().isEveryRequiredFieldSet ());
    assertTrue (_createBuilder ().messageID ("a@b").refToMessageID ("c@d").isEveryRequiredFieldSet ());
  }

  @Test
  public void testInvalidMessageIDIsRejected ()
  {
    for (final String s : new String [] { "abc", "   ", "<a@b>" })
      assertFalse ("The message ID '" + s + "' should be rejected",
                   _createBuilder ().messageID (s).isEveryRequiredFieldSet ());
  }

  @Test
  public void testInvalidRefToMessageIDIsAccepted ()
  {
    for (final String s : new String [] { "abc", "   ", "<a@b>" })
      assertTrue ("The referenced message ID '" + s + "' should be accepted",
                  _createBuilder ().refToMessageID (s).isEveryRequiredFieldSet ());
  }
}
