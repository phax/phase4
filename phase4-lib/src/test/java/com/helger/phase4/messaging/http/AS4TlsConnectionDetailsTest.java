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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.json.IJsonObject;
import com.helger.xml.microdom.IMicroElement;

/**
 * Test class for class {@link AS4TlsConnectionDetails}.
 *
 * @author Philip Helger
 */
public final class AS4TlsConnectionDetailsTest
{
  private static final int NO_KEY_SIZE = AS4TlsConnectionDetails.KEY_SIZE_UNDEFINED;

  @Test
  public void testEmpty ()
  {
    final AS4TlsConnectionDetails aDetails = new AS4TlsConnectionDetails (null,
                                                                          null,
                                                                          NO_KEY_SIZE,
                                                                          null,
                                                                          null,
                                                                          null,
                                                                          null,
                                                                          null,
                                                                          null);
    assertTrue (aDetails.isEmpty ());
    assertFalse (aDetails.hasProtocol ());
    assertFalse (aDetails.hasCipherSuite ());
    assertFalse (aDetails.hasKeySize ());
    assertFalse (aDetails.hasSessionID ());
    assertFalse (aDetails.hasRequestedServerNames ());
    assertFalse (aDetails.hasPeerPrincipal ());
    assertFalse (aDetails.hasPeerCerts ());
    assertFalse (aDetails.hasLocalPrincipal ());
    assertFalse (aDetails.hasLocalCerts ());
    assertEquals (NO_KEY_SIZE, aDetails.getKeySize ());
    assertNull (aDetails.peerCerts ());
    assertNull (aDetails.localCerts ());
    assertNull (aDetails.requestedServerNames ());
    assertEquals (0, aDetails.getAsJsonObject ().size ());
  }

  @Test
  public void testOutgoing ()
  {
    // As it looks for an outgoing connection - no key size available
    final AS4TlsConnectionDetails aDetails = new AS4TlsConnectionDetails ("TLSv1.3",
                                                                          "TLS_AES_256_GCM_SHA384",
                                                                          NO_KEY_SIZE,
                                                                          "abcdef01",
                                                                          new CommonsArrayList <> ("ap.example.org"),
                                                                          "CN=ap.example.org",
                                                                          null,
                                                                          "CN=me",
                                                                          null);
    assertFalse (aDetails.isEmpty ());
    assertEquals ("TLSv1.3", aDetails.getProtocol ());
    assertEquals ("TLS_AES_256_GCM_SHA384", aDetails.getCipherSuite ());
    assertFalse (aDetails.hasKeySize ());
    assertEquals ("abcdef01", aDetails.getSessionID ());
    assertEquals (new CommonsArrayList <> ("ap.example.org"), aDetails.requestedServerNames ());
    assertEquals ("CN=ap.example.org", aDetails.getPeerPrincipal ());
    assertEquals ("CN=me", aDetails.getLocalPrincipal ());

    final IJsonObject aJson = aDetails.getAsJsonObject ();
    assertEquals (6, aJson.size ());
    assertEquals ("TLSv1.3", aJson.getAsString (AS4TlsConnectionDetails.JSON_PROTOCOL));
    assertEquals ("TLS_AES_256_GCM_SHA384", aJson.getAsString (AS4TlsConnectionDetails.JSON_CIPHER_SUITE));
    assertNull (aJson.get (AS4TlsConnectionDetails.JSON_KEY_SIZE));
    assertEquals ("abcdef01", aJson.getAsString (AS4TlsConnectionDetails.JSON_SESSION_ID));
    assertEquals (1, aJson.getAsArray (AS4TlsConnectionDetails.JSON_REQUESTED_SERVER_NAMES).size ());
    assertEquals ("CN=ap.example.org", aJson.getAsString (AS4TlsConnectionDetails.JSON_PEER_PRINCIPAL));
    assertEquals ("CN=me", aJson.getAsString (AS4TlsConnectionDetails.JSON_LOCAL_PRINCIPAL));

    final IMicroElement aElement = aDetails.getAsMicroElement (null, "Tls");
    assertEquals ("Tls", aElement.getTagName ());
    assertEquals ("TLSv1.3", aElement.getFirstChildElement ("Protocol").getTextContent ());
    assertEquals ("TLS_AES_256_GCM_SHA384", aElement.getFirstChildElement ("CipherSuite").getTextContent ());
    assertNull (aElement.getFirstChildElement ("KeySize"));
    assertEquals ("abcdef01", aElement.getFirstChildElement ("SessionID").getTextContent ());
    assertEquals ("ap.example.org",
                  aElement.getFirstChildElement ("RequestedServerNames")
                          .getFirstChildElement ("ServerName")
                          .getTextContent ());
    assertEquals ("CN=ap.example.org", aElement.getFirstChildElement ("PeerPrincipal").getTextContent ());
    assertEquals ("CN=me", aElement.getFirstChildElement ("LocalPrincipal").getTextContent ());
  }

  @Test
  public void testIncoming ()
  {
    // As it looks for an incoming request - no protocol version available
    final AS4TlsConnectionDetails aDetails = new AS4TlsConnectionDetails (null,
                                                                          "TLS_AES_128_GCM_SHA256",
                                                                          128,
                                                                          "0011",
                                                                          null,
                                                                          null,
                                                                          null,
                                                                          null,
                                                                          null);
    assertFalse (aDetails.isEmpty ());
    assertFalse (aDetails.hasProtocol ());
    assertTrue (aDetails.hasKeySize ());
    assertEquals (128, aDetails.getKeySize ());

    final IJsonObject aJson = aDetails.getAsJsonObject ();
    assertEquals (3, aJson.size ());
    assertNull (aJson.get (AS4TlsConnectionDetails.JSON_PROTOCOL));
    assertEquals (128, aJson.getAsInt (AS4TlsConnectionDetails.JSON_KEY_SIZE, -1));
  }

  @Test
  public void testNegativeKeySizeIsUndefined ()
  {
    final AS4TlsConnectionDetails aDetails = new AS4TlsConnectionDetails (null,
                                                                          null,
                                                                          -42,
                                                                          null,
                                                                          null,
                                                                          null,
                                                                          null,
                                                                          null,
                                                                          null);
    assertEquals (NO_KEY_SIZE, aDetails.getKeySize ());
    assertFalse (aDetails.hasKeySize ());
    assertTrue (aDetails.isEmpty ());
  }

  @Test
  public void testEmptyListsAreNull ()
  {
    final AS4TlsConnectionDetails aDetails = new AS4TlsConnectionDetails (null,
                                                                          null,
                                                                          NO_KEY_SIZE,
                                                                          null,
                                                                          new CommonsArrayList <> (),
                                                                          null,
                                                                          new CommonsArrayList <> (),
                                                                          null,
                                                                          new CommonsArrayList <> ());
    assertFalse (aDetails.hasRequestedServerNames ());
    assertFalse (aDetails.hasPeerCerts ());
    assertFalse (aDetails.hasLocalCerts ());
    assertTrue (aDetails.isEmpty ());
  }

  @Test
  public void testCreateFromSSLSessionNull ()
  {
    assertNull (AS4TlsConnectionDetails.createFromSSLSession (null));
  }
}
