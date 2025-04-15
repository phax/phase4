/*
 * Copyright (C) 2020-2025 Philip Helger (www.helger.com)
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

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.EJavaVersion;
import com.helger.commons.timing.StopWatch;
import com.helger.commons.wrapper.Wrapper;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageBuilder;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.helger.phase4.peppol.server.APConfig;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.util.Phase4Exception;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.read.DOMReader;

/**
 * API to send a document via Peppol. The SBDH is created internally.
 *
 * @author Philip Helger
 */
public final class APIPostSendDocument extends AbstractVerifyingAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIPostSendDocument.class);

  private final EPeppolNetwork m_eStage;

  public APIPostSendDocument (@Nonnull final EPeppolNetwork eStage)
  {
    m_eStage = eStage;
  }

  @Override
  protected void verifiedInvokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                                    @Nonnull @Nonempty final String sPath,
                                    @Nonnull final Map <String, String> aPathVariables,
                                    @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                    @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sSenderID = aPathVariables.get (Phase4API.PARAM_SENDER_ID);
    final String sReceiverID = aPathVariables.get (Phase4API.PARAM_RECEIVER_ID);
    final String sDocTypeID = aPathVariables.get (Phase4API.PARAM_DOC_TYPE_ID);
    final String sProcessID = aPathVariables.get (Phase4API.PARAM_PROCESS_ID);
    final String sCountryCodeC1 = aPathVariables.get (Phase4API.PARAM_COUNTRY_CODE_C1);
    final byte [] aPayloadBytes = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());

    // Check parameters
    if (StringHelper.hasNoText (sSenderID))
      throw new APIParamException ("API call retrieved an empty Sender ID");
    if (StringHelper.hasNoText (sReceiverID))
      throw new APIParamException ("API call retrieved an empty Receiver ID");
    if (StringHelper.hasNoText (sDocTypeID))
      throw new APIParamException ("API call retrieved an empty Document Type ID");
    if (StringHelper.hasNoText (sProcessID))
      throw new APIParamException ("API call retrieved an empty Process ID");
    if (StringHelper.hasNoText (sCountryCodeC1))
      throw new APIParamException ("API call retrieved an empty Country Code C1");
    if (ArrayHelper.isEmpty (aPayloadBytes))
      throw new APIParamException ("API call retrieved an empty payload");

    LOGGER.info ("Trying to send Peppol " +
                 (m_eStage.isTest () ? "Test" : "Production") +
                 " message from '" +
                 sSenderID +
                 "' to '" +
                 sReceiverID +
                 "' using '" +
                 sDocTypeID +
                 "' and '" +
                 sProcessID +
                 "' for '" +
                 sCountryCodeC1 +
                 "'");

    final IIdentifierFactory aIF = PeppolIdentifierFactory.INSTANCE;
    final ISMLInfo aSmlInfo = m_eStage.isTest () ? ESML.DIGIT_TEST : ESML.DIGIT_PRODUCTION;
    final TrustedCAChecker aAPCAChecker = m_eStage.isTest () ? PeppolTrustedCA.peppolTestAP () : PeppolTrustedCA
                                                                                                                .peppolProductionAP ();
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
      if (aDoc == null)
        throw new IllegalStateException ("Failed to read provided payload as XML");

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
                                                                  .smpClient (aSMPClient)
                                                                  .sbdDocumentConsumer (sbdDoc -> {
                                                                    // The created SBDH Instance
                                                                    // Identifier
                                                                    aSendingReport.setSBDHInstanceIdentifier (sbdDoc.getStandardBusinessDocumentHeader ()
                                                                                                                    .getDocumentIdentification ()
                                                                                                                    .getInstanceIdentifier ());
                                                                  })
                                                                  .endpointURLConsumer (endpointUrl -> {
                                                                    // Determined by SMP lookup
                                                                    aSendingReport.setC3EndpointURL (endpointUrl);
                                                                  })
                                                                  .certificateConsumer ( (aAPCertificate,
                                                                                          aCheckDT,
                                                                                          eCertCheckResult) -> {
                                                                    // Determined by SMP lookup
                                                                    aSendingReport.setC3Cert (aAPCertificate);
                                                                    aSendingReport.setC3CertCheckDT (aCheckDT);
                                                                    aSendingReport.setC3CertCheckResult (eCertCheckResult);
                                                                  })
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

    // Return result JSON
    aUnifiedResponse.setContentAndCharset (aSendingReport.getAsJsonString (), StandardCharsets.UTF_8)
                    .setMimeType (CMimeType.APPLICATION_JSON)
                    .disableCaching ();
  }
}
