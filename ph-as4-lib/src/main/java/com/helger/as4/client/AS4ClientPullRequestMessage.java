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

import javax.annotation.Nonnull;

import org.w3c.dom.Document;

import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.http.HttpXMLEntity;
import com.helger.as4.messaging.domain.AS4PullRequestMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.util.AS4ResourceManager;
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
  private final AS4ResourceManager m_aResMgr;
  private String m_sMPC;

  public AS4ClientPullRequestMessage ()
  {
    this (new AS4ResourceManager ());
  }

  public AS4ClientPullRequestMessage (@Nonnull final AS4ResourceManager aResMgr)
  {
    ValueEnforcer.notNull (aResMgr, "ResMgr");
    m_aResMgr = aResMgr;
  }

  @Nonnull
  public AS4ResourceManager getAS4ResourceManager ()
  {
    return m_aResMgr;
  }

  private void _checkMandatoryAttributes ()
  {
    if (getSOAPVersion () == null)
      throw new IllegalStateException ("A SOAPVersion must be set.");
    if (StringHelper.hasNoText (m_sMPC))
      throw new IllegalStateException ("A MPC has to be present");
  }

  @Override
  public BuiltMessage buildMessage () throws Exception
  {
    _checkMandatoryAttributes ();

    final String sMessageID = createMessageID ();
    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (sMessageID, null);

    final AS4PullRequestMessage aPullRequest = AS4PullRequestMessage.create (getSOAPVersion (),
                                                                             aEbms3MessageInfo,
                                                                             m_sMPC,
                                                                             getAllAny ());

    Document aDoc = aPullRequest.getAsSOAPDocument ();

    final boolean bSign = getCryptoAlgorithmSign () != null && getCryptoAlgorithmSignDigest () != null;

    if (bSign)
    {
      final AS4CryptoFactory aCryptoFactory = internalCreateCryptoFactory ();

      final boolean bMustUnderstand = true;
      aDoc = SignedMessageCreator.createSignedMessage (aCryptoFactory,
                                                       aDoc,
                                                       getSOAPVersion (),
                                                       aPullRequest.getMessagingID (),
                                                       null,
                                                       m_aResMgr,
                                                       bMustUnderstand,
                                                       getCryptoAlgorithmSign (),
                                                       getCryptoAlgorithmSignDigest ());
    }

    // Wrap SOAP XML
    return new BuiltMessage (sMessageID, new HttpXMLEntity (aDoc, getSOAPVersion ()));
  }

  public String getMPC ()
  {
    return m_sMPC;
  }

  public void setMPC (final String sMPC)
  {
    m_sMPC = sMPC;
  }

}
