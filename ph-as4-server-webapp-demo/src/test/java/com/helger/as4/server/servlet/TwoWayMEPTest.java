package com.helger.as4.server.servlet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.as4.esens.ESENSPMode;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;

// TODO fix this up if we know what we wanna do with twoway

public class TwoWayMEPTest extends AbstractUserMessageTestSetUpExt
{
  private static PMode s_aPMode;

  @BeforeClass
  public static void createTwoWayPMode ()
  {
    s_aPMode = ESENSPMode.createESENSPMode (AS4ServerConfiguration.getSettings ()
                                                                .getAsString ("server.address",
                                                                              "http://localhost:8080/as4"));
    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (s_aPMode);
  }

  @AfterClass
  public static void destroyTwoWayPMode ()
  {
    MetaAS4Manager.getPModeMgr ().deletePMode (s_aPMode.getID ());
  }

  @Test
  public void receiveUserMessageAsResponseSuccess ()
  {

  }
}
