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
package com.helger.phase4.multihop.model;

import com.helger.collection.commons.ICommonsList;
import com.helger.io.resource.ClassPathResource;
import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.phase4.CAS4;
import com.helger.phase4.multihop.CAS4MultiHop;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xsds.wsaddr.CWSAddr;

/**
 * Marshaller for {@link MultiHopRoutingInput} objects.<br>
 * By default no XML Schema validation is performed, because that costs time on every single
 * message. Use {@link #createWithValidation()} to get an instance that validates against the
 * OASIS multi-hop schema - that is what the unit tests do.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public class MultiHopRoutingInputMarshaller extends GenericJAXBMarshaller <MultiHopRoutingInput>
{
  /**
   * Constructor without XML Schema validation.
   */
  public MultiHopRoutingInputMarshaller ()
  {
    this (null);
  }

  /**
   * Constructor.
   *
   * @param aXSDs
   *        The XSDs to validate against, or <code>null</code> for no validation.
   */
  protected MultiHopRoutingInputMarshaller (final ICommonsList <ClassPathResource> aXSDs)
  {
    super (MultiHopRoutingInput.class, aXSDs, createSimpleJAXBElement (CAS4MultiHop.QNAME_ROUTING_INPUT,
                                                                       MultiHopRoutingInput.class));

    final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext ();
    aNSCtx.addMapping (CAS4MultiHop.EBINT_PREFIX, CAS4MultiHop.EBINT_NS);
    aNSCtx.addMapping ("eb", CAS4.EBMS_NS);
    aNSCtx.addMapping (CWSAddr.DEFAULT_PREFIX, CWSAddr.NAMESPACE_URI);
    aNSCtx.addMapping ("wsu", CAS4.WSU_NS);
    setNamespaceContext (aNSCtx);
  }

  /**
   * @return A new marshaller that validates against the OASIS ebMS3 Part 2 multi-hop schema. Never
   *         <code>null</code>. Validation is offline - see {@link MultiHopXSDs}.
   */
  public static MultiHopRoutingInputMarshaller createWithValidation ()
  {
    return new MultiHopRoutingInputMarshaller (MultiHopXSDs.getAllXSDs ());
  }
}
