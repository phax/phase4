package com.helger.as4.server.servlet;

import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.pmode.DefaultPMode;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.util.AS4XMLHelper;

public class PModePingTest extends AbstractUserMessageTestSetUpExt
{

  // Can only check success, since we cannot check if SPIs got called or not
  @Test
  public void usePModePingSuccessful () throws Exception
  {
    final PMode aPMode = (PMode) DefaultPMode.getDefaultPMode ();
    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);

    final Document aDoc = _modifyUserMessage (aPMode.getConfigID (), null, null, _defaultProperties ());

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);

    MetaAS4Manager.getPModeMgr ().deletePMode (aPMode.getID ());
  }
}
