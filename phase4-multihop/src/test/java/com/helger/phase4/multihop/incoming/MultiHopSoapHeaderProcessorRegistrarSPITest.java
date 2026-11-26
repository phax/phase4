/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.incoming;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.phase4.incoming.soap.SoapHeaderElementProcessorRegistry;
import com.helger.phase4.multihop.CAS4MultiHop;

/**
 * Test class for {@link MultiHopSoapHeaderProcessorRegistrarSPI}.<br>
 * Covers R10 - a routed signal carrying wsa:To, wsa:Action and ebint:RoutingInput with
 * mustUnderstand must be accepted.
 *
 * @author Philip Helger
 */
public final class MultiHopSoapHeaderProcessorRegistrarSPITest
{
  /**
   * R10 - all three processors are registered, purely by having this module on the classpath.
   */
  @Test
  public void testAllThreeProcessorsAreRegistered ()
  {
    final SoapHeaderElementProcessorRegistry aRegistry = new SoapHeaderElementProcessorRegistry ();
    new MultiHopSoapHeaderProcessorRegistrarSPI ().registerSoapHeaderElementProcessors (aRegistry);

    assertTrue (aRegistry.containsHeaderElementProcessor (CAS4MultiHop.QNAME_WSA_TO));
    assertTrue (aRegistry.containsHeaderElementProcessor (CAS4MultiHop.QNAME_WSA_ACTION));
    assertTrue (aRegistry.containsHeaderElementProcessor (CAS4MultiHop.QNAME_ROUTING_INPUT));

    assertNotNull (aRegistry.getHeaderElementProcessor (CAS4MultiHop.QNAME_ROUTING_INPUT));
  }

  /**
   * Registering twice must not throw - registerHeaderElementProcessor rejects duplicates, so the
   * implementation has to check first.
   */
  @Test
  public void testRegisteringTwiceIsSafe ()
  {
    final SoapHeaderElementProcessorRegistry aRegistry = new SoapHeaderElementProcessorRegistry ();
    final MultiHopSoapHeaderProcessorRegistrarSPI aSPI = new MultiHopSoapHeaderProcessorRegistrarSPI ();
    aSPI.registerSoapHeaderElementProcessors (aRegistry);
    aSPI.registerSoapHeaderElementProcessors (aRegistry);

    assertTrue (aRegistry.containsHeaderElementProcessor (CAS4MultiHop.QNAME_ROUTING_INPUT));
  }
}
