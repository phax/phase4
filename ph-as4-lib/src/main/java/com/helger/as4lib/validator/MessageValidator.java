package com.helger.as4lib.validator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.error.ErrorConverter;
import com.helger.as4lib.marshaller.Ebms3ReaderBuilder;
import com.helger.as4lib.marshaller.Ebms3WriterBuilder;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.jaxb.validation.CollectingValidationEventHandler;

public class MessageValidator
{
  // TODO Split Message and SOAP CHECK? SOAP currently treated the same as xml
  // error
  public void validateXML (final File aXML)
  {
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final Soap11Envelope aEnv = Ebms3ReaderBuilder.soap11 ().setValidationEventHandler (aCVEH).read (aXML);
    final Ebms3Messaging aMessage = Ebms3ReaderBuilder.ebms3Messaging ()
                                                      .setValidationEventHandler (aCVEH)
                                                      .read ((Element) aEnv.getHeader ().getAnyAtIndex (0));
    if (aCVEH.getResourceErrors ().containsAtLeastOneError ())
    {
      final List <EEbmsError> aOccurredErrors = new ArrayList <EEbmsError> ();
      aOccurredErrors.add (EEbmsError.EBMS_INVALID_HEADER);
      sendErrorResponse (aOccurredErrors);
    }
  }

  public void sendErrorResponse (final List <EEbmsError> aOccurredErrors)
  {
    final Ebms3Messaging aResponse = new Ebms3Messaging ();
    final Ebms3SignalMessage aErrorResponse = new Ebms3SignalMessage ();
    final List <Ebms3Error> aErrorList = new ArrayList <Ebms3Error> ();
    // TODO how to get Messageinfo for response?
    // aErrorResponse.setMessageInfo (value);
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

  public void validatePOJO (final Ebms3Messaging aMessage)
  {
    final String aConvertedMessage = Ebms3WriterBuilder.ebms3Messaging ().getAsString (aMessage);
    System.out.println (aConvertedMessage);
  }
}
