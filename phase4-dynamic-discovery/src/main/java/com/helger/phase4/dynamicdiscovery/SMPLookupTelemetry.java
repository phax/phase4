/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.dynamicdiscovery;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.iface.IThrowingRunnable;
import com.helger.base.timing.StopWatch;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phase4.telemetry.CPhase4Telemetry;
import com.helger.phase4.telemetry.Phase4Metrics;
import com.helger.phase4.util.Phase4Exception;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.ITelemetrySpan;
import com.helger.telemetry.Telemetry;
import com.helger.telemetry.TelemetryAttributes;

/**
 * Records the span and the metrics of one SMP endpoint lookup. The participant identifier is never
 * recorded.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@Immutable
final class SMPLookupTelemetry
{
  private SMPLookupTelemetry ()
  {}

  /**
   * Run the provided SMP lookup inside a span and record the metrics.
   *
   * @param sSMPProvider
   *        The SMP client flavour. One of the <code>CPhase4Telemetry.SMP_PROVIDER_*</code>
   *        constants. May not be <code>null</code>.
   * @param aDocTypeID
   *        The document type identifier to look up. Only used on the span. May not be
   *        <code>null</code>.
   * @param aProcID
   *        The process identifier to look up. Only used on the span. May not be <code>null</code>.
   * @param sTransportProfileID
   *        The ID of the transport profile to look up. May not be <code>null</code>.
   * @param aLookup
   *        The lookup to perform. May not be <code>null</code>.
   * @throws Phase4Exception
   *         if the lookup failed
   */
  static void runLookup (@NonNull final String sSMPProvider,
                         @NonNull final IDocumentTypeIdentifier aDocTypeID,
                         @NonNull final IProcessIdentifier aProcID,
                         @NonNull final String sTransportProfileID,
                         @NonNull final IThrowingRunnable <Phase4Exception> aLookup) throws Phase4Exception
  {
    final StopWatch aSW = StopWatch.createdStarted ();
    try (final ITelemetrySpan aSpan = Telemetry.startSpan (CPhase4Telemetry.SPAN_SMP_LOOKUP, ETelemetrySpanKind.CLIENT))
    {
      aSpan.setAttribute (CPhase4Telemetry.ATTR_SMP_PROVIDER, sSMPProvider);
      aSpan.setAttribute (CPhase4Telemetry.ATTR_TRANSPORT_PROFILE_ID, sTransportProfileID);
      aSpan.setAttribute (CPhase4Telemetry.ATTR_DOCTYPE_ID, aDocTypeID.getURIEncoded ());
      aSpan.setAttribute (CPhase4Telemetry.ATTR_PROCESS_ID, aProcID.getURIEncoded ());

      final TelemetryAttributes.Builder aAttrs = TelemetryAttributes.builder ()
                                                                    .put (CPhase4Telemetry.ATTR_SMP_PROVIDER, sSMPProvider)
                                                                    .put (CPhase4Telemetry.ATTR_TRANSPORT_PROFILE_ID,
                                                                          sTransportProfileID);
      try
      {
        aLookup.run ();

        aSpan.setAttribute (CPhase4Telemetry.ATTR_OUTCOME, CPhase4Telemetry.OUTCOME_SUCCESS);
        aSpan.setStatusOk ();
        aAttrs.put (CPhase4Telemetry.ATTR_OUTCOME, CPhase4Telemetry.OUTCOME_SUCCESS);
      }
      catch (final Phase4Exception ex)
      {
        aSpan.setAttribute (CPhase4Telemetry.ATTR_OUTCOME, CPhase4Telemetry.OUTCOME_ERROR);
        aSpan.setAttribute (CPhase4Telemetry.ATTR_RETRY_FEASIBLE, ex.isRetryFeasible ());
        aSpan.recordException (ex);
        aSpan.setStatusError (ex.getMessage ());
        aAttrs.put (CPhase4Telemetry.ATTR_OUTCOME, CPhase4Telemetry.OUTCOME_ERROR)
              .put (CPhase4Telemetry.ATTR_RETRY_FEASIBLE, ex.isRetryFeasible ());
        throw ex;
      }
      catch (final RuntimeException ex)
      {
        aSpan.setAttribute (CPhase4Telemetry.ATTR_OUTCOME, CPhase4Telemetry.OUTCOME_EXCEPTION);
        aSpan.recordException (ex);
        aSpan.setStatusError (ex.getMessage ());
        aAttrs.put (CPhase4Telemetry.ATTR_OUTCOME, CPhase4Telemetry.OUTCOME_EXCEPTION);
        throw ex;
      }
      finally
      {
        aSW.stop ();
        final TelemetryAttributes aBuiltAttrs = aAttrs.build ();
        Phase4Metrics.SMP_LOOKUPS.add (1, aBuiltAttrs);
        Phase4Metrics.SMP_LOOKUP_DURATION.record (Phase4Metrics.getSeconds (aSW), aBuiltAttrs);
      }
    }
  }
}
