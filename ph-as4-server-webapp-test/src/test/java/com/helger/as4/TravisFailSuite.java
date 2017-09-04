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
