package com.helger.as4.client;

import javax.annotation.Nonnull;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4.messaging.domain.AS4ReceiptMessage;
import com.helger.as4.messaging.domain.CreateReceiptMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.string.StringHelper;

public class AS4ReceiptMessageClient extends AS4SignalmessageClient
{

  private final AS4ResourceManager m_aResMgr;
  private boolean bNonRepudiation = false;
  private Node aSOAPDocument;
  private Ebms3UserMessage aEbms3UserMessage;
  private final boolean bReceiptShouldBeSigned = false;

  public AS4ReceiptMessageClient (@Nonnull final AS4ResourceManager aResMgr)
  {
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

    if (aSOAPDocument == null && aEbms3UserMessage == null)
      throw new IllegalStateException ("A SOAPDocument or a Ebms3UserMessage has to be set.");

    if (bNonRepudiation && aSOAPDocument == null)
      throw new IllegalStateException ("Nonrepudiation only works in conjunction with a set SOAPDocument.");

  }

  @Override
  public HttpEntity buildMessage () throws Exception
  {
    _checkMandatoryAttributes ();

    // Create a new message ID for each build!
    final String sMessageID = StringHelper.getConcatenatedOnDemand (getMessageIDPrefix (),
                                                                    '@',
                                                                    MessageHelperMethods.createRandomMessageID ());

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (sMessageID, null);

    final AS4ReceiptMessage aReceiptMsg = CreateReceiptMessage.createReceiptMessage (getSOAPVersion (),
                                                                                     aEbms3MessageInfo,
                                                                                     aEbms3UserMessage,
                                                                                     aSOAPDocument,
                                                                                     bNonRepudiation);

    Document aDoc = aReceiptMsg.getAsSOAPDocument ();

    if (bReceiptShouldBeSigned)
    {
      _checkKeystoreAttributes ();

      final SignedMessageCreator aCreator = new SignedMessageCreator ();
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
    return new StringEntity (AS4XMLHelper.serializeXML (aDoc));
  }

  /**
   * Default value is false.
   *
   * @return if nonrepudiation is used or not
   */
  public boolean isNonRepudiation ()
  {
    return bNonRepudiation;
  }

  public void setNonRepudiation (final boolean bNonRepudiation)
  {
    this.bNonRepudiation = bNonRepudiation;
  }

  public Node getSOAPDocument ()
  {
    return aSOAPDocument;
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
    this.aSOAPDocument = aSOAPDocument;
  }

  public Ebms3UserMessage getEbms3UserMessage ()
  {
    return aEbms3UserMessage;
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
    this.aEbms3UserMessage = aEbms3UserMessage;
  }

}
