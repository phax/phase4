/*
 * Copyright (C) 2019-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.nemhandel;

import com.helger.annotation.concurrent.Immutable;

/**
 * Constants for Nemhandel eDelivery.
 *
 * @author Philip Helger
 */
@Immutable
public final class CNemhandel
{
  /**
   * The SMP transport profile ID to be used for Nemhandel eDelivery (Peppol uses
   * <code>peppol-transport-as4-v2_0</code>). Source: Nemhandel eDelivery Network Specification
   * 1.2.0, section "SMP".
   */
  public static final String TRANSPORT_PROFILE_ID_NEMHANDEL_AS4_V2 = "nemhandel-transport-as4-v2_0";

  /**
   * The URL of the Nemhandel SML (SML DK). Source: Nemhandel eDelivery Network Specification 1.2.0,
   * section "SML".
   */
  public static final String SML_URL = "https://sml.erst.dk/nemhandel-sml/";

  /**
   * The SBDH business scope type that contains the XAdES document signature. Source: Nemhandel
   * eDelivery Network Specification 1.2.0, section "Document signatures".
   */
  public static final String SBDH_SCOPE_TYPE_SIGNATURE = "NEMHANDEL_EDELIVERY_SIGNATURE";

  /**
   * The SBDH business scope identifier that accompanies the XAdES document signature. Source:
   * Nemhandel eDelivery Network Specification 1.2.0, section "Document signatures".
   */
  public static final String SBDH_SCOPE_IDENTIFIER_SIGNATURE = "nemhandel-edelivery-1.2";

  private CNemhandel ()
  {}
}
