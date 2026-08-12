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
package com.helger.phase4.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.base.system.SystemProperties;
import com.helger.config.source.IConfigurationSource;
import com.helger.config.source.resource.IConfigurationSourceResource;
import com.helger.config.value.ConfiguredValue;

/**
 * Test class of class {@link AS4Configuration}.
 *
 * @author Philip Helger
 */
public final class AS4ConfigurationTest
{
  @Test
  public void testBasic ()
  {
    assertTrue (AS4Configuration.isUseInMemoryManagers ());
    assertTrue (AS4Configuration.isWSS4JSynchronizedSecurity ());

    final ConfiguredValue aCV = AS4Configuration.getConfig ().getConfiguredValue (AS4Configuration.PROPERTY_PHASE4_WSS4J_SYNCSECURITY);
    assertNotNull (aCV);

    final IConfigurationSource aCS = aCV.getConfigurationSource ();
    assertNotNull (aCS);
    assertTrue (aCS instanceof IConfigurationSourceResource);
    final IConfigurationSourceResource aCSR = (IConfigurationSourceResource) aCS;
    assertEquals ("phase4.properties", aCSR.getResource ().getPath ());
  }

  @Test
  public void testIncomingMimeMaxPartHeaderSizeBytes ()
  {
    // Default value
    assertEquals (AS4Configuration.DEFAULT_PHASE4_INCOMING_MIME_MAX_PART_HEADER_SIZE_BYTES,
                  AS4Configuration.getIncomingMimeMaxPartHeaderSizeBytes ());

    final String sKey = AS4Configuration.PROPERTY_PHASE4_INCOMING_MIME_MAX_PART_HEADER_SIZE_BYTES;
    try
    {
      // Valid custom value
      SystemProperties.setPropertyValue (sKey, 2048);
      assertEquals (2048, AS4Configuration.getIncomingMimeMaxPartHeaderSizeBytes ());

      // The minimum value itself is valid
      SystemProperties.setPropertyValue (sKey, AS4Configuration.MIN_PHASE4_INCOMING_MIME_MAX_PART_HEADER_SIZE_BYTES);
      assertEquals (AS4Configuration.MIN_PHASE4_INCOMING_MIME_MAX_PART_HEADER_SIZE_BYTES,
                    AS4Configuration.getIncomingMimeMaxPartHeaderSizeBytes ());

      // Values below the minimum are rejected - the default value is used
      SystemProperties.setPropertyValue (sKey, AS4Configuration.MIN_PHASE4_INCOMING_MIME_MAX_PART_HEADER_SIZE_BYTES - 1);
      assertEquals (AS4Configuration.DEFAULT_PHASE4_INCOMING_MIME_MAX_PART_HEADER_SIZE_BYTES,
                    AS4Configuration.getIncomingMimeMaxPartHeaderSizeBytes ());

      SystemProperties.setPropertyValue (sKey, 0);
      assertEquals (AS4Configuration.DEFAULT_PHASE4_INCOMING_MIME_MAX_PART_HEADER_SIZE_BYTES,
                    AS4Configuration.getIncomingMimeMaxPartHeaderSizeBytes ());

      SystemProperties.setPropertyValue (sKey, -1);
      assertEquals (AS4Configuration.DEFAULT_PHASE4_INCOMING_MIME_MAX_PART_HEADER_SIZE_BYTES,
                    AS4Configuration.getIncomingMimeMaxPartHeaderSizeBytes ());
    }
    finally
    {
      SystemProperties.removePropertyValue (sKey);
    }
  }

  @Test
  public void testIncomingSizeLimits ()
  {
    // Default values
    assertEquals (AS4Configuration.DEFAULT_PHASE4_INCOMING_MAX_MESSAGE_SIZE_BYTES,
                  AS4Configuration.getIncomingMaxMessageSizeBytes ());
    assertEquals (AS4Configuration.DEFAULT_PHASE4_INCOMING_MAX_ATTACHMENT_COUNT,
                  AS4Configuration.getIncomingMaxAttachmentCount ());
    assertEquals (AS4Configuration.DEFAULT_PHASE4_INCOMING_ATTACHMENT_MAX_SIZE_BYTES,
                  AS4Configuration.getIncomingAttachmentMaxSizeBytes ());
    assertEquals (AS4Configuration.DEFAULT_PHASE4_INCOMING_ATTACHMENT_MAX_DECOMPRESSED_SIZE_BYTES,
                  AS4Configuration.getIncomingAttachmentMaxDecompressedSizeBytes ());
    assertEquals (AS4Configuration.DEFAULT_PHASE4_INCOMING_ATTACHMENT_MAX_COMPRESSION_RATIO,
                  AS4Configuration.getIncomingAttachmentMaxCompressionRatio ());

    final String sKey = AS4Configuration.PROPERTY_PHASE4_INCOMING_MAX_MESSAGE_SIZE_BYTES;
    try
    {
      // Custom value
      SystemProperties.setPropertyValue (sKey, 12345);
      assertEquals (12345, AS4Configuration.getIncomingMaxMessageSizeBytes ());

      // A negative value means "no limit" and is passed through
      SystemProperties.setPropertyValue (sKey, -1);
      assertEquals (-1, AS4Configuration.getIncomingMaxMessageSizeBytes ());
    }
    finally
    {
      SystemProperties.removePropertyValue (sKey);
    }
  }

  @Test
  public void testIncomingSignatureRequireFullCoverage ()
  {
    // Secure by default
    assertTrue (AS4Configuration.isIncomingSignatureRequireFullCoverage ());

    final String sKey = AS4Configuration.PROPERTY_PHASE4_INCOMING_SIGNATURE_REQUIRE_FULL_COVERAGE;
    try
    {
      SystemProperties.setPropertyValue (sKey, "false");
      assertFalse (AS4Configuration.isIncomingSignatureRequireFullCoverage ());
    }
    finally
    {
      SystemProperties.removePropertyValue (sKey);
    }
  }
}
