package com.helger.as4server.message;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3Receipt;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.messaging.AS4ReceiptMessage;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.xml.ChildElementIterator;
import com.helger.xml.XMLHelper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings ("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
public class CreateReceiptMessage
{
  // TODO maybe find a better way
  private static Node _findChildElement (@Nullable final Node aStart, @Nonnull final String sLocalName)
  {
    return XMLHelper.getFirstChildElementOfName (aStart, sLocalName);
  }

  private static Node _findChildElement (@Nullable final Node aStart,
                                         @Nonnull final String sNamespaceURI,
                                         @Nonnull final String sLocalName)
  {
    return XMLHelper.getFirstChildElementOfName (aStart, sNamespaceURI, sLocalName);
  }

  private static void _findAllChildElements (@Nullable final Node aStart,
                                             @Nonnull final String sNamespaceURI,
                                             @Nonnull final String sLocalName,
                                             @Nonnull final Collection <Node> aTarget)
  {
    new ChildElementIterator (aStart).findAll (XMLHelper.filterElementWithNamespaceAndLocalName (sNamespaceURI,
                                                                                                 sLocalName),
                                               aTarget::add);
  }

  private String _findRefToMessageId (@Nonnull final Document aUserMessage)
  {
    {
      Node aNext = _findChildElement (aUserMessage.getDocumentElement (), "Header");
      aNext = _findChildElement (aNext, CAS4.EBMS_NS, "Messaging");
      aNext = _findChildElement (aNext, CAS4.EBMS_NS, "UserMessage");
      aNext = _findChildElement (aNext, CAS4.EBMS_NS, "MessageInfo");
      aNext = _findChildElement (aNext, CAS4.EBMS_NS, "MessageId");
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
      aNext = _findChildElement (aNext, CAS4.WSSE_NS, "Security");
      aNext = _findChildElement (aNext, CAS4.DS_NS, "Signature");
      aNext = _findChildElement (aNext, CAS4.DS_NS, "SignedInfo");
      if (aNext != null)
        _findAllChildElements (aNext, CAS4.DS_NS, "Reference", aDSRefs);
    }
    return aDSRefs;
  }

  @Nonnull
  public AS4ReceiptMessage createReceiptMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                                 @Nonnull final Ebms3MessageInfo aEbms3MessageInfo,
                                                 @Nonnull final Document aUserMessage)
  {
    aEbms3MessageInfo.setRefToMessageId (_findRefToMessageId (aUserMessage));

    final ICommonsList <Node> aDSRefs = _getAllReferences (aUserMessage);

    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();

    // Message Info
    aSignalMessage.setMessageInfo (aEbms3MessageInfo);

    // PullRequest
    final Ebms3Receipt aEbms3Receipt = new Ebms3Receipt ();
    for (final Node aRef : aDSRefs)
      aEbms3Receipt.addAny (aRef.cloneNode (true));
    aSignalMessage.setReceipt (aEbms3Receipt);

    return new AS4ReceiptMessage (eSOAPVersion, aSignalMessage);
  }

  // TODO ReftomessageID maybe not needed here since, it comes with the
  // usermessage
  public Ebms3MessageInfo createEbms3MessageInfo (@Nonnull final String sMessageId,
                                                  @Nullable final String sRefToMessageId)
  {
    return MessageHelperMethods.createEbms3MessageInfo (sMessageId, sRefToMessageId);
  }
}
