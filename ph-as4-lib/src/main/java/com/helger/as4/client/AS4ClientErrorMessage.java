package com.helger.as4.client;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.w3c.dom.Document;

import com.helger.as4.error.IEbmsError;
import com.helger.as4.messaging.domain.AS4ErrorMessage;
import com.helger.as4.messaging.domain.CreateErrorMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.string.StringHelper;

public class AS4ClientErrorMessage extends AbstractAS4ClientSignalMessage
{
  private final ICommonsList <Ebms3Error> m_aErrorMessages = new CommonsArrayList<> ();
  private String m_sRefToMessageId;

  public AS4ClientErrorMessage ()
  {}

  public void addErrorMessage (@Nonnull final IEbmsError aError, @Nonnull final Locale aLocale)
  {
    ValueEnforcer.notNull (aError, "Error");
    ValueEnforcer.notNull (aLocale, "Locale");

    addErrorMessage (aError.getAsEbms3Error (aLocale, null));
  }

  public void addErrorMessage (@Nonnull final Ebms3Error aError)
  {
    ValueEnforcer.notNull (aError, "Error");

    m_aErrorMessages.add (aError);
  }

  @Nullable
  public String getRefToMessageId ()
  {
    return m_sRefToMessageId;
  }

  public void setRefToMessageId (@Nullable final String sRefToMessageId)
  {
    m_sRefToMessageId = sRefToMessageId;
  }

  private void _checkMandatoryAttributes ()
  {
    if (getSOAPVersion () == null)
      throw new IllegalStateException ("A SOAPVersion must be set.");

    if (m_aErrorMessages.isEmpty ())
      throw new IllegalStateException ("No Errors specified!");

    if (StringHelper.hasNoText (m_sRefToMessageId))
      throw new IllegalStateException ("No reference to a message set.");
  }

  @Override
  public HttpEntity buildMessage () throws Exception
  {
    _checkMandatoryAttributes ();

    // Create a new message ID for each build!
    final String sMessageID = StringHelper.getConcatenatedOnDemand (getMessageIDPrefix (),
                                                                    '@',
                                                                    MessageHelperMethods.createRandomMessageID ());

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (sMessageID,
                                                                                            m_sRefToMessageId);

    final AS4ErrorMessage aErrorMsg = CreateErrorMessage.createErrorMessage (getSOAPVersion (),
                                                                             aEbms3MessageInfo,
                                                                             m_aErrorMessages);

    final Document aDoc = aErrorMsg.getAsSOAPDocument ();

    // Wrap SOAP XML
    return new StringEntity (AS4XMLHelper.serializeXML (aDoc));
  }
}
