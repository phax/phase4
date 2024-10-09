/*
 * Copyright (C) 2015-2024 Pavel Rotek
 * pavel[dot]rotek[at]gmail[dot]com
 *
 * Copyright (C) 2021-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.entsog;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.dao.DAOException;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModeManagerXML;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.profile.entsog.ENTSOGPMode;

public final class Issue118Test
{
  @Rule
  public final TestRule m_aTestRule = new AS4TestRule ();

  @Test
  public void test () throws DAOException
  {
    final PModeManagerXML pModeManager = new PModeManagerXML (null);

    final PMode pMode = ENTSOGPMode.createENTSOGPMode ("SenderX",
                                                       "ResponderY",
                                                       "https://localhost:8443/as4",
                                                       (i, r) -> "PModeId",
                                                       true);

    pModeManager.createOrUpdatePMode (pMode);

    final PMode pMode2 = ENTSOGPMode.createENTSOGPMode ("SenderX",
                                                        "ResponderY",
                                                        "https://localhost:8443/as4",
                                                        (i, r) -> "PModeId",
                                                        true);
    // alter the role of the initiator
    final var initiator = pMode2.getInitiator ();
    final var newInitiator = new PModeParty (initiator.getIDType (),
                                             initiator.getID (),
                                             "ZSH",
                                             initiator.getUserName (),
                                             initiator.getPassword ());
    pMode2.setInitiator (newInitiator);

    pModeManager.createOrUpdatePMode (pMode2);
  }
}
