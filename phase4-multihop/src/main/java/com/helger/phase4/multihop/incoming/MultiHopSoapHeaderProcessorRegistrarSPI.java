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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.phase4.incoming.soap.IAS4SoapHeaderElementProcessorRegistrarSPI;
import com.helger.phase4.incoming.soap.SoapHeaderElementProcessorRegistry;
import com.helger.phase4.multihop.CAS4MultiHop;

/**
 * Registers the three multi-hop SOAP header element processors, so that an incoming routed signal
 * message carrying <code>wsa:To</code>, <code>wsa:Action</code> and
 * <code>ebint:RoutingInput</code> with <code>mustUnderstand</code> is accepted (R10).<br>
 * Simply having this module on the classpath is enough.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@IsSPIImplementation
public final class MultiHopSoapHeaderProcessorRegistrarSPI implements IAS4SoapHeaderElementProcessorRegistrarSPI
{
  @UsedViaReflection
  public MultiHopSoapHeaderProcessorRegistrarSPI ()
  {}

  public void registerSoapHeaderElementProcessors (@NonNull final SoapHeaderElementProcessorRegistry aRegistry)
  {
    if (!aRegistry.containsHeaderElementProcessor (CAS4MultiHop.QNAME_WSA_TO))
      aRegistry.registerHeaderElementProcessor (CAS4MultiHop.QNAME_WSA_TO, new SoapHeaderElementProcessorWsaTo ());

    if (!aRegistry.containsHeaderElementProcessor (CAS4MultiHop.QNAME_WSA_ACTION))
      aRegistry.registerHeaderElementProcessor (CAS4MultiHop.QNAME_WSA_ACTION,
                                                new SoapHeaderElementProcessorWsaAction ());

    if (!aRegistry.containsHeaderElementProcessor (CAS4MultiHop.QNAME_ROUTING_INPUT))
      aRegistry.registerHeaderElementProcessor (CAS4MultiHop.QNAME_ROUTING_INPUT,
                                                new SoapHeaderElementProcessorMultiHopRoutingInput ());
  }
}
