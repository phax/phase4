package com.helger.phase4.bdew;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.wrapper.Wrapper;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.bdew.Phase4BDEWSender.BDEWPayloadParams;
import com.helger.phase4.crypto.ECryptoKeyIdentifierType;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilder.ESimpleUserMessageSendResult;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;

public class MainPhase4BDEWSenderExample
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4BDEWSenderExample.class);

  public static void main (final String [] args)
  {
    // Create scope for global variables that can be shut down gracefully
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Optional dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    try
    {
      // Read XML payload to send
      final byte [] aPayloadBytes = Files.readAllBytes (new File ("src/test/resources/external/examples/base-example.xml").toPath ());
      if (aPayloadBytes == null)
        throw new IllegalStateException ("Failed to read file to be send");

      final BDEWPayloadParams aBDEWPayloadParams = new BDEWPayloadParams ();
      aBDEWPayloadParams.setDocumentType ("DT1");
      aBDEWPayloadParams.setDocumentDate (PDTFactory.getCurrentZonedDateTimeUTC ());
      aBDEWPayloadParams.setDocumentNumber (1234);
      aBDEWPayloadParams.setFulfillmentDate (PDTFactory.getCurrentZonedDateTimeUTC ().minusMonths (2));
      aBDEWPayloadParams.setSubjectPartyId ("Party1");
      aBDEWPayloadParams.setSubjectPartyRole ("Role1");

      final Wrapper <Ebms3SignalMessage> aSignalMsgHolder = new Wrapper <> ();

      // Start configuring here
      final ESimpleUserMessageSendResult eResult;
      eResult = Phase4BDEWSender.builder ()
                                .encryptionKeyIdentifierType (ECryptoKeyIdentifierType.X509_KEY_IDENTIFIER)
                                .signingKeyIdentifierType (ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE)
                                .fromPartyID ("AS4-Sender")
                                .fromRole (CAS4.DEFAULT_INITIATOR_URL)
                                .toPartyID ("AS4-Receiver")
                                .toRole (CAS4.DEFAULT_RESPONDER_URL)
                                .endpointURL ("https://receiver.example.org/bdew/as4")
                                .service ("AS4-Service")
                                .action ("AS4-Action")
                                .payload (AS4OutgoingAttachment.builder ()
                                                               .data (aPayloadBytes)
                                                               .compressionGZIP ()
                                                               .mimeTypeXML ()
                                                               .charset (StandardCharsets.UTF_8), aBDEWPayloadParams)
                                .signalMsgConsumer (aSignalMsgHolder::set)
                                .sendMessageAndCheckForReceipt ();
      LOGGER.info ("BDEW send result: " + eResult);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending BDEW message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
