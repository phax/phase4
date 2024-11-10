/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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

import static org.junit.Assert.fail;

import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Node;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.server.AbstractAS4TestSetUp;
import com.helger.phase4.server.MockJettySetup;
import com.helger.phase4.server.message.MockMessages;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreAndKeyDescriptor;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test class for class {@link AS4ClientReceiptMessage}
 *
 * @author Philip Helger
 */
public final class AS4ClientReceiptMessageTest extends AbstractAS4TestSetUp
{
  @WillNotClose
  private static AS4ResourceHelper s_aResMgr;

  @BeforeClass
  public static void startServer () throws Exception
  {
    MockJettySetup.startServer ();
    s_aResMgr = MockJettySetup.getResourceManagerInstance ();
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
      aClient.buildMessage ("bla", null);
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
      aClient.buildMessage ("bla", null);
    }
    catch (final IllegalStateException ex)
    {
      fail ();
    }
  }

  @Test
  public void testBuildMessageMandatoryCheckFailure () throws Exception
  {
    final AS4ClientReceiptMessage aClient = new AS4ClientReceiptMessage (s_aResMgr);
    _ensureInvalidState (aClient);
    aClient.setSoapVersion (ESoapVersion.AS4_DEFAULT);
    _ensureInvalidState (aClient);
    // Parse EBMS3 Messaging object
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    aClient.setSoapDocument (MockMessages.createUserMessageSigned (aClient.getSoapVersion (),
                                                                   aPayload,
                                                                   null,
                                                                   s_aResMgr));
    _ensureInvalidState (aClient);
    aClient.setNonRepudiation (true);
    _ensureValidState (aClient);
  }

  @Test
  public void testBuildMessageSignedChecks () throws Exception
  {
    final AS4ClientReceiptMessage aClient = new AS4ClientReceiptMessage (s_aResMgr);
    aClient.setSoapVersion (ESoapVersion.AS4_DEFAULT);
    // Parse EBMS3 Messaging object
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    aClient.setSoapDocument (MockMessages.createUserMessageSigned (aClient.getSoapVersion (),
                                                                   aPayload,
                                                                   null,
                                                                   s_aResMgr));
    aClient.setNonRepudiation (true);
    aClient.setReceiptShouldBeSigned (true);

    aClient.setCryptoFactory (new AS4CryptoFactoryInMemoryKeyStore (KeyStoreAndKeyDescriptor.builder ()
                                                                                            .type (EKeyStoreType.JKS)
                                                                                            .path ("keys/dummy-pw-test.jks")
                                                                                            .password ("test")
                                                                                            .keyAlias ("ph-as4")
                                                                                            .keyPassword ("test")
                                                                                            .build (),
                                                                    null));

    aClient.signingParams ()
           .setAlgorithmSign (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT)
           .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

    _ensureValidState (aClient);
  }
}
