package com.helger.as4.lib.client;

import static org.junit.Assert.fail;

import javax.annotation.Nonnull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Node;

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
import com.helger.xml.serialize.read.DOMReader;

public class AS4ClientReceiptMessageTest
{
  private static AS4ResourceManager s_aResMgr;
  private static final String SERVER_URL = "http://127.0.0.1:8080/as4";

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
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
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
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    aClient.setSOAPDocument (MockMessages.testSignedUserMessage (aClient.getSOAPVersion (), aPayload, null, s_aResMgr));
    aClient.setNonRepudiation (true);
    aClient.setReceiptShouldBeSigned (true);

    aClient.setKeyStoreAlias ("ph-as4");
    aClient.setKeyStorePassword ("test");
    aClient.setKeyStoreFile (new ClassPathResource ("keys/dummy-pw-test.jks").getAsFile ());
    aClient.setKeyStoreType ("jks");

    aClient.setCryptoAlgorithmSign (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT);
    aClient.setCryptoAlgorithmSignDigest (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

    _ensureValidState (aClient);
  }
}
