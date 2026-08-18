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
package com.helger.phase4.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import org.junit.Test;

import com.helger.phase4.profile.IAS4ProfileValidator.ESignedPart;

/**
 * Test class for interface {@link IAS4ProfileValidator}.
 *
 * @author Philip Helger
 */
public final class IAS4ProfileValidatorTest
{
  @Test
  public void testDefaultRequiredSignedParts ()
  {
    // AS4 profile chapter 5.1.4 - Messaging header and the (possibly empty) SOAP Body
    final EnumSet <ESignedPart> aWithout = IAS4ProfileValidator.getDefaultRequiredSignedParts (false);
    assertEquals (EnumSet.of (ESignedPart.EBMS_MESSAGING, ESignedPart.SOAP_BODY), aWithout);

    // AS4 profile chapter 5.1.5 - Messaging header and all MIME body parts, but not the SOAP Body
    final EnumSet <ESignedPart> aWith = IAS4ProfileValidator.getDefaultRequiredSignedParts (true);
    assertEquals (EnumSet.of (ESignedPart.EBMS_MESSAGING, ESignedPart.ATTACHMENTS), aWith);
    assertFalse (aWith.contains (ESignedPart.SOAP_BODY));
  }

  @Test
  public void testDefaultImplementationOfInterface ()
  {
    // An implementation that does not override anything must use the generic AS4 rules
    final IAS4ProfileValidator aValidator = new IAS4ProfileValidator ()
    {};
    for (final boolean bHasAttachments : new boolean [] { true, false })
      assertEquals (IAS4ProfileValidator.getDefaultRequiredSignedParts (bHasAttachments),
                    aValidator.getRequiredSignedParts (bHasAttachments));

    // The returned set must be a mutable copy
    final EnumSet <ESignedPart> aParts = aValidator.getRequiredSignedParts (true);
    aParts.add (ESignedPart.SOAP_BODY);
    assertTrue (aValidator.getRequiredSignedParts (true).size () < aParts.size ());
  }
}
