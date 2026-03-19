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
package com.helger.phase4.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.helger.base.string.StringHelper;

/**
 * Test class for class {@link ESoapVersion}.
 *
 * @author Philip Helger
 */
public final class ESoapVersionTest
{
  @Test
  public void testBasic ()
  {
    for (final ESoapVersion e : ESoapVersion.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getNamespaceURI ()));
      assertTrue (StringHelper.isNotEmpty (e.getNamespacePrefix ()));
      assertTrue (StringHelper.isNotEmpty (e.getVersion ()));
      assertNotNull (e.getMimeType ());
      assertNotNull (e.getHeaderElementName ());
      assertNotNull (e.getBodyElementName ());
      assertSame (e, ESoapVersion.getFromVersionOrNull (e.getVersion ()));
      assertSame (e, ESoapVersion.getFromNamespaceURIOrNull (e.getNamespaceURI ()));
      assertSame (e, ESoapVersion.getFromMimeTypeOrNull (e.getMimeType ()));
    }
  }

  @Test
  public void testSoap11 ()
  {
    assertEquals ("1.1", ESoapVersion.SOAP_11.getVersion ());
    assertEquals ("S11", ESoapVersion.SOAP_11.getNamespacePrefix ());
    assertFalse (ESoapVersion.SOAP_11.isAS4Default ());
    assertEquals ("1", ESoapVersion.SOAP_11.getMustUnderstandValue (true));
    assertEquals ("0", ESoapVersion.SOAP_11.getMustUnderstandValue (false));
  }

  @Test
  public void testSoap12 ()
  {
    assertEquals ("1.2", ESoapVersion.SOAP_12.getVersion ());
    assertEquals ("S12", ESoapVersion.SOAP_12.getNamespacePrefix ());
    assertTrue (ESoapVersion.SOAP_12.isAS4Default ());
    assertEquals ("true", ESoapVersion.SOAP_12.getMustUnderstandValue (true));
    assertEquals ("false", ESoapVersion.SOAP_12.getMustUnderstandValue (false));
  }

  @Test
  public void testAS4Default ()
  {
    assertSame (ESoapVersion.SOAP_12, ESoapVersion.AS4_DEFAULT);
  }

  @Test
  public void testMimeTypeWithCharset ()
  {
    assertNotNull (ESoapVersion.SOAP_12.getMimeType (StandardCharsets.UTF_8));
  }

  @Test
  public void testUnknown ()
  {
    assertNull (ESoapVersion.getFromVersionOrNull (null));
    assertNull (ESoapVersion.getFromVersionOrNull (""));
    assertNull (ESoapVersion.getFromVersionOrNull ("9.9"));
    assertNull (ESoapVersion.getFromNamespaceURIOrNull (null));
    assertNull (ESoapVersion.getFromNamespaceURIOrNull (""));
    assertNull (ESoapVersion.getFromMimeTypeOrNull (null));
    assertSame (ESoapVersion.SOAP_12,
                ESoapVersion.getFromVersionOrDefault ("does-not-exist", ESoapVersion.SOAP_12));
  }
}
