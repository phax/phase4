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
package com.helger.phase4.model.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.CAS4;

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
    assertTrue (StringHelper.hasText (sMessageID));
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
      assertTrue (StringHelper.hasText (sMessageID));
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
      assertTrue (StringHelper.hasText (sMessageID));
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
      assertTrue (StringHelper.hasText (sMessageID));
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
                                          "\t" })
      try
      {
        MessageHelperMethods.setCustomMessageIDSuffix (s);
        fail ("The message ID suffix '" + s + "' should be detected as invalid");
      }
      catch (final IllegalArgumentException ex)
      {
        // Expected
      }

    // At the end
    assertNull (MessageHelperMethods.getCustomMessageIDSuffix ());
  }
}
