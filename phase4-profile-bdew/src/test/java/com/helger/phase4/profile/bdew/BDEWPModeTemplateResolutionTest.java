/*
 * Copyright (C) 2023-2026 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
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
package com.helger.phase4.profile.bdew;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.ClassRule;
import org.junit.Test;

import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.resolve.AS4DefaultPModeResolver;
import com.helger.phase4.profile.IAS4Profile;
import com.helger.phase4.profile.IAS4ProfileValidator;
import com.helger.phase4.profile.IAS4ProfileValidator.EAS4ProfileValidationMode;
import com.helger.photon.app.mock.PhotonAppWebTestRule;

/**
 * Test for issue #213 - a PMode template that is synthesized for an incoming message must carry the
 * Service and the Action of that message, otherwise the BDEW profile validator can never accept it.
 *
 * @author Philip Helger
 */
public final class BDEWPModeTemplateResolutionTest
{
  @ClassRule
  public static final PhotonAppWebTestRule RULE = new PhotonAppWebTestRule ();

  private static final String INITIATOR_ID = "TestInitiator";
  private static final String RESPONDER_ID = "TestResponder";
  private static final String ADDRESS = "https://test.example.org/as4";

  @Test
  public void testSynthesizedTemplateCarriesServiceAndAction ()
  {
    final IAS4Profile aProfile = MetaAS4Manager.getProfileMgr ()
                                               .getProfileOfID (AS4BDEWProfileRegistarSPI.AS4_PROFILE_ID);
    assertNotNull (aProfile);

    // No PMode is registered, so the resolver falls back to the profile template
    final AS4DefaultPModeResolver aResolver = new AS4DefaultPModeResolver (AS4BDEWProfileRegistarSPI.AS4_PROFILE_ID);
    final IPMode aPMode = aResolver.findPMode (null,
                                               BDEWPMode.SERVICE_MARKTPROZESSE,
                                               BDEWPMode.ACTION_DEFAULT,
                                               INITIATOR_ID,
                                               RESPONDER_ID,
                                               null,
                                               ADDRESS);
    assertNotNull (aPMode);

    // These two were "null" before the fix of issue #213
    assertEquals (BDEWPMode.SERVICE_MARKTPROZESSE, aPMode.getLeg1 ().getBusinessInfo ().getService ());
    assertEquals (BDEWPMode.ACTION_DEFAULT, aPMode.getLeg1 ().getBusinessInfo ().getAction ());
  }

  @Test
  public void testSynthesizedTemplatePassesBusinessInfoValidation ()
  {
    final IAS4Profile aProfile = MetaAS4Manager.getProfileMgr ()
                                               .getProfileOfID (AS4BDEWProfileRegistarSPI.AS4_PROFILE_ID);
    assertNotNull (aProfile);

    final IAS4ProfileValidator aValidator = aProfile.getValidator ();
    assertNotNull (aValidator);

    final AS4DefaultPModeResolver aResolver = new AS4DefaultPModeResolver (AS4BDEWProfileRegistarSPI.AS4_PROFILE_ID);
    final IPMode aPMode = aResolver.findPMode (null,
                                               BDEWPMode.SERVICE_MARKTPROZESSE,
                                               BDEWPMode.ACTION_DEFAULT,
                                               INITIATOR_ID,
                                               RESPONDER_ID,
                                               null,
                                               ADDRESS);
    assertNotNull (aPMode);

    final ErrorList aErrorList = new ErrorList ();
    aValidator.validatePMode (aPMode, aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);

    // The reported error was "PMode.Leg[1].BusinessInfo.Service 'null' is unsupported"
    for (final IError aError : aErrorList)
      assertTrue ("Unexpected error: " + aError.getErrorText (Locale.US),
                  !aError.getErrorText (Locale.US).contains ("BusinessInfo.Service") &&
                                                             !aError.getErrorText (Locale.US)
                                                                    .contains ("BusinessInfo.Action"));
  }
}
