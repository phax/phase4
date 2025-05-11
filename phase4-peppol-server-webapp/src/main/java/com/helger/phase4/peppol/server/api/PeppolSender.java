/*
 * Copyright (C) 2023-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol.server.api;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.system.EJavaVersion;
import com.helger.commons.timing.StopWatch;
import com.helger.commons.wrapper.Wrapper;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderPeppol;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageBuilder;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageSBDHBuilder;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.helger.phase4.peppol.server.APConfig;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.util.Phase4Exception;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.xml.serialize.read.DOMReader;

/**
 * This contains the main Peppol sending code. It was extracted from the controller to make it more
 * readable
 *
 * @author Philip Helger
 */
@Immutable
public final class PeppolSender
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolSender.class);

  private PeppolSender ()
  {}

  /**
   * Send a Peppol message where the SBDH is created internally by phase4
   *
   * @param aSmlInfo
   *        The SML to be used for receiver lookup
   * @param aAPCAChecker
   *        The Peppol CA checker to be used.
   * @param aPayloadBytes
   *        The main business document to be send
   * @param sSenderID
   *        The Peppol sender Participant ID
   * @param sReceiverID
   *        The Peppol receiver Participant ID
   * @param sDocTypeID
   *        The Peppol document type ID
   * @param sProcessID
   *        The Peppol process ID
   * @param sCountryCodeC1
   *        The Country Code of the sender (C1)
   * @return The created sending report and never <code>null</code>.
   */
  @Nonnull
  public static Phase4PeppolSendingReport sendPeppolMessageCreatingSbdh (@Nonnull final ISMLInfo aSmlInfo,
                                                                         @Nonnull final TrustedCAChecker aAPCAChecker,
                                                                         @Nonnull final byte [] aPayloadBytes,
                                                                         @Nonnull @Nonempty final String sSenderID,
                                                                         @Nonnull @Nonempty final String sReceiverID,
                                                                         @Nonnull @Nonempty final String sDocTypeID,
                                                                         @Nonnull @Nonempty final String sProcessID,
                                                                         @Nonnull @Nonempty final String sCountryCodeC1)
  {
    final IIdentifierFactory aIF = PeppolIdentifierFactory.INSTANCE;
    final String sMyPeppolSeatID = APConfig.getMyPeppolSeatID ();

    final Phase4PeppolSendingReport aSendingReport = new Phase4PeppolSendingReport (aSmlInfo);
    aSendingReport.setCountryC1 (sCountryCodeC1);
    aSendingReport.setSenderPartyID (sMyPeppolSeatID);

    EAS4UserMessageSendResult eResult = null;
    boolean bExceptionCaught = false;
    final StopWatch aSW = StopWatch.createdStarted ();
    try
    {
      // Payload must be XML - even for Text and Binary content
      final Document aDoc = DOMReader.readXMLDOM (aPayloadBytes);
      if (aDoc == null || aDoc.getDocumentElement () == null)
        throw new IllegalStateException ("Failed to read provided payload as XML");
      if (aDoc.getDocumentElement ().getNamespaceURI () == null)
        throw new IllegalStateException ("Only XML payloads with a namespace are supported");

      // Start configuring here
      final IParticipantIdentifier aSenderID = aIF.createParticipantIdentifierWithDefaultScheme (sSenderID);
      aSendingReport.setSenderID (aSenderID);

      final IParticipantIdentifier aReceiverID = aIF.createParticipantIdentifierWithDefaultScheme (sReceiverID);
      aSendingReport.setReceiverID (aReceiverID);

      IDocumentTypeIdentifier aDocTypeID = aIF.parseDocumentTypeIdentifier (sDocTypeID);
      if (aDocTypeID == null)
      {
        // Fallback to default scheme
        aDocTypeID = aIF.createDocumentTypeIdentifierWithDefaultScheme (sDocTypeID);
      }
      aSendingReport.setDocTypeID (aDocTypeID);
      IProcessIdentifier aProcessID = aIF.parseProcessIdentifier (sProcessID);
      if (aProcessID == null)
      {
        // Fallback to default scheme
        aProcessID = aIF.createProcessIdentifierWithDefaultScheme (sProcessID);
      }
      aSendingReport.setProcessID (aProcessID);

      final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                                                                  aReceiverID,
                                                                  aSmlInfo);

      aSMPClient.withHttpClientSettings (aHCS -> {
        // TODO Add SMP HTTP outbound proxy settings here
        // If this block is not used, it may be removed
      });

      if (EJavaVersion.getCurrentVersion ().isNewerOrEqualsThan (EJavaVersion.JDK_17))
      {
        // Work around the disabled SHA-1 in XMLDsig issue
        aSMPClient.setSecureValidation (false);
      }

      final Phase4PeppolHttpClientSettings aHCS = new Phase4PeppolHttpClientSettings ();
      // TODO Add AP HTTP outbound proxy settings here

      final PeppolUserMessageBuilder aBuilder = Phase4PeppolSender.builder ()
                                                                  .httpClientFactory (aHCS)
                                                                  .documentTypeID (aDocTypeID)
                                                                  .processID (aProcessID)
                                                                  .senderParticipantID (aSenderID)
                                                                  .receiverParticipantID (aReceiverID)
                                                                  .senderPartyID (sMyPeppolSeatID)
                                                                  .countryC1 (sCountryCodeC1)
                                                                  .payload (aDoc.getDocumentElement ())
                                                                  .peppolAP_CAChecker (aAPCAChecker)
                                                                  .endpointDetailProvider (AS4EndpointDetailProviderPeppol.create (aSMPClient)
                                                                                                                          .setUsePFUOI430 (APConfig.isUsePFUOI430 ()))
                                                                  .sbdDocumentConsumer (sbd -> {
                                                                    // Remember SBDH Instance
                                                                    // Identifier
                                                                    aSendingReport.setSBDHInstanceIdentifier (sbd.getStandardBusinessDocumentHeader ()
                                                                                                                 .getDocumentIdentification ()
                                                                                                                 .getInstanceIdentifier ());
                                                                  })
                                                                  .endpointURLConsumer (sEndpointUrl -> {
                                                                    // Determined by SMP lookup
                                                                    aSendingReport.setC3EndpointURL (sEndpointUrl);
                                                                  })
                                                                  .certificateConsumer ( (aAPCertificate,
                                                                                          aCheckDT,
                                                                                          eCertCheckResult) -> {
                                                                    // Determined by SMP lookup
                                                                    aSendingReport.setC3Cert (aAPCertificate);
                                                                    aSendingReport.setC3CertCheckDT (aCheckDT);
                                                                    aSendingReport.setC3CertCheckResult (eCertCheckResult);
                                                                  })
                                                                  .sendingDateTimeConsumer (aSendingReport::setAS4SendingDT)
                                                                  .buildMessageCallback (new IAS4ClientBuildMessageCallback ()
                                                                  {
                                                                    public void onAS4Message (@Nonnull final AbstractAS4Message <?> aMsg)
                                                                    {
                                                                      // Created AS4 fields
                                                                      final AS4UserMessage aUserMsg = (AS4UserMessage) aMsg;
                                                                      aSendingReport.setAS4MessageID (aUserMsg.getEbms3UserMessage ()
                                                                                                              .getMessageInfo ()
                                                                                                              .getMessageId ());
                                                                      aSendingReport.setAS4ConversationID (aUserMsg.getEbms3UserMessage ()
                                                                                                                   .getCollaborationInfo ()
                                                                                                                   .getConversationId ());
                                                                    }
                                                                  })
                                                                  .signalMsgConsumer ( (aSignalMsg,
                                                                                        aMessageMetadata,
                                                                                        aState) -> {
                                                                    aSendingReport.setAS4ReceivedSignalMsg (aSignalMsg);
                                                                  })
                                                                  .disableValidation ();
      final Wrapper <Phase4Exception> aCaughtEx = new Wrapper <> ();
      eResult = aBuilder.sendMessageAndCheckForReceipt (aCaughtEx::set);
      LOGGER.info ("Peppol client send result: " + eResult);

      if (eResult.isSuccess ())
      {
        // TODO determine the enduser ID of the outbound message
        // In many simple cases, this might be the sender's participant ID
        final String sEndUserID = "TODO";

        // TODO Enable Peppol Reporting when ready
        if (false)
          aBuilder.createAndStorePeppolReportingItemAfterSending (sEndUserID);
      }

      aSendingReport.setAS4SendingResult (eResult);

      if (aCaughtEx.isSet ())
      {
        final Phase4Exception ex = aCaughtEx.get ();
        LOGGER.error ("Error sending Peppol message via AS4", ex);
        aSendingReport.setAS4SendingException (ex);
        bExceptionCaught = true;
      }
    }
    catch (final Exception ex)
    {
      // Mostly errors on HTTP level
      LOGGER.error ("Error sending Peppol message via AS4", ex);
      aSendingReport.setAS4SendingException (ex);
      bExceptionCaught = true;
    }
    finally
    {
      aSW.stop ();
      aSendingReport.setOverallDurationMillis (aSW.getMillis ());
    }

    // Result may be null
    final boolean bSendingSuccess = eResult != null && eResult.isSuccess ();
    aSendingReport.setSendingSuccess (bSendingSuccess);
    aSendingReport.setOverallSuccess (bSendingSuccess && !bExceptionCaught);

    return aSendingReport;
  }

  /**
   * Send a Peppol message where the SBDH is passed in from the outside
   *
   * @param aData
   *        The Peppol SBDH data to be send
   * @param aSmlInfo
   *        The SML to be used for receiver lookup
   * @param aAPCAChecker
   *        The Peppol CA checker to be used.
   * @param aSendingReport
   *        The sending report to be filled.
   */
  static void sendPeppolMessagePredefinedSbdh (@Nonnull final PeppolSBDHData aData,
                                               @Nonnull final ISMLInfo aSmlInfo,
                                               @Nonnull final TrustedCAChecker aAPCAChecker,
                                               @Nonnull final Phase4PeppolSendingReport aSendingReport)
  {
    final String sMyPeppolSeatID = APConfig.getMyPeppolSeatID ();
    aSendingReport.setSenderPartyID (sMyPeppolSeatID);

    EAS4UserMessageSendResult eResult = null;
    boolean bExceptionCaught = false;
    final StopWatch aSW = StopWatch.createdStarted ();
    try
    {
      // Start configuring here
      final IParticipantIdentifier aReceiverID = aData.getReceiverAsIdentifier ();

      final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                                                                  aReceiverID,
                                                                  aSmlInfo);

      aSMPClient.withHttpClientSettings (aHCS -> {
        // TODO Add SMP HTTP outbound proxy settings here
        // If this block is not used, it may be removed
      });

      if (EJavaVersion.getCurrentVersion ().isNewerOrEqualsThan (EJavaVersion.JDK_17))
      {
        // Work around the disabled SHA-1 in XMLDsig issue
        aSMPClient.setSecureValidation (false);
      }

      final Phase4PeppolHttpClientSettings aHCS = new Phase4PeppolHttpClientSettings ();
      // TODO Add AP HTTP outbound proxy settings here

      final PeppolUserMessageSBDHBuilder aBuilder = Phase4PeppolSender.sbdhBuilder ()
                                                                      .httpClientFactory (aHCS)
                                                                      .payloadAndMetadata (aData)
                                                                      .senderPartyID (sMyPeppolSeatID)
                                                                      .peppolAP_CAChecker (aAPCAChecker)
                                                                      .endpointDetailProvider (AS4EndpointDetailProviderPeppol.create (aSMPClient)
                                                                                                                              .setUsePFUOI430 (APConfig.isUsePFUOI430 ()))
                                                                      .endpointURLConsumer (sEndpointUrl -> {
                                                                        // Determined by SMP lookup
                                                                        aSendingReport.setC3EndpointURL (sEndpointUrl);
                                                                      })
                                                                      .certificateConsumer ( (aAPCertificate,
                                                                                              aCheckDT,
                                                                                              eCertCheckResult) -> {
                                                                        // Determined by SMP lookup
                                                                        aSendingReport.setC3Cert (aAPCertificate);
                                                                        aSendingReport.setC3CertCheckDT (aCheckDT);
                                                                        aSendingReport.setC3CertCheckResult (eCertCheckResult);
                                                                      })
                                                                      .sendingDateTimeConsumer (aSendingReport::setAS4SendingDT)
                                                                      .buildMessageCallback (new IAS4ClientBuildMessageCallback ()
                                                                      {
                                                                        public void onAS4Message (@Nonnull final AbstractAS4Message <?> aMsg)
                                                                        {
                                                                          // Created AS4 fields
                                                                          final AS4UserMessage aUserMsg = (AS4UserMessage) aMsg;
                                                                          aSendingReport.setAS4MessageID (aUserMsg.getEbms3UserMessage ()
                                                                                                                  .getMessageInfo ()
                                                                                                                  .getMessageId ());
                                                                          aSendingReport.setAS4ConversationID (aUserMsg.getEbms3UserMessage ()
                                                                                                                       .getCollaborationInfo ()
                                                                                                                       .getConversationId ());
                                                                        }
                                                                      })
                                                                      .signalMsgConsumer ( (aSignalMsg,
                                                                                            aMessageMetadata,
                                                                                            aState) -> {
                                                                        aSendingReport.setAS4ReceivedSignalMsg (aSignalMsg);
                                                                      });
      final Wrapper <Phase4Exception> aCaughtEx = new Wrapper <> ();
      eResult = aBuilder.sendMessageAndCheckForReceipt (aCaughtEx::set);
      LOGGER.info ("Peppol client send result: " + eResult);

      if (eResult.isSuccess ())
      {
        // TODO determine the enduser ID of the outbound message
        // In many simple cases, this might be the sender's participant ID
        final String sEndUserID = "TODO";

        // TODO Enable Peppol Reporting when ready
        if (false)
          aBuilder.createAndStorePeppolReportingItemAfterSending (sEndUserID);
      }

      aSendingReport.setAS4SendingResult (eResult);

      if (aCaughtEx.isSet ())
      {
        final Phase4Exception ex = aCaughtEx.get ();
        LOGGER.error ("Error sending Peppol message via AS4", ex);
        aSendingReport.setAS4SendingException (ex);
        bExceptionCaught = true;
      }
    }
    catch (final Exception ex)
    {
      // Mostly errors on HTTP level
      LOGGER.error ("Error sending Peppol message via AS4", ex);
      aSendingReport.setAS4SendingException (ex);
      bExceptionCaught = true;
    }
    finally
    {
      aSW.stop ();
      aSendingReport.setOverallDurationMillis (aSW.getMillis ());
    }

    // Result may be null
    final boolean bSendingSuccess = eResult != null && eResult.isSuccess ();
    aSendingReport.setSendingSuccess (bSendingSuccess);
    aSendingReport.setOverallSuccess (bSendingSuccess && !bExceptionCaught);
  }
}
