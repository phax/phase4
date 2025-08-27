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
import java.time.YearMonth;
import java.util.Map;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringParser;
import com.helger.mime.CMimeType;
import com.helger.phase4.peppol.server.reporting.AppReportingHelper;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nonnull;

/**
 * This API creates a TSR and EUSR report for the provided year and month, validate them, store them
 * and send them to the dedicated receiver.
 *
 * @author Philip Helger
 */
public final class APIGetDoPeppolReporting extends AbstractVerifyingAPIExecutor
{
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
    AppReportingHelper.createAndSendPeppolReports (aYearMonth);

    aUnifiedResponse.setContentAndCharset ("Done - check report storage", StandardCharsets.UTF_8)
                    .setMimeType (CMimeType.TEXT_PLAIN)
                    .disableCaching ();
  }
}
