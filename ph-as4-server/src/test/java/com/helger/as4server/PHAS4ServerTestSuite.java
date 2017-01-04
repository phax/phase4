package com.helger.as4server;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.helger.as4lib.attachment.EAS4CompressionModeFuncTest;
import com.helger.as4server.message.ReceiptMessageTests;
import com.helger.as4server.message.UserMessageCompressionTest;
import com.helger.as4server.message.UserMessageFailureForgeryTest;
import com.helger.as4server.message.UserMessageManyAttachmentTests;
import com.helger.as4server.message.UserMessageOneAttachmentTests;
import com.helger.as4server.message.UserMessageSoapBodyPayloadTests;
import com.helger.as4server.servlet.PModeCheckTest;
import com.helger.as4server.servlet.PartnerTest;
import com.helger.as4server.settings.AS4ServerConfiguration;
import com.helger.as4server.supplementary.test.EncryptionTest;
import com.helger.as4server.supplementary.test.SignatureTest;

@RunWith (Suite.class)
@SuiteClasses ({ SPITest.class,
                 ReceiptMessageTests.class,
                 UserMessageCompressionTest.class,
                 UserMessageFailureForgeryTest.class,
                 UserMessageManyAttachmentTests.class,
                 UserMessageOneAttachmentTests.class,
                 UserMessageSoapBodyPayloadTests.class,
                 PartnerTest.class,
                 PModeCheckTest.class,
                 EncryptionTest.class,
                 SignatureTest.class,
                 EAS4CompressionModeFuncTest.class,
                 ReceiptMessageTests.class })
public class PHAS4ServerTestSuite
{
  public static String DEFAULT_URI = "http://localhost:8081/as4";

  @BeforeClass
  public static void init ()
  {
    AS4ServerConfiguration.reinitForTestOnly ();
    AS4ServerConfiguration.getMutableSettings ().setValue ("server.jetty.enabled", true);
    AS4ServerConfiguration.getMutableSettings ().setValue ("server.address", DEFAULT_URI);
  }
}
