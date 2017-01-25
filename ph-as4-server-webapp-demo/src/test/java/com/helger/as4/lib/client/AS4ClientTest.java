package com.helger.as4.lib.client;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.as4.client.AS4Client;
import com.helger.as4.server.MockJettySetup;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.util.AS4ResourceManager;

public class AS4ClientTest
{

  private static AS4ResourceManager s_aResMgr;

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

  @Test
  public void sendBodyPayloadMessageSuccessful ()
  {
    final AS4Client aClient = new AS4Client ();
  }
}
