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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientManager;
import com.sun.net.httpserver.HttpServer;

/**
 * Test class for class {@link BasicHttpPoster}.
 *
 * @author Greg Taube
 */
public final class BasicHttpPosterTest
{
  @Nullable
  private static String _send (@NonNull final BasicHttpPoster aPoster, @NonNull final String sURL) throws IOException
  {
    return aPoster.sendGenericMessage (sURL,
                                       null,
                                       new StringEntity ("request", ContentType.TEXT_PLAIN),
                                       aResponse -> EntityUtils.toString (aResponse.getEntity ()),
                                       null);
  }

  @Test
  public void testSharedHttpClientManagerReusesConnection () throws IOException
  {
    final Set <Integer> aRemotePorts = ConcurrentHashMap.newKeySet ();
    final HttpServer aServer = HttpServer.create (new InetSocketAddress ("127.0.0.1", 0), 0);
    aServer.createContext ("/", aExchange -> {
      aRemotePorts.add (Integer.valueOf (aExchange.getRemoteAddress ().getPort ()));
      aExchange.getRequestBody ().readAllBytes ();
      final byte [] aResponse = "ok".getBytes (StandardCharsets.UTF_8);
      aExchange.sendResponseHeaders (200, aResponse.length);
      aExchange.getResponseBody ().write (aResponse);
      aExchange.close ();
    });
    aServer.start ();

    try (final HttpClientManager aHttpClientManager = new HttpClientManager (new HttpClientFactory ()))
    {
      final BasicHttpPoster aPoster = new BasicHttpPoster ();
      assertNull (aPoster.getSharedHttpClientManager ());
      assertSame (aPoster, aPoster.setSharedHttpClientManager (aHttpClientManager));
      assertSame (aHttpClientManager, aPoster.getSharedHttpClientManager ());

      final String sURL = "http://127.0.0.1:" + aServer.getAddress ().getPort () + '/';
      assertEquals ("ok", _send (aPoster, sURL));
      assertEquals ("ok", _send (aPoster, sURL));

      assertEquals (1, aRemotePorts.size ());
      assertFalse (aHttpClientManager.isClosed ());
    }
    finally
    {
      aServer.stop (0);
    }
  }
}
