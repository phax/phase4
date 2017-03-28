package com.helger.as4.lib.client;

import static org.junit.Assert.fail;

import java.util.Locale;

import javax.annotation.Nonnull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.as4.client.AS4ClientErrorMessage;
import com.helger.as4.error.EEbmsError;
import com.helger.as4.server.MockJettySetup;
import com.helger.as4.server.MockPModeGenerator;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.soap.ESOAPVersion;

public class AS4ClientErrorMessageTest
{
  @BeforeClass
  public static void startServer () throws Exception
  {
    AS4ServerConfiguration.internalReinitForTestOnly ();
    MockJettySetup.startServer ();
    MockPModeGenerator.ensureMockPModesArePresent ();
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    MockJettySetup.shutDownServer ();
  }

  private static void _ensureInvalidState (@Nonnull final AS4ClientErrorMessage aClient) throws Exception
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

  private static void _ensureValidState (@Nonnull final AS4ClientErrorMessage aClient) throws Exception
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
    final AS4ClientErrorMessage aClient = new AS4ClientErrorMessage ();
    _ensureInvalidState (aClient);
    aClient.setSOAPVersion (ESOAPVersion.AS4_DEFAULT);
    _ensureInvalidState (aClient);
    aClient.addErrorMessage (EEbmsError.EBMS_INVALID_HEADER, Locale.US);
    _ensureInvalidState (aClient);
    aClient.setRefToMessageId ("referencefortestingpurpose");
    _ensureValidState (aClient);
  }
}
