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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.telemetry.CPhase4Telemetry;
import com.helger.phase4.telemetry.Phase4Metrics;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.TelemetryMetrics;
import com.helger.telemetry.mock.CapturingTelemetry;
import com.helger.telemetry.mock.CapturingTelemetry.CapturedMeasurement;
import com.helger.telemetry.mock.CapturingTelemetry.CapturedSpan;

/**
 * Test class for class {@link SMPLookupTelemetry}.
 *
 * @author Philip Helger
 */
public final class SMPLookupTelemetryTest
{
  private static final CapturingTelemetry CT = new CapturingTelemetry ();
  private static final IDocumentTypeIdentifier DOCTYPE_ID = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
  private static final IProcessIdentifier PROCESS_ID = PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");
  private static final String TRANSPORT_PROFILE_ID = "tp";

  @BeforeClass
  public static void beforeClass ()
  {
    // Must happen before Phase4Metrics is class-loaded, because the instruments are resolved once
    // in its static initializer
    CT.install ();
  }

  @AfterClass
  public static void afterClass ()
  {
    CapturingTelemetry.uninstall ();
  }

  @Before
  public void before ()
  {
    CT.reset ();
  }

  /**
   * The tests share one JVM. If another test loaded {@link Phase4Metrics} before, the instruments
   * are no-ops and the metric assertions are skipped.
   */
  private static void _assumeMetricsAreCaptured ()
  {
    Assume.assumeTrue ("Phase4Metrics was loaded before the capturing telemetry was installed",
                       !(Phase4Metrics.SMP_LOOKUPS instanceof TelemetryMetrics.NoOpTelemetryCounter));
  }

  @Test
  public void testSuccess () throws Exception
  {
    SMPLookupTelemetry.runLookup (CPhase4Telemetry.SMP_PROVIDER_PEPPOL,
                                  DOCTYPE_ID,
                                  PROCESS_ID,
                                  TRANSPORT_PROFILE_ID,
                                  () -> {});

    final CapturedSpan aSpan = CT.getFirstSpan (CPhase4Telemetry.SPAN_SMP_LOOKUP);
    assertNotNull (aSpan);
    assertEquals (ETelemetrySpanKind.CLIENT, aSpan.getKind ());
    assertEquals (CPhase4Telemetry.SMP_PROVIDER_PEPPOL, aSpan.getAttribute (CPhase4Telemetry.ATTR_SMP_PROVIDER));
    assertEquals (TRANSPORT_PROFILE_ID, aSpan.getAttribute (CPhase4Telemetry.ATTR_TRANSPORT_PROFILE_ID));
    assertEquals (DOCTYPE_ID.getURIEncoded (), aSpan.getAttribute (CPhase4Telemetry.ATTR_DOCTYPE_ID));
    assertEquals (PROCESS_ID.getURIEncoded (), aSpan.getAttribute (CPhase4Telemetry.ATTR_PROCESS_ID));
    assertEquals (CPhase4Telemetry.OUTCOME_SUCCESS, aSpan.getAttribute (CPhase4Telemetry.ATTR_OUTCOME));
    assertTrue (aSpan.isStatusOk ());
    assertTrue (aSpan.isClosed ());

    _assumeMetricsAreCaptured ();
    assertEquals (1, CT.getCounterValue (CPhase4Telemetry.METRIC_SMP_LOOKUPS));
    final CapturedMeasurement aLookup = CT.getFirstMeasurement (CPhase4Telemetry.METRIC_SMP_LOOKUPS);
    assertNotNull (aLookup);
    assertEquals (CPhase4Telemetry.OUTCOME_SUCCESS, aLookup.getAttribute (CPhase4Telemetry.ATTR_OUTCOME));
    assertEquals (CPhase4Telemetry.SMP_PROVIDER_PEPPOL, aLookup.getAttribute (CPhase4Telemetry.ATTR_SMP_PROVIDER));
    // High cardinality values are never used on metrics
    assertNull (aLookup.getAttribute (CPhase4Telemetry.ATTR_DOCTYPE_ID));
    assertEquals (1, CT.getHistogramValues (CPhase4Telemetry.METRIC_SMP_LOOKUP_DURATION).size ());
  }

  @Test
  public void testFailure ()
  {
    final Phase4SMPException aEx = new Phase4SMPException ("Not found");
    aEx.setRetryFeasible (false);
    try
    {
      SMPLookupTelemetry.runLookup (CPhase4Telemetry.SMP_PROVIDER_BDXR2,
                                    DOCTYPE_ID,
                                    PROCESS_ID,
                                    TRANSPORT_PROFILE_ID,
                                    () -> { throw aEx; });
      fail ();
    }
    catch (final Exception ex)
    {
      assertSame (aEx, ex);
    }

    final CapturedSpan aSpan = CT.getFirstSpan (CPhase4Telemetry.SPAN_SMP_LOOKUP);
    assertNotNull (aSpan);
    assertEquals (CPhase4Telemetry.OUTCOME_ERROR, aSpan.getAttribute (CPhase4Telemetry.ATTR_OUTCOME));
    assertEquals (Boolean.FALSE, aSpan.getAttribute (CPhase4Telemetry.ATTR_RETRY_FEASIBLE));
    assertSame (aEx, aSpan.getRecordedException ());
    assertTrue (aSpan.isStatusError ());
    assertTrue (aSpan.isClosed ());

    _assumeMetricsAreCaptured ();
    final CapturedMeasurement aLookup = CT.getFirstMeasurement (CPhase4Telemetry.METRIC_SMP_LOOKUPS);
    assertNotNull (aLookup);
    assertEquals (CPhase4Telemetry.OUTCOME_ERROR, aLookup.getAttribute (CPhase4Telemetry.ATTR_OUTCOME));
    assertEquals (Boolean.FALSE, aLookup.getAttribute (CPhase4Telemetry.ATTR_RETRY_FEASIBLE));
  }
}
