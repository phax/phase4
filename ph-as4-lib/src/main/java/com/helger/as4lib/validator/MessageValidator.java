package com.helger.as4lib.validator;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.error.IEbmsError;
import com.helger.as4lib.marshaller.Ebms3ReaderBuilder;
import com.helger.as4lib.marshaller.Ebms3WriterBuilder;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.as4lib.soap12.Soap12Envelope;
import com.helger.commons.callback.exception.CollectingExceptionCallback;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.error.IResourceError;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.jaxb.validation.CollectingValidationEventHandler;
import com.helger.xml.NodeListIterator;

// TODO USE ESOAPVersion
public class MessageValidator
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (MessageValidator.class);

  // TODO Check P-Modes they should define which SOAP Version should be used for
  // the conversation
  public Document getSoapEnvelope (final IReadableResource aXML)
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
  public boolean validateXML (final IReadableResource aXML, @Nonnull final Locale aContentLocale)
  {
    final Document aDocument = getSoapEnvelope (aXML);
    if (aDocument != null)
    {
      for (final Node aChildNode : NodeListIterator.createChildNodeIterator (aDocument))
      {
        final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
        final Ebms3Messaging aMessage = Ebms3ReaderBuilder.ebms3Messaging ()
                                                          .setValidationEventHandler (aCVEH)
                                                          .read (aChildNode);

        if (aMessage == null || aCVEH.getResourceErrors ().containsAtLeastOneError ())
        {
          sendErrorResponse (new CommonsArrayList<> (EEbmsError.EBMS_INVALID_HEADER), aContentLocale);
          return false;
        }
      }
      return true;
    }
    return false;
  }

  public void sendErrorResponse (@Nonnull final Iterable <? extends IEbmsError> aOccurredErrors,
                                 @Nonnull final Locale aContentLocale)
  {
    final Ebms3Messaging aResponse = new Ebms3Messaging ();

    final Ebms3SignalMessage aErrorResponse = new Ebms3SignalMessage ();
    // TODO how to get Messageinfo for response?
    // aErrorResponse.setMessageInfo (value);
    // TODO set S11MustUnderstand or S12MustUnderstand depending on MessageInfo?
    // or through other means
    for (final IEbmsError aError : aOccurredErrors)
    {
      aErrorResponse.addError (aError.getAsEbms3Error (aContentLocale));
    }

    aResponse.addSignalMessage (aErrorResponse);
    // TODO Send SignalMessage (aResponse) back
  }

  public boolean validatePOJO (@Nonnull final Ebms3Messaging aMessage)
  {
    final CollectingValidationEventHandler aCVEH = new CollectingValidationEventHandler ();
    final CollectingExceptionCallback <JAXBException> aExHdl = new CollectingExceptionCallback<> ();
    final String test = Ebms3WriterBuilder.ebms3Messaging ()
                                          .setValidationEventHandler (aCVEH)
                                          .setExceptionHandler (aExHdl)
                                          .getAsString (aMessage);
    if (test == null || aCVEH.getResourceErrors ().containsAtLeastOneError () || aExHdl.hasException ())
    {
      if (aExHdl.hasException ())
        s_aLogger.error (aExHdl.getException ().getCause ().getMessage ());
      return false;
    }
    return true;

  }
}
