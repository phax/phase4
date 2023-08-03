package com.helger.phase4.entsog;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.dao.DAOException;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModeManager;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.profile.entsog.ENTSOGPMode;

public final class Issue118Test
{
  @Rule
  public final TestRule m_aTestRule = new AS4TestRule ();

  @Test
  public void test () throws DAOException
  {
    final PModeManager pModeManager = new PModeManager (null);

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
