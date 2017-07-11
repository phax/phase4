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

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.http.HttpXMLEntity;
import com.helger.as4.messaging.domain.AS4ReceiptMessage;
import com.helger.as4.messaging.domain.CreateReceiptMessage;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.ValueEnforcer;

public class AS4ClientReceiptMessage extends AbstractAS4ClientSignalMessage
{
  private final AS4ResourceManager m_aResMgr;
  private boolean m_bNonRepudiation = false;
  private Node m_aSOAPDocument;
  private Ebms3UserMessage m_aEbms3UserMessage;
  private boolean m_bReceiptShouldBeSigned = false;

  public AS4ClientReceiptMessage ()
  {
    this (new AS4ResourceManager ());
  }

  public AS4ClientReceiptMessage (@Nonnull final AS4ResourceManager aResMgr)
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

    if (m_aSOAPDocument == null && m_aEbms3UserMessage == null)
      throw new IllegalStateException ("A SOAPDocument or a Ebms3UserMessage has to be set.");

    if (m_bNonRepudiation && m_aSOAPDocument == null)
      throw new IllegalStateException ("Nonrepudiation only works in conjunction with a set SOAPDocument.");

    if (!m_bNonRepudiation && m_aEbms3UserMessage == null)
      throw new IllegalStateException ("Ebms3UserMessage has to be set, if the SOAPDocument is not signed.");

  }

  @Override
  public HttpXMLEntity buildMessage () throws Exception
  {
    _checkMandatoryAttributes ();

    final AS4ReceiptMessage aReceiptMsg = CreateReceiptMessage.createReceiptMessage (getSOAPVersion (),
                                                                                     m_aEbms3UserMessage,
                                                                                     m_aSOAPDocument,
                                                                                     m_bNonRepudiation);

    Document aDoc = aReceiptMsg.getAsSOAPDocument ();

    final boolean bSign = getCryptoAlgorithmSign () != null && getCryptoAlgorithmSignDigest () != null;

    if (m_bReceiptShouldBeSigned && bSign)
    {
      final AS4CryptoFactory aCryptoFactory = internalCreateCryptoFactory ();

      final SignedMessageCreator aCreator = new SignedMessageCreator (aCryptoFactory);
      final boolean bMustUnderstand = true;
      aDoc = aCreator.createSignedMessage (aDoc,
                                           getSOAPVersion (),
                                           null,
                                           m_aResMgr,
                                           bMustUnderstand,
                                           getCryptoAlgorithmSign (),
                                           getCryptoAlgorithmSignDigest ());
    }

    // Wrap SOAP XML
    return new HttpXMLEntity (aDoc);
  }

  /**
   * Default value is false.
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
  public void setSOAPDocument (final Node aSOAPDocument)
  {
    m_aSOAPDocument = aSOAPDocument;
  }

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
  public void setEbms3UserMessage (final Ebms3UserMessage aEbms3UserMessage)
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
