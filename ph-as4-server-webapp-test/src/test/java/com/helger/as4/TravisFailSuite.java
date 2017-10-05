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
package com.helger.as4;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@Ignore ("Enable only on demand - tests are run anyway")
@RunWith (Suite.class)
@SuiteClasses ({ com.helger.as4.server.message.UserMessageSoapBodyPayloadTest.class,
                 com.helger.as4.server.message.EbmsMessagingTest.class,
                 com.helger.as4.server.message.ReceiptMessageTest.class,
                 com.helger.as4.server.message.UserMessageCompressionTest.class,
                 com.helger.as4.server.message.UserMessageFailureForgeryTest.class,
                 com.helger.as4.server.message.UserMessageOneAttachmentTest.class,
                 com.helger.as4.server.message.UserMessageManyAttachmentTest.class,
                 com.helger.as4.server.supplementary.test.EncryptionTest.class,
                 com.helger.as4.server.supplementary.test.SignatureTest.class,
                 com.helger.as4.server.servlet.ErrorMessageTest.class,
                 com.helger.as4.server.servlet.PullRequestTest.class,
                 com.helger.as4.server.servlet.TwoWayAsyncPushPullTest.class,
                 com.helger.as4.server.servlet.UserMessageDuplicateTest.class,
                 com.helger.as4.server.servlet.TwoWayMEPTest.class,
                 com.helger.as4.server.servlet.PModePingTest.class,
                 com.helger.as4.server.servlet.PModeCheckTest.class,
                 com.helger.as4.server.SPITest.class,
                 com.helger.as4.erb.ERBTest.class,
                 com.helger.as4.CEF.AS4CEFOneWayTest.class,
                 com.helger.as4.CEF.AS4eSENSCEFTwoWayTest.class,
                 com.helger.as4.CEF.AS4CEFTwoWayTest.class,
                 com.helger.as4.CEF.AS4eSENSCEFOneWayTest.class,
                 com.helger.as4.lib.client.AS4ClientUserMessageTest.class,
                 com.helger.as4.lib.client.AS4ClientReceiptMessageTest.class,
                 com.helger.as4.lib.client.AS4PullRequestTest.class })
public class TravisFailSuite
{

  @BeforeClass
  public static void setUpClass ()
  {}

  @AfterClass
  public static void tearDownClass ()
  {}
}
