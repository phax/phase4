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
package com.helger.as4.server;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.helger.as4.server.message.ReceiptMessageTest;
import com.helger.as4.server.message.UserMessageCompressionTest;
import com.helger.as4.server.message.UserMessageFailureForgeryTest;
import com.helger.as4.server.message.UserMessageManyAttachmentTest;
import com.helger.as4.server.message.UserMessageOneAttachmentTest;
import com.helger.as4.server.message.UserMessageSoapBodyPayloadTest;
import com.helger.as4.server.servlet.PModeCheckTest;
import com.helger.as4.server.servlet.PartnerTest;
import com.helger.as4.server.supplementary.test.EncryptionTest;
import com.helger.as4.server.supplementary.test.SignatureTest;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;

@RunWith (Suite.class)
@SuiteClasses ({ SPITest.class,
                 ReceiptMessageTest.class,
                 UserMessageCompressionTest.class,
                 UserMessageFailureForgeryTest.class,
                 UserMessageManyAttachmentTest.class,
                 UserMessageOneAttachmentTest.class,
                 UserMessageSoapBodyPayloadTest.class,
                 PartnerTest.class,
                 PModeCheckTest.class,
                 EncryptionTest.class,
                 SignatureTest.class,
                 ReceiptMessageTest.class })
@Ignore
public class PHAS4ServerTestSuite
{
  @BeforeClass
  public static void init ()
  {
    // Force re-init
    AS4ServerConfiguration.reinit (true);
  }
}
