package com.helger.as4.server.servlet;

import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4.model.pmode.DefaultPMode;
import com.helger.as4.util.AS4XMLHelper;

public class PModePingTest extends AbstractUserMessageTestSetUpExt
{

  // Can only check success, since we cannot check if SPIs got called or not
  @Test
  public void usePModePingSuccessful () throws Exception
  {
    final Document aDoc = _modifyUserMessage (DefaultPMode.DEFAULT_PMODE_ID, null, null, _defaultProperties ());

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);
  }
}
