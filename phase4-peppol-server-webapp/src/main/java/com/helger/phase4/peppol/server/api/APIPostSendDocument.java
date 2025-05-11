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

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.string.StringHelper;
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
