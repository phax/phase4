package com.helger.as4server;

import org.junit.runners.Suite.SuiteClasses;

import com.helger.as4server.message.ReceiptMessageTests;
import com.helger.as4server.message.UserMessageCompressionTest;
import com.helger.as4server.message.UserMessageFailureForgeryTest;
import com.helger.as4server.message.UserMessageManyAttachmentTests;
import com.helger.as4server.message.UserMessageOneAttachmentTests;
import com.helger.as4server.message.UserMessageSoapBodyPayloadTests;
import com.helger.as4server.servlet.PModeCheckTest;
import com.helger.as4server.servlet.PartnerTest;
import com.helger.as4server.supplementary.test.EncryptionTest;
import com.helger.as4server.supplementary.test.SignatureTest;
import com.helger.compression.CompressionTest;

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
                 CompressionTest.class,
                 ReceiptMessageTests.class })
public class PHAS4ServerTestSuite
{

}
