package com.helger.phase4.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.concurrent.ThreadHelper;
import com.helger.base.url.URLHelper;
import com.helger.http.CHttpHeader;
import com.helger.mime.CMimeType;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilderMIMEPayload;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.server.AS4JettyRunner;
import com.helger.phase4.server.MockJettySetup;
import com.helger.phase4.test.profile.AS4TestProfileRegistarSPI;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.url.URLBuilder;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

import jakarta.mail.MessagingException;

public class AS4ClientUserMessageTestWithCrappyReceiver
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4ClientUserMessageTestWithCrappyReceiver.class);

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

  @Test
  public void testSendingAnythingAndReceiveCrapViaClient () throws WSSecurityException, IOException, MessagingException
  {
    final String sServerURL = MockJettySetup.getServerAddressFromSettings ();

    final MockAS4ClientUserMessage aClient = new MockAS4ClientUserMessage (s_aResHelper);
    aClient.setSoapVersion (ESoapVersion.SOAP_12);

    // Use a pmode that you know is currently running on the server your trying
    // to send the message too
    aClient.setAction ("AnAction");
    aClient.setServiceType ("MyServiceType");
    aClient.setServiceValue ("OrderPaper");
    aClient.setConversationID (MessageHelperMethods.createRandomConversationID ());
    aClient.setAgreementRefValue ("bla");
    aClient.setFromRole (CAS4.DEFAULT_ROLE);
    aClient.setFromPartyID ("MyPartyIDforSending");
    aClient.setToRole (CAS4.DEFAULT_ROLE);
    aClient.setToPartyID ("MyPartyIDforReceving");
    aClient.ebms3Properties ().setAll (AS4TestConstants.getEBMSProperties ());

    aClient.setPayload (DOMReader.readXMLDOM ("<root xmlns='urn:any'/>"));

    final HttpClientResponseHandler <byte []> aResponseHandlerPlain = aHttpResponse -> {
      final HttpEntity aEntity = aHttpResponse.getEntity ();
      return EntityUtils.toByteArray (aEntity);
    };

    // 1
    AS4ClientSentMessage <byte []> ret = aClient.sendMessageWithRetries (sServerURL,
                                                                         aResponseHandlerPlain,
                                                                         null,
                                                                         null,
                                                                         null);
    assertNotNull (ret);
    assertEquals ("Plain Text", new String (ret.getResponseContent (), StandardCharsets.UTF_8));
    assertEquals (200, ret.getResponseStatusLine ().getStatusCode ());
    assertEquals ("text/plain;charset=utf-8", ret.getResponseHeaders ().getFirstHeaderValue (CHttpHeader.CONTENT_TYPE));

    // 2
    ret = aClient.sendMessageWithRetries (URLBuilder.of (sServerURL)
                                                    .addParam ("content", "<crap/>")
                                                    .addParam ("statuscode", 401)
                                                    .addParam ("mimetype", CMimeType.APPLICATION_XML.getAsString ())
                                                    .build ()
                                                    .getAsString (), aResponseHandlerPlain, null, null, null);
    assertNotNull (ret);
    assertEquals ("<crap/>", new String (ret.getResponseContent (), StandardCharsets.UTF_8));
    assertEquals (401, ret.getResponseStatusLine ().getStatusCode ());
    assertEquals ("application/xml;charset=utf-8",
                  ret.getResponseHeaders ().getFirstHeaderValue (CHttpHeader.CONTENT_TYPE));
  }

  private static final class MockBuilder extends AbstractAS4UserMessageBuilderMIMEPayload <MockBuilder>
  {}

  @Test
  public void testSendingAnythingAndReceiveCrapViaBuilder ()
  {
    final String sServerURL = MockJettySetup.getServerAddressFromSettings ();

    final MockBuilder aUserMessage = new MockBuilder ().as4ProfileID (AS4TestProfileRegistarSPI.AS4_PROFILE_ID_MAY_SIGN_MAY_CRYPT)
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

    // 1
    EAS4UserMessageSendResult eResult = aUserMessage.endpointURL (sServerURL).sendMessageAndCheckForReceipt ();
    assertSame (EAS4UserMessageSendResult.NO_SIGNAL_MESSAGE_RECEIVED, eResult);

    // 2
    eResult = aUserMessage.endpointURL (URLBuilder.of (sServerURL)
                                                  .addParam ("contentid", "receipt12")
                                                  .addParam ("statuscode", 401)
                                                  .addParam ("mimetype", CMimeType.APPLICATION_XML.getAsString ())
                                                  .build ()
                                                  .getAsString ()).sendMessageAndCheckForReceipt ();
    assertSame (EAS4UserMessageSendResult.TRANSPORT_ERROR_NO_RETRY, eResult);
  }
}
