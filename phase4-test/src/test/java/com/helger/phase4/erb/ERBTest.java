/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.phase4.erb;

import static org.junit.Assert.assertTrue;

import org.apache.http.HttpEntity;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.error.EEbmsError;
import com.helger.phase4.http.HttpXMLEntity;
import com.helger.phase4.server.message.AbstractUserMessageTestSetUp;
import com.helger.phase4.server.message.MockMessages;
import com.helger.phase4.soap.ESOAPVersion;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.xml.serialize.read.DOMReader;

public final class ERBTest extends AbstractUserMessageTestSetUp
{
  @Test
  public void duplicateSignedMessage () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final ESOAPVersion eSOAPVersion = ESOAPVersion.AS4_DEFAULT;
    final Document aDoc = MockMessages.testSignedUserMessage (eSOAPVersion, aPayload, null, new AS4ResourceHelper ());

    final HttpEntity aEntity = new HttpXMLEntity (aDoc, eSOAPVersion.getMimeType ());

    final String sResponse = sendPlainMessage (aEntity, true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));

    sendPlainMessage (aEntity, false, EEbmsError.EBMS_OTHER.getErrorCode ());
  }
}
