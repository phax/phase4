/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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
package com.helger.phase4.client;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;

import org.w3c.dom.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.error.IEbmsError;
import com.helger.phase4.http.HttpXMLEntity;
import com.helger.phase4.messaging.domain.AS4ErrorMessage;
import com.helger.phase4.messaging.domain.EAS4MessageType;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * AS4 client for {@link AS4ErrorMessage} objects.
 *
 * @author Philip Helger
 */
public class AS4ClientErrorMessage extends AbstractAS4ClientSignalMessage <AS4ClientErrorMessage>
{
  private final ICommonsList <Ebms3Error> m_aErrorMessages = new CommonsArrayList <> ();

  public AS4ClientErrorMessage (@Nonnull @WillNotClose final AS4ResourceHelper aResHelper)
  {
    super (EAS4MessageType.ERROR_MESSAGE, aResHelper);
  }

  public final void addErrorMessage (@Nonnull final IEbmsError aError, @Nonnull final Locale aLocale)
  {
    ValueEnforcer.notNull (aError, "Error");
    ValueEnforcer.notNull (aLocale, "Locale");

    m_aErrorMessages.add (aError.errorBuilder (aLocale).refToMessageInError (getRefToMessageID ()).build ());
  }

  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <Ebms3Error> errorMessages ()
  {
    return m_aErrorMessages;
  }

  private void _checkMandatoryAttributes ()
  {
    // Soap version can never be null

    if (m_aErrorMessages.isEmpty ())
      throw new IllegalStateException ("No Errors specified!");
    if (m_aErrorMessages.containsAny (Objects::isNull))
      throw new IllegalStateException ("Errors may not contain null elements.");

    if (!hasRefToMessageID ())
      throw new IllegalStateException ("No reference to a message set.");
  }

  @Override
  public AS4ClientBuiltMessage buildMessage (@Nonnull @Nonempty final String sMessageID,
                                             @Nullable final IAS4ClientBuildMessageCallback aCallback) throws IOException
  {
    _checkMandatoryAttributes ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (sMessageID,
                                                                                            getRefToMessageID (),
                                                                                            ensureSendingDateTime ().getSendingDateTime ());

    final AS4ErrorMessage aErrorMsg = AS4ErrorMessage.create (getSoapVersion (), aEbms3MessageInfo, m_aErrorMessages);

    if (aCallback != null)
      aCallback.onAS4Message (aErrorMsg);

    final Document aDoc = aErrorMsg.getAsSoapDocument ();

    if (aCallback != null)
      aCallback.onSoapDocument (aDoc);

    // Wrap SOAP XML
    return new AS4ClientBuiltMessage (sMessageID, new HttpXMLEntity (aDoc, getSoapVersion ().getMimeType ()));
  }
}
