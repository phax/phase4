/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
import org.w3c.dom.Node;

import com.helger.as4.AS4TestConstants;
import com.helger.as4.client.AS4ClientReceiptMessage;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.server.MockJettySetup;
import com.helger.as4.server.MockPModeGenerator;
import com.helger.as4.server.message.MockMessages;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.xml.serialize.read.DOMReader;

public final class AS4ClientReceiptMessageTest
{
  private static AS4ResourceManager s_aResMgr;

  @BeforeClass
  public static void startServer () throws Exception
  {
    AS4ServerConfiguration.internalReinitForTestOnly ();
    MockJettySetup.startServer ();
    s_aResMgr = MockJettySetup.getResourceManagerInstance ();
    MockPModeGenerator.ensureMockPModesArePresent ();
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    s_aResMgr = null;
    MockJettySetup.shutDownServer ();
  }

  private static void _ensureInvalidState (@Nonnull final AS4ClientReceiptMessage aClient) throws Exception
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

  private static void _ensureValidState (@Nonnull final AS4ClientReceiptMessage aClient) throws Exception
  {
    try
    {
      aClient.buildMessage ();
    }
    catch (final IllegalStateException ex)
    {
      fail ();
    }
  }

  @Test
  public void buildMessageMandatoryCheckFailure () throws Exception
  {
    final AS4ClientReceiptMessage aClient = new AS4ClientReceiptMessage (s_aResMgr);
    _ensureInvalidState (aClient);
    aClient.setSOAPVersion (ESOAPVersion.AS4_DEFAULT);
    _ensureInvalidState (aClient);
    // Parse EBMS3 Messaging object
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    aClient.setSOAPDocument (MockMessages.testSignedUserMessage (aClient.getSOAPVersion (), aPayload, null, s_aResMgr));
    _ensureInvalidState (aClient);
    aClient.setNonRepudiation (true);
    _ensureValidState (aClient);
  }

  @Test
  public void buildMessageSignedChecks () throws Exception
  {
    final AS4ClientReceiptMessage aClient = new AS4ClientReceiptMessage (s_aResMgr);
    aClient.setSOAPVersion (ESOAPVersion.AS4_DEFAULT);
    // Parse EBMS3 Messaging object
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    aClient.setSOAPDocument (MockMessages.testSignedUserMessage (aClient.getSOAPVersion (), aPayload, null, s_aResMgr));
    aClient.setNonRepudiation (true);
    aClient.setReceiptShouldBeSigned (true);

    aClient.setKeyStoreResource (new ClassPathResource ("keys/dummy-pw-test.jks"));
    aClient.setKeyStorePassword ("test");
    aClient.setKeyStoreType (EKeyStoreType.JKS);
    aClient.setKeyStoreAlias ("ph-as4");
    aClient.setKeyStoreKeyPassword ("test");

    aClient.setCryptoAlgorithmSign (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT);
    aClient.setCryptoAlgorithmSignDigest (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

    _ensureValidState (aClient);
  }
}
