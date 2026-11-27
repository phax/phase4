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
package com.helger.phase4.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.wss4j.common.crypto.Crypto;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.base.io.iface.IHasInputStream;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.collection.commons.ICommonsList;
import com.helger.http.header.HttpHeaderMap;
import com.helger.mime.IMimeType;
import com.helger.phase4.AS4TestRule;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.crypto.ECryptoMode;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.incoming.AS4IncomingProfileSelectorConstant;
import com.helger.phase4.incoming.AS4IncomingReceiverConfiguration;
import com.helger.phase4.incoming.AS4IncomingMessageMetadata;
import com.helger.phase4.incoming.AS4RequestHandler;
import com.helger.phase4.incoming.IAS4ResponseAbstraction;
import com.helger.phase4.incoming.crypto.AS4IncomingSecurityConfiguration;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.messaging.http.BasicHttpPoster;
import com.helger.phase4.messaging.http.EHttpRetryClassification;
import com.helger.phase4.messaging.http.HttpRetrySettings;
import com.helger.phase4.model.pmode.resolve.AS4DefaultPModeResolver;
import com.helger.phase4.util.Phase4Exception;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.TelemetryMetrics;
import com.helger.telemetry.mock.CapturingTelemetry;
import com.helger.telemetry.mock.CapturingTelemetry.CapturedMeasurement;
import com.helger.telemetry.mock.CapturingTelemetry.CapturedSpan;
import com.sun.net.httpserver.HttpServer;

/**
 * Test class for the telemetry emitted by phase4.
 *
 * @author Philip Helger
 */
public final class Phase4TelemetryTest
{
  private static final class MockCryptoFactory implements IAS4CryptoFactory
  {
    @Nullable
    public Crypto getCrypto (@NonNull final ECryptoMode eCryptoMode)
    {
      return null;
    }

    @Nullable
    public KeyStore getKeyStore ()
    {
      return null;
    }

    public KeyStore.@Nullable PrivateKeyEntry getPrivateKeyEntry ()
    {
      return null;
    }

    @Nullable
    public String getKeyAlias ()
    {
      return null;
    }

    public char @Nullable [] getKeyPasswordPerAliasCharArray (@Nullable final String sSearchKeyAlias)
    {
      return null;
    }

    @Nullable
    public KeyStore getTrustStore ()
    {
      return null;
    }
  }

  private static final CapturingTelemetry CT = new CapturingTelemetry ();

  @Rule
  public final TestRule m_aTestRule = new AS4TestRule ();

  @BeforeClass
  public static void beforeClass ()
  {
    // Must happen before Phase4Metrics is class-loaded, because the instruments are resolved once
    // in its static initializer
    CT.install ();
  }

  @AfterClass
  public static void afterClass ()
  {
    CapturingTelemetry.uninstall ();
  }

  @Before
  public void before ()
  {
    CT.reset ();
  }

  /**
   * The tests share one JVM. If another test loaded {@link Phase4Metrics} before, the instruments
   * are no-ops and the metric assertions are skipped.
   */
  private static void _assumeMetricsAreCaptured ()
  {
    Assume.assumeTrue ("Phase4Metrics was loaded before the capturing telemetry was installed",
                       !(Phase4Metrics.INCOMING_REQUESTS instanceof TelemetryMetrics.NoOpTelemetryCounter));
  }

  @NonNull
  private static HttpServer _startServer (@NonNull final AtomicInteger aRequestCount,
                                          final int... aStatusCodes) throws IOException
  {
    final HttpServer aServer = HttpServer.create (new InetSocketAddress ("127.0.0.1", 0), 0);
    aServer.createContext ("/", aExchange -> {
      final int nIndex = aRequestCount.getAndIncrement ();
      aExchange.getRequestBody ().readAllBytes ();
      final byte [] aResponse = ("try" + nIndex).getBytes (StandardCharsets.UTF_8);
      aExchange.sendResponseHeaders (aStatusCodes[Math.min (nIndex, aStatusCodes.length - 1)], aResponse.length);
      aExchange.getResponseBody ().write (aResponse);
      aExchange.close ();
    });
    aServer.start ();
    return aServer;
  }

  @NonNull
  private static IAS4ResponseAbstraction _createResponse (@NonNull final AtomicInteger aStatusCode)
  {
    return new IAS4ResponseAbstraction ()
    {
      public void setContent (final byte @NonNull [] aBytes, @NonNull final Charset aCharset)
      {}

      public void setContent (@NonNull final HttpHeaderMap aHeaderMap, @NonNull final IHasInputStream aHasIS)
      {}

      public void setMimeType (@NonNull final IMimeType aMimeType)
      {}

      public void setStatus (final int nStatusCode)
      {
        aStatusCode.set (nStatusCode);
      }
    };
  }

  @NonNull
  private static AS4RequestHandler _createRequestHandler ()
  {
    // Same defaults as in AS4XServletHandler
    final String sAS4ProfileID = AS4ProfileSelector.getDefaultAS4ProfileID ();
    final AS4RequestHandler ret = new AS4RequestHandler (AS4IncomingMessageMetadata.createForRequest ());
    // No security is used in the tests
    ret.setCryptoFactory (new MockCryptoFactory ());
    ret.setPModeResolver (new AS4DefaultPModeResolver (sAS4ProfileID));
    ret.setIncomingProfileSelector (new AS4IncomingProfileSelectorConstant (sAS4ProfileID, true));
    ret.setIncomingAttachmentFactory (IAS4IncomingAttachmentFactory.DEFAULT_INSTANCE);
    ret.setIncomingSecurityConfiguration (AS4IncomingSecurityConfiguration.createDefaultInstance ());
    ret.setIncomingReceiverConfiguration (new AS4IncomingReceiverConfiguration ());
    return ret;
  }

  @Test
  public void testHttpRetry () throws IOException
  {
    final AtomicInteger aRequestCount = new AtomicInteger (0);
    final HttpServer aServer = _startServer (aRequestCount, 503, 200);
    try
    {
      final String sRequest = "request";
      final String sResponse = new BasicHttpPoster ().sendGenericMessageWithRetries ("http://127.0.0.1:" +
                                                                                     aServer.getAddress ()
                                                                                            .getPort () +
                                                                                     '/',
                                                                                     null,
                                                                                     new StringEntity (sRequest,
                                                                                                       ContentType.TEXT_PLAIN),
                                                                                     "msgid",
                                                                                     new HttpRetrySettings ().setMaxRetries (2)
                                                                                                             .setDurationBeforeRetry (Duration.ofMillis (10)),
                                                                                     aHttpResponse -> EntityUtils.toString (aHttpResponse.getEntity ()),
                                                                                     null,
                                                                                     null,
                                                                                     null);
      assertEquals ("try1", sResponse);
      assertEquals (2, aRequestCount.get ());

      // One span per attempt
      final ICommonsList <CapturedSpan> aSpans = CT.getSpans (CPhase4Telemetry.SPAN_HTTP_SEND);
      assertEquals (2, aSpans.size ());
      for (final CapturedSpan aSpan : aSpans)
      {
        assertEquals (ETelemetrySpanKind.CLIENT, aSpan.getKind ());
        assertEquals ("127.0.0.1", aSpan.getAttribute (CPhase4Telemetry.ATTR_SERVER_ADDRESS));
        assertTrue (aSpan.isClosed ());
      }
      final CapturedSpan aFailedSpan = aSpans.get (0);
      assertEquals (CPhase4Telemetry.OUTCOME_ERROR, aFailedSpan.getAttribute (CPhase4Telemetry.ATTR_OUTCOME));
      assertEquals (Long.valueOf (503), aFailedSpan.getAttribute (CPhase4Telemetry.ATTR_HTTP_RESPONSE_STATUS_CODE));
      assertTrue (aFailedSpan.isStatusError ());
      final CapturedSpan aSuccessSpan = aSpans.get (1);
      assertEquals (CPhase4Telemetry.OUTCOME_SUCCESS, aSuccessSpan.getAttribute (CPhase4Telemetry.ATTR_OUTCOME));
      assertTrue (aSuccessSpan.isStatusOk ());

      _assumeMetricsAreCaptured ();
      assertEquals (2, CT.getCounterValue (CPhase4Telemetry.METRIC_HTTP_ATTEMPTS));
      assertEquals (2, CT.getHistogramValues (CPhase4Telemetry.METRIC_HTTP_DURATION).size ());
      for (final Double aSize : CT.getHistogramValues (CPhase4Telemetry.METRIC_HTTP_REQUEST_SIZE))
        assertEquals (sRequest.length (), aSize.doubleValue (), 0.0001);

      assertEquals (1, CT.getCounterValue (CPhase4Telemetry.METRIC_HTTP_RETRIES));
      final CapturedMeasurement aRetry = CT.getFirstMeasurement (CPhase4Telemetry.METRIC_HTTP_RETRIES);
      assertNotNull (aRetry);
      assertEquals (EHttpRetryClassification.RETRY.name (),
                    aRetry.getAttribute (CPhase4Telemetry.ATTR_HTTP_RETRY_CLASSIFICATION));
    }
    finally
    {
      aServer.stop (0);
    }
  }

  @Test
  public void testIncomingWithoutContentType () throws Exception
  {
    final byte [] aPayload = "<x/>".getBytes (StandardCharsets.UTF_8);
    try (final AS4RequestHandler aHandler = _createRequestHandler ())
    {
      aHandler.handleRequest (new NonBlockingByteArrayInputStream (aPayload),
                              new HttpHeaderMap (),
                              _createResponse (new AtomicInteger (0)));
      fail ();
    }
    catch (final Phase4Exception ex)
    {
      // expected - Content-Type is missing
    }

    final CapturedSpan aSpan = CT.getFirstSpan (CPhase4Telemetry.SPAN_INCOMING_REQUEST);
    assertNotNull (aSpan);
    assertEquals (ETelemetrySpanKind.SERVER, aSpan.getKind ());
    assertEquals (CPhase4Telemetry.OUTCOME_EXCEPTION, aSpan.getAttribute (CPhase4Telemetry.ATTR_OUTCOME));
    assertEquals (CPhase4Telemetry.MESSAGE_TYPE_UNKNOWN, aSpan.getAttribute (CPhase4Telemetry.ATTR_MESSAGE_TYPE));
    assertNotNull (aSpan.getRecordedException ());
    assertTrue (aSpan.isStatusError ());
    assertTrue (aSpan.isClosed ());

    _assumeMetricsAreCaptured ();
    assertEquals (1, CT.getCounterValue (CPhase4Telemetry.METRIC_INCOMING_REQUESTS));
    final CapturedMeasurement aRequest = CT.getFirstMeasurement (CPhase4Telemetry.METRIC_INCOMING_REQUESTS);
    assertNotNull (aRequest);
    assertEquals (CPhase4Telemetry.OUTCOME_EXCEPTION, aRequest.getAttribute (CPhase4Telemetry.ATTR_OUTCOME));
    assertEquals (1, CT.getHistogramValues (CPhase4Telemetry.METRIC_INCOMING_DURATION).size ());
    assertEquals (1, CT.getHistogramValues (CPhase4Telemetry.METRIC_INCOMING_SIZE).size ());
  }

  @Test
  public void testIncomingWithoutEbmsHeader () throws Exception
  {
    final String sSoap = "<S12:Envelope xmlns:S12=\"http://www.w3.org/2003/05/soap-envelope\"><S12:Header/><S12:Body/></S12:Envelope>";
    final byte [] aPayload = sSoap.getBytes (StandardCharsets.UTF_8);
    final HttpHeaderMap aHeaders = new HttpHeaderMap ();
    aHeaders.setContentType ("application/soap+xml;charset=utf-8");
    final AtomicInteger aStatusCode = new AtomicInteger (0);
    try (final AS4RequestHandler aHandler = _createRequestHandler ())
    {
      aHandler.handleRequest (new NonBlockingByteArrayInputStream (aPayload), aHeaders, _createResponse (aStatusCode));
    }

    final CapturedSpan aSpan = CT.getFirstSpan (CPhase4Telemetry.SPAN_INCOMING_REQUEST);
    assertNotNull (aSpan);
    assertEquals (CPhase4Telemetry.OUTCOME_EBMS_ERROR, aSpan.getAttribute (CPhase4Telemetry.ATTR_OUTCOME));
    assertEquals (CPhase4Telemetry.MESSAGE_TYPE_UNKNOWN, aSpan.getAttribute (CPhase4Telemetry.ATTR_MESSAGE_TYPE));
    assertTrue (aSpan.isStatusError ());
    assertTrue (aSpan.isClosed ());

    _assumeMetricsAreCaptured ();
    final CapturedMeasurement aRequest = CT.getFirstMeasurement (CPhase4Telemetry.METRIC_INCOMING_REQUESTS);
    assertNotNull (aRequest);
    assertEquals (CPhase4Telemetry.OUTCOME_EBMS_ERROR, aRequest.getAttribute (CPhase4Telemetry.ATTR_OUTCOME));
    assertTrue (CT.getCounterValue (CPhase4Telemetry.METRIC_INCOMING_EBMS_ERRORS) > 0);
    final ICommonsList <Double> aSizes = CT.getHistogramValues (CPhase4Telemetry.METRIC_INCOMING_SIZE);
    assertEquals (1, aSizes.size ());
    assertEquals (aPayload.length, aSizes.getFirstOrNull ().doubleValue (), 0.0001);
  }
}
