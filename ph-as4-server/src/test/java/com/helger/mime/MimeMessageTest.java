package com.helger.mime;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

import com.helger.as4server.client.TestMessages;

public class MimeMessageTest
{

  @Test
  public void testMessage () throws MessagingException, IOException
  {
    final MimeMessage message = TestMessages.testMIMEMessage ();
    message.writeTo (System.out);
  }
}
