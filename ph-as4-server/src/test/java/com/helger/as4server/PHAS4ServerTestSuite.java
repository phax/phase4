package com.helger.as4server;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.helger.as4server.message.ReceiptMessageTest;
import com.helger.as4server.message.UserMessageCompressionTest;
import com.helger.as4server.message.UserMessageFailureForgeryTest;
import com.helger.as4server.message.UserMessageManyAttachmentTest;
import com.helger.as4server.message.UserMessageOneAttachmentTest;
import com.helger.as4server.message.UserMessageSoapBodyPayloadTest;
import com.helger.as4server.servlet.PModeCheckTest;
import com.helger.as4server.servlet.PartnerTest;
import com.helger.as4server.settings.AS4ServerConfiguration;
import com.helger.as4server.supplementary.test.EncryptionTest;
import com.helger.as4server.supplementary.test.SignatureTest;

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
