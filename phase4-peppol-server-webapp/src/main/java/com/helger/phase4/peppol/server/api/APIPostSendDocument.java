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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import com.helger.annotation.Nonempty;
import com.helger.base.array.ArrayHelper;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.mime.CMimeType;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.peppol.sml.ESML;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * API to send a document via Peppol. The SBDH is created internally.
 *
 * @author Philip Helger
 */
public final class APIPostSendDocument extends AbstractVerifyingAPIExecutor
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (APIPostSendDocument.class);

  private final EPeppolNetwork m_eStage;

  public APIPostSendDocument (@NonNull final EPeppolNetwork eStage)
  {
    m_eStage = eStage;
  }

  @Override
  protected void verifiedInvokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                                    @NonNull @Nonempty final String sPath,
                                    @NonNull final Map <String, String> aPathVariables,
                                    @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                    @NonNull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sSenderID = aPathVariables.get (Phase4API.PARAM_SENDER_ID);
    final String sReceiverID = aPathVariables.get (Phase4API.PARAM_RECEIVER_ID);
    final String sDocTypeID = aPathVariables.get (Phase4API.PARAM_DOC_TYPE_ID);
    final String sProcessID = aPathVariables.get (Phase4API.PARAM_PROCESS_ID);
    final String sCountryCodeC1 = aPathVariables.get (Phase4API.PARAM_COUNTRY_CODE_C1);
    final byte [] aPayloadBytes = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());

    // Check parameters
    if (StringHelper.isEmpty (sSenderID))
      throw new APIParamException ("API call retrieved an empty Sender ID");
    if (StringHelper.isEmpty (sReceiverID))
      throw new APIParamException ("API call retrieved an empty Receiver ID");
    if (StringHelper.isEmpty (sDocTypeID))
      throw new APIParamException ("API call retrieved an empty Document Type ID");
    if (StringHelper.isEmpty (sProcessID))
      throw new APIParamException ("API call retrieved an empty Process ID");
    if (StringHelper.isEmpty (sCountryCodeC1))
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
    final Phase4PeppolSendingReport aSendingReport = PeppolSender.sendPeppolMessageCreatingSbdh (m_eStage.isTest () ? ESML.DIGIT_TEST
                                                                                                                    : ESML.DIGIT_PRODUCTION,
                                                                                                 m_eStage.isTest () ? PeppolTrustedCA.peppolTestAP ()
                                                                                                                    : PeppolTrustedCA.peppolProductionAP (),
                                                                                                 aPayloadBytes,
                                                                                                 sSenderID,
                                                                                                 sReceiverID,
                                                                                                 sDocTypeID,
                                                                                                 sProcessID,
                                                                                                 sCountryCodeC1);

    // Return result JSON
    aUnifiedResponse.setContentAndCharset (aSendingReport.getAsJsonString (), StandardCharsets.UTF_8)
                    .setMimeType (CMimeType.APPLICATION_JSON)
                    .disableCaching ();
  }
}
