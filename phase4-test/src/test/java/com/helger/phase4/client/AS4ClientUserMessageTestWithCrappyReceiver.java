package com.helger.phase4.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.concurrent.ThreadHelper;
import com.helger.base.url.URLHelper;
import com.helger.http.CHttpHeader;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.server.AS4JettyRunner;
import com.helger.phase4.server.MockJettySetup;
import com.helger.phase4.test.profile.AS4TestProfileRegistarSPI;
import com.helger.phase4.util.AS4ResourceHelper;
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
  public void testSendingAnythingAndReceiveCrap () throws WSSecurityException, IOException, MessagingException
  {
    final String sServerURL = MockJettySetup.getServerAddressFromSettings ();

    final MockAS4ClientUserMessage aUserMessage = AS4ClientUserMessageTest.createMandatoryAttributesSuccessMessage (s_aResHelper);
    aUserMessage.setPayload (DOMReader.readXMLDOM ("<root xmlns='urn:any'/>"));
    final AS4ClientSentMessage <byte []> ret = aUserMessage.sendMessageWithRetries (sServerURL,
                                                                                    new ResponseHandlerByteArray (),
                                                                                    null,
                                                                                    null,
                                                                                    null);
    assertNotNull (ret);
    assertEquals ("Plain Text", new String (ret.getResponseContent (), StandardCharsets.UTF_8));
    assertEquals ("text/plain;charset=utf-8", ret.getResponseHeaders ().getFirstHeaderValue (CHttpHeader.CONTENT_TYPE));
  }
}
