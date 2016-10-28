/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.marshaller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Ignore;
import org.junit.Test;

import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.testfiles.CAS4TestFiles;
import com.helger.as4lib.validator.MessageValidator;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;

public final class MessageValidatorTest
{
  private static final Locale LOCALE = Locale.US;

  @Ignore
  @Test
  public void messageValidatorXMLSuccessSOAP11 ()
  {
    final ICommonsList <String> aGoodFiles = CAS4TestFiles.getTestFilesSOAP11ValidXML ();
    final MessageValidator aMessageValidator = new MessageValidator ();

    for (final String sFilePath : aGoodFiles)
    {
      final IReadableResource aTMPFile = new ClassPathResource (CAS4TestFiles.TEST_FILE_PATH_SOAP_11 + sFilePath);

      assertTrue (sFilePath +
                  " should have gone through, inspect file/code. ",
                  aMessageValidator.validateXML (aTMPFile, LOCALE));
    }
  }

  @Test
  public void messageValidatorXMLSuccessSOAP12 ()
  {
    final ICommonsList <String> aGoodFiles = CAS4TestFiles.getTestFilesSOAP12ValidXML ();
    final MessageValidator aMessageValidator = new MessageValidator ();

    for (final String sFilePath : aGoodFiles)
    {
      final IReadableResource aTMPFile = new ClassPathResource (CAS4TestFiles.TEST_FILE_PATH_SOAP_12 + sFilePath);

      assertTrue (sFilePath +
                  " should have gone through, inspect file/code.",
                  aMessageValidator.validateXML (aTMPFile, LOCALE));
    }
  }

  @Test
  public void messageValidatorXMLInvalidSOAP11 ()
  {
    final ICommonsList <String> aInvalidFiles = CAS4TestFiles.getTestFilesSOAP11InvalidXML ();
    final MessageValidator aMessageValidator = new MessageValidator ();

    for (final String sFilePath : aInvalidFiles)
    {
      final IReadableResource aTMPFile = new ClassPathResource (CAS4TestFiles.TEST_FILE_PATH_SOAP_11 + sFilePath);

      assertFalse (sFilePath +
                   " should have not gone through, inspect file/code.",
                   aMessageValidator.validateXML (aTMPFile, LOCALE));
    }
  }

  @Test
  public void messageValidatorXMLInvalidSOAP12 ()
  {
    final ICommonsList <String> aInvalidFiles = CAS4TestFiles.getTestFilesSOAP12InvalidXML ();
    final MessageValidator aMessageValidator = new MessageValidator ();

    for (final String sFilePath : aInvalidFiles)
    {
      final IReadableResource aTMPFile = new ClassPathResource (CAS4TestFiles.TEST_FILE_PATH_SOAP_12 + sFilePath);

      assertFalse (sFilePath +
                   " should not have gone through, inspect file/code. ",
                   aMessageValidator.validateXML (aTMPFile, LOCALE));
    }
  }

  @Test
  public void messageValidatorPOJO ()
  {
    final Ebms3Messaging aMessage = new Ebms3Messaging ();
    aMessage.setS11MustUnderstand (true);
    final List <Ebms3SignalMessage> aSignalMessages = new ArrayList<> ();
    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();
    final Ebms3MessageInfo aMessageInfo = new Ebms3MessageInfo ();
    aSignalMessage.setMessageInfo (aMessageInfo);
    aSignalMessages.add (aSignalMessage);
    aMessage.addSignalMessage (aSignalMessage);

    final MessageValidator aMessageValidator = new MessageValidator ();
    assertFalse (aMessageValidator.validatePOJO (aMessage));
  }
}
