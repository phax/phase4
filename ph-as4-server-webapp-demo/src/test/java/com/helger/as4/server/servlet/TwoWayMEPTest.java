package com.helger.as4.server.servlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.entity.StringEntity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4.esens.ESENSPMode;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.config.PModeConfig;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.util.AS4XMLHelper;

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
    // ESENS PMode is One Way on default settings need to change to two way
    final PModeConfig aPModeConfig = new PModeConfig ("esens-two-way");
    aPModeConfig.setMEP (EMEP.TWO_WAY_PUSH_PUSH);
    aPModeConfig.setAgreement (s_aPMode.getConfig ().getAgreement ());
    aPModeConfig.setLeg1 (s_aPMode.getConfig ().getLeg1 ());
    aPModeConfig.setMEPBinding (s_aPMode.getConfig ().getMEPBinding ());
    aPModeConfig.setPayloadService (s_aPMode.getConfig ().getPayloadService ());
    aPModeConfig.setReceptionAwareness (s_aPMode.getConfig ().getReceptionAwareness ());
    MetaAS4Manager.getPModeConfigMgr ().createOrUpdatePModeConfig (aPModeConfig);
    s_aPMode = new PMode (s_aPMode.getInitiator (), s_aPMode.getResponder (), aPModeConfig);
    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (s_aPMode);
  }

  @AfterClass
  public static void destroyTwoWayPMode ()
  {
    MetaAS4Manager.getPModeMgr ().deletePMode (s_aPMode.getID ());
  }

  @Test
  public void receiveUserMessageAsResponseSuccess () throws Exception
  {
    final Document aDoc = _modifyUserMessage (s_aPMode.getConfigID (), null, null, _defaultProperties ());
    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);
    assertTrue (m_sResponse.contains ("UserMessage"));
    assertFalse (m_sResponse.contains ("Receipt"));
  }
}
