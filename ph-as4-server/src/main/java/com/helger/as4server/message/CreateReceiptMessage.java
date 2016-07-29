package com.helger.as4server.message;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3Receipt;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.marshaller.Ebms3WriterBuilder;
import com.helger.as4lib.soap11.Soap11Body;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.as4lib.soap11.Soap11Header;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.xml.XMLHelper;

public class CreateReceiptMessage
{
  // TODO maybe find a better way
  private static Node _findChildElement (@Nullable final Node aStart, @Nonnull final String sLocalName)
  {
    final NodeList aNL = aStart == null ? null : aStart.getChildNodes ();
    if (aNL != null)
    {
      final int nMax = aNL.getLength ();
      for (int i = 0; i < nMax; ++i)
      {
        final Node aNode = aNL.item (i);
        if (aNode.getNodeType () == Node.ELEMENT_NODE && aNode.getLocalName ().equals (sLocalName))
        {
          return aNode;
        }
      }
    }
    return null;
  }

  private static Node _findChildElement (@Nullable final Node aStart,
                                         @Nonnull final String sNamespaceURI,
                                         @Nonnull final String sLocalName)
  {
    final NodeList aNL = aStart == null ? null : aStart.getChildNodes ();
    if (aNL != null)
    {
      final int nMax = aNL.getLength ();
      for (int i = 0; i < nMax; ++i)
      {
        final Node aNode = aNL.item (i);
        if (aNode.getNodeType () == Node.ELEMENT_NODE &&
            XMLHelper.hasNamespaceURI (aNode, sNamespaceURI) &&
            aNode.getLocalName ().equals (sLocalName))
        {
          return aNode;
        }
      }
    }
    return null;
  }

  private static void _findAllChildElements (@Nullable final Node aStart,
                                             @Nonnull final String sNamespaceURI,
                                             @Nonnull final String sLocalName,
                                             @Nonnull final Collection <Node> aTarget)
  {
    final NodeList aNL = aStart == null ? null : aStart.getChildNodes ();
    if (aNL != null)
    {
      final int nMax = aNL.getLength ();
      for (int i = 0; i < nMax; ++i)
      {
        final Node aNode = aNL.item (i);
        if (aNode.getNodeType () == Node.ELEMENT_NODE &&
            XMLHelper.hasNamespaceURI (aNode, sNamespaceURI) &&
            aNode.getLocalName ().equals (sLocalName))
        {
          aTarget.add (aNode);
        }
      }
    }
  }

  private String _findRefToMessageId (@Nonnull final Document aUserMessage)
  {
    {
      Node aNext = _findChildElement (aUserMessage.getDocumentElement (), "Header");
      aNext = _findChildElement (aNext, MessageHelperMethods.EBMS_NS, "Messaging");
      aNext = _findChildElement (aNext, MessageHelperMethods.EBMS_NS, "UserMessage");
      aNext = _findChildElement (aNext, MessageHelperMethods.EBMS_NS, "MessageInfo");
      aNext = _findChildElement (aNext, MessageHelperMethods.EBMS_NS, "MessageId");
      if (aNext != null)
        return (aNext.getFirstChild ().getNodeValue ());
    }
    return null;
  }

  private ICommonsList <Node> _getAllReferences (@Nonnull final Document aUserMessage)
  {
    final ICommonsList <Node> aDSRefs = new CommonsArrayList<> ();
    {
      Node aNext = _findChildElement (aUserMessage.getDocumentElement (), "Header");
      aNext = _findChildElement (aNext, MessageHelperMethods.WSSE_NS, "Security");
      aNext = _findChildElement (aNext, MessageHelperMethods.DS_NS, "Signature");
      aNext = _findChildElement (aNext, MessageHelperMethods.DS_NS, "SignedInfo");
      if (aNext != null)
        _findAllChildElements (aNext, MessageHelperMethods.DS_NS, "Reference", aDSRefs);
    }
    return aDSRefs;
  }

  public Document createReceiptMessage (@Nonnull final Ebms3MessageInfo aEbms3MessageInfo,
                                        @Nonnull final Document aUserMessage)
  {
    aEbms3MessageInfo.setRefToMessageId (_findRefToMessageId (aUserMessage));

    final ICommonsList <Node> aDSRefs = _getAllReferences (aUserMessage);

    // Creating SOAP
    final Soap11Envelope aSoapEnv = new Soap11Envelope ();
    aSoapEnv.setHeader (new Soap11Header ());
    aSoapEnv.setBody (new Soap11Body ());

    // Creating Message
    final Ebms3Messaging aMessage = new Ebms3Messaging ();
    // TODO needs to be set to false because holodeck throws error if it is set
    // to true
    aMessage.setS11MustUnderstand (Boolean.FALSE);
    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();

    // Message Info
    aSignalMessage.setMessageInfo (aEbms3MessageInfo);

    // PullRequest
    final Ebms3Receipt aEbms3Receipt = new Ebms3Receipt ();
    for (final Node aRef : aDSRefs)
      aEbms3Receipt.addAny (aRef.cloneNode (true));
    aSignalMessage.setReceipt (aEbms3Receipt);

    aMessage.addSignalMessage (aSignalMessage);

    // Adding the signal message to the existing soap
    final Document aEbms3Message = Ebms3WriterBuilder.ebms3Messaging ().getAsDocument (aMessage);
    aSoapEnv.getHeader ().addAny (aEbms3Message.getDocumentElement ());

    return Ebms3WriterBuilder.soap11 ().getAsDocument (aSoapEnv);
  }

  // TODO ReftomessageID maybe not needed here since, it comes with the
  // usermessage
  public Ebms3MessageInfo createEbms3MessageInfo (@Nonnull final String sMessageId,
                                                  @Nullable final String sRefToMessageId)
  {
    return MessageHelperMethods.createEbms3MessageInfo (sMessageId, sRefToMessageId);
  }
}
