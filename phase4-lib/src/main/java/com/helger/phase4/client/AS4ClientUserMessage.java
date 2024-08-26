/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3PayloadInfo;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.messaging.crypto.AS4Encryptor;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.messaging.http.AS4HttpDebug;
import com.helger.phase4.messaging.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.AS4MimeMessageHelper;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.EAS4MessageType;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xsds.xmldsig.ReferenceType;

import jakarta.mail.MessagingException;

/**
 * AS4 client for {@link AS4UserMessage} objects.
 *
 * @author Philip Helger
 * @author bayerlma
 */
@NotThreadSafe
public class AS4ClientUserMessage extends AbstractAS4Client <AS4ClientUserMessage>
{
  public static final boolean DEFAULT_FORCE_MIME_MESSAGE = false;

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4ClientUserMessage.class);

  private Node m_aSoapBodyPayload;
  private final ICommonsList <WSS4JAttachment> m_aAttachments = new CommonsArrayList <> ();
  private boolean m_bForceMimeMessage = DEFAULT_FORCE_MIME_MESSAGE;

  // Document related attributes
  private final ICommonsList <Ebms3Property> m_aEbms3Properties = new CommonsArrayList <> ();

  // CollaborationInfo
  private String m_sAction;

  private String m_sServiceType;
  private String m_sServiceValue;

  private String m_sConversationID;

  private String m_sAgreementRefValue;
  private String m_sAgreementTypeValue;

  private String m_sFromRole = CAS4.DEFAULT_ROLE;
  private String m_sFromPartyIDType;
  private String m_sFromPartyIDValue;

  private String m_sToRole = CAS4.DEFAULT_ROLE;
  private String m_sToPartyIDType;
  private String m_sToPartyIDValue;

  private boolean m_bUseLeg1 = true;
  private IPMode m_aPMode;
  private Function <AS4ClientUserMessage, String> m_aPModeIDFactory = x -> x.getFromPartyID () +
                                                                           "-" +
                                                                           x.getToPartyID ();

  public AS4ClientUserMessage (@Nonnull @WillNotClose final AS4ResourceHelper aResHelper)
  {
    super (EAS4MessageType.USER_MESSAGE, aResHelper);
  }

  /**
   * @return The payload of the user message that will be placed in the SOAP
   *         body. May be <code>null</code>.
   */
  @Nullable
  public final Node getPayload ()
  {
    return m_aSoapBodyPayload;
  }

  /**
   * Sets the payload for a usermessage. The payload unlike an attachment will
   * be added into the SOAP-Body of the message.
   *
   * @param aPayload
   *        the Payload to be added
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientUserMessage setPayload (@Nullable final Node aPayload)
  {
    m_aSoapBodyPayload = aPayload;
    return this;
  }

  /**
   * @return The list of attachments that are part of the message. If this list
   *         is not empty, a MIME message is created. Having attachments and no
   *         SOAP body is totally valid.
   */
  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <WSS4JAttachment> attachments ()
  {
    return m_aAttachments;
  }

  /**
   * @return <code>true</code> if a MIME message is always created, even if no
   *         attachments are present, <code>false</code> to use a simple SOAP
   *         message in case of absence of attachments. The default value is
   *         {@value #DEFAULT_FORCE_MIME_MESSAGE}
   * @since 2.5.1
   */
  public final boolean isForceMimeMessage ()
  {
    return m_bForceMimeMessage;
  }

  /**
   * Enable the enforcement of packaging the AS4 user message in a MIME message.
   *
   * @param bForceMimeMessage
   *        <code>true</code> to enforce it, <code>false</code> to make it
   *        dynamic.
   * @return this for chaining
   * @since 2.5.1
   */
  @Nonnull
  public final AS4ClientUserMessage setForceMimeMessage (final boolean bForceMimeMessage)
  {
    m_bForceMimeMessage = bForceMimeMessage;
    return this;
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
  public final AS4ClientUserMessage addAttachment (@Nonnull final File aAttachment,
                                                   @Nonnull final IMimeType aMimeType,
                                                   @Nullable final EAS4CompressionMode eAS4CompressionMode) throws IOException
  {
    ValueEnforcer.notNull (aAttachment, "Attachment");
    ValueEnforcer.notNull (aMimeType, "MimeType");
    return addAttachment (WSS4JAttachment.createOutgoingFileAttachment (aAttachment,
                                                                        FilenameHelper.getWithoutPath (aAttachment),
                                                                        (String) null,
                                                                        aMimeType,
                                                                        eAS4CompressionMode,
                                                                        (Charset) null,
                                                                        getAS4ResourceHelper ()));
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
  public final AS4ClientUserMessage addAttachment (@Nonnull final WSS4JAttachment aAttachment)
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
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientUserMessage setAction (@Nullable final String sAction)
  {
    m_sAction = sAction;
    return this;
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
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientUserMessage setServiceType (@Nullable final String sServiceType)
  {
    m_sServiceType = sServiceType;
    return this;
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
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientUserMessage setServiceValue (@Nullable final String sServiceValue)
  {
    m_sServiceValue = sServiceValue;
    return this;
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
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientUserMessage setConversationID (@Nullable final String sConversationID)
  {
    m_sConversationID = sConversationID;
    return this;
  }

  /**
   * @return The value of the <code>eb:AgreementRef</code> element. May be
   *         <code>null</code>.
   */
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
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientUserMessage setAgreementRefValue (@Nullable final String sAgreementRefValue)
  {
    m_sAgreementRefValue = sAgreementRefValue;
    return this;
  }

  /**
   * @return The value of the <code>eb:AgreementRef/@type</code> attribute. May
   *         be <code>null</code>.
   * @since 2.7.6
   */
  @Nullable
  public final String getAgreementTypeValue ()
  {
    return m_sAgreementTypeValue;
  }

  /**
   * Set the value of the <code>eb:AgreementRef/@type</code> attribute.
   *
   * @param sAgreementTypeValue
   *        The value to be set. May be <code>null</code>.
   * @return this for chaining
   * @since 2.7.6
   */
  @Nonnull
  public final AS4ClientUserMessage setAgreementTypeValue (@Nullable final String sAgreementTypeValue)
  {
    m_sAgreementTypeValue = sAgreementTypeValue;
    return this;
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
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientUserMessage setFromRole (@Nullable final String sFromRole)
  {
    m_sFromRole = sFromRole;
    return this;
  }

  @Nullable
  public final String getFromPartyIDType ()
  {
    return m_sFromPartyIDType;
  }

  /**
   * The PartyID is an ID that identifies the C2 over which the message gets
   * sent.
   *
   * @param sFromPartyIDType
   *        the partyID type that should be set
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientUserMessage setFromPartyIDType (@Nullable final String sFromPartyIDType)
  {
    m_sFromPartyIDType = sFromPartyIDType;
    return this;
  }

  @Nullable
  public final String getFromPartyID ()
  {
    return m_sFromPartyIDValue;
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
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientUserMessage setFromPartyID (@Nullable final String sFromPartyID)
  {
    m_sFromPartyIDValue = sFromPartyID;
    return this;
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
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientUserMessage setToRole (@Nullable final String sToRole)
  {
    m_sToRole = sToRole;
    return this;
  }

  @Nullable
  public final String getToPartyIDType ()
  {
    return m_sToPartyIDType;
  }

  /**
   * * @see #setFromPartyIDType(String)
   *
   * @param sToPartyIDType
   *        the PartyID type that should be set
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientUserMessage setToPartyIDType (@Nullable final String sToPartyIDType)
  {
    m_sToPartyIDType = sToPartyIDType;
    return this;
  }

  @Nullable
  public final String getToPartyID ()
  {
    return m_sToPartyIDValue;
  }

  /**
   * * @see #setFromPartyID(String)
   *
   * @param sToPartyID
   *        the PartyID that should be set
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientUserMessage setToPartyID (@Nullable final String sToPartyID)
  {
    m_sToPartyIDValue = sToPartyID;
    return this;
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
   * @return this for chaining
   */
  @Nonnull
  public final AS4ClientUserMessage setUseLeg1 (final boolean bUseLeg1)
  {
    m_bUseLeg1 = bUseLeg1;
    return this;
  }

  @Nullable
  public final IPMode getPMode ()
  {
    return m_aPMode;
  }

  public final void setUserMessageValuesFromPMode (@Nonnull final IPMode aPMode, @Nonnull final PModeLeg aEffectiveLeg)
  {
    ValueEnforcer.notNull (aPMode, "PMode");
    ValueEnforcer.notNull (aEffectiveLeg, "EffectiveLeg");

    if (aEffectiveLeg.hasBusinessInfo ())
    {
      setAction (aEffectiveLeg.getBusinessInfo ().getAction ());
      setServiceValue (aEffectiveLeg.getBusinessInfo ().getService ());
      setServiceType (aEffectiveLeg.getBusinessInfo ().getServiceType ());
    }
    else
    {
      setAction (null);
      setServiceValue (null);
      setServiceType (null);
    }
    setAgreementRefValue (aPMode.getAgreement ());
    if (aPMode.hasInitiator ())
    {
      setFromRole (aPMode.getInitiator ().getRole ());
      setFromPartyIDType (aPMode.getInitiator ().getIDType ());
      setFromPartyID (aPMode.getInitiator ().getIDValue ());
    }
    else
    {
      setFromRole (null);
      setFromPartyIDType (null);
      setFromPartyID (null);
    }
    if (aPMode.hasResponder ())
    {
      setToRole (aPMode.getResponder ().getRole ());
      setToPartyIDType (aPMode.getResponder ().getIDType ());
      setToPartyID (aPMode.getResponder ().getIDValue ());
    }
    else
    {
      setToRole (null);
      setToPartyIDType (null);
      setToPartyID (null);
    }

    setValuesFromPMode (aPMode, aEffectiveLeg);
  }

  /**
   * This method should be used if you do not want to set each parameter and
   * have a PMode ready that you wish to use. Some parameters still must be set
   * with the remaining setters.
   *
   * @param aPMode
   *        that should be used. May be <code>null</code>
   * @param bSetValuesFromPMode
   *        <code>true</code> to set all values in the client, that can be
   *        derived from the PMode, <code>false</code> to not do it.
   */
  public final void setPMode (@Nullable final IPMode aPMode, final boolean bSetValuesFromPMode)
  {
    m_aPMode = aPMode;
    // if pmode is set use attribute from pmode
    if (aPMode != null && bSetValuesFromPMode)
    {
      final PModeLeg aEffectiveLeg = m_bUseLeg1 ? aPMode.getLeg1 () : aPMode.getLeg2 ();
      if (aEffectiveLeg != null)
        setUserMessageValuesFromPMode (aPMode, aEffectiveLeg);
    }
  }

  @Nonnull
  public final Function <AS4ClientUserMessage, String> getPModeIDFactory ()
  {
    return m_aPModeIDFactory;
  }

  @Nonnull
  public final AS4ClientUserMessage setPModeID (@Nullable final String sPModeID)
  {
    // Just set a constant PMode factory
    return setPModeIDFactory (x -> sPModeID);
  }

  @Nonnull
  public final AS4ClientUserMessage setPModeIDFactory (@Nonnull final Function <AS4ClientUserMessage, String> aPModeIDFactory)
  {
    ValueEnforcer.notNull (aPModeIDFactory, "PModeIDFactory");
    m_aPModeIDFactory = aPModeIDFactory;
    return this;
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

    if (m_sConversationID == null)
      throw new IllegalStateException ("ConversationID needs to be set (but may be empty)");

    if (false)
      if (StringHelper.hasNoText (m_sAgreementRefValue))
        throw new IllegalStateException ("AgreementRefValue needs to be set");

    if (StringHelper.hasNoText (m_sFromRole))
      throw new IllegalStateException ("FromRole needs to be set");

    if (StringHelper.hasNoText (m_sFromPartyIDValue))
      throw new IllegalStateException ("FromPartyID needs to be set");

    if (StringHelper.hasNoText (m_sToRole))
      throw new IllegalStateException ("ToRole needs to be set");

    if (StringHelper.hasNoText (m_sToPartyIDValue))
      throw new IllegalStateException ("ToPartyID needs to be set");
  }

  @Override
  @Nonnull
  public AS4ClientBuiltMessage buildMessage (@Nonnull @Nonempty final String sMessageID,
                                             @Nullable final IAS4ClientBuildMessageCallback aCallback) throws WSSecurityException,
                                                                                                       MessagingException
  {
    LOGGER.info ("phase4 --- usermessage-building:start");

    final String sAgreementRefPMode = m_aPModeIDFactory.apply (this);

    // check mandatory attributes
    _checkMandatoryAttributes ();

    final boolean bSign = signingParams ().isSigningEnabled ();
    final boolean bEncrypt = cryptParams ().isCryptEnabled (LOGGER::warn);
    final boolean bSoapBoayPayloadPresent = m_aSoapBodyPayload != null;
    final boolean bAttachmentsPresent = m_aAttachments.isNotEmpty ();
    final ESoapVersion eSoapVersion = getSoapVersion ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (sMessageID,
                                                                                            getRefToMessageID (),
                                                                                            ensureSendingDateTime ().getSendingDateTime ());
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (m_aSoapBodyPayload != null,
                                                                                            m_aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo (sAgreementRefPMode,
                                                                                                              m_sAgreementRefValue,
                                                                                                              m_sAgreementTypeValue,
                                                                                                              m_sServiceType,
                                                                                                              m_sServiceValue,
                                                                                                              m_sAction,
                                                                                                              m_sConversationID);
    final Ebms3PartyInfo aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo (m_sFromRole,
                                                                                      m_sFromPartyIDType,
                                                                                      m_sFromPartyIDValue,
                                                                                      m_sToRole,
                                                                                      m_sToPartyIDType,
                                                                                      m_sToPartyIDValue);

    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (m_aEbms3Properties);

    final AS4UserMessage aUserMsg = AS4UserMessage.create (aEbms3MessageInfo,
                                                           aEbms3PayloadInfo,
                                                           aEbms3CollaborationInfo,
                                                           aEbms3PartyInfo,
                                                           aEbms3MessageProperties,
                                                           null,
                                                           eSoapVersion).setMustUnderstand (true);

    if (aCallback != null)
      aCallback.onAS4Message (aUserMsg);

    final Document aPureSoapDoc = aUserMsg.getAsSoapDocument (m_aSoapBodyPayload);

    if (aCallback != null)
      aCallback.onSoapDocument (aPureSoapDoc);

    // 1. compress
    // Is done when the attachments are added

    // 2. sign and/or encrypt
    Document aResultSoapDoc = aPureSoapDoc;
    AS4MimeMessage aMimeMsg = null;
    ICommonsList <ReferenceType> aCreatedDSReferences = null;
    if (bSign || bEncrypt)
    {
      AS4HttpDebug.debug ( () -> "Unsigned/unencrypted UserMessage:\n" +
                                 XMLWriter.getNodeAsString (aPureSoapDoc, AS4HttpDebug.getDebugXMLWriterSettings ()));

      // 2a. sign
      if (bSign)
      {
        final IAS4CryptoFactory aCryptoFactorySign = internalGetCryptoFactorySign ();

        final boolean bMustUnderstand = true;
        final Document aSignedSoapDoc = AS4Signer.createSignedMessage (aCryptoFactorySign,
                                                                       aResultSoapDoc,
                                                                       eSoapVersion,
                                                                       aUserMsg.getMessagingID (),
                                                                       m_aAttachments,
                                                                       getAS4ResourceHelper (),
                                                                       bMustUnderstand,
                                                                       signingParams ().getClone ());
        aResultSoapDoc = aSignedSoapDoc;

        // Extract the created references
        aCreatedDSReferences = MessageHelperMethods.getAllDSigReferences (aSignedSoapDoc);

        if (aCallback != null)
          aCallback.onSignedSoapDocument (aSignedSoapDoc);

        AS4HttpDebug.debug ( () -> "Signed UserMessage:\n" +
                                   XMLWriter.getNodeAsString (aSignedSoapDoc,
                                                              AS4HttpDebug.getDebugXMLWriterSettings ()));
      }

      // 2b. encrypt
      if (bEncrypt)
      {
        final IAS4CryptoFactory aCryptoFactoryCrypt = internalGetCryptoFactoryCrypt ();

        // MustUnderstand always set to true
        final boolean bMustUnderstand = true;
        if (bAttachmentsPresent)
        {
          // Attachments are never empty
          aMimeMsg = AS4Encryptor.encryptToMimeMessage (eSoapVersion,
                                                        aResultSoapDoc,
                                                        m_aAttachments,
                                                        aCryptoFactoryCrypt,
                                                        bMustUnderstand,
                                                        getAS4ResourceHelper (),
                                                        cryptParams ().getClone ());

          if (aCallback != null)
            aCallback.onEncryptedMimeMessage (aMimeMsg);
        }
        else
          if (bSoapBoayPayloadPresent)
          {
            final Document aEncryptedSoapDoc = AS4Encryptor.encryptSoapBodyPayload (aCryptoFactoryCrypt,
                                                                                    eSoapVersion,
                                                                                    aResultSoapDoc,
                                                                                    bMustUnderstand,
                                                                                    cryptParams ().getClone ());

            if (aCallback != null)
              aCallback.onEncryptedSoapDocument (aEncryptedSoapDoc);

            aResultSoapDoc = aEncryptedSoapDoc;
          }
          else
          {
            // Empty message - nothing to encrypt
            LOGGER.info ("AS4 encryption is enabled but neither a SOAP Body payload nor attachments are present");
          }
      }
    }

    if ((bAttachmentsPresent || m_bForceMimeMessage) && aMimeMsg == null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Enforcing the creation of a MIME message");

      // * not encrypted, not signed
      // * not encrypted, signed
      // * forced by flag
      aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (eSoapVersion, aResultSoapDoc, m_aAttachments);
    }

    final AS4ClientBuiltMessage ret;
    if (aMimeMsg != null)
    {
      // Wrap MIME message
      ret = new AS4ClientBuiltMessage (sMessageID, HttpMimeMessageEntity.create (aMimeMsg), aCreatedDSReferences);
    }
    else
    {
      // Wrap SOAP XML
      ret = new AS4ClientBuiltMessage (sMessageID,
                                       new HttpXMLEntity (aResultSoapDoc, eSoapVersion.getMimeType ()),
                                       aCreatedDSReferences);
    }

    LOGGER.info ("phase4 --- usermessage-building:end");

    return ret;
  }
}
