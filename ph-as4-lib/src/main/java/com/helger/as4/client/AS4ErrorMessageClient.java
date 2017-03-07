package com.helger.as4.client;

import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.w3c.dom.Document;

import com.helger.as4.error.EEbmsError;
import com.helger.as4.messaging.domain.AS4ErrorMessage;
import com.helger.as4.messaging.domain.CreateErrorMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.string.StringHelper;

public class AS4ErrorMessageClient extends AS4SignalmessageClient
{
  private final ICommonsList <Ebms3Error> aErrorMessages = new CommonsArrayList <> ();

  @Override
  public HttpEntity buildMessage () throws Exception
  {
    // Create a new message ID for each build!
    final String sMessageID = StringHelper.getConcatenatedOnDemand (getMessageIDPrefix (),
                                                                    '@',
                                                                    MessageHelperMethods.createRandomMessageID ());

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (sMessageID, null);

    final AS4ErrorMessage aErrorMsg = CreateErrorMessage.createErrorMessage (getSOAPVersion (),
                                                                             aEbms3MessageInfo,
                                                                             aErrorMessages);

    final Document aDoc = aErrorMsg.getAsSOAPDocument ();

    // Wrap SOAP XML
    return new StringEntity (AS4XMLHelper.serializeXML (aDoc));
  }

  public void addErrorMessage (final EEbmsError aError)
  {
    aErrorMessages.add (aError.getAsEbms3Error (Locale.US, null));
  }
}
