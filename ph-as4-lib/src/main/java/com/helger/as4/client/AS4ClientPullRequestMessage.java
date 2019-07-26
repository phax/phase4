/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;

import org.w3c.dom.Document;

import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.http.HttpXMLEntity;
import com.helger.as4.messaging.crypto.AS4Signer;
import com.helger.as4.messaging.domain.AS4PullRequestMessage;
import com.helger.as4.messaging.domain.AbstractAS4Message;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.util.AS4ResourceHelper;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.StringHelper;

/**
 * AS4 client for {@link AS4PullRequestMessage} objects.
 *
 * @author Philip Helger
 */
public class AS4ClientPullRequestMessage extends AbstractAS4ClientSignalMessage
{
  private final AS4ResourceHelper m_aResHelper;
  private String m_sMPC;

  public AS4ClientPullRequestMessage (@Nonnull @WillNotClose final AS4ResourceHelper aResHelper)
  {
    ValueEnforcer.notNull (aResHelper, "ResHelper");
    m_aResHelper = aResHelper;
  }

  @Nonnull
  public final AS4ResourceHelper getAS4ResourceHelper ()
  {
    return m_aResHelper;
  }

  private void _checkMandatoryAttributes ()
  {
    if (getSOAPVersion () == null)
      throw new IllegalStateException ("A SOAPVersion must be set.");
    if (StringHelper.hasNoText (m_sMPC))
      throw new IllegalStateException ("A MPC has to be present");
  }

  @Override
  public AS4BuiltMessage buildMessage (@Nullable final Consumer <? super AbstractAS4Message <?>> aMsgConsumer) throws Exception
  {
    _checkMandatoryAttributes ();

    final String sMessageID = createMessageID ();
    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (sMessageID,
                                                                                            getRefToMessageID ());

    final AS4PullRequestMessage aPullRequest = AS4PullRequestMessage.create (getSOAPVersion (),
                                                                             aEbms3MessageInfo,
                                                                             m_sMPC,
                                                                             getAllAny ());

    if (aMsgConsumer != null)
      aMsgConsumer.accept (aPullRequest);

    Document aDoc = aPullRequest.getAsSOAPDocument ();

    if (signingParams ().isSigningEnabled ())
    {
      final AS4CryptoFactory aCryptoFactory = internalCreateCryptoFactory ();

      final boolean bMustUnderstand = true;
      aDoc = AS4Signer.createSignedMessage (aCryptoFactory,
                                            aDoc,
                                            getSOAPVersion (),
                                            aPullRequest.getMessagingID (),
                                            null,
                                            m_aResHelper,
                                            bMustUnderstand,
                                            signingParams ().getClone ());
    }

    // Wrap SOAP XML
    return new AS4BuiltMessage (sMessageID, new HttpXMLEntity (aDoc, getSOAPVersion ()));
  }

  @Nullable
  public final String getMPC ()
  {
    return m_sMPC;
  }

  public final void setMPC (@Nullable final String sMPC)
  {
    m_sMPC = sMPC;
  }
}
