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

import java.time.YearMonth;
import java.util.Map;

import org.slf4j.Logger;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringParser;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.mime.CMimeType;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.reporting.eusr.EndUserStatisticsReport;
import com.helger.peppol.reporting.jaxb.eusr.EndUserStatisticsReport110Marshaller;
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.peppol.server.APConfig;
import com.helger.phase4.peppol.server.reporting.AppReportingHelper;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.XMLWriterSettings;

import jakarta.annotation.Nonnull;

/**
 * API to create a Peppol Reporting EUSR.
 *
 * @author Philip Helger
 */
public final class APIGetCreateEUSR extends AbstractVerifyingAPIExecutor
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (APIGetCreateEUSR.class);

  @Override
  protected void verifiedInvokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                                    @Nonnull @Nonempty final String sPath,
                                    @Nonnull final Map <String, String> aPathVariables,
                                    @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                    @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sYear = aPathVariables.get (Phase4API.PARAM_YEAR);
    final String sMonth = aPathVariables.get (Phase4API.PARAM_MONTH);

    // Check parameters
    final YearMonth aYearMonth = AppReportingHelper.getValidYearMonthInAPI (StringParser.parseInt (sYear, -1),
                                                                            StringParser.parseInt (sMonth, -1));

    LOGGER.info ("Trying to create Peppol Reporting EUSR for " + aYearMonth);

    try
    {
      // Now get all items from data storage and store them in a list (we start
      // with an initial size of 1K to avoid too many copy operations)
      final ICommonsList <PeppolReportingItem> aReportingItems = new CommonsArrayList <> (1024);
      if (PeppolReportingBackend.withBackendDo (APConfig.getConfig (),
                                                aBackend -> aBackend.forEachReportingItem (aYearMonth,
                                                                                           aReportingItems::add))
                                .isSuccess ())
      {
        // Create report with the read transactions
        final EndUserStatisticsReportType aReport = EndUserStatisticsReport.builder ()
                                                                           .monthOf (aYearMonth)
                                                                           .reportingServiceProviderID (APConfig.getMyPeppolSeatID ())
                                                                           .reportingItemList (aReportingItems)
                                                                           .build ();
        final byte [] aXML = new EndUserStatisticsReport110Marshaller ().getAsBytes (aReport);
        aUnifiedResponse.setContent (aXML)
                        .setCharset (XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ)
                        .setMimeType (CMimeType.APPLICATION_XML)
                        .disableCaching ();
      }
      else
        throw new IllegalStateException ("Failed to read Peppol Reporting backend data");
    }
    catch (final PeppolReportingBackendException ex)
    {
      LOGGER.error ("Failed to read Peppol Reporting Items", ex);
      throw new IllegalStateException ("Failed to read Peppol Reporting backend data: " + ex.getMessage ());
    }
  }
}
