/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.server.message;

import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.base.system.SystemProperties;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.messaging.crypto.AS4Encryptor;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test the enforcement of the PMode leg security settings on incoming UserMessages. The PMode used
 * by the mock messages defines both a signature and an encryption algorithm. See issue #404.
 *
 * @author Philip Helger
 */
@RunWith (Parameterized.class)
public final class UserMessageEnforcePModeSecurityTest extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return new CommonsArrayList <> (ESoapVersion.values (), x -> new Object [] { x });
  }

  private final ESoapVersion m_eSoapVersion;

  public UserMessageEnforcePModeSecurityTest (@NonNull final ESoapVersion eSOAPVersion)
  {
    m_eSoapVersion = eSOAPVersion;
  }

  @Before
  public void enableEnforcement ()
  {
    SystemProperties.setPropertyValue (AS4Configuration.PROPERTY_PHASE4_INCOMING_SECURITY_ENFORCE_PMODE, "true");
  }

  @After
  public void resetEnforcement ()
  {
    SystemProperties.removePropertyValue (AS4Configuration.PROPERTY_PHASE4_INCOMING_SECURITY_ENFORCE_PMODE);
  }

  @NonNull
  private static Node _readPayload ()
  {
    return DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
  }

  @Test
  public void testNotSignedNotEncryptedShouldFail () throws Exception
  {
    final Node aPayload = _readPayload ();
    final Document aDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, aPayload, null)
                                      .getAsSoapDocument (aPayload);

    sendPlainMessageExpectError (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                                 EEbmsError.EBMS_POLICY_NONCOMPLIANCE.getErrorCode ());
  }

  @Test
  public void testSignedNotEncryptedShouldFail () throws Exception
  {
    final Document aDoc = MockMessages.createUserMessageSigned (m_eSoapVersion, _readPayload (), null, s_aResMgr);

    sendPlainMessageExpectError (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                                 EEbmsError.EBMS_POLICY_NONCOMPLIANCE.getErrorCode ());
  }

  @Test
  public void testEncryptedNotSignedShouldFail () throws Exception
  {
    final Node aPayload = _readPayload ();
    Document aDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, aPayload, null)
                                .getAsSoapDocument (aPayload);
    aDoc = AS4Encryptor.encryptSoapBodyPayload (m_aCryptoFactory, m_eSoapVersion, aDoc, false, m_aCryptParams);

    sendPlainMessageExpectError (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                                 EEbmsError.EBMS_POLICY_NONCOMPLIANCE.getErrorCode ());
  }

  @Test
  public void testSignedAndEncryptedSuccess () throws Exception
  {
    Document aDoc = MockMessages.createUserMessageSigned (m_eSoapVersion, _readPayload (), null, s_aResMgr);
    aDoc = AS4Encryptor.encryptSoapBodyPayload (m_aCryptoFactory, m_eSoapVersion, aDoc, false, m_aCryptParams);

    final String sResponse = sendPlainMessageExpectSuccess (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()));
    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testNotSignedNotEncryptedSuccessIfDisabled () throws Exception
  {
    SystemProperties.setPropertyValue (AS4Configuration.PROPERTY_PHASE4_INCOMING_SECURITY_ENFORCE_PMODE, "false");

    final Node aPayload = _readPayload ();
    final Document aDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, aPayload, null)
                                      .getAsSoapDocument (aPayload);

    final String sResponse = sendPlainMessageExpectSuccess (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()));
    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }
}
