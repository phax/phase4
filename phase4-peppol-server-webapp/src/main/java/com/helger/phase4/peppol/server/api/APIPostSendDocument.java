/*
 * Copyright (C) 2014-2025 Philip Helger (www.helger.com)
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
import java.time.OffsetDateTime;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.EJavaVersion;
import com.helger.commons.timing.StopWatch;
import com.helger.commons.wrapper.Wrapper;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.utils.PeppolCAChecker;
import com.helger.peppol.utils.PeppolCertificateChecker;
import com.helger.peppol.utils.PeppolCertificateHelper;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.dump.AS4RawResponseConsumerWriteToFile;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.marshaller.Ebms3SignalMessageMarshaller;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageBuilder;
import com.helger.phase4.peppol.server.APConfig;
import com.helger.phase4.peppol.server.EStageType;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.util.Phase4Exception;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.read.DOMReader;

public final class APIPostSendDocument extends AbstractAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIPostSendDocument.class);

  private final EStageType m_eStage;

  public APIPostSendDocument (@Nonnull final EStageType eStage)
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
    final String sSenderID = aPathVariables.get (PPAPI.PARAM_SENDER_ID);
    final String sReceiverID = aPathVariables.get (PPAPI.PARAM_RECEIVER_ID);
    final String sDocTypeID = aPathVariables.get (PPAPI.PARAM_DOC_TYPE_ID);
    final String sProcessID = aPathVariables.get (PPAPI.PARAM_PROCESS_ID);
    final String sCountryCodeC1 = aPathVariables.get (PPAPI.PARAM_COUNTRY_CODE_C1);
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
    final PeppolCAChecker aAPCAChecker = m_eStage.isTest () ? PeppolCertificateChecker.peppolTestAP ()
                                                            : PeppolCertificateChecker.peppolProductionAP ();
    final String sMyPeppolSeatID = APConfig.getMyPeppolSeatID ();

    final OffsetDateTime aNowUTC = PDTFactory.getCurrentOffsetDateTimeUTC ();
    final IJsonObject aJson = new JsonObject ();
    aJson.add ("currentDateTimeUTC", PDTWebDateHelper.getAsStringXSD (aNowUTC));
    aJson.add ("senderId", sSenderID);
    aJson.add ("receiverId", sReceiverID);
    aJson.add ("docTypeId", sDocTypeID);
    aJson.add ("processId", sProcessID);
    aJson.add ("countryC1", sCountryCodeC1);
    aJson.add ("senderPartyId", sMyPeppolSeatID);

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
      final IParticipantIdentifier aReceiverID = aIF.createParticipantIdentifierWithDefaultScheme (sReceiverID);

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

      final PeppolUserMessageBuilder aBuilder;
      aBuilder = Phase4PeppolSender.builder ()
                                   .httpClientFactory (aHCS)
                                   .documentTypeID (aIF.createDocumentTypeIdentifierWithDefaultScheme (sDocTypeID))
                                   .processID (aIF.createProcessIdentifierWithDefaultScheme (sProcessID))
                                   .senderParticipantID (aIF.createParticipantIdentifierWithDefaultScheme (sSenderID))
                                   .receiverParticipantID (aReceiverID)
                                   .senderPartyID (sMyPeppolSeatID)
                                   .countryC1 (sCountryCodeC1)
                                   .payload (aDoc.getDocumentElement ())
                                   .peppolAP_CAChecker (aAPCAChecker)
                                   .smpClient (aSMPClient)
                                   .rawResponseConsumer (new AS4RawResponseConsumerWriteToFile ())
                                   .endpointURLConsumer (endpointUrl -> {
                                     // Determined by SMP lookup
                                     aJson.add ("c3EndpointUrl", endpointUrl);
                                   })
                                   .certificateConsumer ( (aAPCertificate, aCheckDT, eCertCheckResult) -> {
                                     // Determined by SMP lookup
                                     aJson.add ("c3Cert", CertificateHelper.getPEMEncodedCertificate (aAPCertificate));
                                     aJson.add ("c3CertSubjectCN",
                                                PeppolCertificateHelper.getSubjectCN (aAPCertificate));
                                     aJson.add ("c3CertCheckDT", PDTWebDateHelper.getAsStringXSD (aCheckDT));
                                     aJson.add ("c3CertCheckResult", eCertCheckResult);
                                   })
                                   .buildMessageCallback (new IAS4ClientBuildMessageCallback ()
                                   {
                                     public void onAS4Message (@Nonnull final AbstractAS4Message <?> aMsg)
                                     {
                                       // Created AS4 fields
                                       final AS4UserMessage aUserMsg = (AS4UserMessage) aMsg;
                                       aJson.add ("as4MessageId",
                                                  aUserMsg.getEbms3UserMessage ().getMessageInfo ().getMessageId ());
                                       aJson.add ("as4ConversationId",
                                                  aUserMsg.getEbms3UserMessage ()
                                                          .getCollaborationInfo ()
                                                          .getConversationId ());
                                     }
                                   })
                                   .signalMsgConsumer ( (aSignalMsg, aMessageMetadata, aState) -> {
                                     aJson.add ("as4ReceivedSignalMsg",
                                                new Ebms3SignalMessageMarshaller ().getAsString (aSignalMsg));

                                     if (aSignalMsg.hasErrorEntries ())
                                     {
                                       final IJsonArray aErrors = new JsonArray ();
                                       for (final Ebms3Error err : aSignalMsg.getError ())
                                       {
                                         final IJsonObject aErrorDetails = new JsonObject ();
                                         if (err.getDescription () != null)
                                           aErrorDetails.add ("description", err.getDescriptionValue ());
                                         if (err.getErrorDetail () != null)
                                           aErrorDetails.add ("errorDetails", err.getErrorDetail ());
                                         if (err.getCategory () != null)
                                           aErrorDetails.add ("category", err.getCategory ());
                                         if (err.getRefToMessageInError () != null)
                                           aErrorDetails.add ("refToMessageInError", err.getRefToMessageInError ());
                                         if (err.getErrorCode () != null)
                                           aErrorDetails.add ("errorCode", err.getErrorCode ());
                                         if (err.getOrigin () != null)
                                           aErrorDetails.add ("origin", err.getOrigin ());
                                         if (err.getSeverity () != null)
                                           aErrorDetails.add ("severity", err.getSeverity ());
                                         if (err.getShortDescription () != null)
                                           aErrorDetails.add ("shortDescription", err.getShortDescription ());
                                         aErrors.add (aErrorDetails);
                                         LOGGER.warn ("AS4 error received: " + aErrorDetails.getAsJsonString ());
                                       }
                                       aJson.add ("as4ResponseErrors", aErrors);
                                       aJson.add ("as4ResponseError", true);
                                     }
                                     else
                                       aJson.add ("as4ResponseError", false);
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

        // TODO Enable when ready
        if (false)
          aBuilder.createAndStorePeppolReportingItemAfterSending (sEndUserID);
      }

      aJson.add ("sendingResult", eResult);

      if (aCaughtEx.isSet ())
      {
        final Phase4Exception ex = aCaughtEx.get ();
        LOGGER.error ("Error sending Peppol message via AS4", ex);
        aJson.add ("sendingException",
                   new JsonObject ().add ("class", ex.getClass ().getName ())
                                    .add ("message", ex.getMessage ())
                                    .add ("stackTrace", StackTraceHelper.getStackAsString (ex)));
        bExceptionCaught = true;
      }
    }
    catch (final Exception ex)
    {
      // Mostly errors on HTTP level
      LOGGER.error ("Error sending Peppol message via AS4", ex);
      aJson.add ("sendingException",
                 new JsonObject ().add ("class", ex.getClass ().getName ())
                                  .add ("message", ex.getMessage ())
                                  .add ("stackTrace", StackTraceHelper.getStackAsString (ex)));
      bExceptionCaught = true;
    }
    finally
    {
      aSW.stop ();
      aJson.add ("overallDurationMillis", aSW.getMillis ());
    }

    // Result may be null
    final boolean bSendingSuccess = eResult != null && eResult.isSuccess ();
    aJson.add ("sendingSuccess", bSendingSuccess);
    aJson.add ("overallSuccess", bSendingSuccess && !bExceptionCaught);

    // Return result JSON
    aUnifiedResponse.setContentAndCharset (aJson.getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED),
                                           StandardCharsets.UTF_8).disableCaching ();
  }
}
