package com.helger.as4.server.servlet;

import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4.error.EEbmsError;
import com.helger.as4.http.HttpXMLEntity;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

public class ErrorMessageTest extends AbstractUserMessageTestSetUpExt
{
  @Test
  public void sendErrorMessage () throws Exception
  {
    final Document aDoc = DOMReader.readXMLDOM (new ClassPathResource ("testfiles/ErrorMessage.xml"));

    sendPlainMessage (new HttpXMLEntity (aDoc), true, null);
  }

  @Test
  public void sendErrorMessageNoRefToMessageID () throws Exception
  {
    final Document aDoc = DOMReader.readXMLDOM (new ClassPathResource ("testfiles/ErrorMessageNoRefToMessageID.xml"));

    sendPlainMessage (new HttpXMLEntity (aDoc), false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }
}
