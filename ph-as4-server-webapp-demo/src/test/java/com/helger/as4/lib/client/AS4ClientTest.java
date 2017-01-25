package com.helger.as4.lib.client;

import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.as4.client.AS4Client;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.server.MockJettySetup;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.util.AS4ResourceManager;

public class AS4ClientTest
{

  private static AS4ResourceManager s_aResMgr;
  private final String sServerURL = "http://127.0.0.1:8080/as4";

  @BeforeClass
  public static void startServer () throws Exception
  {
    AS4ServerConfiguration.reinitForTestOnly ();
    MockJettySetup.startServer ();
    s_aResMgr = MockJettySetup.getResourceManagerInstance ();
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    s_aResMgr = null;
    MockJettySetup.shutDownServer ();
  }

  private static void _test (final AS4Client aClient) throws Exception
  {
    try
    {
      aClient.buildMessage ();
      fail ();
    }
    catch (final IllegalStateException ex)
    {
      // expected
    }
  }

  @Test
  public void buildMessageMandatoryCheck () throws Exception
  {
    final AS4Client aClient = new AS4Client ();
    _test (aClient);
    aClient.setAction ("AnAction");
    _test (aClient);
    aClient.setServiceType ("MyServiceType");
    _test (aClient);
    aClient.setServiceValue ("OrderPaper");
    _test (aClient);
    aClient.setConversationID ("9898");
    _test (aClient);
    aClient.setAgreementRefPMode ("pm-esens-generic-resp");
    _test (aClient);
    aClient.setAgreementRefValue ("http://agreements.holodeckb2b.org/examples/agreement0");
    _test (aClient);
    aClient.setFromRole ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultRole");
    _test (aClient);
    aClient.setFromPartyID ("MyPartyIDforSending");
    _test (aClient);
    aClient.setToRole ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultRole");
    _test (aClient);
    aClient.setToPartyID ("MyPartyIDforReceving");
    _test (aClient);
    // IllegalState still occurs since properties are missing
  }

  @Test
  public void sendBodyPayloadMessageSuccessful () throws Exception
  {
    final AS4Client aClient = new AS4Client ();
    aClient.setAction ("AnAction");
    aClient.setServiceType ("MyServiceType");
    aClient.setServiceValue ("OrderPaper");
    aClient.setConversationID ("9898");
    aClient.setAgreementRefPMode ("pm-esens-generic-resp");
    aClient.setAgreementRefValue ("http://agreements.holodeckb2b.org/examples/agreement0");
    aClient.setFromRole ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultRole");
    aClient.setFromPartyID ("MyPartyIDforSending");
    aClient.setToRole ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultRole");
    aClient.setToPartyID ("MyPartyIDforReceving");
    aClient.setEbms3Properties (MockEbmsHelper.getEBMSProperties ());
    aClient.sendMessage (sServerURL, aClient.buildMessage ());
  }
}
