package com.helger.as4lib.marshaller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.soap11.Soap11Body;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.as4lib.soap11.Soap11Header;
import com.helger.as4lib.testfiles.CAS4TestFiles;
import com.helger.as4lib.validator.MessageValidator;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;

public class MessageValidatorTest
{

  private MessageValidator aMessageValidator;

  private StringBuilder m_aFailedDocuments;

  @Before
  public void setUp ()
  {
    aMessageValidator = new MessageValidator ();
    m_aFailedDocuments = new StringBuilder ();
  }

  @Test
  public void messageValidatorXMLSuccessSOAP11 ()
  {
    final ICommonsList <String> aGoodFiles = CAS4TestFiles.getTestFilesSOAP11ValidXML ();

    for (final String aFilePath : aGoodFiles)
    {
      final IReadableResource aTMPFile = new ClassPathResource (CAS4TestFiles.TEST_FILE_PATH_SOAP_11 + aFilePath);

      if (!aMessageValidator.validateXML (aTMPFile))
      {
        m_aFailedDocuments.append (aFilePath);
        m_aFailedDocuments.append (" should have gone through, inspect file/code. ");
      }

    }

    _failedTestsCheck ();
  }

  @Test
  public void messageValidatorXMLSuccessSOAP12 ()
  {
    final ICommonsList <String> aGoodFiles = CAS4TestFiles.getTestFilesSOAP12ValidXML ();

    for (final String aFilePath : aGoodFiles)
    {
      final IReadableResource aTMPFile = new ClassPathResource (CAS4TestFiles.TEST_FILE_PATH_SOAP_12 + aFilePath);

      if (!aMessageValidator.validateXML (aTMPFile))
      {
        m_aFailedDocuments.append (aFilePath);
        m_aFailedDocuments.append (" should have gone through, inspect file/code. ");
      }

    }

    _failedTestsCheck ();
  }

  @Test
  public void messageValidatorXMLInvalidSOAP11 ()
  {
    final ICommonsList <String> aInvalidFiles = CAS4TestFiles.getTestFilesSOAP11InvalidXML ();

    for (final String aFilePath : aInvalidFiles)
    {
      final IReadableResource aTMPFile = new ClassPathResource (CAS4TestFiles.TEST_FILE_PATH_SOAP_11 + aFilePath);

      if (aMessageValidator.validateXML (aTMPFile))
      {
        m_aFailedDocuments.append (aFilePath);
        m_aFailedDocuments.append (" should have gone through, inspect file/code. ");
      }

    }

    _failedTestsCheck ();
  }

  @Test
  public void messageValidatorXMLInvalidSOAP12 ()
  {
    final ICommonsList <String> aInvalidFiles = CAS4TestFiles.getTestFilesSOAP12InvalidXML ();

    for (final String aFilePath : aInvalidFiles)
    {
      final IReadableResource aTMPFile = new ClassPathResource (CAS4TestFiles.TEST_FILE_PATH_SOAP_12 + aFilePath);

      if (aMessageValidator.validateXML (aTMPFile))
      {
        m_aFailedDocuments.append (aFilePath);
        m_aFailedDocuments.append (" should have gone through, inspect file/code. ");
      }
    }

    _failedTestsCheck ();
  }

  @Test
  public void messageValidatorPOJO ()
  {
    final Ebms3Messaging aMessage = new Ebms3Messaging ();
    aMessage.setS11MustUnderstand (true);
    final List <Ebms3SignalMessage> aSignalMessages = new ArrayList <Ebms3SignalMessage> ();
    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();
    final Ebms3MessageInfo aMessageInfo = new Ebms3MessageInfo ();
    aSignalMessage.setMessageInfo (aMessageInfo);
    aSignalMessages.add (aSignalMessage);
    aMessage.addSignalMessage (aSignalMessage);
    assertFalse (aMessageValidator.validatePOJO (aMessage));
  }

  @Test
  public void createTestErrorMessage () throws DatatypeConfigurationException
  {
    // Creating SOAP
    final Soap11Envelope aSoapEnv = new Soap11Envelope ();
    aSoapEnv.setHeader (new Soap11Header ());
    aSoapEnv.setBody (new Soap11Body ());

    // Creating Message
    final Ebms3Messaging aMessage = new Ebms3Messaging ();
    aMessage.setS11MustUnderstand (true);
    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();

    // Message Info
    final Ebms3MessageInfo aMessageInfo = new Ebms3MessageInfo ();
    aMessageInfo.setMessageId ("UUID-2@receiver.example.com");
    final GregorianCalendar c = new GregorianCalendar ();
    final Date aDate = new Date ();
    c.setTime (aDate);
    final XMLGregorianCalendar aXMLdate = DatatypeFactory.newInstance ().newXMLGregorianCalendar (c);
    aMessageInfo.setTimestamp (aXMLdate);
    aSignalMessage.setMessageInfo (aMessageInfo);

    // Error Message
    final List <Ebms3Error> aErrorMessages = new ArrayList <Ebms3Error> ();
    final Ebms3Error aEbms3Error = new Ebms3Error ();
    aEbms3Error.setErrorCode (EEbmsError.EBMS_INVALID_HEADER.getErrorCode ());
    aEbms3Error.setSeverity (EEbmsError.EBMS_INVALID_HEADER.getSeverity ().getSeverity ());
    aErrorMessages.add (aEbms3Error);
    aSignalMessage.setError (aErrorMessages);
    aMessage.addSignalMessage (aSignalMessage);

    // Adding the signal message to the existing soap
    final Document aEbms3Message = Ebms3WriterBuilder.ebms3Messaging ().getAsDocument (aMessage);
    aSoapEnv.getHeader ().addAny (aEbms3Message.getDocumentElement ());

    System.out.println (Ebms3WriterBuilder.soap11 ().getAsString (aSoapEnv));
  }

  private void _failedTestsCheck ()
  {
    if (!m_aFailedDocuments.toString ().isEmpty ())
    {
      fail ("Documents who did not pass: " + m_aFailedDocuments.toString ());
    }
  }
}
