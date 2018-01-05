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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Node;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4lib.ebms3header.Ebms3AgreementRef;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3From;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartInfo;
import com.helger.as4lib.ebms3header.Ebms3PartProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3Service;
import com.helger.as4lib.ebms3header.Ebms3To;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.string.StringHelper;

/**
 * With the help of this class an usermessage or parts of it can be created.
 *
 * @author bayerlma
 * @author Philip Helger
 */
public final class UserMessageCreator
{
  public static final String PART_PROPERTY_MIME_TYPE = "MimeType";
  public static final String PART_PROPERTY_CHARACTER_SET = "CharacterSet";
  public static final String PART_PROPERTY_COMPRESSION_TYPE = "CompressionType";
  public static final String PREFIX_CID = "cid:";

  private UserMessageCreator ()
  {}

  public static AS4UserMessage getUserMessageAsAS4UserMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                                               @Nonnull final Ebms3UserMessage aUserMessage)
  {
    return new AS4UserMessage (eSOAPVersion, aUserMessage);
  }

  @Nonnull
  public static AS4UserMessage createUserMessage (@Nonnull final Ebms3MessageInfo aEbms3MessageInfo,
                                                  @Nullable final Ebms3PayloadInfo aEbms3PayloadInfo,
                                                  @Nonnull final Ebms3CollaborationInfo aEbms3CollaborationInfo,
                                                  @Nonnull final Ebms3PartyInfo aEbms3PartyInfo,
                                                  @Nullable final Ebms3MessageProperties aEbms3MessageProperties,
                                                  @Nonnull final ESOAPVersion eSOAPVersion)
  {
    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();

    // Party Information
    aUserMessage.setPartyInfo (aEbms3PartyInfo);

    // Collaboration Information
    aUserMessage.setCollaborationInfo (aEbms3CollaborationInfo);

    // Properties
    aUserMessage.setMessageProperties (aEbms3MessageProperties);

    // Payload Information
    aUserMessage.setPayloadInfo (aEbms3PayloadInfo);

    // Message Info
    aUserMessage.setMessageInfo (aEbms3MessageInfo);

    final AS4UserMessage ret = new AS4UserMessage (eSOAPVersion, aUserMessage);
    return ret;
  }

  @Nonnull
  public static Ebms3PartyInfo createEbms3ReversePartyInfo (@Nonnull final Ebms3PartyInfo aOrigPartyInfo)
  {
    return createEbms3PartyInfo (aOrigPartyInfo.getTo ().getRole (),
                                 aOrigPartyInfo.getTo ().getPartyIdAtIndex (0).getValue (),
                                 aOrigPartyInfo.getFrom ().getRole (),
                                 aOrigPartyInfo.getFrom ().getPartyIdAtIndex (0).getValue ());
  }

  @Nonnull
  public static Ebms3PartyInfo createEbms3PartyInfo (@Nonnull final String sFromRole,
                                                     @Nonnull final String sFromPartyID,
                                                     @Nonnull final String sToRole,
                                                     @Nonnull final String sToPartyID)
  {
    ValueEnforcer.notNull (sFromRole, "FromRole");
    ValueEnforcer.notNull (sFromPartyID, "FromPartyID");
    ValueEnforcer.notNull (sToRole, "ToRole");
    ValueEnforcer.notNull (sToPartyID, "ToPartyID");

    final Ebms3PartyInfo aEbms3PartyInfo = new Ebms3PartyInfo ();

    // From => Sender
    final Ebms3From aEbms3From = new Ebms3From ();
    aEbms3From.setRole (sFromRole);
    aEbms3From.addPartyId (MessageHelperMethods.createEbms3PartyId (sFromPartyID));
    aEbms3PartyInfo.setFrom (aEbms3From);

    // To => Receiver
    final Ebms3To aEbms3To = new Ebms3To ();
    aEbms3To.setRole (sToRole);
    aEbms3To.addPartyId (MessageHelperMethods.createEbms3PartyId (sToPartyID));
    aEbms3PartyInfo.setTo (aEbms3To);

    return aEbms3PartyInfo;
  }

  @Nonnull
  public static Ebms3CollaborationInfo createEbms3CollaborationInfo (@Nonnull final String sAction,
                                                                     @Nullable final String sServiceType,
                                                                     @Nonnull final String sServiceValue,
                                                                     @Nonnull final String sConversationID,
                                                                     @Nullable final String sAgreementRefPMode,
                                                                     @Nullable final String sAgreementRefValue)
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
    if (StringHelper.hasText (sAgreementRefValue))
    {
      final Ebms3AgreementRef aEbms3AgreementRef = new Ebms3AgreementRef ();
      if (StringHelper.hasText (sAgreementRefPMode))
        aEbms3AgreementRef.setPmode (sAgreementRefPMode);
      aEbms3AgreementRef.setValue (sAgreementRefValue);
      aEbms3CollaborationInfo.setAgreementRef (aEbms3AgreementRef);
    }
    return aEbms3CollaborationInfo;
  }

  @Nonnull
  public static Ebms3MessageProperties createEbms3MessageProperties (@Nullable final List <Ebms3Property> aEbms3Properties)
  {
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    aEbms3MessageProperties.setProperty (aEbms3Properties);
    return aEbms3MessageProperties;
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
        aEbms3PartProperties.addProperty (MessageHelperMethods.createEbms3Property (PART_PROPERTY_MIME_TYPE,
                                                                                    aAttachment.getUncompressedMimeType ()));
        if (aAttachment.hasCharset ())
        {
          aEbms3PartProperties.addProperty (MessageHelperMethods.createEbms3Property (PART_PROPERTY_CHARACTER_SET,
                                                                                      aAttachment.getCharset ()
                                                                                                 .name ()));
        }
        if (aAttachment.hasCompressionMode ())
        {
          aEbms3PartProperties.addProperty (MessageHelperMethods.createEbms3Property (PART_PROPERTY_COMPRESSION_TYPE,
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
}
