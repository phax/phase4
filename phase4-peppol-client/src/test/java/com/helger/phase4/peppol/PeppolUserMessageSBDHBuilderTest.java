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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.util.Arrays;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.io.file.SimpleFileIO;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageSBDHBuilder;

/**
 * Test class for class {@link PeppolUserMessageSBDHBuilder}.
 *
 * @author Philip Helger
 */
public final class PeppolUserMessageSBDHBuilderTest
{
  private static final File SBDH_FILE = new File ("src/test/resources/external/examples/base-sbdh.xml");

  private static void _assertBaseSBDHMetadata (@NonNull final PeppolUserMessageSBDHBuilder aBuilder)
  {
    assertEquals ("iso6523-actorid-upis::9915:phase4-test-sender", aBuilder.m_aSenderID.getURIEncoded ());
    assertEquals ("iso6523-actorid-upis::9915:helger", aBuilder.m_aReceiverID.getURIEncoded ());
    assertEquals ("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1",
                  aBuilder.m_aDocTypeID.getURIEncoded ());
    assertEquals ("cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0",
                  aBuilder.m_aProcessID.getURIEncoded ());
    assertEquals ("GB", aBuilder.m_sCountryC1);
  }

  @Test
  public void testPayloadAndMetadataFromBytes () throws Exception
  {
    final byte [] aSBDHBytes = SimpleFileIO.getAllFileBytes (SBDH_FILE);
    assertNotNull (aSBDHBytes);

    final PeppolUserMessageSBDHBuilder aBuilder = Phase4PeppolSender.sbdhBuilder ().payloadAndMetadata (aSBDHBytes);

    // All metadata must have been extracted
    _assertBaseSBDHMetadata (aBuilder);

    // The original bytes must be used unaltered
    assertSame (aSBDHBytes, aBuilder.payloadBytes ());
  }

  @Test
  public void testPayloadAndMetadataFromData () throws Exception
  {
    final byte [] aSBDHBytes = SimpleFileIO.getAllFileBytes (SBDH_FILE);
    final PeppolSBDHData aData = new PeppolSBDHDataReader (Phase4PeppolSender.IF).extractData (new NonBlockingByteArrayInputStream (aSBDHBytes));

    final PeppolUserMessageSBDHBuilder aBuilder = Phase4PeppolSender.sbdhBuilder ().payloadAndMetadata (aData);

    // The same metadata is extracted as from the bytes
    _assertBaseSBDHMetadata (aBuilder);

    // But the SBDH was recreated, so the bytes differ from the original ones
    assertNotNull (aBuilder.payloadBytes ());
    assertFalse (Arrays.equals (aSBDHBytes, aBuilder.payloadBytes ()));
  }

  @Test
  public void testPayloadAndMetadataFromBytesWithCustomReader () throws Exception
  {
    final byte [] aSBDHBytes = SimpleFileIO.getAllFileBytes (SBDH_FILE);

    final PeppolUserMessageSBDHBuilder aBuilder = Phase4PeppolSender.sbdhBuilder ()
                                                                    .payloadAndMetadata (aSBDHBytes,
                                                                                         new PeppolSBDHDataReader (Phase4PeppolSender.IF).setPerformValueChecks (false));

    _assertBaseSBDHMetadata (aBuilder);
    assertArrayEquals (aSBDHBytes, aBuilder.payloadBytes ());
  }
}
