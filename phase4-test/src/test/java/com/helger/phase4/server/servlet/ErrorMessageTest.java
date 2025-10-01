/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.io.resource.ClassPathResource;
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
    assertNotNull (aDoc);

    // This is handled by MockMessageProcessorSPI
    sendPlainMessage (new HttpXMLEntity (aDoc, ESoapVersion.SOAP_12.getMimeType ()), true, null);
  }

  @Test
  public void testSendErrorMessageWithFault () throws Exception
  {
    // Test file must not contain WSSE, because no default PMode is set
    final Document aDoc = DOMReader.readXMLDOM (new ClassPathResource ("testfiles/ErrorMessageWithFault.xml"));
    assertNotNull (aDoc);

    // This is handled by MockMessageProcessorSPI
    sendPlainMessage (new HttpXMLEntity (aDoc, ESoapVersion.SOAP_12.getMimeType ()), true, null);
  }

  @Test
  @Ignore ("Allowed since 0.9.14")
  public void testSendErrorMessageNoRefToMessageID () throws Exception
  {
    final Document aDoc = DOMReader.readXMLDOM (new ClassPathResource ("testfiles/ErrorMessageNoRefToMessageID.xml"));
    assertNotNull (aDoc);

    sendPlainMessage (new HttpXMLEntity (aDoc, ESoapVersion.SOAP_12.getMimeType ()),
                      false,
                      EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }
}
