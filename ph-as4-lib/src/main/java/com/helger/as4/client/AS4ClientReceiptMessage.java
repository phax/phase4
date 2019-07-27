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
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.http.HttpXMLEntity;
import com.helger.as4.messaging.crypto.AS4Signer;
import com.helger.as4.messaging.domain.AS4ReceiptMessage;
import com.helger.as4.util.AS4ResourceHelper;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.ValueEnforcer;

/**
 * AS4 client for {@link AS4ReceiptMessage} objects.
 *
 * @author Philip Helger
 */
public class AS4ClientReceiptMessage extends AbstractAS4ClientSignalMessage
{
  private final AS4ResourceHelper m_aResHelper;
  private boolean m_bNonRepudiation = false;
  private Node m_aSOAPDocument;
  private Ebms3UserMessage m_aEbms3UserMessage;
  private boolean m_bReceiptShouldBeSigned = false;

  public AS4ClientReceiptMessage (@Nonnull @WillNotClose final AS4ResourceHelper aResHelper)
  {
    ValueEnforcer.notNull (aResHelper, "ResHelper");
    m_aResHelper = aResHelper;
  }

  @Nonnull
  public AS4ResourceHelper getAS4ResourceHelper ()
  {
    return m_aResHelper;
  }

  private void _checkMandatoryAttributes ()
  {
    if (getSOAPVersion () == null)
      throw new IllegalStateException ("A SOAPVersion must be set.");

    if (m_aSOAPDocument == null && m_aEbms3UserMessage == null)
      throw new IllegalStateException ("A SOAPDocument or a Ebms3UserMessage has to be set.");

    if (m_bNonRepudiation && m_aSOAPDocument == null)
      throw new IllegalStateException ("Nonrepudiation only works in conjunction with a set SOAPDocument.");

    if (!m_bNonRepudiation && m_aEbms3UserMessage == null)
      throw new IllegalStateException ("Ebms3UserMessage has to be set, if the SOAPDocument is not signed.");
  }

  @Override
  public AS4BuiltMessage buildMessage (@Nullable final IAS4ClientBuildMessageCallback aCallback) throws Exception
  {
    _checkMandatoryAttributes ();

    final String sMessageID = createMessageID ();
    final AS4ReceiptMessage aReceiptMsg = AS4ReceiptMessage.create (getSOAPVersion (),
                                                                    sMessageID,
                                                                    m_aEbms3UserMessage,
                                                                    m_aSOAPDocument,
                                                                    m_bNonRepudiation);

    if (aCallback != null)
      aCallback.onAS4Message (aReceiptMsg);

    final Document aPureDoc = aReceiptMsg.getAsSOAPDocument ();

    if (aCallback != null)
      aCallback.onSOAPDocument (aPureDoc);

    Document aDoc = aPureDoc;

    if (m_bReceiptShouldBeSigned && signingParams ().isSigningEnabled ())
    {
      final AS4CryptoFactory aCryptoFactory = internalCreateCryptoFactory ();

      final boolean bMustUnderstand = true;
      final Document aSignedDoc = AS4Signer.createSignedMessage (aCryptoFactory,
                                                                 aDoc,
                                                                 getSOAPVersion (),
                                                                 aReceiptMsg.getMessagingID (),
                                                                 null,
                                                                 m_aResHelper,
                                                                 bMustUnderstand,
                                                                 signingParams ().getClone ());

      if (aCallback != null)
        aCallback.onSignedSOAPDocument (aSignedDoc);

      aDoc = aSignedDoc;
    }

    // Wrap SOAP XML
    return new AS4BuiltMessage (sMessageID, new HttpXMLEntity (aDoc, getSOAPVersion ()));
  }

  /**
   * Default value is <code>false</code>
   *
   * @return if nonrepudiation is used or not
   */
  public boolean isNonRepudiation ()
  {
    return m_bNonRepudiation;
  }

  public void setNonRepudiation (final boolean bNonRepudiation)
  {
    m_bNonRepudiation = bNonRepudiation;
  }

  @Nullable
  public Node getSOAPDocument ()
  {
    return m_aSOAPDocument;
  }

  /**
   * As node set the usermessage if it is signed, so the references can be
   * counted and used in non repudiation.
   *
   * @param aSOAPDocument
   *        Signed UserMessage
   */
  public void setSOAPDocument (@Nullable final Node aSOAPDocument)
  {
    m_aSOAPDocument = aSOAPDocument;
  }

  @Nullable
  public Ebms3UserMessage getEbms3UserMessage ()
  {
    return m_aEbms3UserMessage;
  }

  /**
   * Needs to be set to refer to the message which this receipt is the response
   * and if nonrepudiation is not used, to fill the receipt content
   *
   * @param aEbms3UserMessage
   *        UserMessage which this receipt should be the response for
   */
  public void setEbms3UserMessage (@Nullable final Ebms3UserMessage aEbms3UserMessage)
  {
    m_aEbms3UserMessage = aEbms3UserMessage;
  }

  public boolean isReceiptShouldBeSigned ()
  {
    return m_bReceiptShouldBeSigned;
  }

  public void setReceiptShouldBeSigned (final boolean bReceiptShouldBeSigned)
  {
    m_bReceiptShouldBeSigned = bReceiptShouldBeSigned;
  }
}
