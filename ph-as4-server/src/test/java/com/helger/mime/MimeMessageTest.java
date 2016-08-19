package com.helger.mime;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4server.client.TestMessages;

public final class MimeMessageTest
{
  @Test
  public void testMessage () throws MessagingException, IOException
  {
    final MimeMessage message = TestMessages.testMIMEMessage (ESOAPVersion.SOAP_12);
    assertNotNull (message);
    message.writeTo (System.out);
  }
}
