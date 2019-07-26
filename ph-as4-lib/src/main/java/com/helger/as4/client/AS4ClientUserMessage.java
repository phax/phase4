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

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;
import javax.mail.internet.MimeMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4.CAS4;
import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.http.AS4HttpDebug;
import com.helger.as4.http.HttpMimeMessageEntity;
import com.helger.as4.http.HttpXMLEntity;
import com.helger.as4.messaging.crypto.AS4Encryptor;
import com.helger.as4.messaging.crypto.AS4Signer;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.AbstractAS4Message;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.mime.MimeMessageCreator;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.util.AS4ResourceHelper;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.functional.IFunction;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.string.StringHelper;
import com.helger.xml.serialize.write.XMLWriter;

/**
 * AS4 client for {@link AS4UserMessage} objects.
 *
 * @author Philip Helger
 * @author bayerlma
 */
@NotThreadSafe
public class AS4ClientUserMessage extends AbstractAS4Client
{
  private final AS4ResourceHelper m_aResHelper;

  private Node m_aPayload;
  private final ICommonsList <WSS4JAttachment> m_aAttachments = new CommonsArrayList <> ();

  // Document related attributes
  private final ICommonsList <Ebms3Property> m_aEbms3Properties = new CommonsArrayList <> ();

  // CollaborationInfo
  private String m_sAction;

  private String m_sServiceType;
  private String m_sServiceValue;

  private String m_sConversationID;

  private String m_sAgreementRefValue;

  private String m_sFromRole = CAS4.DEFAULT_ROLE;
  private String m_sFromPartyID;

  private String m_sToRole = CAS4.DEFAULT_ROLE;
  private String m_sToPartyID;

  private boolean m_bUseLeg1 = true;
  private IPMode m_aPMode;
  private IFunction <AS4ClientUserMessage, String> m_aPModeIDFactory = x -> x.getFromPartyID () +
                                                                            "-" +
                                                                            x.getToPartyID ();

  public AS4ClientUserMessage (@Nonnull @WillNotClose final AS4ResourceHelper aResHelper)
  {
    ValueEnforcer.notNull (aResHelper, "ResHelper");
    m_aResHelper = aResHelper;
  }

  @Nonnull
  public final AS4ResourceHelper getResourceHelper ()
  {
    return m_aResHelper;
  }

  public final void setPModeID (@Nullable final String sPModeID)
  {
    // Just set a constant PMode factory
    setPModeIDFactory (x -> sPModeID);
  }

  public final void setPModeIDFactory (@Nonnull final IFunction <AS4ClientUserMessage, String> aPModeIDFactory)
  {
    ValueEnforcer.notNull (aPModeIDFactory, "PModeIDFactory");
    m_aPModeIDFactory = aPModeIDFactory;
  }

  private void _checkMandatoryAttributes ()
  {
    if (StringHelper.hasNoText (m_sAction))
      throw new IllegalStateException ("Action needs to be set");

    if (false)
      if (StringHelper.hasNoText (m_sServiceType))
        throw new IllegalStateException ("ServiceType needs to be set");

    if (StringHelper.hasNoText (m_sServiceValue))
      throw new IllegalStateException ("ServiceValue needs to be set");

    if (StringHelper.hasNoText (m_sConversationID))
      throw new IllegalStateException ("ConversationID needs to be set");

    if (false)
      if (StringHelper.hasNoText (m_sAgreementRefValue))
        throw new IllegalStateException ("AgreementRefValue needs to be set");

    if (StringHelper.hasNoText (m_sFromRole))
      throw new IllegalStateException ("FromRole needs to be set");

    if (StringHelper.hasNoText (m_sFromPartyID))
      throw new IllegalStateException ("FromPartyID needs to be set");

    if (StringHelper.hasNoText (m_sToRole))
      throw new IllegalStateException ("ToRole needs to be set");

    if (StringHelper.hasNoText (m_sToPartyID))
      throw new IllegalStateException ("ToPartyID needs to be set");

    if (m_aEbms3Properties.containsNone (x -> x.getName ().equals (CAS4.ORIGINAL_SENDER)))
      throw new IllegalStateException ("Mandatory property originalSender is missing");
    if (m_aEbms3Properties.containsNone (x -> x.getName ().equals (CAS4.FINAL_RECIPIENT)))
      throw new IllegalStateException ("Mandatory property finalRecipient is missing");
  }

  /**
   * Build the AS4 message to be sent. It uses all the attributes of this class
   * to build the final message. Compression, signing and encryption happens in
   * this methods.
   *
   * @return The HTTP entity to be sent. Never <code>null</code>.
   * @throws Exception
   *         in case something goes wrong
   */
  @Override
  @Nonnull
  public AS4BuiltMessage buildMessage (@Nullable final Consumer <? super AbstractAS4Message <?>> aMsgConsumer) throws Exception
  {
    final String sAgreementRefPMode = m_aPModeIDFactory.apply (this);

    // check mandatory attributes
    _checkMandatoryAttributes ();

    final boolean bSign = signingParams ().isSigningEnabled ();
    final boolean bEncrypt = cryptParams ().isCryptEnabled ();
    final boolean bAttachmentsPresent = m_aAttachments.isNotEmpty ();

    // Create a new message ID for each build!
    final String sMessageID = createMessageID ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (sMessageID,
                                                                                            getRefToMessageID ());
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (m_aPayload != null,
                                                                                            m_aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo (sAgreementRefPMode,
                                                                                                              m_sAgreementRefValue,
                                                                                                              m_sServiceType,
                                                                                                              m_sServiceValue,
                                                                                                              m_sAction,
                                                                                                              m_sConversationID);
    final Ebms3PartyInfo aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo (m_sFromRole,
                                                                                      m_sFromPartyID,
                                                                                      m_sToRole,
                                                                                      m_sToPartyID);

    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (m_aEbms3Properties);

    final AS4UserMessage aUserMsg = AS4UserMessage.create (aEbms3MessageInfo,
                                                           aEbms3PayloadInfo,
                                                           aEbms3CollaborationInfo,
                                                           aEbms3PartyInfo,
                                                           aEbms3MessageProperties,
                                                           getSOAPVersion ())
                                                  .setMustUnderstand (true);

    if (aMsgConsumer != null)
      aMsgConsumer.accept (aUserMsg);

    Document aDoc = aUserMsg.getAsSOAPDocument (m_aPayload);

    // 1. compress
    // Is done when the attachments are added

    // 2. sign and/or encrpyt
    MimeMessage aMimeMsg = null;
    if (bSign || bEncrypt)
    {
      final Document aPureDoc = aDoc;
      AS4HttpDebug.debug ( () -> "Unsigned/unencrypted UserMessage:\n" +
                                 XMLWriter.getNodeAsString (aPureDoc, AS4HttpDebug.getDebugXMLWriterSettings ()));

      final AS4CryptoFactory aCryptoFactory = internalCreateCryptoFactory ();

      // 2a. sign
      if (bSign)
      {
        final boolean bMustUnderstand = true;
        final Document aSignedDoc = AS4Signer.createSignedMessage (aCryptoFactory,
                                                                   aDoc,
                                                                   getSOAPVersion (),
                                                                   aUserMsg.getMessagingID (),
                                                                   m_aAttachments,
                                                                   m_aResHelper,
                                                                   bMustUnderstand,
                                                                   signingParams ().getClone ());
        aDoc = aSignedDoc;

        AS4HttpDebug.debug ( () -> "Signed UserMessage:\n" +
                                   XMLWriter.getNodeAsString (aSignedDoc, AS4HttpDebug.getDebugXMLWriterSettings ()));
      }

      // 2b. encrypt
      if (bEncrypt)
      {
        // MustUnderstand always set to true
        final boolean bMustUnderstand = true;
        if (bAttachmentsPresent)
        {
          aMimeMsg = AS4Encryptor.encryptMimeMessage (getSOAPVersion (),
                                                      aDoc,
                                                      m_aAttachments,
                                                      aCryptoFactory,
                                                      bMustUnderstand,
                                                      m_aResHelper,
                                                      cryptParams ().getClone ());
        }
        else
        {
          aDoc = AS4Encryptor.encryptSoapBodyPayload (aCryptoFactory,
                                                      getSOAPVersion (),
                                                      aDoc,
                                                      bMustUnderstand,
                                                      cryptParams ().getClone ());
        }
      }
    }

    if (bAttachmentsPresent && aMimeMsg == null)
    {
      // * not encrypted, not signed
      // * not encrypted, signed
      aMimeMsg = MimeMessageCreator.generateMimeMessage (getSOAPVersion (), aDoc, m_aAttachments);
    }

    if (aMimeMsg != null)
    {
      return new AS4BuiltMessage (sMessageID, new HttpMimeMessageEntity (aMimeMsg));
    }

    // Wrap SOAP XML
    return new AS4BuiltMessage (sMessageID, new HttpXMLEntity (aDoc, getSOAPVersion ()));
  }

  @Nullable
  public final Node getPayload ()
  {
    return m_aPayload;
  }

  /**
   * Sets the payload for a usermessage. The payload unlike an attachment will
   * be added into the SOAP-Body of the message.
   *
   * @param aPayload
   *        the Payload to be added
   */
  public final void setPayload (@Nullable final Node aPayload)
  {
    m_aPayload = aPayload;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <WSS4JAttachment> getAllAttachments ()
  {
    return m_aAttachments.getClone ();
  }

  /**
   * Adds a file as attachment to the message.
   *
   * @param aAttachment
   *        Attachment to be added. May not be <code>null</code>.
   * @param aMimeType
   *        MIME type of the given file. May not be <code>null</code>.
   * @return this for chaining
   * @throws IOException
   *         if something goes wrong in the adding process
   */
  @Nonnull
  public AS4ClientUserMessage addAttachment (@Nonnull final File aAttachment,
                                             @Nonnull final IMimeType aMimeType) throws IOException
  {
    return addAttachment (aAttachment, aMimeType, null);
  }

  /**
   * Adds a file as attachment to the message.
   *
   * @param aAttachment
   *        Attachment to be added. May not be <code>null</code>.
   * @param aMimeType
   *        MIME type of the given file. May not be <code>null</code>.
   * @param eAS4CompressionMode
   *        which compression type should be used to compress the attachment.
   *        May be <code>null</code>.
   * @return this for chaining
   * @throws IOException
   *         if something goes wrong in the adding process or the compression
   */
  @Nonnull
  public AS4ClientUserMessage addAttachment (@Nonnull final File aAttachment,
                                             @Nonnull final IMimeType aMimeType,
                                             @Nullable final EAS4CompressionMode eAS4CompressionMode) throws IOException
  {
    return addAttachment (WSS4JAttachment.createOutgoingFileAttachment (aAttachment,
                                                                        null,
                                                                        aMimeType,
                                                                        eAS4CompressionMode,
                                                                        m_aResHelper));
  }

  /**
   * Adds a file as attachment to the message. The caller of the method must
   * ensure the attachment is already compressed (if desired)!
   *
   * @param aAttachment
   *        Attachment to be added. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public AS4ClientUserMessage addAttachment (@Nonnull final WSS4JAttachment aAttachment)
  {
    ValueEnforcer.notNull (aAttachment, "Attachment");
    m_aAttachments.add (aAttachment);
    return this;
  }

  /**
   * With properties optional info can be added for the receiving party. If you
   * want to be AS4 Profile conform you need to add two properties to your
   * message: originalSender and finalRecipient these two correlate to C1 and
   * C4.
   *
   * @return The mutable list. Never <code>null</code>.
   * @since 0.8.2
   */
  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <Ebms3Property> ebms3Properties ()
  {
    return m_aEbms3Properties;
  }

  @Nullable
  public final String getAction ()
  {
    return m_sAction;
  }

  /**
   * The element is a string identifying an operation or an activity within a
   * Service that may support several of these.<br>
   * Example of what will be written in the user message:
   * <code>&lt;eb:Action&gt;NewPurchaseOrder&lt;/eb:Action&gt;</code><br>
   * This is MANDATORY.
   *
   * @param sAction
   *        the action that should be there.
   */
  public final void setAction (@Nullable final String sAction)
  {
    m_sAction = sAction;
  }

  @Nullable
  public final String getServiceType ()
  {
    return m_sServiceType;
  }

  /**
   * It is a string identifying the service type of the service specified in
   * service value.<br>
   * Example of what will be written in the user message:
   * <code>&lt;eb:Service type= "MyServiceTypes"&gt;QuoteToCollect&lt;/eb:Service&gt;</code><br>
   *
   * @param sServiceType
   *        serviceType that should be set
   */
  public final void setServiceType (@Nullable final String sServiceType)
  {
    m_sServiceType = sServiceType;
  }

  @Nullable
  public final String getServiceValue ()
  {
    return m_sServiceValue;
  }

  /**
   * It is a string identifying the service that acts on the message 1639 and it
   * is specified by the designer of the service.<br>
   * Example of what will be written in the user message: <code>&lt;eb:Service
   * type="MyServiceTypes"&gt;QuoteToCollect&lt;/eb:Service&gt;</code><br>
   * This is MANDATORY.
   *
   * @param sServiceValue
   *        the service value that should be set
   */
  public final void setServiceValue (@Nullable final String sServiceValue)
  {
    m_sServiceValue = sServiceValue;
  }

  @Nullable
  public final String getConversationID ()
  {
    return m_sConversationID;
  }

  /**
   * The element is a string identifying the set of related messages that make
   * up a conversation between Parties.<br>
   * Example of what will be written in the user message:
   * <code>&lt;eb:ConversationId&gt;4321&lt;/eb:ConversationId&gt;</code><br>
   * This is MANDATORY.
   *
   * @param sConversationID
   *        the conversationID that should be set
   */
  public final void setConversationID (@Nullable final String sConversationID)
  {
    m_sConversationID = sConversationID;
  }

  @Nullable
  public final String getAgreementRefValue ()
  {
    return m_sAgreementRefValue;
  }

  /**
   * The AgreementRef element is a string that identifies the entity or artifact
   * governing the exchange of messages between the parties.<br>
   * Example of what will be written in the user message:
   * <code>&lt;eb:AgreementRef pmode=
   * "pm-esens-generic-resp"&gt;http://agreements.holodeckb2b.org/examples/agreement0&lt;/eb:AgreementRef&gt;</code><br>
   * This is MANDATORY.
   *
   * @param sAgreementRefValue
   *        agreement reference that should be set
   */
  public final void setAgreementRefValue (@Nullable final String sAgreementRefValue)
  {
    m_sAgreementRefValue = sAgreementRefValue;
  }

  @Nullable
  public final String getFromRole ()
  {
    return m_sFromRole;
  }

  /**
   * The value of the Role element is a non-empty string, with a default value
   * of
   * <code>http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultRole</code>
   * .
   *
   * @param sFromRole
   *        the role that should be set
   */
  public final void setFromRole (@Nullable final String sFromRole)
  {
    m_sFromRole = sFromRole;
  }

  @Nullable
  public final String getFromPartyID ()
  {
    return m_sFromPartyID;
  }

  /**
   * The PartyID is an ID that identifies the C2 over which the message gets
   * sent.<br>
   * Example of what will be written in the user message:
   * <code>&lt;eb:PartyId&gt;ImAPartyID&lt;/eb:PartyId&gt;</code><br>
   * This is MANDATORY.
   *
   * @param sFromPartyID
   *        the partyID that should be set
   */
  public final void setFromPartyID (@Nullable final String sFromPartyID)
  {
    m_sFromPartyID = sFromPartyID;
  }

  @Nullable
  public final String getToRole ()
  {
    return m_sToRole;
  }

  /**
   * @see #setFromRole(String)
   * @param sToRole
   *        the role that should be used
   */
  public final void setToRole (@Nullable final String sToRole)
  {
    m_sToRole = sToRole;
  }

  @Nullable
  public final String getToPartyID ()
  {
    return m_sToPartyID;
  }

  /**
   * * @see #setFromPartyID(String)
   *
   * @param sToPartyID
   *        the PartyID that should be set
   */
  public final void setToPartyID (@Nullable final String sToPartyID)
  {
    m_sToPartyID = sToPartyID;
  }

  public final boolean isUseLeg1 ()
  {
    return m_bUseLeg1;
  }

  /**
   * DEFAULT is set to <code>true</code>, if you want to use leg2 for the
   * message set <code>false</code>.
   *
   * @param bUseLeg1
   *        <code>true</code> if leg1 should be used, <code>false</code> if leg2
   *        should be used
   */
  public final void setUseLeg1 (final boolean bUseLeg1)
  {
    m_bUseLeg1 = bUseLeg1;
  }

  @Nullable
  public final IPMode getPMode ()
  {
    return m_aPMode;
  }

  protected void setValuesFromPMode (@Nonnull final IPMode aPMode, @Nonnull final PModeLeg aEffectiveLeg)
  {
    if (aEffectiveLeg.hasBusinessInfo ())
    {
      setAction (aEffectiveLeg.getBusinessInfo ().getAction ());
      setServiceValue (aEffectiveLeg.getBusinessInfo ().getService ());
    }
    else
    {
      setAction (null);
      setServiceValue (null);
    }
    setAgreementRefValue (aPMode.getAgreement ());
    if (aPMode.hasInitiator ())
    {
      setFromRole (aPMode.getInitiator ().getRole ());
      setFromPartyID (aPMode.getInitiator ().getID ());
    }
    else
    {
      setFromRole (null);
      setFromPartyID (null);
    }
    if (aPMode.hasResponder ())
    {
      setToRole (aPMode.getResponder ().getRole ());
      setToPartyID (aPMode.getResponder ().getID ());
    }
    else
    {
      setToRole (null);
      setToPartyID (null);
    }

    setCryptoValuesFromPMode (aEffectiveLeg);
  }

  /**
   * This method should be used if you do not want to set each parameter and
   * have a PMode ready that you wish to use. Some parameters still must be set
   * with the remaining setters.
   *
   * @param aPMode
   *        that should be used
   */
  public final void setPMode (@Nullable final IPMode aPMode)
  {
    m_aPMode = aPMode;
    // if pmode is set use attribute from pmode
    if (aPMode != null)
    {
      final PModeLeg aEffectiveLeg = m_bUseLeg1 ? aPMode.getLeg1 () : aPMode.getLeg2 ();
      if (aEffectiveLeg != null)
        setValuesFromPMode (aPMode, aEffectiveLeg);
    }
  }
}
