package com.helger.as4lib.marshaller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Element;

import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.validation.CollectingValidationEventHandler;

public final class Ebms3ReaderBuilderTest
{
  @Test
  public void testSoap ()
  {
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Soap11Envelope aEnv = Ebms3ReaderBuilder.soap11 ()
                                                  .setValidationEventHandler (aCVEH)
                                                  .read (new ClassPathResource ("/soap11test/UserMessage.xml"));
    assertNotNull (aEnv);
    assertTrue (aCVEH.getResourceErrors ().isEmpty ());
    assertNotNull (aEnv.getHeader ());
    assertEquals (1, aEnv.getHeader ().getAnyCount ());
    assertTrue (aEnv.getHeader ().getAnyAtIndex (0) instanceof Element);

    final Ebms3Messaging aMessage = Ebms3ReaderBuilder.ebms3Messaging ()
                                                      .setValidationEventHandler (aCVEH)
                                                      .read ((Element) aEnv.getHeader ().getAnyAtIndex (0));
    assertNotNull (aMessage);

    final String sReRead = Ebms3WriterBuilder.soap11 ().getAsString (aEnv);
    System.out.println (sReRead);
  }

  @Test
  public void testNoSoap ()
  {
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Ebms3Messaging aMessage = Ebms3ReaderBuilder.ebms3Messaging ()
                                                      .setValidationEventHandler (aCVEH)
                                                      .read (new ClassPathResource ("/soap11test/UserMessage-no-soap.xml"));
    assertNotNull (aMessage);
    assertTrue (aCVEH.getResourceErrors ().isEmpty ());

    aMessage.getUserMessageAtIndex (0).getMessageInfo ().setMessageId ("blaFoo");

    final String sReRead = Ebms3WriterBuilder.ebms3Messaging ().getAsString (aMessage);
    System.out.println (sReRead);
  }

  @Test
  public void testUserMessageMessageInfoMissing ()
  {
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Soap11Envelope aEnv = Ebms3ReaderBuilder.soap11 ()
                                                  .setValidationEventHandler (aCVEH)
                                                  .read (new ClassPathResource ("/soap11test/MessageInfoMissing.xml"));
    assertNotNull (aEnv);
    assertTrue (aCVEH.getResourceErrors ().isEmpty ());
    assertNotNull (aEnv.getHeader ());
    assertEquals (1, aEnv.getHeader ().getAnyCount ());
    assertTrue (aEnv.getHeader ().getAnyAtIndex (0) instanceof Element);

    final Ebms3Messaging aMessage = Ebms3ReaderBuilder.ebms3Messaging ()
                                                      .setValidationEventHandler (aCVEH)
                                                      .read ((Element) aEnv.getHeader ().getAnyAtIndex (0));
    assertTrue (aCVEH.getResourceErrors ().containsAtLeastOneError ());
    assertNull (aMessage);
  }

  @Test
  public void testUserMessageMessageInfoIDMissing ()
  {
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Soap11Envelope aEnv = Ebms3ReaderBuilder.soap11 ()
                                                  .setValidationEventHandler (aCVEH)
                                                  .read (new ClassPathResource ("/soap11test/MessageInfoIDMissing.xml"));
    assertNotNull (aEnv);
    assertTrue (aCVEH.getResourceErrors ().isEmpty ());
    assertNotNull (aEnv.getHeader ());
    assertEquals (1, aEnv.getHeader ().getAnyCount ());
    assertTrue (aEnv.getHeader ().getAnyAtIndex (0) instanceof Element);

    final Ebms3Messaging aMessage = Ebms3ReaderBuilder.ebms3Messaging ()
                                                      .setValidationEventHandler (aCVEH)
                                                      .read ((Element) aEnv.getHeader ().getAnyAtIndex (0));
    assertTrue (aCVEH.getResourceErrors ().containsAtLeastOneError ());
    assertNull (aMessage);

  }

}
