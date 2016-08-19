package com.helger.as4server.message;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.ebms3header.Ebms3AgreementRef;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3From;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3PartInfo;
import com.helger.as4lib.ebms3header.Ebms3PartProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyId;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3Service;
import com.helger.as4lib.ebms3header.Ebms3To;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.marshaller.Ebms3WriterBuilder;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.soap11.Soap11Body;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.as4lib.soap11.Soap11Header;
import com.helger.as4lib.soap12.Soap12Body;
import com.helger.as4lib.soap12.Soap12Envelope;
import com.helger.as4lib.soap12.Soap12Header;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;

/**
 * With the help of this class an usermessage or parts of it can be created.
 *
 * @author bayerlma
 */
public class CreateUserMessage
{

  // TODO Payload as SOAP Body only supported
  public Document createUserMessage (@Nonnull final Ebms3MessageInfo aMessageInfo,
                                     @Nonnull final Ebms3PayloadInfo aEbms3PayloadInfo,
                                     @Nonnull final Ebms3CollaborationInfo aEbms3CollaborationInfo,
                                     @Nonnull final Ebms3PartyInfo aEbms3PartyInfo,
                                     @Nonnull final Ebms3MessageProperties aEbms3MessageProperties,
                                     @Nullable final String sPayloadPath,
                                     @Nonnull final ESOAPVersion eSOAPVersion) throws SAXException,
                                                                               IOException,
                                                                               ParserConfigurationException
  {

    // Creating Message
    final Ebms3Messaging aMessage = new Ebms3Messaging ();
    // TODO Needs to beset to 0 (equals false) since holodeck currently throws
    // a exception he does not understand mustUnderstand
    if (eSOAPVersion.equals (ESOAPVersion.SOAP_11))
      aMessage.setS11MustUnderstand (Boolean.FALSE);
    else
      aMessage.setS12MustUnderstand (Boolean.FALSE);

    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();

    // Party Information
    aUserMessage.setPartyInfo (aEbms3PartyInfo);

    // Collabration Information
    aUserMessage.setCollaborationInfo (aEbms3CollaborationInfo);

    // Properties
    aUserMessage.setMessageProperties (aEbms3MessageProperties);

    // Payload Information
    aUserMessage.setPayloadInfo (aEbms3PayloadInfo);

    // Message Info
    aUserMessage.setMessageInfo (aMessageInfo);

    aMessage.addUserMessage (aUserMessage);

    // Adding the user message to the existing soap
    final Document aEbms3Message = Ebms3WriterBuilder.ebms3Messaging ().getAsDocument (aMessage);
    return _createSOAPEnvelopeAsDocument (eSOAPVersion, aEbms3Message, sPayloadPath);
  }

  public Ebms3PartyInfo createEbms3PartyInfo (final String sFromRole,
                                              final String sFromPartyID,
                                              final String sToRole,
                                              final String sToPartyID)
  {
    final Ebms3PartyInfo aEbms3PartyInfo = new Ebms3PartyInfo ();

    // From => Sender
    final Ebms3From aEbms3From = new Ebms3From ();
    aEbms3From.setRole (sFromRole);
    ICommonsList <Ebms3PartyId> aEbms3PartyIdList = new CommonsArrayList<> ();
    Ebms3PartyId aEbms3PartyId = new Ebms3PartyId ();
    aEbms3PartyId.setValue (sFromPartyID);
    aEbms3PartyIdList.add (aEbms3PartyId);
    aEbms3From.setPartyId (aEbms3PartyIdList);
    aEbms3PartyInfo.setFrom (aEbms3From);

    // To => Receiver
    final Ebms3To aEbms3To = new Ebms3To ();
    aEbms3To.setRole (sToRole);
    aEbms3PartyIdList = new CommonsArrayList<> ();
    aEbms3PartyId = new Ebms3PartyId ();
    aEbms3PartyId.setValue (sToPartyID);
    aEbms3PartyIdList.add (aEbms3PartyId);
    aEbms3To.setPartyId (aEbms3PartyIdList);
    aEbms3PartyInfo.setTo (aEbms3To);
    return aEbms3PartyInfo;
  }

  public Ebms3CollaborationInfo createEbms3CollaborationInfo (final String sAction,
                                                              final String sServiceType,
                                                              final String sServiceValue,
                                                              final String sConversationID,
                                                              final String sAgreementRefPMode,
                                                              final String sAgreementRefValue)
  {
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = new Ebms3CollaborationInfo ();
    aEbms3CollaborationInfo.setAction (sAction);
    {
      final Ebms3Service aEbms3Service = new Ebms3Service ();
      aEbms3Service.setType (sServiceType);
      aEbms3Service.setValue (sServiceValue);
      aEbms3CollaborationInfo.setService (aEbms3Service);
    }
    aEbms3CollaborationInfo.setConversationId (sConversationID);
    {
      final Ebms3AgreementRef aEbms3AgreementRef = new Ebms3AgreementRef ();
      aEbms3AgreementRef.setPmode (sAgreementRefPMode);
      aEbms3AgreementRef.setValue (sAgreementRefValue);
      aEbms3CollaborationInfo.setAgreementRef (aEbms3AgreementRef);
    }
    return aEbms3CollaborationInfo;
  }

  public Ebms3MessageProperties createEbms3MessageProperties (final ICommonsList <Ebms3Property> aEbms3Properties)
  {
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    aEbms3MessageProperties.setProperty (aEbms3Properties);
    return aEbms3MessageProperties;
  }

  public Ebms3PayloadInfo createEbms3PayloadInfoEmpty ()
  {
    final Ebms3PayloadInfo aEbms3PayloadInfo = new Ebms3PayloadInfo ();
    aEbms3PayloadInfo.setPartInfo (new CommonsArrayList<> (new Ebms3PartInfo ()));
    return aEbms3PayloadInfo;
  }

  /**
   * TODO make dynamic ok<eb:PartInfo href="cid:test-xml"> <eb:PartProperties>
   * <eb:Property name="MimeType">application/xml</eb:Property>
   * <eb:Property name="CharacterSet">utf-8</eb:Property>
   * <eb:Property name="CompressionType">application/gzip</eb:Property>
   * </eb:PartProperties> </eb:PartInfo>
   *
   * @param aAttachments
   *        Used attachments
   * @return Never <code>null</code>.
   */
  @Nonnull
  public Ebms3PayloadInfo createEbms3PayloadInfo (@Nonnull final Iterable <? extends IAS4Attachment> aAttachments)
  {
    final Ebms3PayloadInfo aEbms3PayloadInfo = new Ebms3PayloadInfo ();
    for (final IAS4Attachment aAttachment : aAttachments)
    {
      final Ebms3PartProperties aEbms3PartProperties = new Ebms3PartProperties ();
      {
        final Ebms3Property aMimeType = new Ebms3Property ();
        aMimeType.setName ("MimeType");
        aMimeType.setValue (aAttachment.getMimeType ().getAsString ());
        aEbms3PartProperties.addProperty (aMimeType);
      }
      if (aAttachment.hasCharset ())
      {
        final Ebms3Property aCharacterSet = new Ebms3Property ();
        aCharacterSet.setName ("CharacterSet");
        aCharacterSet.setValue (aAttachment.getCharset ().name ());
        aEbms3PartProperties.addProperty (aCharacterSet);
      }
      if (aAttachment.hasCompressionMode ())
      {
        final Ebms3Property aCompressionType = new Ebms3Property ();
        aCompressionType.setName ("CompressionType");
        aCompressionType.setValue (aAttachment.getCompressionMode ().getMimeTypeAsString ());
        aEbms3PartProperties.addProperty (aCompressionType);
      }

      final Ebms3PartInfo aEbms3PartInfo = new Ebms3PartInfo ();
      aEbms3PartInfo.setHref ("cid:" + aAttachment.getID ());
      aEbms3PartInfo.setPartProperties (aEbms3PartProperties);
      aEbms3PayloadInfo.addPartInfo (aEbms3PartInfo);
    }
    return aEbms3PayloadInfo;
  }

  public Ebms3MessageInfo createEbms3MessageInfo (final String sMessageId)
  {
    return MessageHelperMethods.createEbms3MessageInfo (sMessageId, null);
  }

  private Document _createSOAPEnvelopeAsDocument (@Nonnull final ESOAPVersion eSOAPVersion,
                                                  final Document aEbms3Message,
                                                  final String sPayloadPath) throws SAXException,
                                                                             IOException,
                                                                             ParserConfigurationException
  {

    if (eSOAPVersion.equals (ESOAPVersion.SOAP_11))
    {
      // Creating SOAP 11 Envelope
      final Soap11Envelope aSoapEnv = new Soap11Envelope ();
      aSoapEnv.setHeader (new Soap11Header ());
      aSoapEnv.setBody (new Soap11Body ());
      aSoapEnv.getHeader ().addAny (aEbms3Message.getDocumentElement ());
      if (sPayloadPath != null)
        aSoapEnv.getBody ().addAny (MessageHelperMethods.getSoapEnvelope11ForTest (sPayloadPath).getDocumentElement ());
      return Ebms3WriterBuilder.soap11 ().getAsDocument (aSoapEnv);
    }
    // Creating SOAP 12 Envelope
    final Soap12Envelope aSoapEnv = new Soap12Envelope ();
    aSoapEnv.setHeader (new Soap12Header ());
    aSoapEnv.setBody (new Soap12Body ());
    aSoapEnv.getHeader ().addAny (aEbms3Message.getDocumentElement ());
    if (sPayloadPath != null)
      aSoapEnv.getBody ().addAny (MessageHelperMethods.getSoapEnvelope11ForTest (sPayloadPath).getDocumentElement ());
    return Ebms3WriterBuilder.soap12 ().getAsDocument (aSoapEnv);

  }

}
