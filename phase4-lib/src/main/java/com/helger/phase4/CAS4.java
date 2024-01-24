/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xsds.xmldsig.CXMLDSig;

/**
 * AS4 constants
 *
 * @author Philip Helger
 */
@Immutable
public final class CAS4
{
  @Nonnull
  private static ClassLoader _getCL ()
  {
    return CAS4.class.getClassLoader ();
  }

  // XSD
  public static final String PATH_SCHEMATA = "/external/schemas/";
  public static final ClassPathResource XSD_EBMS_HEADER = new ClassPathResource (PATH_SCHEMATA +
                                                                                 "ebms-header-3_0-200704.xsd",
                                                                                 _getCL ());
  public static final ClassPathResource XSD_EBBP_SIGNALS = new ClassPathResource (PATH_SCHEMATA +
                                                                                  "ebbp-signals-2.0.4.xsd",
                                                                                  _getCL ());
  public static final ClassPathResource XSD_SOAP11 = new ClassPathResource (PATH_SCHEMATA + "soap11.xsd", _getCL ());
  public static final ClassPathResource XSD_SOAP12 = new ClassPathResource (PATH_SCHEMATA + "soap12.xsd", _getCL ());

  // Namespaces
  public static final String DS_NS = CXMLDSig.NAMESPACE_URI;
  public static final String DSISG11_NS = "http://www.w3.org/2009/xmldsig11#";
  public static final String EBBP_NS = "http://docs.oasis-open.org/ebxml-bp/ebbp-signals-2.0";
  public static final String EBMS_NS = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";
  public static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
  public static final String WSU_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
  public static final String XENC_NS = "http://www.w3.org/2001/04/xmlenc#";
  public static final String XENC11_NS = "http://www.w3.org/2009/xmlenc11#";

  /**
   * Name of the library. Must start with a letter and must be a valid XML ID
   * and token
   */
  public static final String LIB_NAME = "phase4";
  /** The URL of the library. */
  public static final String LIB_URL = "https://github.com/phax/phase4";

  // Constant Names
  /**
   * The name of the "original sender" (C1) message property for four-corner
   * topology message exchanges.
   */
  public static final String ORIGINAL_SENDER = "originalSender";
  /**
   * The name of the "final recipient" (C4) message property for four-corner
   * topology message exchanges.
   */
  public static final String FINAL_RECIPIENT = "finalRecipient";

  // Default values
  public static final String DEFAULT_ROLE = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultRole";
  public static final String DEFAULT_RESPONDER_URL = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder";
  public static final String DEFAULT_TO_URL = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultTo";
  public static final String DEFAULT_INITIATOR_URL = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator";
  public static final String DEFAULT_FROM_URL = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultFrom";
  public static final String DEFAULT_ACTION_URL = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/test";
  public static final String DEFAULT_SERVICE_URL = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/service";
  public static final String DEFAULT_MPC_ID = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC";

  private CAS4 ()
  {}
}
