/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.marshaller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.validation.CollectingValidationEventHandler;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.soap11.Soap11Envelope;

public final class Ebms3ReaderBuilderTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (Ebms3ReaderBuilderTest.class);

  @Test
  public void testSoap ()
  {
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Soap11Envelope aEnv = Ebms3ReaderBuilder.soap11 ()
                                                  .setValidationEventHandler (aCVEH)
                                                  .read (new ClassPathResource ("/soap11test/UserMessage.xml"));
    assertNotNull (aEnv);
    assertTrue (aCVEH.getErrorList ().isEmpty ());
    assertNotNull (aEnv.getHeader ());
    assertEquals (1, aEnv.getHeader ().getAnyCount ());
    assertTrue (aEnv.getHeader ().getAnyAtIndex (0) instanceof Element);

    final Ebms3Messaging aMessage = Ebms3ReaderBuilder.ebms3Messaging ()
                                                      .setValidationEventHandler (aCVEH)
                                                      .read ((Element) aEnv.getHeader ().getAnyAtIndex (0));
    assertNotNull (aMessage);

    final String sReRead = Ebms3WriterBuilder.soap11 ().getAsString (aEnv);
    assertNotNull (sReRead);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Just to recheck what was read: " + sReRead);
  }

  @Test
  public void testNoSoap ()
  {
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Ebms3Messaging aMessage = Ebms3ReaderBuilder.ebms3Messaging ()
                                                      .setValidationEventHandler (aCVEH)
                                                      .read (new ClassPathResource ("/soap11test/UserMessage-no-soap.xml"));
    assertNotNull (aMessage);
    assertTrue (aCVEH.getErrorList ().isEmpty ());

    aMessage.getUserMessageAtIndex (0).getMessageInfo ().setMessageId ("blaFoo");

    final String sReRead = Ebms3WriterBuilder.ebms3Messaging ().getAsString (aMessage);
    assertNotNull (sReRead);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Just to recheck what was read: " + sReRead);
  }

  @Test
  public void testUserMessageMessageInfoMissing ()
  {
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Soap11Envelope aEnv = Ebms3ReaderBuilder.soap11 ()
                                                  .setValidationEventHandler (aCVEH)
                                                  .read (new ClassPathResource ("/soap11test/MessageInfoMissing.xml"));
    assertNotNull (aEnv);
    assertTrue (aCVEH.getErrorList ().isEmpty ());
    assertNotNull (aEnv.getHeader ());
    assertEquals (1, aEnv.getHeader ().getAnyCount ());
    assertTrue (aEnv.getHeader ().getAnyAtIndex (0) instanceof Element);

    final Ebms3Messaging aMessage = Ebms3ReaderBuilder.ebms3Messaging ()
                                                      .setValidationEventHandler (aCVEH)
                                                      .read ((Element) aEnv.getHeader ().getAnyAtIndex (0));
    assertTrue (aCVEH.getErrorList ().containsAtLeastOneError ());
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
    assertTrue (aCVEH.getErrorList ().isEmpty ());
    assertNotNull (aEnv.getHeader ());
    assertEquals (1, aEnv.getHeader ().getAnyCount ());
    assertTrue (aEnv.getHeader ().getAnyAtIndex (0) instanceof Element);

    final Ebms3Messaging aMessage = Ebms3ReaderBuilder.ebms3Messaging ()
                                                      .setValidationEventHandler (aCVEH)
                                                      .read ((Element) aEnv.getHeader ().getAnyAtIndex (0));
    assertTrue (aCVEH.getErrorList ().containsAtLeastOneError ());
    assertNull (aMessage);
  }

  @Test
  public void expectSoap11ButFileIsSoap12 ()
  {
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Soap11Envelope aEnv = Ebms3ReaderBuilder.soap11 ()
                                                  .setValidationEventHandler (aCVEH)
                                                  .read (new ClassPathResource ("/soap12test/UserMessage12.xml"));

    assertNull (aEnv);
    assertFalse (aCVEH.getErrorList ().isEmpty ());
  }
}
