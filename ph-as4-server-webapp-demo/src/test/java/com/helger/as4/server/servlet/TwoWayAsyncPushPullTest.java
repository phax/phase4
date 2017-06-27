package com.helger.as4.server.servlet;

import org.junit.Before;
import org.junit.Test;

import com.helger.as4.AS4TestConstants;
import com.helger.as4.CAS4;
import com.helger.as4.esens.ESENSPMode;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.PModeParty;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.commons.id.factory.GlobalIDFactory;

public class TwoWayAsyncPushPullTest extends AbstractUserMessageTestSetUpExt
{
  private PMode m_aPMode;

  @Before
  public void createTwoWayPMode ()
  {
    final PMode aPMode = ESENSPMode.createESENSPMode (AS4TestConstants.TEST_INITIATOR,
                                                      AS4TestConstants.TEST_RESPONDER,
                                                      AS4ServerConfiguration.getSettings ()
                                                                            .getAsString ("server.address",
                                                                                          AS4TestConstants.DEFAULT_SERVER_ADDRESS),
                                                      (i, r) -> "pmode" + GlobalIDFactory.getNewPersistentLongID ());
    // Setting second leg to the same as first
    final PModeLeg aLeg2 = aPMode.getLeg1 ();

    // ESENS PMode is One Way on default settings need to change to two way
    m_aPMode = new PMode ( (i, r) -> aPMode.getID (),
                           PModeParty.createSimple (DEFAULT_PARTY_ID + "1", CAS4.DEFAULT_ROLE),
                           PModeParty.createSimple (DEFAULT_PARTY_ID + "1", CAS4.DEFAULT_ROLE),
                           aPMode.getAgreement (),
                           EMEP.TWO_WAY,
                           EMEPBinding.PUSH_PULL,
                           aPMode.getLeg1 (),
                           aLeg2,
                           aPMode.getPayloadService (),
                           aPMode.getReceptionAwareness ());

    // Delete old PMode since it is getting created in the ESENS createPMode
    MetaAS4Manager.getPModeMgr ().deletePMode (aPMode.getID ());
    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (m_aPMode);

  }

  @Test
  public void testasdas ()
  {

  }
}
