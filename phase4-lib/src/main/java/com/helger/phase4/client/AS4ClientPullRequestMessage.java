/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.w3c.dom.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.model.message.AS4PullRequestMessage;
import com.helger.phase4.model.message.EAS4MessageType;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.xsds.xmldsig.ReferenceType;

/**
 * AS4 client for {@link AS4PullRequestMessage} objects.
 *
 * @author Philip Helger
 */
public class AS4ClientPullRequestMessage extends AbstractAS4ClientSignalMessage <AS4ClientPullRequestMessage>
{
  private String m_sMPC;

  public AS4ClientPullRequestMessage (@Nonnull @WillNotClose final AS4ResourceHelper aResHelper)
  {
    super (EAS4MessageType.PULL_REQUEST, aResHelper);
  }

  @Nullable
  public final String getMPC ()
  {
    return m_sMPC;
  }

  @Nonnull
  public final AS4ClientPullRequestMessage setMPC (@Nullable final String sMPC)
  {
    m_sMPC = sMPC;
    return this;
  }

  private void _checkMandatoryAttributes ()
  {
    // SoapVersion can never be null
    if (StringHelper.hasNoText (m_sMPC))
      throw new IllegalStateException ("An MPC has to be present");
  }

  @Override
  public AS4ClientBuiltMessage buildMessage (@Nonnull @Nonempty final String sMessageID,
                                             @Nullable final IAS4ClientBuildMessageCallback aCallback) throws WSSecurityException
  {
    _checkMandatoryAttributes ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (sMessageID,
                                                                                            getRefToMessageID (),
                                                                                            ensureSendingDateTime ().getSendingDateTime ());

    final AS4PullRequestMessage aPullRequest = AS4PullRequestMessage.create (getSoapVersion (),
                                                                             aEbms3MessageInfo,
                                                                             m_sMPC,
                                                                             any ().getClone ());

    if (aCallback != null)
      aCallback.onAS4Message (aPullRequest);

    final Document aPureSoapDoc = aPullRequest.getAsSoapDocument ();

    if (aCallback != null)
      aCallback.onSoapDocument (aPureSoapDoc);

    final Document aDoc;
    ICommonsList <ReferenceType> aCreatedDSReferences = null;
    if (signingParams ().isSigningEnabled ())
    {
      final IAS4CryptoFactory aCryptoFactorySign = internalGetCryptoFactorySign ();

      final boolean bMustUnderstand = true;
      final Document aSignedSoapDoc = AS4Signer.createSignedMessage (aCryptoFactorySign,
                                                                     aPureSoapDoc,
                                                                     getSoapVersion (),
                                                                     aPullRequest.getMessagingID (),
                                                                     null,
                                                                     getAS4ResourceHelper (),
                                                                     bMustUnderstand,
                                                                     signingParams ().getClone ());

      // Extract the created references
      aCreatedDSReferences = MessageHelperMethods.getAllDSigReferences (aSignedSoapDoc);

      if (aCallback != null)
        aCallback.onSignedSoapDocument (aSignedSoapDoc);

      aDoc = aSignedSoapDoc;
    }
    else
    {
      aDoc = aPureSoapDoc;
    }

    // Wrap SOAP XML
    return new AS4ClientBuiltMessage (sMessageID,
                                      new HttpXMLEntity (aDoc, getSoapVersion ().getMimeType ()),
                                      aCreatedDSReferences);
  }
}
