/*
 * Copyright (C) 2019-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.peppol;

import static org.junit.Assert.assertNotNull;

import java.security.cert.CRL;

import org.junit.Test;

/**
 * Test class for class {@link PeppolCRLDownloader}
 *
 * @author Philip Helger
 */
public final class PeppolCRLDownloaderTest
{
  @Test
  public void testDownloadCRL ()
  {
    final PeppolCRLDownloader aDL = new PeppolCRLDownloader ();
    final CRL aCRL = aDL.downloadCRL ("http://pki-crl.symauth.com/ca_b6d0dc1dc3147723fe36b7575997afc4/LatestCRL.crl");
    assertNotNull (aCRL);
  }
}
