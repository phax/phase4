/*
 * Copyright (C) 2020-2026 Philip Helger (www.helger.com)
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
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.mime.CMimeType;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.PeppolSBDHDataReadException;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * API to send a document via Peppol. Requires a ready Peppol SBDH as input.
 *
 * @author Philip Helger
 */
public final class APIPostSendSBDH extends AbstractVerifyingAPIExecutor
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (APIPostSendSBDH.class);

  private final EPeppolNetwork m_eStage;

  public APIPostSendSBDH (@NonNull final EPeppolNetwork eStage)
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
    final byte [] aPayloadBytes = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());

    // Check parameters
    if (ArrayHelper.isEmpty (aPayloadBytes))
      throw new APIParamException ("API call retrieved an empty payload");

    final ISMLInfo eSML = m_eStage.isTest () ? ESML.DIGIT_TEST : ESML.DIGIT_PRODUCTION;
    final TrustedCAChecker aAPCA = m_eStage.isProduction () ? PeppolTrustedCA.peppolProductionAP () : PeppolTrustedCA
                                                                                                                     .peppolTestAP ();
    final Phase4PeppolSendingReport aSendingReport = new Phase4PeppolSendingReport (eSML);

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

    PeppolSender.sendPeppolMessagePredefinedSbdh (aData, eSML, aAPCA, aSendingReport);

    // Return result JSON
    aUnifiedResponse.setContentAndCharset (aSendingReport.getAsJsonString (), StandardCharsets.UTF_8)
                    .setMimeType (CMimeType.APPLICATION_JSON)
                    .disableCaching ();
  }
}
