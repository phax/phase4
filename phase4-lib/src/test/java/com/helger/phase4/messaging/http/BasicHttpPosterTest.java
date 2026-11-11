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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.base.numeric.mutable.MutableBoolean;
import com.helger.base.state.EContinue;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientManager;
import com.helger.phase4.client.IAS4RetryCallback;
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

  /**
   * Start a server that answers with the provided status codes in order. The last one is repeated.
   * A status code entry may be followed by a Retry-After value, separated by a slash.
   */
  @NonNull
  private static HttpServer _startServer (@NonNull final AtomicInteger aRequestCount,
                                          @NonNull final String... aResponses) throws IOException
  {
    final HttpServer aServer = HttpServer.create (new InetSocketAddress ("127.0.0.1", 0), 0);
    aServer.createContext ("/", aExchange -> {
      final int nIndex = aRequestCount.getAndIncrement ();
      final String [] aParts = aResponses[Math.min (nIndex, aResponses.length - 1)].split ("/");
      aExchange.getRequestBody ().readAllBytes ();
      if (aParts.length > 1)
        aExchange.getResponseHeaders ().add ("Retry-After", aParts[1]);
      final byte [] aResponse = ("try" + nIndex).getBytes (StandardCharsets.UTF_8);
      aExchange.sendResponseHeaders (Integer.parseInt (aParts[0]), aResponse.length);
      aExchange.getResponseBody ().write (aResponse);
      aExchange.close ();
    });
    aServer.start ();
    return aServer;
  }

  @NonNull
  private static String _getURL (@NonNull final HttpServer aServer)
  {
    return "http://127.0.0.1:" + aServer.getAddress ().getPort () + '/';
  }

  @Nullable
  private static String _sendWithRetries (@NonNull final String sURL,
                                          @NonNull final HttpRetrySettings aRetrySettings,
                                          @Nullable final IAS4RetryCallback aRetryCallback) throws IOException
  {
    // Accept all status codes and return them together with the payload
    return new BasicHttpPoster ().sendGenericMessageWithRetries (sURL,
                                                                 null,
                                                                 new StringEntity ("request", ContentType.TEXT_PLAIN),
                                                                 "msgid",
                                                                 aRetrySettings,
                                                                 aResponse -> aResponse.getCode () +
                                                                              ":" +
                                                                              EntityUtils.toString (aResponse.getEntity ()),
                                                                 null,
                                                                 aRetryCallback,
                                                                 null);
  }

  @NonNull
  private static HttpRetrySettings _createRetrySettings (final int nMaxRetries)
  {
    return new HttpRetrySettings ().setMaxRetries (nMaxRetries).setDurationBeforeRetry (Duration.ofMillis (10));
  }

  @NonNull
  private static IAS4RetryCallback _createRecordingCallback (@NonNull final ICommonsList <Long> aIntervals)
  {
    return (sMessageID, sURL, nTry, nMaxTries, nRetryIntervalMS, ex) -> {
      aIntervals.add (Long.valueOf (nRetryIntervalMS));
      return EContinue.CONTINUE;
    };
  }

  @Test
  public void testRetryOnStatusCodeWithRetryAfter () throws IOException
  {
    final AtomicInteger aRequestCount = new AtomicInteger (0);
    final HttpServer aServer = _startServer (aRequestCount, "503/1", "200");
    try
    {
      final ICommonsList <Long> aIntervals = new CommonsArrayList <> ();
      assertEquals ("200:try1",
                    _sendWithRetries (_getURL (aServer), _createRetrySettings (2), _createRecordingCallback (aIntervals)));
      assertEquals (2, aRequestCount.get ());
      // Retry-After of 1 second wins over the configured 10 ms
      assertEquals (new CommonsArrayList <> (Long.valueOf (1000)), aIntervals);
    }
    finally
    {
      aServer.stop (0);
    }
  }

  @Test
  public void testNoRetryOnNonRetriableStatusCode () throws IOException
  {
    final AtomicInteger aRequestCount = new AtomicInteger (0);
    final HttpServer aServer = _startServer (aRequestCount, "400", "200");
    try
    {
      assertEquals ("400:try0", _sendWithRetries (_getURL (aServer), _createRetrySettings (2), null));
      assertEquals (1, aRequestCount.get ());
    }
    finally
    {
      aServer.stop (0);
    }
  }

  @Test
  public void testLastTryHandlesRetriableStatusCodeAsUsual () throws IOException
  {
    final AtomicInteger aRequestCount = new AtomicInteger (0);
    final HttpServer aServer = _startServer (aRequestCount, "429");
    try
    {
      assertEquals ("429:try2", _sendWithRetries (_getURL (aServer), _createRetrySettings (2), null));
      assertEquals (3, aRequestCount.get ());
    }
    finally
    {
      aServer.stop (0);
    }
  }

  @Test
  public void testRetryAfterTooLong () throws IOException
  {
    final AtomicInteger aRequestCount = new AtomicInteger (0);
    final HttpServer aServer = _startServer (aRequestCount, "503/600", "200");
    try
    {
      // The response is handled as usual, because the server asked for too long
      assertEquals ("503:try0", _sendWithRetries (_getURL (aServer), _createRetrySettings (2), null));
      assertEquals (1, aRequestCount.get ());
    }
    finally
    {
      aServer.stop (0);
    }
  }

  @Test
  public void testReplaySafe () throws IOException
  {
    final AtomicInteger aRequestCount = new AtomicInteger (0);
    final HttpServer aServer = _startServer (aRequestCount, "500", "200");
    try
    {
      // Not replay safe - no retry
      assertEquals ("500:try0", _sendWithRetries (_getURL (aServer), _createRetrySettings (2).setReplaySafe (false), null));
      assertEquals (1, aRequestCount.get ());

      // Replay safe - retry
      aRequestCount.set (0);
      assertEquals ("200:try1", _sendWithRetries (_getURL (aServer), _createRetrySettings (2), null));
      assertEquals (2, aRequestCount.get ());
    }
    finally
    {
      aServer.stop (0);
    }
  }

  @Test
  public void testRetryVeto () throws IOException
  {
    final AtomicInteger aRequestCount = new AtomicInteger (0);
    final HttpServer aServer = _startServer (aRequestCount, "500", "200");
    try
    {
      final String sResponse = new BasicHttpPoster ().sendGenericMessageWithRetries (_getURL (aServer),
                                                                                     null,
                                                                                     new StringEntity ("request",
                                                                                                       ContentType.TEXT_PLAIN),
                                                                                     "msgid",
                                                                                     _createRetrySettings (2),
                                                                                     aResponse -> {
                                                                                       HttpRetryVeto.vetoRetry ();
                                                                                       return aResponse.getCode () +
                                                                                              ":" +
                                                                                              EntityUtils.toString (aResponse.getEntity ());
                                                                                     },
                                                                                     null,
                                                                                     null,
                                                                                     null);
      assertEquals ("500:try0", sResponse);
      assertEquals (1, aRequestCount.get ());
      // The veto must not be left behind
      assertFalse (HttpRetryVeto.isVetoed ());
    }
    finally
    {
      aServer.stop (0);
    }
  }

  @Test
  public void testIncreaseFactorAndMaximum () throws IOException
  {
    final AtomicInteger aRequestCount = new AtomicInteger (0);
    final HttpServer aServer = _startServer (aRequestCount, "503");
    try
    {
      final ICommonsList <Long> aIntervals = new CommonsArrayList <> ();
      final HttpRetrySettings aRetrySettings = _createRetrySettings (4).setRetryIncreaseFactor (BigDecimal.valueOf (2))
                                                                        .setMaxDurationBeforeRetry (Duration.ofMillis (30));
      assertEquals ("503:try4", _sendWithRetries (_getURL (aServer), aRetrySettings, _createRecordingCallback (aIntervals)));
      // Increased from the second retry on, limited by the maximum
      assertEquals (new CommonsArrayList <> (Long.valueOf (10), Long.valueOf (20), Long.valueOf (30), Long.valueOf (30)),
                    aIntervals);
    }
    finally
    {
      aServer.stop (0);
    }
  }

  @Test
  public void testConnectionRefusedIsRetried () throws IOException
  {
    // Find a port that is not in use
    final int nPort;
    try (final ServerSocket aSocket = new ServerSocket (0))
    {
      nPort = aSocket.getLocalPort ();
    }

    final ICommonsList <Long> aIntervals = new CommonsArrayList <> ();
    try
    {
      _sendWithRetries ("http://127.0.0.1:" + nPort + '/', _createRetrySettings (2), _createRecordingCallback (aIntervals));
      fail ();
    }
    catch (final ConnectException ex)
    {
      // expected
    }
    assertEquals (2, aIntervals.size ());
  }

  @Test
  public void testUnknownHostIsRetriedOnlyOnce () throws IOException
  {
    final ICommonsList <Long> aIntervals = new CommonsArrayList <> ();
    try
    {
      _sendWithRetries ("http://phase4-does-not-exist.invalid/", _createRetrySettings (3), _createRecordingCallback (aIntervals));
      fail ();
    }
    catch (final UnknownHostException ex)
    {
      // expected
    }
    assertEquals (1, aIntervals.size ());
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

  @Test
  public void testConnectionDetailsForPlainHttp () throws IOException
  {
    final HttpServer aServer = HttpServer.create (new InetSocketAddress ("127.0.0.1", 0), 0);
    aServer.createContext ("/", aExchange -> {
      aExchange.getRequestBody ().readAllBytes ();
      final byte [] aResponse = "ok".getBytes (StandardCharsets.UTF_8);
      aExchange.sendResponseHeaders (200, aResponse.length);
      aExchange.getResponseBody ().write (aResponse);
      aExchange.close ();
    });
    aServer.start ();

    try
    {
      final BasicHttpPoster aPoster = new BasicHttpPoster ();
      final MutableBoolean aInvoked = new MutableBoolean (false);
      final Wrapper <AS4ConnectionDetails> aConnectionDetails = new Wrapper <> ();
      final String sURL = "http://127.0.0.1:" + aServer.getAddress ().getPort () + '/';
      final String sResponse = aPoster.sendGenericMessage (sURL,
                                                           null,
                                                           new StringEntity ("request", ContentType.TEXT_PLAIN),
                                                           x -> EntityUtils.toString (x.getEntity ()),
                                                           x -> {
                                                             aInvoked.set (true);
                                                             aConnectionDetails.set (x);
                                                           });
      assertEquals ("ok", sResponse);
      // The consumer must be invoked, and the details must not contain a TLS layer
      assertTrue (aInvoked.booleanValue ());
      assertNotNull (aConnectionDetails.get ());
      assertFalse (aConnectionDetails.get ().hasTlsDetails ());
      assertEquals ("HTTP/1.1", aConnectionDetails.get ().getHttpVersion ());
      assertEquals ("127.0.0.1", aConnectionDetails.get ().getRemoteAddress ());
    }
    finally
    {
      aServer.stop (0);
    }
  }
}
