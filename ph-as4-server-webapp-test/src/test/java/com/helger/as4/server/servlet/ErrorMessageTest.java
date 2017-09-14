/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.server.servlet;

import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4.error.EEbmsError;
import com.helger.as4.http.HttpXMLEntity;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

public class ErrorMessageTest extends AbstractUserMessageTestSetUpExt
{
  @Test
  public void sendErrorMessage () throws Exception
  {
    final Document aDoc = DOMReader.readXMLDOM (new ClassPathResource ("testfiles/ErrorMessage.xml"));

    sendPlainMessage (new HttpXMLEntity (aDoc, ESOAPVersion.SOAP_12), true, null);
  }

  @Test
  public void sendErrorMessageNoRefToMessageID () throws Exception
  {
    final Document aDoc = DOMReader.readXMLDOM (new ClassPathResource ("testfiles/ErrorMessageNoRefToMessageID.xml"));

    sendPlainMessage (new HttpXMLEntity (aDoc, ESOAPVersion.SOAP_12),
                      false,
                      EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }
}
