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
package com.helger.phase4.messaging.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.junit.Test;

import com.helger.json.IJsonObject;
import com.helger.xml.microdom.IMicroElement;

/**
 * Test class for class {@link AS4ConnectionDetails}.
 *
 * @author Philip Helger
 */
public final class AS4ConnectionDetailsTest
{
  private static final int NO_PORT = AS4ConnectionDetails.PORT_UNDEFINED;

  @Test
  public void testEmpty ()
  {
    final AS4ConnectionDetails aDetails = new AS4ConnectionDetails (null, null, NO_PORT, null, NO_PORT, null);
    assertTrue (aDetails.isEmpty ());
    assertFalse (aDetails.hasHttpVersion ());
    assertFalse (aDetails.hasRemoteAddress ());
    assertFalse (aDetails.hasRemotePort ());
    assertFalse (aDetails.hasLocalAddress ());
    assertFalse (aDetails.hasLocalPort ());
    assertFalse (aDetails.hasTlsDetails ());
    assertNull (aDetails.getTlsDetails ());
    assertEquals (0, aDetails.getAsJsonObject ().size ());
  }

  @Test
  public void testPlainHttp ()
  {
    final AS4ConnectionDetails aDetails = new AS4ConnectionDetails ("HTTP/1.1",
                                                                    "10.0.0.1",
                                                                    80,
                                                                    "10.0.0.2",
                                                                    54321,
                                                                    null);
    assertFalse (aDetails.isEmpty ());
    assertEquals ("HTTP/1.1", aDetails.getHttpVersion ());
    assertEquals ("10.0.0.1", aDetails.getRemoteAddress ());
    assertEquals (80, aDetails.getRemotePort ());
    assertEquals ("10.0.0.2", aDetails.getLocalAddress ());
    assertEquals (54321, aDetails.getLocalPort ());
    assertFalse (aDetails.hasTlsDetails ());

    final IJsonObject aJson = aDetails.getAsJsonObject ();
    assertEquals (5, aJson.size ());
    assertEquals ("HTTP/1.1", aJson.getAsString (AS4ConnectionDetails.JSON_HTTP_VERSION));
    assertEquals ("10.0.0.1", aJson.getAsString (AS4ConnectionDetails.JSON_REMOTE_ADDRESS));
    assertEquals (80, aJson.getAsInt (AS4ConnectionDetails.JSON_REMOTE_PORT, -1));
    assertNull (aJson.get (AS4ConnectionDetails.JSON_TLS));
  }

  @Test
  public void testWithTls ()
  {
    final AS4TlsConnectionDetails aTls = new AS4TlsConnectionDetails ("TLSv1.3",
                                                                      "TLS_AES_256_GCM_SHA384",
                                                                      AS4TlsConnectionDetails.KEY_SIZE_UNDEFINED,
                                                                      null,
                                                                      null,
                                                                      "CN=ap.example.org",
                                                                      null,
                                                                      null,
                                                                      null);
    final AS4ConnectionDetails aDetails = new AS4ConnectionDetails ("HTTP/1.1",
                                                                    "10.0.0.1",
                                                                    443,
                                                                    null,
                                                                    NO_PORT,
                                                                    aTls);
    assertTrue (aDetails.hasTlsDetails ());
    assertSame (aTls, aDetails.getTlsDetails ());

    final IJsonObject aJson = aDetails.getAsJsonObject ();
    assertEquals (4, aJson.size ());
    final IJsonObject aJsonTls = aJson.getAsObject (AS4ConnectionDetails.JSON_TLS);
    assertEquals ("TLSv1.3", aJsonTls.getAsString (AS4TlsConnectionDetails.JSON_PROTOCOL));

    final IMicroElement aElement = aDetails.getAsMicroElement (null, "Connection");
    assertEquals ("Connection", aElement.getTagName ());
    assertEquals ("HTTP/1.1", aElement.getFirstChildElement ("HttpVersion").getTextContent ());
    assertEquals ("10.0.0.1", aElement.getFirstChildElement ("RemoteAddress").getTextContent ());
    assertNull (aElement.getFirstChildElement ("LocalAddress"));
    assertEquals ("TLSv1.3", aElement.getFirstChildElement ("Tls").getFirstChildElement ("Protocol").getTextContent ());
  }

  @Test
  public void testCreateFromHttpClientContext ()
  {
    assertNull (AS4ConnectionDetails.createFromHttpClientContext (null));
    // A fresh context contains nothing usable
    assertNull (AS4ConnectionDetails.createFromHttpClientContext (HttpClientContext.create ()));
  }
}
