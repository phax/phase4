package com.helger.as4.server.servlet;

import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4.error.EEbmsError;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

public class ErrorMessageTest extends AbstractUserMessageTestSetUpExt
{
  @Test
  public void sendErrorMessage () throws Exception
  {
    final Document aDoc = DOMReader.readXMLDOM (new ClassPathResource ("testfiles/ErrorMessage.xml"));

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc), AS4XMLHelper.XWS.getCharsetObj ()),
                      true,
                      null);
  }

  @Test
  public void sendErrorMessageNoRefToMessageID () throws Exception
  {
    final Document aDoc = DOMReader.readXMLDOM (new ClassPathResource ("testfiles/ErrorMessageNoRefToMessageID.xml"));

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc), AS4XMLHelper.XWS.getCharsetObj ()),
                      false,
                      EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }
}
