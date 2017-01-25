/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.lib.client;

import static org.junit.Assert.fail;

import javax.annotation.Nonnull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.as4.CAS4;
import com.helger.as4.client.AS4Client;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.server.MockJettySetup;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.util.AS4ResourceManager;

public class AS4ClientTest
{
  private static AS4ResourceManager s_aResMgr;
  private static final String SERVER_URL = "http://127.0.0.1:8080/as4";

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

  private static void _ensureInvalidState (@Nonnull final AS4Client aClient) throws Exception
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
    final AS4Client aClient = new AS4Client (s_aResMgr);
    _ensureInvalidState (aClient);
    aClient.setAction ("AnAction");
    _ensureInvalidState (aClient);
    aClient.setServiceType ("MyServiceType");
    _ensureInvalidState (aClient);
    aClient.setServiceValue ("OrderPaper");
    _ensureInvalidState (aClient);
    aClient.setConversationID ("9898");
    _ensureInvalidState (aClient);
    aClient.setAgreementRefPMode ("pm-esens-generic-resp");
    _ensureInvalidState (aClient);
    aClient.setAgreementRefValue ("http://agreements.holodeckb2b.org/examples/agreement0");
    _ensureInvalidState (aClient);
    aClient.setFromRole (CAS4.DEFAULT_ROLE);
    _ensureInvalidState (aClient);
    aClient.setFromPartyID ("MyPartyIDforSending");
    _ensureInvalidState (aClient);
    aClient.setToRole (CAS4.DEFAULT_ROLE);
    _ensureInvalidState (aClient);
    aClient.setToPartyID ("MyPartyIDforReceving");
    _ensureInvalidState (aClient);
    // IllegalState still occurs since properties are missing
  }

  @Test
  public void sendBodyPayloadMessageSuccessful () throws Exception
  {
    final AS4Client aClient = new AS4Client (s_aResMgr);
    aClient.setAction ("AnAction");
    aClient.setServiceType ("MyServiceType");
    aClient.setServiceValue ("OrderPaper");
    aClient.setConversationID ("9898");
    aClient.setAgreementRefPMode ("pm-esens-generic-resp");
    aClient.setAgreementRefValue ("http://agreements.holodeckb2b.org/examples/agreement0");
    aClient.setFromRole (CAS4.DEFAULT_ROLE);
    aClient.setFromPartyID ("MyPartyIDforSending");
    aClient.setToRole (CAS4.DEFAULT_ROLE);
    aClient.setToPartyID ("MyPartyIDforReceving");
    aClient.setEbms3Properties (MockEbmsHelper.getEBMSProperties ());
    aClient.sendMessage (SERVER_URL);
  }
}
