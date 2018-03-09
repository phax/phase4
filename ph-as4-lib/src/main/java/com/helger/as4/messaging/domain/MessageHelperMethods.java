/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.messaging.domain;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.http.HttpMessage;
import org.w3c.dom.Node;

import com.helger.as4.CAS4;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4lib.ebms3header.Ebms3AgreementRef;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3Description;
import com.helger.as4lib.ebms3header.Ebms3From;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartInfo;
import com.helger.as4lib.ebms3header.Ebms3PartProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyId;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3Service;
import com.helger.as4lib.ebms3header.Ebms3To;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.string.StringHelper;
import com.helger.datetime.util.PDTXMLConverter;

/**
 * This class contains every method, static variables which are used by more
 * than one message creating classes in this package.
 *
 * @author bayerlma
 * @author Philip Helger
 */
@Immutable
public final class MessageHelperMethods
{
  public static final String PART_PROPERTY_MIME_TYPE = "MimeType";
  public static final String PART_PROPERTY_CHARACTER_SET = "CharacterSet";
  public static final String PART_PROPERTY_COMPRESSION_TYPE = "CompressionType";
  public static final String PREFIX_CID = "cid:";

  private MessageHelperMethods ()
  {}

  @Nonnull
  @Nonempty
  public static String createRandomConversationID ()
  {
    return CAS4.LIB_NAME + "@Conv" + RandomHelper.getRandom ().nextLong ();
  }

  @Nonnull
  @Nonempty
  public static String createRandomMessageID ()
  {
    return CAS4.LIB_NAME + "@" + UUID.randomUUID ().toString ();
  }

  /**
   * Create a new message info with a UUID as message ID.
   *
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static Ebms3MessageInfo createEbms3MessageInfo ()
  {
    return createEbms3MessageInfo (createRandomMessageID (), null);
  }

  /**
   * Create a new message info with a UUID as message ID and a reference to the
   * previous message.
   *
   * @param sRefToMessageID
   *        The message ID of the referenced message. May be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static Ebms3MessageInfo createEbms3MessageInfo (@Nullable final String sRefToMessageID)
  {
    return createEbms3MessageInfo (createRandomMessageID (), sRefToMessageID);
  }

  /**
   * Create a new message info.
   *
   * @param sMessageID
   *        The message ID. May neither be <code>null</code> nor empty.
   * @param sRefToMessageID
   *        to set the reference to the previous message needed for two way
   *        exchanges
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static Ebms3MessageInfo createEbms3MessageInfo (@Nonnull @Nonempty final String sMessageID,
                                                         @Nullable final String sRefToMessageID)
  {
    ValueEnforcer.notEmpty (sMessageID, "MessageID");

    final Ebms3MessageInfo aMessageInfo = new Ebms3MessageInfo ();

    aMessageInfo.setMessageId (sMessageID);
    if (StringHelper.hasText (sRefToMessageID))
      aMessageInfo.setRefToMessageId (sRefToMessageID);

    aMessageInfo.setTimestamp (PDTXMLConverter.getXMLCalendarNowUTC ());
    return aMessageInfo;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Ebms3Description createEbms3Description (@Nonnull final Locale aLocale, @Nonnull final String sText)
  {
    ValueEnforcer.notNull (aLocale, "Locale");
    ValueEnforcer.notNull (sText, "Text");

    final Ebms3Description aDesc = new Ebms3Description ();
    aDesc.setLang (aLocale.getLanguage ());
    aDesc.setValue (sText);
    return aDesc;
  }

  @Nonnull
  public static Ebms3Property createEbms3Property (@Nonnull @Nonempty final String sName, @Nonnull final String sValue)
  {
    final Ebms3Property aProp = new Ebms3Property ();
    aProp.setName (sName);
    aProp.setValue (sValue);
    return aProp;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <Ebms3Property> createEmbs3PropertiesOriginalSenderFinalRecipient (@Nonnull final String sOriginalSender,
                                                                                                @Nonnull final String sFinalRecipient)
  {
    return new CommonsArrayList <> (createEbms3Property (CAS4.ORIGINAL_SENDER, sOriginalSender),
                                    createEbms3Property (CAS4.FINAL_RECIPIENT, sFinalRecipient));
  }

  @Nonnull
  public static Ebms3PartyId createEbms3PartyId (@Nonnull @Nonempty final String sValue)
  {
    return createEbms3PartyId (null, sValue);
  }

  @Nonnull
  public static Ebms3PartyId createEbms3PartyId (@Nullable final String sType, @Nonnull @Nonempty final String sValue)
  {
    ValueEnforcer.notEmpty (sValue, "Value");

    final Ebms3PartyId ret = new Ebms3PartyId ();
    ret.setType (sType);
    ret.setValue (sValue);
    return ret;
  }

  @Nonnull
  public static Ebms3PartyInfo createEbms3ReversePartyInfo (@Nonnull final Ebms3PartyInfo aOrigPartyInfo)
  {
    ValueEnforcer.notNull (aOrigPartyInfo, "OriginalPartyInfo");

    return createEbms3PartyInfo (aOrigPartyInfo.getTo ().getRole (),
                                 aOrigPartyInfo.getTo ().getPartyIdAtIndex (0).getValue (),
                                 aOrigPartyInfo.getFrom ().getRole (),
                                 aOrigPartyInfo.getFrom ().getPartyIdAtIndex (0).getValue ());
  }

  @Nonnull
  public static Ebms3PartyInfo createEbms3PartyInfo (@Nonnull @Nonempty final String sFromRole,
                                                     @Nonnull @Nonempty final String sFromPartyID,
                                                     @Nonnull @Nonempty final String sToRole,
                                                     @Nonnull @Nonempty final String sToPartyID)
  {
    ValueEnforcer.notEmpty (sFromRole, "FromRole");
    ValueEnforcer.notEmpty (sFromPartyID, "FromPartyID");
    ValueEnforcer.notEmpty (sToRole, "ToRole");
    ValueEnforcer.notEmpty (sToPartyID, "ToPartyID");

    final Ebms3PartyInfo aEbms3PartyInfo = new Ebms3PartyInfo ();

    // From => Sender
    final Ebms3From aEbms3From = new Ebms3From ();
    aEbms3From.setRole (sFromRole);
    aEbms3From.addPartyId (createEbms3PartyId (sFromPartyID));
    aEbms3PartyInfo.setFrom (aEbms3From);

    // To => Receiver
    final Ebms3To aEbms3To = new Ebms3To ();
    aEbms3To.setRole (sToRole);
    aEbms3To.addPartyId (createEbms3PartyId (sToPartyID));
    aEbms3PartyInfo.setTo (aEbms3To);

    return aEbms3PartyInfo;
  }

  @Nonnull
  public static Ebms3MessageProperties createEbms3MessageProperties (@Nullable final List <Ebms3Property> aEbms3Properties)
  {
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    aEbms3MessageProperties.setProperty (aEbms3Properties);
    return aEbms3MessageProperties;
  }

  @Nonnull
  public static Ebms3CollaborationInfo createEbms3CollaborationInfo (@Nullable final String sAgreementRefPMode,
                                                                     @Nullable final String sAgreementRefValue,
                                                                     @Nullable final String sServiceType,
                                                                     @Nonnull @Nonempty final String sServiceValue,
                                                                     @Nonnull @Nonempty final String sAction,
                                                                     @Nonnull @Nonempty final String sConversationID)
  {
    ValueEnforcer.notEmpty (sServiceValue, "ServiceValue");
    ValueEnforcer.notEmpty (sAction, "Action");
    ValueEnforcer.notEmpty (sConversationID, "ConversationID");

    final Ebms3CollaborationInfo aEbms3CollaborationInfo = new Ebms3CollaborationInfo ();
    if (StringHelper.hasText (sAgreementRefValue))
    {
      final Ebms3AgreementRef aEbms3AgreementRef = new Ebms3AgreementRef ();
      if (StringHelper.hasText (sAgreementRefPMode))
        aEbms3AgreementRef.setPmode (sAgreementRefPMode);
      aEbms3AgreementRef.setValue (sAgreementRefValue);
      aEbms3CollaborationInfo.setAgreementRef (aEbms3AgreementRef);
    }
    {
      final Ebms3Service aEbms3Service = new Ebms3Service ();
      aEbms3Service.setType (sServiceType);
      aEbms3Service.setValue (sServiceValue);
      aEbms3CollaborationInfo.setService (aEbms3Service);
    }
    aEbms3CollaborationInfo.setAction (sAction);
    aEbms3CollaborationInfo.setConversationId (sConversationID);
    return aEbms3CollaborationInfo;
  }

  /**
   * Add payload info if attachments are present.
   *
   * @param aPayload
   *        Optional SOAP body payload. This must be <code>null</code> when
   *        using MIME message layout!
   * @param aAttachments
   *        Used attachments
   * @return <code>null</code> if no attachments are present.
   */
  @Nullable
  public static Ebms3PayloadInfo createEbms3PayloadInfo (@Nullable final Node aPayload,
                                                         @Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    final Ebms3PayloadInfo aEbms3PayloadInfo = new Ebms3PayloadInfo ();

    // Empty PayloadInfo only if sending as the body of the SOAP message
    if (aPayload != null)
      aEbms3PayloadInfo.addPartInfo (new Ebms3PartInfo ());

    if (aAttachments != null)
      for (final WSS4JAttachment aAttachment : aAttachments)
      {
        final Ebms3PartProperties aEbms3PartProperties = new Ebms3PartProperties ();
        aEbms3PartProperties.addProperty (createEbms3Property (PART_PROPERTY_MIME_TYPE,
                                                               aAttachment.getUncompressedMimeType ()));
        if (aAttachment.hasCharset ())
        {
          aEbms3PartProperties.addProperty (createEbms3Property (PART_PROPERTY_CHARACTER_SET,
                                                                 aAttachment.getCharset ().name ()));
        }
        if (aAttachment.hasCompressionMode ())
        {
          aEbms3PartProperties.addProperty (createEbms3Property (PART_PROPERTY_COMPRESSION_TYPE,
                                                                 aAttachment.getCompressionMode ()
                                                                            .getMimeTypeAsString ()));
        }

        final Ebms3PartInfo aEbms3PartInfo = new Ebms3PartInfo ();
        aEbms3PartInfo.setHref (PREFIX_CID + aAttachment.getId ());
        aEbms3PartInfo.setPartProperties (aEbms3PartProperties);
        aEbms3PayloadInfo.addPartInfo (aEbms3PartInfo);
      }

    if (aEbms3PayloadInfo.getPartInfoCount () == 0)
    {
      // Neither payload nor attachments
      return null;
    }

    return aEbms3PayloadInfo;
  }

  public static void moveMIMEHeadersToHTTPHeader (@Nonnull final MimeMessage aMimeMsg,
                                                  @Nonnull final HttpMessage aHttpMsg) throws MessagingException
  {
    ValueEnforcer.notNull (aMimeMsg, "MimeMsg");
    ValueEnforcer.notNull (aHttpMsg, "HttpMsg");

    // Move all mime headers to the HTTP request
    final Enumeration <Header> aEnum = aMimeMsg.getAllHeaders ();
    while (aEnum.hasMoreElements ())
    {
      final Header h = aEnum.nextElement ();
      // Make a single-line HTTP header value!
      aHttpMsg.addHeader (h.getName (), HttpHeaderMap.getUnifiedValue (h.getValue ()));

      // Remove from MIME message!
      aMimeMsg.removeHeader (h.getName ());
    }
  }
}
