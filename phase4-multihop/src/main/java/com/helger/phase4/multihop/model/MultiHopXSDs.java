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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.io.resource.ClassPathResource;
import com.helger.phase4.CAS4;
import com.helger.xsds.wsaddr.CWSAddr;
import com.helger.xsds.xml.CXML_XSD;

/**
 * Provides the XML Schemas needed to validate an <code>ebint:RoutingInput</code> element.<br>
 * No {@code LSResourceResolver} is needed: when the complete set of schemas is handed to the
 * validator, every {@code xs:import} is resolved by namespace and the remote
 * {@code schemaLocation} hints are never dereferenced. Validation is therefore fully offline.<br>
 * Only the two OASIS ebMS3 Part 2 schemas are shipped with this module - WS-Addressing comes from
 * {@code ph-xsds-wsaddr}, {@code xml.xsd} from {@code ph-xsds-xml} and the two SOAP envelope
 * schemas from {@code phase4-lib}.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@Immutable
public final class MultiHopXSDs
{
  private static final String PATH = "external/schemas/multihop/";

  /** The refactored ebMS3 core schema, which declares the needed global elements */
  public static final ClassPathResource XSD_EBMS_HEADER_REFACTORED = new ClassPathResource (PATH +
                                                                                            "ebms-header-3_0-200704_refactored.xsd",
                                                                                            MultiHopXSDs.class.getClassLoader ());

  /** The ebMS3 Part 2 multi-hop schema, defining ebint:RoutingInput */
  public static final ClassPathResource XSD_MULTIHOP = new ClassPathResource (PATH +
                                                                              "ebms-multihop-1_0-200902_refactored.xsd",
                                                                              MultiHopXSDs.class.getClassLoader ());

  private MultiHopXSDs ()
  {}

  /**
   * @return A new list with all XSD resources needed to validate an <code>ebint:RoutingInput</code>
   *         element, in dependency order. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <ClassPathResource> getAllXSDs ()
  {
    return new CommonsArrayList <> (CXML_XSD.getXSDResource (),
                                    CWSAddr.getXSDResource (),
                                    CAS4.XSD_SOAP11,
                                    CAS4.XSD_SOAP12,
                                    XSD_EBMS_HEADER_REFACTORED,
                                    XSD_MULTIHOP);
  }
}
