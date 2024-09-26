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
package com.helger.phase4.sender;

import java.security.cert.X509Certificate;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.wrapper.Wrapper;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.incoming.IAS4SignalMessageConsumer;
import com.helger.phase4.model.MessageProperty;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.util.Phase4Exception;

/**
 * Abstract builder base class for a user message.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        The implementation type
 * @since 0.10.0
 */
@NotThreadSafe
public abstract class AbstractAS4UserMessageBuilder <IMPLTYPE extends AbstractAS4UserMessageBuilder <IMPLTYPE>> extends
                                                    AbstractAS4MessageBuilder <IMPLTYPE>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractAS4UserMessageBuilder.class);

  protected IPMode m_aPMode;

  protected String m_sServiceType;
  protected String m_sService;
  protected String m_sAction;
  protected String m_sAgreementRef;
  protected String m_sAgreementType;
  protected String m_sPModeID;

  protected String m_sFromPartyIDType;
  protected String m_sFromPartyID;
  protected String m_sFromRole;

  protected String m_sToPartyIDType;
  protected String m_sToPartyID;
  protected String m_sToRole;

  protected String m_sConversationID;

  protected final ICommonsList <MessageProperty> m_aMessageProperties = new CommonsArrayList <> ();

  protected String m_sEndpointURL;

  protected final ICommonsList <AS4OutgoingAttachment> m_aAttachments = new CommonsArrayList <> ();
  protected boolean m_bForceMimeMessage = AS4ClientUserMessage.DEFAULT_FORCE_MIME_MESSAGE;

  protected IAS4SignalMessageConsumer m_aSignalMsgConsumer;
  protected IAS4SignalMessageValidationResultHandler m_aSignalMsgValidationResultHdl;

  /**
   * Create a new builder
   */
  protected AbstractAS4UserMessageBuilder ()
  {
    super ();
    // No additional default values here
  }

  /**
   * @return The currently set P-Mode. May be <code>null</code>.
   */
  @Nullable
  public final IPMode pmode ()
  {
    return m_aPMode;
  }

  /**
   * Set the PMode to be used. By default a generic PMode is used.
   *
   * @param aPMode
   *        The PMode to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE pmode (@Nullable final IPMode aPMode)
  {
    m_aPMode = aPMode;
    return thisAsT ();
  }

  /**
   * @return The optional "Service" type. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String serviceType ()
  {
    return m_sServiceType;
  }

  /**
   * @return The "Service" value. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String service ()
  {
    return m_sService;
  }

  /**
   * Set the "Service" value only, leaving the type <code>null</code>.
   *
   * @param sServiceValue
   *        Service value. May be <code>null</code>.
   * @return this for chaining.
   */
  @Nonnull
  public final IMPLTYPE service (@Nullable final String sServiceValue)
  {
    return service (null, sServiceValue);
  }

  /**
   * Set the "Service" value consisting of type and value. It's optional. If the
   * "Service" value is not set, it the "service type" defaults to the "process
   * identifier scheme" and the "service value" defaults to the "process
   * identifier value".
   *
   * @param sServiceType
   *        Service type. May be <code>null</code>.
   * @param sServiceValue
   *        Service value. May be <code>null</code>.
   * @return this for chaining.
   */
  @Nonnull
  public final IMPLTYPE service (@Nullable final String sServiceType, @Nullable final String sServiceValue)
  {
    m_sServiceType = sServiceType;
    m_sService = sServiceValue;
    return thisAsT ();
  }

  /**
   * @return The "Action" value. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String action ()
  {
    return m_sAction;
  }

  /**
   * Set the "Action" value. It's optional. If the "Action" value is not set, it
   * defaults to the "document type identifier value" (URI encoded).
   *
   * @param sAction
   *        Action value. May be <code>null</code>.
   * @return this for chaining.
   */
  @Nonnull
  public final IMPLTYPE action (@Nullable final String sAction)
  {
    m_sAction = sAction;
    return thisAsT ();
  }

  /**
   * @return The "AgreementRef" value. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String agreementRef ()
  {
    return m_sAgreementRef;
  }

  /**
   * Set the "AgreementRef" value. It's optional.
   *
   * @param sAgreementRef
   *        Agreement reference. May be <code>null</code>.
   * @return this for chaining.
   */
  @Nonnull
  public final IMPLTYPE agreementRef (@Nullable final String sAgreementRef)
  {
    m_sAgreementRef = sAgreementRef;
    return thisAsT ();
  }

  /**
   * @return The "AgreementRef type" value. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String agreementType ()
  {
    return m_sAgreementType;
  }

  /**
   * Set the "AgreementRef type" value. It's optional.
   *
   * @param sAgreementType
   *        Agreement reference type. May be <code>null</code>.
   * @return this for chaining.
   * @since 2.7.8
   */
  @Nullable
  public final IMPLTYPE agreementType (@Nullable final String sAgreementType)
  {
    m_sAgreementType = sAgreementType;
    return thisAsT ();
  }

  /**
   * @return the current PMode ID. May be <code>null</code>.
   * @see #pmodeID(String)
   */
  @Nullable
  public final String pmodeID ()
  {
    return m_sPModeID;
  }

  /**
   * Set the optional PMode ID for packaging in the user message.
   *
   * @param s
   *        PMode ID. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE pmodeID (@Nullable final String s)
  {
    m_sPModeID = s;
    return thisAsT ();
  }

  /**
   * @return The "from party ID type". May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String fromPartyIDType ()
  {
    return m_sFromPartyIDType;
  }

  /**
   * Set the "from party ID type".
   *
   * @param sFromPartyIDType
   *        The from party ID.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE fromPartyIDType (@Nullable final String sFromPartyIDType)
  {
    m_sFromPartyIDType = sFromPartyIDType;
    return thisAsT ();
  }

  /**
   * @return The "from party ID". May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String fromPartyID ()
  {
    return m_sFromPartyID;
  }

  /**
   * Set the "from party ID".
   *
   * @param sFromPartyID
   *        The from party ID.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE fromPartyID (@Nullable final String sFromPartyID)
  {
    m_sFromPartyID = sFromPartyID;
    return thisAsT ();
  }

  /**
   * @return The "from party role". May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String fromRole ()
  {
    return m_sFromRole;
  }

  /**
   * Set the "from party role". This is optional
   *
   * @param sFromRole
   *        The from role. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE fromRole (@Nullable final String sFromRole)
  {
    m_sFromRole = sFromRole;
    return thisAsT ();
  }

  /**
   * @return The "to party ID type". May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String toPartyIDType ()
  {
    return m_sToPartyIDType;
  }

  /**
   * Set the "to party ID type".
   *
   * @param sToPartyIDType
   *        The to party ID.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE toPartyIDType (@Nullable final String sToPartyIDType)
  {
    m_sToPartyIDType = sToPartyIDType;
    return thisAsT ();
  }

  /**
   * @return The "to party ID". May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String toPartyID ()
  {
    return m_sToPartyID;
  }

  /**
   * Set the "to party ID".
   *
   * @param sToPartyID
   *        The to party ID.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE toPartyID (@Nullable final String sToPartyID)
  {
    m_sToPartyID = sToPartyID;
    return thisAsT ();
  }

  /**
   * @return The "to party role". May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String toRole ()
  {
    return m_sToRole;
  }

  /**
   * Set the "to party role". This is optional
   *
   * @param sToRole
   *        The to role. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE toRole (@Nullable final String sToRole)
  {
    m_sToRole = sToRole;
    return thisAsT ();
  }

  /**
   * @return The optional AS4 conversation ID. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String conversationID ()
  {
    return m_sConversationID;
  }

  /**
   * Set the optional AS4 conversation ID. If this field is not set, a random
   * conversation ID is created.
   *
   * @param sConversationID
   *        The optional AS4 conversation ID to be used. May be
   *        <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE conversationID (@Nullable final String sConversationID)
  {
    m_sConversationID = sConversationID;
    return thisAsT ();
  }

  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <MessageProperty> messageProperties ()
  {
    return m_aMessageProperties;
  }

  @Nonnull
  @ReturnsMutableCopy
  public final ICommonsList <MessageProperty> getAllMessageProperties ()
  {
    return m_aMessageProperties.getClone ();
  }

  @Nonnull
  public final IMPLTYPE addMessageProperty (@Nullable final Ebms3Property a)
  {
    return addMessageProperty (a == null ? null : MessageProperty.builder (a));
  }

  @Nonnull
  public final IMPLTYPE addMessageProperty (@Nullable final MessageProperty.Builder a)
  {
    return addMessageProperty (a == null ? null : a.build ());
  }

  @Nonnull
  public final IMPLTYPE addMessageProperty (@Nullable final MessageProperty a)
  {
    if (a != null)
      m_aMessageProperties.add (a);
    return thisAsT ();
  }

  @Nonnull
  public final IMPLTYPE messageProperty (@Nullable final Ebms3Property a)
  {
    return messageProperty (a == null ? null : MessageProperty.builder (a));
  }

  @Nonnull
  public final IMPLTYPE messageProperty (@Nullable final MessageProperty.Builder a)
  {
    return messageProperty (a == null ? null : a.build ());
  }

  @Nonnull
  public final IMPLTYPE messageProperty (@Nullable final MessageProperty a)
  {
    if (a == null)
      m_aMessageProperties.clear ();
    else
      m_aMessageProperties.set (a);
    return thisAsT ();
  }

  @Nonnull
  public final IMPLTYPE messageProperties (@Nullable final MessageProperty... a)
  {
    m_aMessageProperties.setAll (a);
    return thisAsT ();
  }

  @Nonnull
  public final IMPLTYPE messageProperties (@Nullable final Iterable <? extends MessageProperty> a)
  {
    m_aMessageProperties.setAll (a);
    return thisAsT ();
  }

  /**
   * Set the receiver certificate used to encrypt the message with. This is the
   * full certificate. This method overwrites any receiver certificate alias
   * configuration (the later call "wins").
   *
   * @param aCertificate
   *        The certificate of the receiver to be used. May be
   *        <code>null</code>.
   * @return this for chaining
   * @see #receiverCertificateAlias(String)
   */
  @Nonnull
  public final IMPLTYPE receiverCertificate (@Nullable final X509Certificate aCertificate)
  {
    if (StringHelper.hasText (cryptParams ().getAlias ()))
      LOGGER.warn ("Overwriting Receiver Certificate Alias with an actual Receiver Certificate");

    cryptParams ().setCertificate (aCertificate).setAlias (null);
    return thisAsT ();
  }

  /**
   * Set the receiver certificate alias into the CryptoFactory keystore used to
   * encrypt the message with. This is only the alias or name of the entry. This
   * method overwrites any receiver certificate configuration (the later call
   * "wins").
   *
   * @param sAlias
   *        The certificate alias of the receiver to be used. May be
   *        <code>null</code>.
   * @return this for chaining
   * @see #receiverCertificate(X509Certificate)
   * @since 2.1.4
   */
  @Nonnull
  public final IMPLTYPE receiverCertificateAlias (@Nullable final String sAlias)
  {
    if (cryptParams ().getCertificate () != null)
      LOGGER.warn ("Overwriting actual Receiver Certificate with a Receiver Certificate Alias");

    cryptParams ().setCertificate (null).setAlias (sAlias);
    return thisAsT ();
  }

  /**
   * @return The receiver AS4 endpoint URL. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String endpointURL ()
  {
    return m_sEndpointURL;
  }

  /**
   * Set an receiver AS4 endpoint URL, independent of its usability.
   *
   * @param sEndointURL
   *        The endpoint URL to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE endpointURL (@Nullable final String sEndointURL)
  {
    m_sEndpointURL = sEndointURL;
    return thisAsT ();
  }

  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <AS4OutgoingAttachment> attachments ()
  {
    return m_aAttachments;
  }

  @Nonnull
  @ReturnsMutableCopy
  public final ICommonsList <AS4OutgoingAttachment> getAllAttachments ()
  {
    return m_aAttachments.getClone ();
  }

  /**
   * Add an optional attachment
   *
   * @param a
   *        The attachment to be added. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE addAttachment (@Nullable final AS4OutgoingAttachment.Builder a)
  {
    return addAttachment (a == null ? null : a.build ());
  }

  /**
   * Add an optional attachment
   *
   * @param a
   *        The attachment to be added. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE addAttachment (@Nullable final AS4OutgoingAttachment a)
  {
    if (a != null)
      m_aAttachments.add (a);
    return thisAsT ();
  }

  /**
   * Set optional attachment. All existing attachments are overridden.
   *
   * @param a
   *        The attachment to be set. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE attachment (@Nullable final AS4OutgoingAttachment.Builder a)
  {
    return attachment (a == null ? null : a.build ());
  }

  /**
   * Set optional attachment. All existing attachments are overridden.
   *
   * @param a
   *        The attachment to be set. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE attachment (@Nullable final AS4OutgoingAttachment a)
  {
    if (a == null)
      m_aAttachments.clear ();
    else
      m_aAttachments.set (a);
    return thisAsT ();
  }

  /**
   * Set optional attachments. All existing attachments are overridden.
   *
   * @param a
   *        The attachment to be set. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE attachments (@Nullable final AS4OutgoingAttachment... a)
  {
    m_aAttachments.setAll (a);
    return thisAsT ();
  }

  /**
   * Set optional attachments. All existing attachments are overridden.
   *
   * @param a
   *        The attachment to be set. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE attachments (@Nullable final Iterable <? extends AS4OutgoingAttachment> a)
  {
    m_aAttachments.setAll (a);
    return thisAsT ();
  }

  /**
   * @return <code>true</code> if the message is forced into the MIME format,
   *         <code>false</code> otherwise.
   * @since 3.0.0
   */
  @Nullable
  public final boolean forceMimeMessage ()
  {
    return m_bForceMimeMessage;
  }

  /**
   * Enable the enforcement of packaging the AS4 user message in a MIME message.
   *
   * @param b
   *        <code>true</code> to enforce it, <code>false</code> to make it
   *        dynamic.
   * @return this for chaining
   * @since 2.5.1
   */
  @Nonnull
  public final IMPLTYPE forceMimeMessage (final boolean b)
  {
    m_bForceMimeMessage = b;
    return thisAsT ();
  }

  /**
   * @return The optional Ebms3 Signal Message Consumer. May be
   *         <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final IAS4SignalMessageConsumer signalMsgConsumer ()
  {
    return m_aSignalMsgConsumer;
  }

  /**
   * Set an optional Ebms3 Signal Message Consumer. If this consumer is set, the
   * response is trying to be parsed as a Signal Message. This method is
   * optional and must not be called prior to sending.
   *
   * @param aSignalMsgConsumer
   *        The optional signal message consumer. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE signalMsgConsumer (@Nullable final IAS4SignalMessageConsumer aSignalMsgConsumer)
  {
    m_aSignalMsgConsumer = aSignalMsgConsumer;
    return thisAsT ();
  }

  @Override
  @Nonnull
  @OverridingMethodsMustInvokeSuper
  protected ESuccess finishFields () throws Phase4Exception
  {
    if (super.finishFields ().isFailure ())
      return ESuccess.FAILURE;

    if (m_aPMode == null && pmodeResolver () != null)
    {
      // Create a default PMode template
      m_aPMode = pmodeResolver ().findPMode (m_sPModeID, "s", "a", "i", "r", "a", null);
      if (m_aPMode == null)
        LOGGER.warn ("No PMode was provided, and the PMode Resolver delivered a null-PMode as well");
    }

    return ESuccess.SUCCESS;
  }

  @Override
  @OverridingMethodsMustInvokeSuper
  public boolean isEveryRequiredFieldSet ()
  {
    if (!super.isEveryRequiredFieldSet ())
      return false;

    if (m_aPMode == null)
    {
      LOGGER.warn ("The field 'PMode' is not set");
      return false;
    }

    // m_sServiceType may be null
    // m_sService may be null
    // m_sAction may be null
    // m_sAgreementRef may be null
    // m_sAgreementType may be null
    // m_sPModeID may be null

    // m_sFromPartyIDType may be null
    if (StringHelper.hasNoText (m_sFromPartyID))
    {
      LOGGER.warn ("The field 'fromPartyID' is not set");
      return false;
    }
    if (StringHelper.hasNoText (m_sFromRole))
    {
      LOGGER.warn ("The field 'fromRole' is not set");
      return false;
    }

    // m_sToPartyIDType may be null
    if (StringHelper.hasNoText (m_sToPartyID))
    {
      LOGGER.warn ("The field 'toPartyID' is not set");
      return false;
    }
    if (StringHelper.hasNoText (m_sToRole))
    {
      LOGGER.warn ("The field 'toRole' is not set");
      return false;
    }

    // m_sConversationID is optional

    // m_aMessageProperties is final

    if (StringHelper.hasNoText (m_sEndpointURL))
    {
      LOGGER.warn ("The field 'endpointURL' is not set");
      return false;
    }

    // m_aAttachments may be null

    // m_aSignalMsgConsumer may be null

    // All valid
    return true;
  }

  /**
   * This method applies all builder parameters onto the user message, except
   * the attachments.
   *
   * @param aUserMsg
   *        The user message the parameters should be applied to. May not be
   *        <code>null</code>.
   */
  @OverridingMethodsMustInvokeSuper
  protected void applyToUserMessage (@Nonnull final AS4ClientUserMessage aUserMsg)
  {
    if (m_aCustomHttpPoster != null)
    {
      // Special case
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Using a custom IHttpPoster implementation: " + m_aCustomHttpPoster);
      aUserMsg.setHttpPoster (m_aCustomHttpPoster);
    }
    else
    {
      // Default HTTP poster
      aUserMsg.getHttpPoster ().setHttpClientFactory (m_aHttpClientFactory);
      // Otherwise Oxalis dies
      aUserMsg.getHttpPoster ().setQuoteHttpHeaders (false);
    }

    aUserMsg.setSoapVersion (m_eSoapVersion);
    aUserMsg.setSendingDateTime (m_aSendingDateTime);
    // Set the keystore/truststore parameters
    aUserMsg.setCryptoFactorySign (m_aCryptoFactorySign);
    aUserMsg.setCryptoFactoryCrypt (m_aCryptoFactoryCrypt);

    // Copy all values
    m_aCryptParams.cloneTo (aUserMsg.cryptParams ());
    m_aSigningParams.cloneTo (aUserMsg.signingParams ());

    aUserMsg.setPMode (m_aPMode, true);

    // Set after PMode
    if (m_aHttpRetrySettings != null)
      aUserMsg.httpRetrySettings ().assignFrom (m_aHttpRetrySettings);

    aUserMsg.setAgreementRefValue (m_sAgreementRef);
    aUserMsg.setAgreementTypeValue (m_sAgreementType);
    if (StringHelper.hasText (m_sPModeID))
      aUserMsg.setPModeID (m_sPModeID);
    else
      aUserMsg.setPModeIDFactory (x -> null);
    aUserMsg.setServiceType (m_sServiceType);
    aUserMsg.setServiceValue (m_sService);
    aUserMsg.setAction (m_sAction);
    if (StringHelper.hasText (m_sMessageID))
      aUserMsg.setMessageID (m_sMessageID);
    if (StringHelper.hasText (m_sRefToMessageID))
      aUserMsg.setRefToMessageID (m_sRefToMessageID);
    // Empty conversation ID is okay
    aUserMsg.setConversationID (m_sConversationID != null ? m_sConversationID
                                                          : MessageHelperMethods.createRandomConversationID ());

    aUserMsg.setFromPartyIDType (m_sFromPartyIDType);
    aUserMsg.setFromPartyID (m_sFromPartyID);
    aUserMsg.setFromRole (m_sFromRole);

    aUserMsg.setToPartyIDType (m_sToPartyIDType);
    aUserMsg.setToPartyID (m_sToPartyID);
    aUserMsg.setToRole (m_sToRole);

    for (final MessageProperty aItem : m_aMessageProperties)
      aUserMsg.ebms3Properties ().add (aItem.getAsEbms3Property ());

    aUserMsg.setForceMimeMessage (m_bForceMimeMessage);
  }

  /**
   * This is a sanity method that encapsulates all the sending checks that are
   * necessary to determine overall sending success or error.<br>
   * Note: this method is not thread-safe, because it changes the signal message
   * consumer internally.
   *
   * @return {@link EAS4UserMessageSendResult#SUCCESS} only if all parameters
   *         are correct, HTTP transmission was successful and if a positive AS4
   *         Receipt was returned. Never <code>null</code>.
   * @since 0.13.0
   */
  @Nonnull
  public final EAS4UserMessageSendResult sendMessageAndCheckForReceipt ()
  {
    // This information might be crucial to determine what went wrong
    return sendMessageAndCheckForReceipt (ex -> LOGGER.error ("Exception sending AS4 user message", ex));
  }

  /**
   * This is a sanity method that encapsulates all the sending checks that are
   * necessary to determine overall sending success or error.<br>
   * Note: this method is not thread-safe, because it changes the signal message
   * consumer internally.
   *
   * @param aExceptionConsumer
   *        An optional Consumer that takes an eventually thrown
   *        {@link Phase4Exception}. May be <code>null</code>.
   * @return {@link EAS4UserMessageSendResult#SUCCESS} only if all parameters
   *         are correct, HTTP transmission was successful and if a positive AS4
   *         Receipt was returned. Never <code>null</code>.
   * @since 1.0.0-rc1
   */
  @Nonnull
  public final EAS4UserMessageSendResult sendMessageAndCheckForReceipt (@Nullable final Consumer <? super Phase4Exception> aExceptionConsumer)
  {
    final IAS4SignalMessageConsumer aOld = m_aSignalMsgConsumer;
    try
    {
      // Store the received signal message
      final Wrapper <Ebms3SignalMessage> aSignalMsgKeeper = new Wrapper <> ();
      m_aSignalMsgConsumer = aOld == null ? (aSignalMsg, aMMD, aIncomingState) -> aSignalMsgKeeper.set (aSignalMsg)
                                          : (aSignalMsg, aMMD, aIncomingState) -> {
                                            aSignalMsgKeeper.set (aSignalMsg);
                                            aOld.handleSignalMessage (aSignalMsg, aMMD, aIncomingState);
                                          };

      // Main sending
      if (sendMessage ().isFailure ())
      {
        // Parameters are missing/incorrect
        return EAS4UserMessageSendResult.INVALID_PARAMETERS;
      }

      final Ebms3SignalMessage aSignalMsg = aSignalMsgKeeper.get ();
      if (aSignalMsg == null)
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Failed to get a SignalMessage as the response");

        // Unexpected response - invalid XML or at least no Ebms3 signal message
        return EAS4UserMessageSendResult.NO_SIGNAL_MESSAGE_RECEIVED;
      }

      if (aSignalMsg.hasErrorEntries ())
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("The received SignalMessage contains at least one error");

        // An error was returned from the other side
        // Errors have precedence over receipts
        return EAS4UserMessageSendResult.AS4_ERROR_MESSAGE_RECEIVED;
      }

      if (aSignalMsg.getReceipt () != null)
      {
        // A receipt was returned - this is deemed success
        return EAS4UserMessageSendResult.SUCCESS;
      }

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("The SignalMessage contains neither Errors nor a Receipt - unexpected SignalMessage layout.");

      // Neither an error nor a receipt was returned - this is weird
      return EAS4UserMessageSendResult.INVALID_SIGNAL_MESSAGE_RECEIVED;
    }
    catch (final Phase4Exception ex)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("An exception occurred sending out the AS4 message", ex);

      if (aExceptionConsumer != null)
        aExceptionConsumer.accept (ex);
      // Something went wrong - see the logs
      return EAS4UserMessageSendResult.TRANSPORT_ERROR;
    }
    finally
    {
      // Restore the original value
      m_aSignalMsgConsumer = aOld;
    }
  }
}
