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
package com.helger.phase4.CEF;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.messaging.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.AS4MimeMessageHelper;

public final class AS4eSENSCEFTwoWayFuncTest extends AbstractCEFTwoWayTestSetUp
{
  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: Two-Way/Push-and-Push MEP. SMSH sends an AS4 User Message
   * (M1 with ID MessageId) that requires a consumer response to the RMSH. <br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a User Message (M2) with element REFTOMESSAGEID set to
   * MESSAGEID (of M1).
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA02_PushPush () throws Exception
  {
    final Document aDoc = testSignedUserMessage (m_eSoapVersion, m_aPayload, null, s_aResMgr);

    final NodeList nList = aDoc.getElementsByTagName ("eb:MessageId");

    // Should only be called once
    final String aID = nList.item (0).getTextContent ();

    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);

    assertTrue (sResponse.contains ("eb:RefToMessageId"));
    assertTrue (sResponse.contains (aID));

    // Wait for async response to come in
    // Otherwise indeterministic errors
    ThreadHelper.sleepSeconds (2);
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (Two-Way/Push-and-Push MEP). SMSH sends an AS4 User Message
   * to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH returns a non-repudiation receipt within a HTTP response with
   * status code 2XX.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA16_PushPush () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (),
                                                                    s_aResMgr));

    final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion,
                                                                        testSignedUserMessage (m_eSoapVersion,
                                                                                               m_aPayload,
                                                                                               aAttachments,
                                                                                               s_aResMgr),
                                                                        aAttachments);

    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));

    // Wait for async response to come in
    // Otherwise indeterministic errors
    ThreadHelper.sleepSeconds (2);
  }
}
