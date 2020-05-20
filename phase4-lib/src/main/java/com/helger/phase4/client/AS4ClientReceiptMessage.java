/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
import org.w3c.dom.Node;

import com.helger.commons.annotation.Nonempty;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.http.HttpXMLEntity;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.messaging.domain.AS4ReceiptMessage;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * AS4 client for {@link AS4ReceiptMessage} objects.
 *
 * @author Philip Helger
 */
public class AS4ClientReceiptMessage extends AbstractAS4ClientSignalMessage <AS4ClientReceiptMessage>
{
  private boolean m_bNonRepudiation = false;
  private Node m_aSoapDocument;
  private Ebms3UserMessage m_aEbms3UserMessage;
  private boolean m_bReceiptShouldBeSigned = false;

  public AS4ClientReceiptMessage (@Nonnull @WillNotClose final AS4ResourceHelper aResHelper)
  {
    super (aResHelper);
  }

  /**
   * Default value is <code>false</code>
   *
   * @return if non-repudiation is used or not
   */
  public final boolean isNonRepudiation ()
  {
    return m_bNonRepudiation;
  }

  @Nonnull
  public final AS4ClientReceiptMessage setNonRepudiation (final boolean bNonRepudiation)
  {
    m_bNonRepudiation = bNonRepudiation;
    return this;
  }

  @Nullable
  public final Node getSoapDocument ()
  {
    return m_aSoapDocument;
  }

  /**
   * As node set the usermessage if it is signed, so the references can be
   * counted and used in non repudiation.
   *
   * @param aSoapDocument
   *        Signed UserMessage
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientReceiptMessage setSoapDocument (@Nullable final Node aSoapDocument)
  {
    m_aSoapDocument = aSoapDocument;
    return this;
  }

  @Nullable
  public final Ebms3UserMessage getEbms3UserMessage ()
  {
    return m_aEbms3UserMessage;
  }

  /**
   * Needs to be set to refer to the message which this receipt is the response
   * and if non-repudiation is not used, to fill the receipt content
   *
   * @param aEbms3UserMessage
   *        UserMessage which this receipt should be the response for
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientReceiptMessage setEbms3UserMessage (@Nullable final Ebms3UserMessage aEbms3UserMessage)
  {
    m_aEbms3UserMessage = aEbms3UserMessage;
    return this;
  }

  public final boolean isReceiptShouldBeSigned ()
  {
    return m_bReceiptShouldBeSigned;
  }

  @Nonnull
  public final AS4ClientReceiptMessage setReceiptShouldBeSigned (final boolean bReceiptShouldBeSigned)
  {
    m_bReceiptShouldBeSigned = bReceiptShouldBeSigned;
    return this;
  }

  private void _checkMandatoryAttributes ()
  {
    if (getSoapVersion () == null)
      throw new IllegalStateException ("A SOAP version must be set.");

    if (m_aSoapDocument == null && m_aEbms3UserMessage == null)
      throw new IllegalStateException ("A SOAP document or a Ebms3UserMessage has to be set.");

    if (m_bNonRepudiation)
    {
      if (m_aSoapDocument == null)
        throw new IllegalStateException ("Nonrepudiation only works in conjunction with a set SOAP document.");
    }
    else
    {
      if (m_aEbms3UserMessage == null)
        throw new IllegalStateException ("Ebms3UserMessage has to be set, if the SOAP document is not signed.");
    }
  }

  @Override
  public AS4ClientBuiltMessage buildMessage (@Nonnull @Nonempty final String sMessageID,
                                             @Nullable final IAS4ClientBuildMessageCallback aCallback) throws WSSecurityException
  {
    _checkMandatoryAttributes ();

    final AS4ReceiptMessage aReceiptMsg = AS4ReceiptMessage.create (getSoapVersion (),
                                                                    sMessageID,
                                                                    m_aEbms3UserMessage,
                                                                    m_aSoapDocument,
                                                                    m_bNonRepudiation);

    if (aCallback != null)
      aCallback.onAS4Message (aReceiptMsg);

    final Document aPureDoc = aReceiptMsg.getAsSoapDocument ();

    if (aCallback != null)
      aCallback.onSoapDocument (aPureDoc);

    Document aDoc = aPureDoc;

    if (m_bReceiptShouldBeSigned && signingParams ().isSigningEnabled ())
    {
      final IAS4CryptoFactory aCryptoFactory = internalCreateCryptoFactory ();

      final boolean bMustUnderstand = true;
      final Document aSignedDoc = AS4Signer.createSignedMessage (aCryptoFactory,
                                                                 aDoc,
                                                                 getSoapVersion (),
                                                                 aReceiptMsg.getMessagingID (),
                                                                 null,
                                                                 getAS4ResourceHelper (),
                                                                 bMustUnderstand,
                                                                 signingParams ().getClone ());

      if (aCallback != null)
        aCallback.onSignedSoapDocument (aSignedDoc);

      aDoc = aSignedDoc;
    }

    // Wrap SOAP XML
    return new AS4ClientBuiltMessage (sMessageID, new HttpXMLEntity (aDoc, getSoapVersion ().getMimeType ()));
  }
}
