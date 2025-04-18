/*
 * Copyright (C) 2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.dbnalliance.server.api;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.w3c.dom.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.commons.wrapper.Wrapper;
import com.helger.dbnalliance.commons.EDBNAllianceStage;
import com.helger.dbnalliance.commons.security.DBNAllianceTrustStores;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.dbnalliance.Phase4DBNAllianceSender;
import com.helger.phase4.dbnalliance.Phase4DBNAllianceSender.DBNAllianceUserMessageBuilder;
import com.helger.phase4.dbnalliance.Phase4DBNAllianceSendingReport;
import com.helger.phase4.dbnalliance.server.APConfig;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.profile.dbnalliance.Phase4DBNAllianceHttpClientSettings;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.util.Phase4Exception;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.smpclient.bdxr2.BDXR2ClientReadOnly;
import com.helger.smpclient.url.DBNAURLProviderSMP;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.read.DOMReader;

/**
 * API to send a document via Peppol. The SBDH is created internally.
 *
 * @author Philip Helger
 */
public final class APIPostSendDocument extends AbstractVerifyingAPIExecutor
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (APIPostSendDocument.class);

  private final EDBNAllianceStage m_eStage;

  public APIPostSendDocument (@Nonnull final EDBNAllianceStage eStage)
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
    if (ArrayHelper.isEmpty (aPayloadBytes))
      throw new APIParamException ("API call retrieved an empty payload");

    LOGGER.info ("Trying to send DBNAlliance " +
                 m_eStage +
                 " message from '" +
                 sSenderID +
                 "' to '" +
                 sReceiverID +
                 "' using '" +
                 sDocTypeID +
                 "' and '" +
                 sProcessID +
                 "'");

    final IIdentifierFactory aIF = PeppolIdentifierFactory.INSTANCE;
    final EDBNAllianceStage eStage = APConfig.getStage ();
    final TrustedCAChecker aAPCAChecker = DBNAllianceTrustStores.Config2023.PILOT_CA;
    final String sMySeatID = APConfig.getMySeatID ();

    final Phase4DBNAllianceSendingReport aSendingReport = new Phase4DBNAllianceSendingReport (eStage.getSML ()
                                                                                                    .getZoneName ());
    aSendingReport.setSenderPartyID (sMySeatID);

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

      final BDXR2ClientReadOnly aSMPClient = new BDXR2ClientReadOnly (DBNAURLProviderSMP.INSTANCE.getSMPURIOfParticipant (aReceiverID,
                                                                                                                          eStage.getSML ()
                                                                                                                                .getZoneName ()));
      // TODO Set correct truststore here
      aSMPClient.setTrustStore (DBNAllianceTrustStores.Config2023.TRUSTSTORE_PILOT);

      aSMPClient.withHttpClientSettings (aHCS -> {
        // TODO Add SMP outbound proxy settings here
        // If this block is not used, it may be removed
      });

      final Phase4DBNAllianceHttpClientSettings aHCS = new Phase4DBNAllianceHttpClientSettings ();
      // TODO Add AP outbound proxy settings here

      final DBNAllianceUserMessageBuilder aBuilder = Phase4DBNAllianceSender.builder ()
                                                                            .httpClientFactory (aHCS)
                                                                            .documentTypeID (aDocTypeID)
                                                                            .processID (aProcessID)
                                                                            .senderParticipantID (aSenderID)
                                                                            .receiverParticipantID (aReceiverID)
                                                                            .fromPartyID (sMySeatID)
                                                                            .payloadElement (aDoc.getDocumentElement ())
                                                                            .apCAChecker (aAPCAChecker)
                                                                            .smpClient (aSMPClient)
                                                                            .xheDocumentConsumer (xheDoc -> {
                                                                              // The created XHE
                                                                              // header ID
                                                                              aSendingReport.setXHEHeaderID (xheDoc.getHeader ()
                                                                                                                   .getIDValue ());
                                                                            })
                                                                            .endpointURLConsumer (endpointUrl -> {
                                                                              // Determined by SMP
                                                                              // lookup
                                                                              aSendingReport.setC3EndpointURL (endpointUrl);
                                                                            })
                                                                            .certificateConsumer ( (aAPCertificate,
                                                                                                    aCheckDT,
                                                                                                    eCertCheckResult) -> {
                                                                              // Determined by SMP
                                                                              // lookup
                                                                              aSendingReport.setC3Cert (aAPCertificate);
                                                                              aSendingReport.setC3CertCheckDT (aCheckDT);
                                                                              aSendingReport.setC3CertCheckResult (eCertCheckResult);
                                                                            })
                                                                            .buildMessageCallback (new IAS4ClientBuildMessageCallback ()
                                                                            {
                                                                              public void onAS4Message (@Nonnull final AbstractAS4Message <?> aMsg)
                                                                              {
                                                                                // Created AS4
                                                                                // fields
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
      LOGGER.info ("DBNAlliance client send result: " + eResult);

      aSendingReport.setAS4SendingResult (eResult);

      if (aCaughtEx.isSet ())
      {
        final Phase4Exception ex = aCaughtEx.get ();
        LOGGER.error ("Error sending DBNAlliance message via AS4", ex);
        aSendingReport.setAS4SendingException (ex);
        bExceptionCaught = true;
      }
    }
    catch (final Exception ex)
    {
      // Mostly errors on HTTP level
      LOGGER.error ("Error sending DBNAlliance message via AS4", ex);
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
