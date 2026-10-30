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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.OffsetDateTime;

import org.junit.Assume;
import org.junit.Test;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsSet;
import com.helger.phase4.CAS4;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.config.AS4Configuration;

/**
 * Test class for class {@link MessageHelperMethods}.
 *
 * @author Philip Helger
 */
public final class MessageHelperMethodsTest
{
  @Test
  public void testBasic ()
  {
    assertEquals ("cid:", MessageHelperMethods.PREFIX_CID);
  }

  @Test
  public void testCreateRandomMessageID ()
  {
    assertNull (MessageHelperMethods.getCustomMessageIDSuffix ());
    String sMessageID = MessageHelperMethods.createRandomMessageID ();
    assertNotNull (sMessageID);
    assertTrue (StringHelper.isNotEmpty (sMessageID));
    assertTrue (sMessageID.contains ("@"));
    assertTrue (sMessageID.contains (CAS4.LIB_NAME));
    assertTrue (sMessageID.endsWith (CAS4.LIB_NAME));

    // Check uniqueness
    final ICommonsSet <String> aSet = new CommonsHashSet <> ();
    aSet.add (sMessageID);
    for (int i = 0; i < 1000; ++i)
    {
      final String s = MessageHelperMethods.createRandomMessageID ();
      assertTrue ("Message ID duplicate: " + s, aSet.add (s));
    }

    // Test with a custom suffix
    MessageHelperMethods.setCustomMessageIDSuffix ("super.Company12");
    try
    {
      sMessageID = MessageHelperMethods.createRandomMessageID ();
      assertNotNull (sMessageID);
      assertTrue (StringHelper.isNotEmpty (sMessageID));
      assertTrue (sMessageID.contains ("@"));
      assertTrue (sMessageID.contains (CAS4.LIB_NAME));
      assertTrue (sMessageID.contains ("super.Company12"));
      assertTrue (sMessageID.endsWith ("@" + CAS4.LIB_NAME + ".super.Company12"));
    }
    finally
    {
      MessageHelperMethods.setCustomMessageIDSuffix (null);
    }

    // Custom suffix with a leading dot
    MessageHelperMethods.setCustomMessageIDSuffix (".Company12");
    try
    {
      sMessageID = MessageHelperMethods.createRandomMessageID ();
      assertNotNull (sMessageID);
      assertTrue (StringHelper.isNotEmpty (sMessageID));
      assertTrue (sMessageID.contains ("@"));
      assertTrue (sMessageID.contains (CAS4.LIB_NAME));
      assertTrue (sMessageID.contains ("Company12"));
      assertTrue (sMessageID.endsWith ("@" + CAS4.LIB_NAME + ".Company12"));
    }
    finally
    {
      MessageHelperMethods.setCustomMessageIDSuffix (null);
    }

    // Custom suffix that will be stripped to "no suffix"
    MessageHelperMethods.setCustomMessageIDSuffix (".");
    try
    {
      sMessageID = MessageHelperMethods.createRandomMessageID ();
      assertNotNull (sMessageID);
      assertTrue (StringHelper.isNotEmpty (sMessageID));
      assertTrue (sMessageID.contains ("@"));
      assertTrue (sMessageID.contains (CAS4.LIB_NAME));
      assertTrue (sMessageID.endsWith ("@" + CAS4.LIB_NAME));
    }
    finally
    {
      MessageHelperMethods.setCustomMessageIDSuffix (null);
    }

    // Invalid suffixes
    for (final String s : new String [] { " ",
                                          "ab c",
                                          "<",
                                          ">",
                                          "(",
                                          ")",
                                          "@",
                                          ",",
                                          ";",
                                          ":",
                                          "\\",
                                          "\"",
                                          "[",
                                          "]",
                                          "\t",
                                          "a.",
                                          "a..b",
                                          ".." })
      try
      {
        MessageHelperMethods.setCustomMessageIDSuffix (s);
        if (AS4Configuration.isEnforceRfc2822MessageIds ()) {
          fail ("The message ID suffix '" + s + "' should be detected as invalid");
        }
      }
      catch (final IllegalArgumentException ex)
      {
        // Expected
      }

    // At the end
    if (AS4Configuration.isEnforceRfc2822MessageIds ()) {
      assertNull (MessageHelperMethods.getCustomMessageIDSuffix ());
    }
  }

  @Test
  public void testIsValidMessageID ()
  {
    Assume.assumeTrue(AS4Configuration.isEnforceRfc2822MessageIds());
    // Created IDs must always be valid
    assertTrue (MessageHelperMethods.isValidMessageID (MessageHelperMethods.createRandomMessageID ()));
    MessageHelperMethods.setCustomMessageIDSuffix ("super.Company12");
    try
    {
      assertTrue (MessageHelperMethods.isValidMessageID (MessageHelperMethods.createRandomMessageID ()));
    }
    finally
    {
      MessageHelperMethods.setCustomMessageIDSuffix (null);
    }

    for (final String s : new String [] { "a@b",
                                          "UUID-1@requester.example.com",
                                          "0c5307dc-5f80-49f5-94ac-2422a8e0d595@phase4",
                                          "2011-921@5209999001264.example.com",
                                          "a.b.c@d.e.f",
                                          "!#$%&'*+/=?^_`{|}~-@x",
                                          "\"quoted.id\"@example.com",
                                          "\"quoted\\\"escaped\"@example.com",
                                          "\"quoted\\ space\"@example.com",
                                          "\"a@b\"@example.com",
                                          "id@[192.168.0.1]",
                                          "id@[IPv6:2001:db8::1]" })
      assertTrue ("The message ID '" + s + "' should be valid", MessageHelperMethods.isValidMessageID (s));

    for (final String s : new String [] { null,
                                          "",
                                          " ",
                                          "   ",
                                          "\t",
                                          "abc",
                                          "28a699c5-c7ae-4731-b0b8-ebf3b4bc8477",
                                          "@",
                                          "@b",
                                          "a@",
                                          "a@b@c",
                                          " a@b",
                                          "a@b ",
                                          "a @b",
                                          "a@b c",
                                          "<a@b>",
                                          "<a@b",
                                          "a@b>",
                                          ".a@b",
                                          "a.@b",
                                          "a..b@c",
                                          "a@.b",
                                          "a@b.",
                                          "a@b..c",
                                          "a,b@c",
                                          "a;b@c",
                                          "a:b@c",
                                          "a(b)@c",
                                          "a[b]@c",
                                          "a\\b@c",
                                          "\"unterminated@b",
                                          "\"quoted id\"@example.com",
                                          "a@[1.2.3.4 ]",
                                          "\"a\"b\"@c",
                                          "a@[unterminated",
                                          "a@[a]b]",
                                          "a@b\nc",
                                          "ä@b",
                                          "a@ä" })
      assertFalse ("The message ID '" + s + "' should be invalid", MessageHelperMethods.isValidMessageID (s));
  }

  @Test
  public void testCreateEbms3MessageInfo ()
  {
    Assume.assumeTrue(AS4Configuration.isEnforceRfc2822MessageIds());

    final OffsetDateTime aNow = OffsetDateTime.now ();
    Ebms3MessageInfo aMI = MessageHelperMethods.createEbms3MessageInfo ("a@b", "c@d", aNow);
    assertEquals ("a@b", aMI.getMessageId ());
    assertEquals ("c@d", aMI.getRefToMessageId ());

    // An invalid referenced ID is sent unchanged (only a warning is logged)
    aMI = MessageHelperMethods.createEbms3MessageInfo ("a@b", "1234", aNow);
    assertEquals ("1234", aMI.getRefToMessageId ());

    // An invalid own ID is rejected
    for (final String s : new String [] { "abc", " a@b", "<a@b>", "   " })
      try
      {
        MessageHelperMethods.createEbms3MessageInfo (s, null, aNow);
        fail ("The message ID '" + s + "' should be rejected");
      }
      catch (final IllegalArgumentException ex)
      {
        // Expected
      }
  }
}
