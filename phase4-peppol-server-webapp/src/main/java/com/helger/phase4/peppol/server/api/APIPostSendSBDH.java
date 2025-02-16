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

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.system.EJavaVersion;
import com.helger.commons.timing.StopWatch;
import com.helger.commons.wrapper.Wrapper;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.PeppolSBDHDataReadException;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.utils.PeppolCAChecker;
import com.helger.peppol.utils.PeppolCertificateChecker;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.dump.AS4RawResponseConsumerWriteToFile;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageSBDHBuilder;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.helger.phase4.peppol.server.APConfig;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.util.Phase4Exception;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * API to send a document via Peppol. Requires a ready Peppol SBDH as input.
 *
 * @author Philip Helger
 */
public final class APIPostSendSBDH extends AbstractVerifyingAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIPostSendSBDH.class);

  private final EPeppolNetwork m_eStage;

  public APIPostSendSBDH (@Nonnull final EPeppolNetwork eStage)
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
    final byte [] aPayloadBytes = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());

    // Check parameters
    if (ArrayHelper.isEmpty (aPayloadBytes))
      throw new APIParamException ("API call retrieved an empty payload");

    final ISMLInfo aSmlInfo = m_eStage.isTest () ? ESML.DIGIT_TEST : ESML.DIGIT_PRODUCTION;
    final Phase4PeppolSendingReport aSendingReport = new Phase4PeppolSendingReport (aSmlInfo);

    final PeppolSBDHData aData;
    try
    {
      aData = new PeppolSBDHDataReader (PeppolIdentifierFactory.INSTANCE).extractData (new NonBlockingByteArrayInputStream (aPayloadBytes));
    }
    catch (final PeppolSBDHDataReadException ex)
    {
      // TODO This error handling might be improved to return a status error
      // instead
      aSendingReport.setSBDHParseException (ex);
      aSendingReport.setSendingSuccess (false);
      aSendingReport.setOverallSuccess (false);
      aUnifiedResponse.setContentAndCharset (aSendingReport.getAsJsonString (), StandardCharsets.UTF_8)
                      .setMimeType (CMimeType.APPLICATION_JSON)
                      .disableCaching ();
      return;
    }

    aSendingReport.setSenderID (aData.getSenderAsIdentifier ());
    aSendingReport.setReceiverID (aData.getReceiverAsIdentifier ());
    aSendingReport.setDocTypeID (aData.getDocumentTypeAsIdentifier ());
    aSendingReport.setProcessID (aData.getProcessAsIdentifier ());
    aSendingReport.setCountryC1 (aData.getCountryC1 ());
    aSendingReport.setSBDHInstanceIdentifier (aData.getInstanceIdentifier ());

    final String sSenderID = aData.getSenderAsIdentifier ().getURIEncoded ();
    final String sReceiverID = aData.getReceiverAsIdentifier ().getURIEncoded ();
    final String sDocTypeID = aData.getDocumentTypeAsIdentifier ().getURIEncoded ();
    final String sProcessID = aData.getProcessAsIdentifier ().getURIEncoded ();
    final String sCountryCodeC1 = aData.getCountryC1 ();

    LOGGER.info ("Trying to send Peppol Test SBDH message from '" +
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

    final PeppolCAChecker aAPCAChecker = m_eStage.isTest () ? PeppolCertificateChecker.peppolTestAP ()
                                                            : PeppolCertificateChecker.peppolProductionAP ();

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
        // TODO Add SMP outbound proxy settings here
        // If this block is not used, it may be removed
      });

      if (EJavaVersion.getCurrentVersion ().isNewerOrEqualsThan (EJavaVersion.JDK_17))
      {
        // Work around the disabled SHA-1 in XMLDsig issue
        aSMPClient.setSecureValidation (false);
      }

      final Phase4PeppolHttpClientSettings aHCS = new Phase4PeppolHttpClientSettings ();
      // TODO Add AP outbound proxy settings here

      final PeppolUserMessageSBDHBuilder aBuilder;
      aBuilder = Phase4PeppolSender.sbdhBuilder ()
                                   .httpClientFactory (aHCS)
                                   .payloadAndMetadata (aData)
                                   .senderPartyID (sMyPeppolSeatID)
                                   .peppolAP_CAChecker (aAPCAChecker)
                                   .smpClient (aSMPClient)
                                   .rawResponseConsumer (new AS4RawResponseConsumerWriteToFile ())
                                   .endpointURLConsumer (endpointUrl -> {
                                     // Determined by SMP lookup
                                     aSendingReport.setC3EndpointURL (endpointUrl);
                                   })
                                   .certificateConsumer ( (aAPCertificate, aCheckDT, eCertCheckResult) -> {
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
                                   .signalMsgConsumer ( (aSignalMsg, aMessageMetadata, aState) -> {
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

        // TODO Enable when ready
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
