/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.server.servlet;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.xml.serialize.read.DOMReader;

public final class ErrorMessageTest extends AbstractUserMessageTestSetUpExt
{
  @Test
  public void testSendErrorMessage () throws Exception
  {
    final Document aDoc = DOMReader.readXMLDOM (new ClassPathResource ("testfiles/ErrorMessage.xml"));

    sendPlainMessage (new HttpXMLEntity (aDoc, ESoapVersion.SOAP_12.getMimeType ()), true, null);
  }

  @Test
  @Ignore ("Allowed since 0.9.14")
  public void testSendErrorMessageNoRefToMessageID () throws Exception
  {
    final Document aDoc = DOMReader.readXMLDOM (new ClassPathResource ("testfiles/ErrorMessageNoRefToMessageID.xml"));

    sendPlainMessage (new HttpXMLEntity (aDoc, ESoapVersion.SOAP_12.getMimeType ()),
                      false,
                      EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }
}
