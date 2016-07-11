package com.helger.as4lib.validator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.error.ErrorConverter;
import com.helger.as4lib.marshaller.Ebms3ReaderBuilder;
import com.helger.as4lib.marshaller.Ebms3WriterBuilder;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.as4lib.soap12.Soap12Envelope;
import com.helger.commons.callback.exception.CollectingExceptionCallback;
import com.helger.commons.error.IResourceError;
import com.helger.jaxb.validation.CollectingValidationEventHandler;

public class MessageValidator
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (MessageValidator.class);

  // TODO Check P-Modes they should define which SOAP Version should be used for
  // the conversation
  public Document getSoapEnvelope (final File aXML)
  {
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final CollectingExceptionCallback <JAXBException> aExHdl = new CollectingExceptionCallback<> ();
    final Soap11Envelope aEnv = Ebms3ReaderBuilder.soap11 ()
                                                  .setExceptionHandler (aExHdl)
                                                  .setValidationEventHandler (aCVEH)
                                                  .read (aXML);
    if (aExHdl.hasException ())
    {
      final String sCause = aExHdl.getException ().getCause ().toString ();
      if (!sCause.contains ("S12:Envelope"))
      {
        s_aLogger.info (sCause);
      }
    }
    // If the document can not be read by the soap11 Reader, try soap12 Reader
    if (aCVEH.getResourceErrors ().containsAtLeastOneError ())
    {
      final IResourceError aError = aCVEH.getResourceErrors ().iterator ().next ();
      if (aError.getDisplayText (Locale.getDefault ()).contains ("S12:Envelope"))
      {
        final Soap12Envelope aEnv12 = Ebms3ReaderBuilder.soap12 ().setValidationEventHandler (aCVEH).read (aXML);
        return Ebms3WriterBuilder.soap12 ().getAsDocument (aEnv12);
      }
    }
    return aEnv != null ? Ebms3WriterBuilder.soap11 ().getAsDocument (aEnv) : null;

  }

  // TODO Split Message and SOAP CHECK? SOAP currently treated the same as xml
  // error
  public boolean validateXML (final File aXML)
  {
    final Document aDocument = getSoapEnvelope (aXML);
    if (aDocument != null)
    {
      final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
      for (int i = 0; i < aDocument.getChildNodes ().getLength (); i++)
      {
        final Ebms3Messaging aMessage = Ebms3ReaderBuilder.ebms3Messaging ()
                                                          .setValidationEventHandler (aCVEH)
                                                          .read (aDocument.getChildNodes ().item (i));

        if (aCVEH.getResourceErrors ().containsAtLeastOneError ())
        {
          final List <EEbmsError> aOccurredErrors = new ArrayList <EEbmsError> ();
          aOccurredErrors.add (EEbmsError.EBMS_INVALID_HEADER);
          sendErrorResponse (aOccurredErrors);
          return false;
        }
      }
      return true;
    }
    return false;
  }

  public void sendErrorResponse (final List <EEbmsError> aOccurredErrors)
  {
    final Ebms3Messaging aResponse = new Ebms3Messaging ();
    final Ebms3SignalMessage aErrorResponse = new Ebms3SignalMessage ();
    final List <Ebms3Error> aErrorList = new ArrayList <Ebms3Error> ();
    // TODO how to get Messageinfo for response?
    // aErrorResponse.setMessageInfo (value);
    // TODO set S11MustUnderstand or S12MustUnderstand depending on MessageInfo?
    // or through other means
    for (final EEbmsError aError : aOccurredErrors)
    {
      aErrorList.add (new ErrorConverter ().convertEnumToEbms3Error (aError));
    }

    aErrorResponse.setError (aErrorList);
    final List <Ebms3SignalMessage> aReponseList = new ArrayList <Ebms3SignalMessage> ();
    aReponseList.add (aErrorResponse);
    aResponse.setSignalMessage (aReponseList);
    // TODO Send SignalMessage (aResponse) back
  }

  public boolean validatePOJO (final Ebms3Messaging aMessage)
  {

    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final CollectingExceptionCallback <JAXBException> aExHdl = new CollectingExceptionCallback<> ();
    final String test = Ebms3WriterBuilder.ebms3Messaging ()
                                          .setValidationEventHandler (aCVEH)
                                          .setExceptionHandler (aExHdl)
                                          .getAsString (aMessage);
    if (aCVEH.getResourceErrors ().containsAtLeastOneError () || aExHdl.hasException ())
    {
      // TODO Switch to logger
      System.out.println (aExHdl.getException ().getCause ());
      return false;
    }
    return true;

  }
}
