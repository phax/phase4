package com.helger.as4server.message;

import javax.annotation.Nonnull;

import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.message.AS4ErrorMessage;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.collection.ext.ICommonsList;

public class CreateErrorMessage
{
  @Nonnull
  public AS4ErrorMessage createErrorMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                             @Nonnull final Ebms3MessageInfo aEbms3MessageInfo,
                                             @Nonnull final ICommonsList <Ebms3Error> aErrorMessages)
  {
    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();

    // Message Info
    aSignalMessage.setMessageInfo (aEbms3MessageInfo);

    // Error Message
    aSignalMessage.setError (aErrorMessages);

    final AS4ErrorMessage ret = new AS4ErrorMessage (eSOAPVersion, aSignalMessage);
    return ret;
  }

  public Ebms3MessageInfo createEbms3MessageInfo (@Nonnull final String sMessageId)
  {
    return MessageHelperMethods.createEbms3MessageInfo (sMessageId, null);
  }
}
