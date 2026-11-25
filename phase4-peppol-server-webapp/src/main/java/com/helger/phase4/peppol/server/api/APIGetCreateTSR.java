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

import java.time.YearMonth;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringParser;
import com.helger.mime.CMimeType;
import com.helger.peppol.reporting.api.backend.IPeppolReportingItemReader;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.reporting.jaxb.tsr.TransactionStatisticsReport101Marshaller;
import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;
import com.helger.peppol.reporting.tsr.TransactionStatisticsReport;
import com.helger.peppol.reporting.tsr.model.TSRReportingItemAccumulator;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.peppol.server.APConfig;
import com.helger.phase4.peppol.server.reporting.AppReportingBackend;
import com.helger.phase4.peppol.server.reporting.AppReportingHelper;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * API to create a Peppol Reporting TSR.
 *
 * @author Philip Helger
 */
public final class APIGetCreateTSR extends AbstractVerifyingAPIExecutor
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (APIGetCreateTSR.class);

  @Override
  protected void verifiedInvokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                                    @NonNull @Nonempty final String sPath,
                                    @NonNull final Map <String, String> aPathVariables,
                                    @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                    @NonNull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sYear = aPathVariables.get (Phase4API.PARAM_YEAR);
    final String sMonth = aPathVariables.get (Phase4API.PARAM_MONTH);

    // Check parameters
    final YearMonth aYearMonth = AppReportingHelper.getValidYearMonthInAPI (StringParser.parseInt (sYear, -1),
                                                                            StringParser.parseInt (sMonth, -1));

    LOGGER.info ("Trying to create Peppol Reporting TSR for " + aYearMonth);

    try
    {
      // Aggregate all items from data storage while reading them, without keeping
      // them in memory
      final IPeppolReportingItemReader aReader = AppReportingBackend.getItemReader ();
      if (aReader != null)
      {
        final TSRReportingItemAccumulator aAccumulator = new TSRReportingItemAccumulator ();
        aReader.forEachReportingItem (aYearMonth, aAccumulator::accept);

        // Create report with the aggregated transactions
        final TransactionStatisticsReportType aReport = TransactionStatisticsReport.builder ()
                                                                                   .monthOf (aYearMonth)
                                                                                   .reportingServiceProviderID (APConfig.getMyPeppolSeatID ())
                                                                                   .reportingItemAccumulator (aAccumulator)
                                                                                   .build ();
        final byte [] aXML = new TransactionStatisticsReport101Marshaller ().getAsBytes (aReport);
        aUnifiedResponse.setContent (aXML)
                        .setCharset (XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ)
                        .setMimeType (CMimeType.APPLICATION_XML)
                        .disableCaching ();
      }
      else
        throw new IllegalStateException ("No Peppol Reporting backend that can read reporting items is available");
    }
    catch (final PeppolReportingBackendException ex)
    {
      LOGGER.error ("Failed to read Peppol Reporting Items", ex);
      throw new IllegalStateException ("Failed to read Peppol Reporting backend data: " + ex.getMessage ());
    }
  }
}
