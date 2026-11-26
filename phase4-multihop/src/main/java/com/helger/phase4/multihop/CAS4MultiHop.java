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
package com.helger.phase4.multihop;

import javax.xml.namespace.QName;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.phase4.CAS4;
import com.helger.xsds.wsaddr.CWSAddr;

/**
 * Constants for the AS4 Multi-Hop endpoint support.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@Immutable
public final class CAS4MultiHop
{
  /**
   * The SOAP role/actor that identifies the next intermediary MSH. See ebMS3 Part 2 section 2.4.5.
   */
  public static final String NEXT_MSH_ROLE = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/part2/200811/nextmsh";

  /**
   * The <code>wsa:To</code> value used for messages that are routed through an I-Cloud. See AS4
   * Profile section 4.2.
   */
  public static final String ICLOUD_URI = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/part2/200811/icloud";

  /**
   * The namespace of the multi-hop extension. Note the trailing slash - it is part of the
   * namespace.
   */
  public static final String EBINT_NS = "http://docs.oasis-open.org/ebxml-msg/ns/ebms/v3.0/multihop/200902/";

  /** The default namespace prefix for {@link #EBINT_NS} */
  public static final String EBINT_PREFIX = "ebint";

  /** The WS-Addressing namespace URI */
  public static final String WSA_NS = CWSAddr.NAMESPACE_URI;

  /** The <code>wsa:Action</code> value of a routed Receipt */
  public static final String WSA_ACTION_ONEWAY_RECEIPT = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay.receipt";

  /** The <code>wsa:Action</code> value of a routed Error */
  public static final String WSA_ACTION_ONEWAY_ERROR = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay.error";

  /** Normative MPC suffix for Receipts. AS4 Profile section 4.4. */
  public static final String MPC_SUFFIX_RECEIPT = ".receipt";

  /** Normative MPC suffix for Errors. AS4 Profile section 4.4. */
  public static final String MPC_SUFFIX_ERROR = ".error";

  /** The default suffix appended to the Action of an inferred reverse RoutingInput */
  public static final String DEFAULT_ACTION_SUFFIX = ".response";

  /** PMode ID suffix of the initiating PMode unit. ebMS3 Part 2 section 2.7.2. */
  public static final String PMODE_SUFFIX_INIT = ".init";

  /** PMode ID suffix of the responding PMode unit. ebMS3 Part 2 section 2.7.2. */
  public static final String PMODE_SUFFIX_RESP = ".resp";

  /** The QName of the <code>ebint:RoutingInput</code> SOAP header element */
  public static final QName QNAME_ROUTING_INPUT = new QName (EBINT_NS, "RoutingInput", EBINT_PREFIX);

  /** The QName of the <code>wsa:To</code> SOAP header element */
  public static final QName QNAME_WSA_TO = new QName (WSA_NS, "To", CWSAddr.DEFAULT_PREFIX);

  /** The QName of the <code>wsa:Action</code> SOAP header element */
  public static final QName QNAME_WSA_ACTION = new QName (WSA_NS, "Action", CWSAddr.DEFAULT_PREFIX);

  /** The QName of the <code>wsa:IsReferenceParameter</code> attribute */
  public static final QName QNAME_WSA_IS_REFERENCE_PARAMETER = new QName (WSA_NS, "IsReferenceParameter");

  /** The QName of the <code>wsu:Id</code> attribute */
  public static final QName QNAME_WSU_ID = new QName (CAS4.WSU_NS, "Id");

  /** The prefix used for all wsu:Id values created by this module */
  public static final String ID_PREFIX = "phase4-mh-";

  /** Incoming state attribute holding the parsed {@code ebint:RoutingInput} */
  public static final String STATE_ATTR_ROUTING_INPUT = "multihop.routinginput";

  /** Incoming state attribute holding the {@code wsa:To} value */
  public static final String STATE_ATTR_WSA_TO = "multihop.wsa.to";

  /** Incoming state attribute holding the {@code wsa:Action} value */
  public static final String STATE_ATTR_WSA_ACTION = "multihop.wsa.action";

  private CAS4MultiHop ()
  {}

  /**
   * Strip the <code>.init</code> or <code>.resp</code> suffix from a PMode ID. ebMS3 Part 2 section
   * 2.7.2 requires that <code>eb:AgreementRef/@pmode</code> carries the PMode ID without the
   * suffix of the PMode unit.
   *
   * @param sPModeID
   *        The PMode ID to handle. May be <code>null</code>.
   * @return <code>null</code> if the source value was <code>null</code>, the unchanged value if it
   *         has no such suffix, and the stripped value otherwise.
   */
  @Nullable
  public static String getAgreementPModeID (@Nullable final String sPModeID)
  {
    if (StringHelper.isEmpty (sPModeID))
      return sPModeID;

    if (sPModeID.endsWith (PMODE_SUFFIX_INIT))
      return sPModeID.substring (0, sPModeID.length () - PMODE_SUFFIX_INIT.length ());
    if (sPModeID.endsWith (PMODE_SUFFIX_RESP))
      return sPModeID.substring (0, sPModeID.length () - PMODE_SUFFIX_RESP.length ());
    return sPModeID;
  }

  /**
   * @param sID
   *        The ID suffix. May not be <code>null</code>.
   * @return A new wsu:Id value with the module specific prefix. Never <code>null</code>.
   */
  @NonNull
  public static String createID (@NonNull final String sID)
  {
    return ID_PREFIX + sID;
  }
}
