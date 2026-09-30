/*
 * Copyright (C) 2015-2021 Pavel Rotek
 * pavel[dot]rotek[at]gmail[dot]com
 *
 * Copyright (C) 2021-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.entsog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.ClassRule;
import org.junit.Test;

import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.PMode;
import com.helger.photon.app.mock.PhotonAppWebTestRule;

/**
 * Test class for class {@link ENTSOGPMode}.
 *
 * @author Pavel Rotek
 */
public final class ENTSOGPModeTest
{
  @ClassRule
  public static final PhotonAppWebTestRule RULE = new PhotonAppWebTestRule ();

  @Test
  public void testENTSOGPMode ()
  {
    final PMode aPMode = ENTSOGPMode.createENTSOGPMode ("TestInitiator",
                                                        "TestResponder",
                                                        "https://test.example.org",
                                                        IPModeIDProvider.DEFAULT_DYNAMIC,
                                                        false);
    assertNotNull (aPMode);

    // ENTSOG AS4 Usage Profile: "The type attribute on the PartyId element MUST be present and
    // set to the fixed value http://www.entsoe.eu/eic-codes/eic-party-codes-x"
    assertEquals ("http://www.entsoe.eu/eic-codes/eic-party-codes-x", aPMode.getInitiator ().getIDType ());
    assertEquals ("http://www.entsoe.eu/eic-codes/eic-party-codes-x", aPMode.getResponder ().getIDType ());

    // ENTSOG AS4 Usage Profile, section 2.3.1.2.2: default action for business messages
    assertEquals ("http://docs.oasis-open.org/ebxml-msg/as4/200902/action",
                  aPMode.getLeg1 ().getBusinessInfo ().getAction ());
  }

  @Test
  public void testENTSOG4PMode ()
  {
    final PMode aPMode = ENTSOG4PMode.createENTSOG4PMode ("TestInitiator",
                                                          "TestResponder",
                                                          "https://test.example.org",
                                                          IPModeIDProvider.DEFAULT_DYNAMIC,
                                                          false,
                                                          ENTSOG4PMode.generatePModeLegSecurityEdDSA ());
    assertNotNull (aPMode);
    assertEquals ("http://www.entsoe.eu/eic-codes/eic-party-codes-x", aPMode.getInitiator ().getIDType ());
    assertEquals ("http://www.entsoe.eu/eic-codes/eic-party-codes-x", aPMode.getResponder ().getIDType ());
    assertEquals ("http://docs.oasis-open.org/ebxml-msg/as4/200902/action",
                  aPMode.getLeg1 ().getBusinessInfo ().getAction ());
  }

  @Test
  public void testCreateAgreementID ()
  {
    // Example from the ENTSOG AS4 Usage Profile 4.0, section 3.1
    assertEquals ("http://entsog.eu/communication/agreements/21X-EU-A-X0A0Y-Z/21X-EU-B-P0Q0R-S/3",
                  ENTSOGPMode.createAgreementID ("21X-EU-A-X0A0Y-Z", "21X-EU-B-P0Q0R-S", 3));
    // Order of the parties does not matter
    assertEquals ("http://entsog.eu/communication/agreements/21X-EU-A-X0A0Y-Z/21X-EU-B-P0Q0R-S/1",
                  ENTSOGPMode.createAgreementID ("21X-EU-B-P0Q0R-S", "21X-EU-A-X0A0Y-Z", 1));
    assertEquals (ENTSOGPMode.createAgreementID ("21X-EU-A-X0A0Y-Z", "21X-EU-B-P0Q0R-S", 2),
                  ENTSOG4PMode.createAgreementID ("21X-EU-B-P0Q0R-S", "21X-EU-A-X0A0Y-Z", 2));

    try
    {
      ENTSOGPMode.createAgreementID ("21X-EU-A-X0A0Y-Z", "21X-EU-B-P0Q0R-S", 0);
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }
  }
}
