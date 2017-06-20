/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
  private final ICommonsList <Ebms3Error> m_aErrorMessages = new CommonsArrayList <> ();
  private String m_sRefToMessageId;

  public AS4ClientErrorMessage ()
  {}

  public void addErrorMessage (@Nonnull final IEbmsError aError, @Nonnull final Locale aLocale)
  {
    ValueEnforcer.notNull (aError, "Error");
    ValueEnforcer.notNull (aLocale, "Locale");

    addErrorMessage (aError.getAsEbms3Error (aLocale, getRefToMessageId ()));
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
