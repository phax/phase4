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
package com.helger.phase4.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import com.helger.base.concurrent.ThreadHelper;
import com.helger.base.state.EContinue;
import com.helger.base.string.StringHelper;
import com.helger.base.url.URLHelper;
import com.helger.base.wrapper.Wrapper;
import com.helger.http.header.HttpHeaderMap;
import com.helger.mime.CMimeType;
import com.helger.mime.IMimeType;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.CAS4;
import com.helger.phase4.ScopedAS4Configuration;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.messaging.http.HttpRetrySettings;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.model.soapfault.AS4SoapFault;
import com.helger.phase4.model.soapfault.EAS4FaultDisposition;
import com.helger.phase4.sender.AbstractAS4PullRequestBuilder;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilderMIMEPayload;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.server.AS4JettyRunner;
import com.helger.phase4.server.MockJettySetup;
import com.helger.phase4.test.profile.AS4TestProfileRegistarSPI;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.typeconvert.collection.IStringMap;
import com.helger.typeconvert.collection.StringMap;
import com.helger.url.URLBuilder;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

/**
 * Test class for receiving plain SOAP Fault responses to an outgoing AS4 UserMessage. Uses the
 * {@link MockAS4Servlet} to produce arbitrary fault responses.
 *
 * @author Philip Helger
 */
public final class AS4ClientUserMessageSoapFaultTest
{
  private static final class MockAS4Builder extends AbstractAS4UserMessageBuilderMIMEPayload <MockAS4Builder>
  {}

  private static final class MockAS4PullBuilder extends AbstractAS4PullRequestBuilder <MockAS4PullBuilder>
  {}

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4ClientUserMessageSoapFaultTest.class);

  private static final String SOAP12_FAULT_SENDER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                                    "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                                                    " <env:Body>\n" +
                                                    "  <env:Fault>\n" +
                                                    "   <env:Code>\n" +
                                                    "    <env:Value>env:Sender</env:Value>\n" +
                                                    "   </env:Code>\n" +
                                                    "   <env:Reason>\n" +
                                                    "    <env:Text xml:lang=\"en\">Signing certificate has been revoked</env:Text>\n" +
                                                    "   </env:Reason>\n" +
                                                    "  </env:Fault>\n" +
                                                    " </env:Body>\n" +
                                                    "</env:Envelope>";

  private static final String SOAP12_FAULT_RECEIVER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                                      "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                                                      " <env:Body>\n" +
                                                      "  <env:Fault>\n" +
                                                      "   <env:Code>\n" +
                                                      "    <env:Value>env:Receiver</env:Value>\n" +
                                                      "   </env:Code>\n" +
                                                      "   <env:Reason>\n" +
                                                      "    <env:Text xml:lang=\"en\">Database temporarily unavailable</env:Text>\n" +
                                                      "   </env:Reason>\n" +
                                                      "  </env:Fault>\n" +
                                                      " </env:Body>\n" +
                                                      "</env:Envelope>";

  private static final String SOAP12_FAULT_DATA_ENCODING_UNKNOWN = SOAP12_FAULT_SENDER.replace ("env:Sender",
                                                                                                 "env:DataEncodingUnknown");

  private static final String SOAP11_FAULT_CLIENT = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                                    "<S11:Envelope xmlns:S11=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                                                    " <S11:Body>\n" +
                                                    "  <S11:Fault>\n" +
                                                    "   <faultcode>S11:Client</faultcode>\n" +
                                                    "   <faultstring>Message does not conform</faultstring>\n" +
                                                    "  </S11:Fault>\n" +
                                                    " </S11:Body>\n" +
                                                    "</S11:Envelope>";

  private static final String HTML_ERROR_PAGE = "<html><head><title>502 Bad Gateway</title></head>" +
                                                "<body><center><h1>502 Bad Gateway</h1></center></body></html>";

  private static AS4JettyRunner s_aJetty;
  private static AS4ResourceHelper s_aResHelper;

  @BeforeClass
  public static void startServer () throws Exception
  {
    LOGGER.info ("MockJettySetup - starting");
    final int nPort = URLHelper.getAsURL (MockJettySetup.getServerAddressFromSettings ()).getPort ();
    s_aJetty = new AS4JettyRunner ();
    s_aJetty.setWebXmlResource (s_aJetty.getResourceFactory ()
                                        .newResource (s_aJetty.getResourceBase ().getName () + "/WEB-INF/web-mock.xml")
                                        .getName ());
    s_aJetty.setPort (nPort).setStopPort (nPort + 1000).setAllowAnnotationBasedConfig (false);
    s_aJetty.startServer ();

    RequestTrackerSettings.setLongRunningRequestsCheckEnabled (false);
    RequestTrackerSettings.setParallelRunningRequestsCheckEnabled (false);
    s_aResHelper = new AS4ResourceHelper ();

    AS4ProfileSelector.setCustomDefaultAS4ProfileID (AS4TestProfileRegistarSPI.AS4_PROFILE_ID_MAY_SIGN_MAY_CRYPT);

    LOGGER.info ("MockJettySetup - started");
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    LOGGER.info ("MockJettySetup - stopping");

    if (s_aResHelper != null)
      s_aResHelper.close ();
    if (s_aJetty != null)
    {
      s_aJetty.shutDownServer ();
      // Wait a little until shutdown happened
      ThreadHelper.sleep (500);
    }
    s_aJetty = null;
    LOGGER.info ("MockJettySetup - stopped");
  }

  @NonNull
  private static MockAS4Builder _createBuilder ()
  {
    return new MockAS4Builder ().as4ProfileID (AS4TestProfileRegistarSPI.AS4_PROFILE_ID_MAY_SIGN_MAY_CRYPT)
                                .action ("AnAction")
                                .service ("MyServiceType", "OrderPaper")
                                .conversationID (MessageHelperMethods.createRandomConversationID ())
                                .agreementRef ("bla")
                                .fromRole (CAS4.DEFAULT_ROLE)
                                .fromPartyID ("MyPartyIDforSending")
                                .toRole (CAS4.DEFAULT_ROLE)
                                .toPartyID ("MyPartyIDforReceving")
                                .addEbmsProperties (AS4TestConstants.getEBMSProperties ())
                                .payload (AS4OutgoingAttachment.builder ()
                                                               .data ("<root xmlns='urn:any'/>".getBytes (StandardCharsets.UTF_8))
                                                               .mimeTypeXML ()
                                                               .build ());
  }

  @NonNull
  private static String _buildURL (final String sContent, final int nStatusCode, final IMimeType aMimeType)
  {
    return URLBuilder.of (MockJettySetup.getServerAddressFromSettings ())
                     .addParam (MockAS4Servlet.CONTENT, sContent)
                     .addParam (MockAS4Servlet.STATUSCODE, nStatusCode)
                     .addParam (MockAS4Servlet.MIMETYPE, aMimeType.getAsString ())
                     .build ()
                     .getAsString ();
  }

  @NonNull
  private static String _buildURL (final String sContent,
                                   final String sContentID,
                                   final int nStatusCode,
                                   final IMimeType aMimeType)
  {
    return URLBuilder.of (MockJettySetup.getServerAddressFromSettings ())
                     .addParam (MockAS4Servlet.CONTENT, sContent)
                     .addParam (MockAS4Servlet.CONTENTID, sContentID)
                     .addParam (MockAS4Servlet.STATUSCODE, nStatusCode)
                     .addParam (MockAS4Servlet.MIMETYPE, aMimeType.getAsString ())
                     .build ()
                     .getAsString ();
  }

  @NonNull
  private static EAS4UserMessageSendResult _send (@NonNull final String sURL,
                                                  @NonNull final Wrapper <AS4SoapFault> aFaultKeeper,
                                                  @NonNull final AtomicInteger aRetryCount)
  {
    return _createBuilder ().endpointURL (sURL)
                            .httpRetrySettings (new HttpRetrySettings ().setMaxRetries (2)
                                                                        .setDurationBeforeRetry (Duration.ofMillis (50)))
                            .retryCallback ( (sMessageID, sRetryURL, nTry, nMaxTries, nRetryIntervalMS, ex) -> {
                              aRetryCount.incrementAndGet ();
                              return EContinue.CONTINUE;
                            })
                            .soapFaultConsumer ( (sSentMessageID, aSoapFault, aClientSentMessage) -> aFaultKeeper.set (aSoapFault))
                            .sendMessageAndCheckForReceipt ();
  }

  @Test
  public void testSoap12PermanentFaultWithHttp400 ()
  {
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();
    final AtomicInteger aRetryCount = new AtomicInteger (0);

    assertEquals (EAS4UserMessageSendResult.SOAP_FAULT_RECEIVED,
                  _send (_buildURL (SOAP12_FAULT_SENDER, 400, ESoapVersion.SOAP_12.getMimeType ()),
                         aFaultKeeper,
                         aRetryCount));
    assertNotNull (aFaultKeeper.get ());
    assertSame (EAS4FaultDisposition.PERMANENT, aFaultKeeper.get ().getDisposition ());
    assertEquals (0, aRetryCount.intValue ());
  }

  @Test
  public void testSoap12TransientFaultWithHttp500 ()
  {
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();

    assertEquals (EAS4UserMessageSendResult.SOAP_FAULT_RECEIVED,
                  _send (_buildURL (SOAP12_FAULT_RECEIVER, 500, ESoapVersion.SOAP_12.getMimeType ()),
                         aFaultKeeper,
                         new AtomicInteger (0)));
    assertNotNull (aFaultKeeper.get ());
    assertSame (EAS4FaultDisposition.TRANSIENT, aFaultKeeper.get ().getDisposition ());
  }

  @Test
  public void testSoap12TransientFaultWithHttp503 ()
  {
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();

    assertEquals (EAS4UserMessageSendResult.SOAP_FAULT_RECEIVED,
                  _send (_buildURL (SOAP12_FAULT_RECEIVER, 503, ESoapVersion.SOAP_12.getMimeType ()),
                         aFaultKeeper,
                         new AtomicInteger (0)));
    assertNotNull (aFaultKeeper.get ());
    assertSame (EAS4FaultDisposition.TRANSIENT, aFaultKeeper.get ().getDisposition ());
  }

  @Test
  public void testSoap12PermanentFaultWithHttp422 ()
  {
    // Unknown 4xx codes are handled like 400
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();
    final AtomicInteger aRetryCount = new AtomicInteger (0);

    assertEquals (EAS4UserMessageSendResult.SOAP_FAULT_RECEIVED,
                  _send (_buildURL (SOAP12_FAULT_SENDER, 422, ESoapVersion.SOAP_12.getMimeType ()),
                         aFaultKeeper,
                         aRetryCount));
    assertNotNull (aFaultKeeper.get ());
    assertSame (EAS4FaultDisposition.PERMANENT, aFaultKeeper.get ().getDisposition ());
    assertEquals (0, aRetryCount.intValue ());
  }

  @Test
  public void testSoap12DataEncodingUnknownFaultIsPermanent ()
  {
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();

    assertEquals (EAS4UserMessageSendResult.SOAP_FAULT_RECEIVED,
                  _send (_buildURL (SOAP12_FAULT_DATA_ENCODING_UNKNOWN, 500, ESoapVersion.SOAP_12.getMimeType ()),
                         aFaultKeeper,
                         new AtomicInteger (0)));
    assertNotNull (aFaultKeeper.get ());
    assertSame (EAS4FaultDisposition.PERMANENT, aFaultKeeper.get ().getDisposition ());
  }

  @Test
  public void testHttp401IsHardFailureWithoutRetry ()
  {
    for (final boolean bAcceptAll : new boolean [] { true, false })
    {
      final IStringMap aSettings = new StringMap ();
      aSettings.putIn (AS4Configuration.PROPERTY_PHASE4_HTTP_RESPONSE_ACCEPT_ALL_STATUS_CODES, bAcceptAll);
      try (final ScopedAS4Configuration aSC = ScopedAS4Configuration.create (aSettings))
      {
        final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();
        final AtomicInteger aRetryCount = new AtomicInteger (0);

        // Even a SOAP Fault body must not be processed
        assertEquals (EAS4UserMessageSendResult.TRANSPORT_ERROR_NO_RETRY,
                      _send (_buildURL (SOAP12_FAULT_SENDER, 401, ESoapVersion.SOAP_12.getMimeType ()),
                             aFaultKeeper,
                             aRetryCount));
        assertNull (aFaultKeeper.get ());
        assertEquals (0, aRetryCount.intValue ());
      }
    }
  }

  @Test
  public void testHttp405WithoutEnvelope ()
  {
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();

    assertEquals (EAS4UserMessageSendResult.NO_SIGNAL_MESSAGE_RECEIVED,
                  _send (_buildURL ("Method Not Allowed", 405, CMimeType.TEXT_PLAIN),
                         aFaultKeeper,
                         new AtomicInteger (0)));
    assertNull (aFaultKeeper.get ());
  }

  @Test
  public void testHttp405WithFaultIsTransportFailure ()
  {
    // The envelope is not processed as a SOAP Fault, so it is rejected as an invalid signal message
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();

    assertEquals (EAS4UserMessageSendResult.TRANSPORT_ERROR_NO_RETRY,
                  _send (_buildURL (SOAP12_FAULT_SENDER, 405, ESoapVersion.SOAP_12.getMimeType ()),
                         aFaultKeeper,
                         new AtomicInteger (0)));
    assertNull (aFaultKeeper.get ());
  }

  @Test
  public void testHttp415WithFaultIsTransportFailure ()
  {
    // The envelope is not processed as a SOAP Fault, so it is rejected as an invalid signal message
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();

    assertEquals (EAS4UserMessageSendResult.TRANSPORT_ERROR_NO_RETRY,
                  _send (_buildURL (SOAP12_FAULT_SENDER, 415, ESoapVersion.SOAP_12.getMimeType ()),
                         aFaultKeeper,
                         new AtomicInteger (0)));
    assertNull (aFaultKeeper.get ());
  }

  @Test
  public void testPullRequestSoapFaultWithHttp500 () throws Exception
  {
    // The fault is not interpreted as ebMS message but routed to the SOAP Fault consumer and the
    // incoming dumper
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();
    final ByteArrayOutputStream aDumped = new ByteArrayOutputStream ();
    final AtomicInteger aDumpCount = new AtomicInteger (0);
    final IAS4IncomingDumper aDumper = new IAS4IncomingDumper ()
    {
      public OutputStream onNewRequest (final IAS4IncomingMessageMetadata aMetadata, final HttpHeaderMap aHeaders)
      {
        aDumpCount.incrementAndGet ();
        return aDumped;
      }

      public void onEndRequest (final IAS4IncomingMessageMetadata aMetadata, final Exception aCaughtException)
      {}
    };

    new MockAS4PullBuilder ().as4ProfileID (AS4TestProfileRegistarSPI.AS4_PROFILE_ID_MAY_SIGN_MAY_CRYPT)
                             .mpc ("urn:test:mpc")
                             .endpointURL (_buildURL (SOAP12_FAULT_RECEIVER, 500, ESoapVersion.SOAP_12.getMimeType ()))
                             .incomingDumper (aDumper)
                             .soapFaultConsumer ( (sSentMessageID, aSoapFault, aClientSentMessage) -> aFaultKeeper.set (aSoapFault))
                             .sendMessage ();
    assertNotNull (aFaultKeeper.get ());
    assertSame (EAS4FaultDisposition.TRANSIENT, aFaultKeeper.get ().getDisposition ());
    assertEquals (1, aDumpCount.intValue ());
    assertTrue (new String (aDumped.toByteArray (), StandardCharsets.UTF_8).contains ("Database temporarily unavailable"));
  }

  @Test
  public void testHttp202EmptyBody ()
  {
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();

    assertEquals (EAS4UserMessageSendResult.NO_SIGNAL_MESSAGE_RECEIVED,
                  _send (_buildURL ("", 202, CMimeType.TEXT_PLAIN), aFaultKeeper, new AtomicInteger (0)));
    assertNull (aFaultKeeper.get ());
  }

  @Test
  @Ignore ("Needs a mock that returns a valid Receipt for the sent message; TestReceipt12.xml fails WSS validation")
  public void testHttp202WithEnvelopeIsProcessed ()
  {
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();

    assertEquals (EAS4UserMessageSendResult.SUCCESS,
                  _send (_buildURL ("", "receipt12", 202, CMimeType.APPLICATION_XML),
                         aFaultKeeper,
                         new AtomicInteger (0)));
    assertNull (aFaultKeeper.get ());
  }

  @Test
  public void testSoap12PermanentFaultWithHttp500 ()
  {
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();
    final Wrapper <String> aMessageIDKeeper = new Wrapper <> ();

    final EAS4UserMessageSendResult eResult = _createBuilder ().endpointURL (_buildURL (SOAP12_FAULT_SENDER,
                                                                                        500,
                                                                                        ESoapVersion.SOAP_12.getMimeType ()))
                                                               .soapFaultConsumer ((sSentMessageID,
                                                                                    aSoapFault,
                                                                                    aClientSentMessage) -> {
                                                                 aFaultKeeper.set (aSoapFault);
                                                                 aMessageIDKeeper.set (sSentMessageID);
                                                                 assertNotNull (aClientSentMessage);
                                                               })
                                                               .sendMessageAndCheckForReceipt ();
    assertEquals (EAS4UserMessageSendResult.SOAP_FAULT_RECEIVED, eResult);

    final AS4SoapFault aSoapFault = aFaultKeeper.get ();
    assertNotNull (aSoapFault);
    assertSame (ESoapVersion.SOAP_12, aSoapFault.getSoapVersion ());
    assertTrue (aSoapFault.hasFaultCode ());
    assertEquals (ESoapVersion.SOAP_12.getNamespaceURI (), aSoapFault.getFaultCode ().getNamespaceURI ());
    assertEquals ("Sender", aSoapFault.getFaultCode ().getLocalPart ());
    assertEquals ("Signing certificate has been revoked", aSoapFault.getFaultReason ());
    assertSame (EAS4FaultDisposition.PERMANENT, aSoapFault.getDisposition ());
    assertTrue (StringHelper.isNotEmpty (aMessageIDKeeper.get ()));
  }

  @Test
  public void testSoap11FaultWithHttp500InSoap12Conversation ()
  {
    // The client sends SOAP 1.2, but the fault comes back as SOAP 1.1 - it must still be detected
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();

    final EAS4UserMessageSendResult eResult = _createBuilder ().endpointURL (_buildURL (SOAP11_FAULT_CLIENT,
                                                                                        500,
                                                                                        ESoapVersion.SOAP_11.getMimeType ()))
                                                               .soapFaultConsumer ((sSentMessageID,
                                                                                    aSoapFault,
                                                                                    aClientSentMessage) -> aFaultKeeper.set (aSoapFault))
                                                               .sendMessageAndCheckForReceipt ();
    assertEquals (EAS4UserMessageSendResult.SOAP_FAULT_RECEIVED, eResult);

    final AS4SoapFault aSoapFault = aFaultKeeper.get ();
    assertNotNull (aSoapFault);
    // The fault carries its own SOAP version, not the conversation one
    assertSame (ESoapVersion.SOAP_11, aSoapFault.getSoapVersion ());
    assertTrue (aSoapFault.hasFaultCode ());
    assertEquals ("Client", aSoapFault.getFaultCode ().getLocalPart ());
    assertEquals ("Message does not conform", aSoapFault.getFaultReason ());
    assertSame (EAS4FaultDisposition.PERMANENT, aSoapFault.getDisposition ());
  }

  @Test
  public void testSoap12TransientFaultWithHttp200 ()
  {
    // Non-conforming peers may return a fault with HTTP 200 - it must still be detected
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();

    final EAS4UserMessageSendResult eResult = _createBuilder ().endpointURL (_buildURL (SOAP12_FAULT_RECEIVER,
                                                                                        200,
                                                                                        ESoapVersion.SOAP_12.getMimeType ()))
                                                               .soapFaultConsumer ((sSentMessageID,
                                                                                    aSoapFault,
                                                                                    aClientSentMessage) -> aFaultKeeper.set (aSoapFault))
                                                               .sendMessageAndCheckForReceipt ();
    assertEquals (EAS4UserMessageSendResult.SOAP_FAULT_RECEIVED, eResult);

    final AS4SoapFault aSoapFault = aFaultKeeper.get ();
    assertNotNull (aSoapFault);
    assertEquals ("Receiver", aSoapFault.getFaultCode ().getLocalPart ());
    assertSame (EAS4FaultDisposition.TRANSIENT, aSoapFault.getDisposition ());
  }

  @Test
  public void testHtmlErrorPageWithHttp502 ()
  {
    // An HTML proxy error page is not a SOAP Fault
    final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();

    final EAS4UserMessageSendResult eResult = _createBuilder ().endpointURL (_buildURL (HTML_ERROR_PAGE,
                                                                                        502,
                                                                                        CMimeType.TEXT_HTML))
                                                               .soapFaultConsumer ((sSentMessageID,
                                                                                    aSoapFault,
                                                                                    aClientSentMessage) -> aFaultKeeper.set (aSoapFault))
                                                               .sendMessageAndCheckForReceipt ();
    assertEquals (EAS4UserMessageSendResult.NO_SIGNAL_MESSAGE_RECEIVED, eResult);
    assertNull (aFaultKeeper.get ());
  }

  @Test
  public void testPermanentFaultStopsRetriesInLegacyMode ()
  {
    // If "accept all status codes" is disabled, a non-2xx response leads to an IOException that is
    // normally retried. A permanent SOAP Fault must stop all retries immediately.
    final IStringMap aSettings = new StringMap ();
    aSettings.putIn (AS4Configuration.PROPERTY_PHASE4_HTTP_RESPONSE_ACCEPT_ALL_STATUS_CODES, false);
    try (final ScopedAS4Configuration aSC = ScopedAS4Configuration.create (aSettings))
    {
      final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();
      final AtomicInteger aRetryCount = new AtomicInteger (0);

      final EAS4UserMessageSendResult eResult = _createBuilder ().endpointURL (_buildURL (SOAP12_FAULT_SENDER,
                                                                                          500,
                                                                                          ESoapVersion.SOAP_12.getMimeType ()))
                                                                 .httpRetrySettings (new HttpRetrySettings ().setMaxRetries (2)
                                                                                                             .setDurationBeforeRetry (Duration.ofMillis (50)))
                                                                 .retryCallback ((sMessageID,
                                                                                  sURL,
                                                                                  nTry,
                                                                                  nMaxTries,
                                                                                  nRetryIntervalMS,
                                                                                  ex) -> {
                                                                   aRetryCount.incrementAndGet ();
                                                                   return EContinue.CONTINUE;
                                                                 })
                                                                 .soapFaultConsumer ((sSentMessageID,
                                                                                      aSoapFault,
                                                                                      aClientSentMessage) -> aFaultKeeper.set (aSoapFault))
                                                                 .sendMessageAndCheckForReceipt ();
      assertEquals (EAS4UserMessageSendResult.SOAP_FAULT_RECEIVED, eResult);

      final AS4SoapFault aSoapFault = aFaultKeeper.get ();
      assertNotNull (aSoapFault);
      assertSame (EAS4FaultDisposition.PERMANENT, aSoapFault.getDisposition ());
      // The permanent fault must have stopped all retries
      assertEquals (0, aRetryCount.intValue ());
    }
  }

  @Test
  public void testTransientFaultRetriesInLegacyMode ()
  {
    // If "accept all status codes" is disabled, a transient SOAP Fault must keep the normal retry
    // behaviour, but must still be surfaced after all retries are exhausted
    final IStringMap aSettings = new StringMap ();
    aSettings.putIn (AS4Configuration.PROPERTY_PHASE4_HTTP_RESPONSE_ACCEPT_ALL_STATUS_CODES, false);
    try (final ScopedAS4Configuration aSC = ScopedAS4Configuration.create (aSettings))
    {
      final Wrapper <AS4SoapFault> aFaultKeeper = new Wrapper <> ();
      final AtomicInteger aRetryCount = new AtomicInteger (0);

      final EAS4UserMessageSendResult eResult = _createBuilder ().endpointURL (_buildURL (SOAP12_FAULT_RECEIVER,
                                                                                          500,
                                                                                          ESoapVersion.SOAP_12.getMimeType ()))
                                                                 .httpRetrySettings (new HttpRetrySettings ().setMaxRetries (2)
                                                                                                             .setDurationBeforeRetry (Duration.ofMillis (50)))
                                                                 .retryCallback ((sMessageID,
                                                                                  sURL,
                                                                                  nTry,
                                                                                  nMaxTries,
                                                                                  nRetryIntervalMS,
                                                                                  ex) -> {
                                                                   aRetryCount.incrementAndGet ();
                                                                   return EContinue.CONTINUE;
                                                                 })
                                                                 .soapFaultConsumer ((sSentMessageID,
                                                                                      aSoapFault,
                                                                                      aClientSentMessage) -> aFaultKeeper.set (aSoapFault))
                                                                 .sendMessageAndCheckForReceipt ();
      assertEquals (EAS4UserMessageSendResult.SOAP_FAULT_RECEIVED, eResult);

      final AS4SoapFault aSoapFault = aFaultKeeper.get ();
      assertNotNull (aSoapFault);
      assertSame (EAS4FaultDisposition.TRANSIENT, aSoapFault.getDisposition ());
      // The transient fault must have used all configured retries
      assertEquals (2, aRetryCount.intValue ());
    }
  }
}
