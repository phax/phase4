package com.helger.as4lib.marshaller;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.soap11.Soap11Body;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.as4lib.soap11.Soap11Header;
import com.helger.as4lib.validator.MessageValidator;
import com.helger.commons.io.resource.ClassPathResource;

public class MessageValidatorTest
{

  private MessageValidator mv;

  @Before
  public void setUp ()
  {
    mv = new MessageValidator ();
  }

  @Test
  public void messageValidatorXML ()
  {
    mv.validateXML (new ClassPathResource ("/soap11test/MessageInfoImaginaryTimestamp.xml").getAsFile ());
  }

  @Test (expected = NullPointerException.class)
  public void messageValidatorXMLNoMessaging ()
  {
    mv.validateXML (new ClassPathResource ("/soap11test/NoMessaging.xml").getAsFile ());
  }

  @Test
  public void messageValidatorPOJO ()
  {
    final Ebms3Messaging aMessage = new Ebms3Messaging ();
    mv.validatePOJO (aMessage);
  }

  @Test
  public void soapEnvCreation ()
  {
    final Soap11Envelope aSoapEnv = new Soap11Envelope ();
    aSoapEnv.setHeader (new Soap11Header ());
    aSoapEnv.setBody (new Soap11Body ());
    final Ebms3Messaging aMessage = new Ebms3Messaging ();
    final Document aEbms3Message = Ebms3WriterBuilder.ebms3Messaging ().getAsDocument (aMessage);
    aSoapEnv.getHeader ().addAny (aEbms3Message.getDocumentElement ());

    System.out.println (Ebms3WriterBuilder.soap11 ().getAsString (aSoapEnv));
  }
}
