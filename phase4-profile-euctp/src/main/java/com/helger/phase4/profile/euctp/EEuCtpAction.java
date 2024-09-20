/*
 * Copyright (C) 2019-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.euctp;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;

/**
 * This enum contains all the EU CTP AS4 actions.
 *
 * @author Jon Rios
 * @author Philip Helger
 */
public enum EEuCtpAction
{
  // Sea and inland waterways
  IE3F10,
  IE3F11,
  IE3F12,
  IE3F13,
  IE3F14,
  IE3F15,
  IE3F16,
  IE3F17,
  IE3F18,
  IE3F19,

  // Air cargo (general)
  IE3F20,
  IE3F21,
  IE3F22,
  IE3F23,
  IE3F24,
  IE3F25,
  IE3F26,
  IE3F27,
  IE3F28,
  IE3F29,

  // Express consignments
  IE3F30,
  IE3F31,
  IE3F32,
  IE3F33,
  IE3F34,
  IE3F35,
  IE3F36,
  IE3F37,
  IE3F38,
  IE3F39,

  // Postal consignments
  IE3F40,
  IE3F41,
  IE3F42,
  IE3F43,
  IE3F44,
  IE3F45,
  IE3F46,
  IE3F47,
  IE3F48,
  IE3F49,

  // Road
  IE3F50,

  // Rail
  IE3F51,

  // Amend ENS
  IE3A10,
  IE3A11,
  IE3A12,
  IE3A13,
  IE3A14,
  IE3A15,
  IE3A16,
  IE3A17,
  IE3A18,
  IE3A19,
  IE3A20,
  IE3A21,
  IE3A22,
  IE3A23,
  IE3A24,
  IE3A25,
  IE3A26,
  IE3A27,
  IE3A28,
  IE3A29,
  IE3A30,
  IE3A31,
  IE3A32,
  IE3A33,
  IE3A34,
  IE3A35,
  IE3A36,
  IE3A37,
  IE3A38,
  IE3A39,
  IE3A40,
  IE3A41,
  IE3A42,
  IE3A43,
  IE3A44,
  IE3A45,
  IE3A46,
  IE3A47,
  IE3A48,
  IE3A49,
  IE3A50,
  IE3A51,

  // Do Not Load Request
  IE3Q01,

  // Additional Information request
  IE3Q02,

  // High Risk Cargo & Mail screening request
  IE3Q03,

  // Invalidation Request
  IE3Q04,

  // Consult ENS
  IE3Q05,

  // ENS lifecycle validation error notification
  IE3N01,

  // ENS Not complete notification
  IE3N02,

  // Assessment Complete notification
  IE3N03,

  // Additional Information Request notification
  IE3N04,

  // High Risk Cargo & Mail screening request notification
  IE3N05,

  // ENS In Incorrect State notification
  IE3N07,

  // Control notification
  IE3N08,

  // AEO Control notification
  IE3N09,

  // Arrival Notification
  IE3N06,

  // Amendment notification
  IE3N10,

  // ENS Pending Notification
  IE3N11,

  // Notify Error
  IE3N99,

  // ENS Registration Response
  IE3R01,

  // Additional Information Response
  IE3R02,

  // High Risk Cargo & Mail screening response
  IE3R03,

  // Arrival Registration Response
  IE3R04,
  IE3R05,
  IE3R06,

  // Invalidation Acceptance Response
  IE3R07,

  // ENS Consultation results
  IE3R08;

  EEuCtpAction ()
  {}

  @Nonnull
  @Nonempty
  public String getValue ()
  {
    return name ();
  }
}
