package com.helger.as4.server.servlet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.as4.esens.ESENSPMode;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.pmode.PMode;

public class TwoWayMEPTest extends AbstractUserMessageTestSetUpExt
{
  private static PMode aPMode;

  @BeforeClass
  public void createTwoWayPMode ()
  {
    aPMode = ESENSPMode.createESENSPMode (m_aSettings.getAsString ("server.address", "http://localhost:8080/as4"));
    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);
  }

  @AfterClass
  public void destroyTwoWayPMode ()
  {
    MetaAS4Manager.getPModeMgr ().deletePMode (aPMode.getID ());
  }

  @Test
  public void receiveUserMessageAsResponseSuccess ()
  {

  }
}
