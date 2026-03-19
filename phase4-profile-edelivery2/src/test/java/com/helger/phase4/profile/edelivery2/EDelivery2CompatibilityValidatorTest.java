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
package com.helger.phase4.profile.edelivery2;

import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Test;

import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.profile.IAS4ProfileValidator;
import com.helger.photon.app.mock.PhotonAppTestRule;

/**
 * Test class for class {@link EDelivery2CompatibilityValidator}.
 *
 * @author Philip Helger
 */
public final class EDelivery2CompatibilityValidatorTest
{
  @ClassRule
  public static final PhotonAppTestRule RULE = new PhotonAppTestRule ();

  @Test
  public void testValidEdDSAPMode ()
  {
    final PMode aPMode = EDelivery2PMode.createEDelivery2PMode ("initiator",
                                                                 "responder",
                                                                 "https://test.example.com",
                                                                 IPModeIDProvider.DEFAULT_DYNAMIC,
                                                                 false,
                                                                 EDelivery2PMode.generatePModeLegSecurityEdDSA ());
    final IAS4ProfileValidator aValidator = new EDelivery2CompatibilityValidator ().setExpectFourCornerModel (false);
    final ErrorList aErrorList = new ErrorList ();
    aValidator.validatePMode (aPMode,
                              aErrorList,
                              IAS4ProfileValidator.EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue ("Errors: " + aErrorList.toString (), aErrorList.containsNoError ());
  }

  @Test
  public void testValidECDSAPMode ()
  {
    final PMode aPMode = EDelivery2PMode.createEDelivery2PMode ("initiator",
                                                                 "responder",
                                                                 "https://test.example.com",
                                                                 IPModeIDProvider.DEFAULT_DYNAMIC,
                                                                 false,
                                                                 EDelivery2PMode.generatePModeLegSecurityECDSA ());
    final IAS4ProfileValidator aValidator = new EDelivery2CompatibilityValidator ().setExpectFourCornerModel (false)
                                                                                   .setAllowECDSA (true);
    final ErrorList aErrorList = new ErrorList ();
    aValidator.validatePMode (aPMode,
                              aErrorList,
                              IAS4ProfileValidator.EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue ("Errors: " + aErrorList.toString (), aErrorList.containsNoError ());
  }

  @Test
  public void testECDSANotAllowedByDefault ()
  {
    final PMode aPMode = EDelivery2PMode.createEDelivery2PMode ("initiator",
                                                                 "responder",
                                                                 "https://test.example.com",
                                                                 IPModeIDProvider.DEFAULT_DYNAMIC,
                                                                 false,
                                                                 EDelivery2PMode.generatePModeLegSecurityECDSA ());
    // Default validator does NOT allow ECDSA
    final IAS4ProfileValidator aValidator = new EDelivery2CompatibilityValidator ().setExpectFourCornerModel (false)
                                                                                   .setAllowECDSA (false);
    final ErrorList aErrorList = new ErrorList ();
    aValidator.validatePMode (aPMode,
                              aErrorList,
                              IAS4ProfileValidator.EAS4ProfileValidationMode.USER_MESSAGE);
    // Should have error about signature algorithm
    assertTrue ("Expected errors for ECDSA when not allowed", aErrorList.containsAtLeastOneError ());
  }

  @Test
  public void testValidTwoWayPMode ()
  {
    final PMode aPMode = EDelivery2PMode.createEDelivery2PModeTwoWay ("initiator",
                                                                       "responder",
                                                                       "https://test.example.com",
                                                                       IPModeIDProvider.DEFAULT_DYNAMIC,
                                                                       false,
                                                                       EDelivery2PMode.generatePModeLegSecurityEdDSA ());
    final IAS4ProfileValidator aValidator = new EDelivery2CompatibilityValidator ().setExpectFourCornerModel (false);
    final ErrorList aErrorList = new ErrorList ();
    aValidator.validatePMode (aPMode,
                              aErrorList,
                              IAS4ProfileValidator.EAS4ProfileValidationMode.USER_MESSAGE);
    assertTrue ("Errors: " + aErrorList.toString (), aErrorList.containsNoError ());
  }
}
