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
package com.helger.phase4.telemetry;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.CGlobal;
import com.helger.base.timing.StopWatch;
import com.helger.telemetry.ITelemetryCounter;
import com.helger.telemetry.ITelemetryHistogram;
import com.helger.telemetry.TelemetryMetrics;

/**
 * Central registry of the metric instruments of phase4. Each instrument is created once when this
 * class is loaded via the vendor-neutral {@link TelemetryMetrics} facade. If no
 * <code>ITelemetryMeterSPI</code> is registered, the instruments are cheap no-ops.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@Immutable
public final class Phase4Metrics
{
  public static final ITelemetryCounter INCOMING_REQUESTS = TelemetryMetrics.counter (CPhase4Telemetry.METRIC_INCOMING_REQUESTS,
                                                                                      "Incoming AS4 requests handled, tagged with outcome, message type and AS4 profile",
                                                                                      "{request}");
  public static final ITelemetryHistogram INCOMING_DURATION = TelemetryMetrics.histogram (CPhase4Telemetry.METRIC_INCOMING_DURATION,
                                                                                          "Duration of handling an incoming AS4 request, including the invocation of the synchronous SPIs",
                                                                                          "s");
  public static final ITelemetryHistogram INCOMING_SIZE = TelemetryMetrics.histogram (CPhase4Telemetry.METRIC_INCOMING_SIZE,
                                                                                      "Number of bytes read from an incoming AS4 request",
                                                                                      "By");
  public static final ITelemetryCounter INCOMING_DUPLICATES = TelemetryMetrics.counter (CPhase4Telemetry.METRIC_INCOMING_DUPLICATES,
                                                                                        "Incoming AS4 messages detected as duplicates, tagged with the configured duplicate handling",
                                                                                        "{message}");
  public static final ITelemetryCounter INCOMING_EBMS_ERRORS = TelemetryMetrics.counter (CPhase4Telemetry.METRIC_INCOMING_EBMS_ERRORS,
                                                                                         "ebMS errors created while handling incoming AS4 requests, tagged with the ebMS error code",
                                                                                         "{error}");

  public static final ITelemetryCounter OUTGOING_USER_MESSAGES = TelemetryMetrics.counter (CPhase4Telemetry.METRIC_OUTGOING_USER_MESSAGES,
                                                                                           "AS4 user messages sent with receipt checking, tagged with the send result and AS4 profile",
                                                                                           "{message}");
  public static final ITelemetryHistogram OUTGOING_DURATION = TelemetryMetrics.histogram (CPhase4Telemetry.METRIC_OUTGOING_DURATION,
                                                                                          "Duration of sending an AS4 user message until the response is evaluated, including SMP lookup and HTTP retries",
                                                                                          "s");

  public static final ITelemetryCounter HTTP_ATTEMPTS = TelemetryMetrics.counter (CPhase4Telemetry.METRIC_HTTP_ATTEMPTS,
                                                                                  "Outgoing HTTP transmission attempts, tagged with the outcome",
                                                                                  "{attempt}");
  public static final ITelemetryHistogram HTTP_DURATION = TelemetryMetrics.histogram (CPhase4Telemetry.METRIC_HTTP_DURATION,
                                                                                      "Duration of one outgoing HTTP transmission attempt",
                                                                                      "s");
  public static final ITelemetryHistogram HTTP_REQUEST_SIZE = TelemetryMetrics.histogram (CPhase4Telemetry.METRIC_HTTP_REQUEST_SIZE,
                                                                                          "Size of outgoing HTTP request bodies, if the length is known in advance",
                                                                                          "By");
  public static final ITelemetryCounter HTTP_RETRIES = TelemetryMetrics.counter (CPhase4Telemetry.METRIC_HTTP_RETRIES,
                                                                                 "Outgoing HTTP retries scheduled, tagged with the retry classification",
                                                                                 "{retry}");

  public static final ITelemetryCounter SMP_LOOKUPS = TelemetryMetrics.counter (CPhase4Telemetry.METRIC_SMP_LOOKUPS,
                                                                                "SMP endpoint lookups, tagged with the outcome and the SMP client flavour",
                                                                                "{lookup}");
  public static final ITelemetryHistogram SMP_LOOKUP_DURATION = TelemetryMetrics.histogram (CPhase4Telemetry.METRIC_SMP_LOOKUP_DURATION,
                                                                                            "Duration of one SMP endpoint lookup",
                                                                                            "s");

  private Phase4Metrics ()
  {}

  /**
   * Get the duration measured by the provided stop watch in seconds, as used by all duration
   * histograms.
   *
   * @param aSW
   *        The stop watch to use. May not be <code>null</code>.
   * @return The duration in seconds.
   */
  public static double getSeconds (@NonNull final StopWatch aSW)
  {
    return aSW.getNanos () / (double) CGlobal.NANOSECONDS_PER_SECOND;
  }
}
