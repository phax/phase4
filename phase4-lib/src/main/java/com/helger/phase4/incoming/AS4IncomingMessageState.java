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
package com.helger.phase4.incoming;

import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.Locale;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.wss4j.dom.WSConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.attr.AttributeContainerAny;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.datetime.XMLOffsetDateTime;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.mpc.IMPC;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.profile.IAS4Profile;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * This class keeps track of the status of an incoming message. It is basically
 * a String to any map.<br>
 * Keys starting with <code>phase4.</code> are reserved for internal use.<br>
 * Instances of this object are only modified in the SOAP header handlers.<br>
 * Old name before v3: <code>AS4MessageState</code>
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class AS4IncomingMessageState extends AttributeContainerAny <String> implements IAS4IncomingMessageState
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4IncomingMessageState.class);

  private static final String KEY_EBMS3_MESSAGING = "phase4.ebms3.messaging";
  private static final String KEY_PMODE = "phase4.pmode";
  private static final String KEY_MPC = "phase4.mpc";
  private static final String KEY_ORIGINAL_SOAP_DOCUMENT = "phase4.soap.document";
  private static final String KEY_ORIGINAL_ATTACHMENT_LIST = "phase4.soap.attachmentlist";
  private static final String KEY_DECRYPTED_SOAP_DOCUMENT = "phase4.soap.decrypted.document";
  private static final String KEY_DECRYPTED_ATTACHMENT_LIST = "phase4.soap.decrypted.attachmentlist";
  private static final String KEY_COMPRESSED_ATTACHMENT_IDS = "phase4.compressed.attachment.ids";
  private static final String KEY_SOAP_BODY_PAYLOAD_PRESENT = "phase4.soap.body.payload.present";
  private static final String KEY_INITIATOR_ID = "phase4.initiator.id";
  private static final String KEY_RESPONDER_ID = "phase4.responder.id";
  private static final String KEY_USED_CERTIFICATE = "phase4.used.certificate";
  private static final String KEY_EFFECTIVE_PMODE_LEG = "phase4.pmode.effective.leg";
  private static final String KEY_EFFECTIVE_PMODE_LEG_NUMBER = "phase4.pmode.effective.leg.number";
  private static final String KEY_WSS4J_SECURITY_ACTIONS = "phase4.soap.wss4j-security-actions";
  private static final String KEY_WSS4J_EXCEPTION = "phase4.soap.wss4j-exception";
  private static final String KEY_PHASE4_PROFILE = "phase4.profile";
  private static final String KEY_AS4_MESSAGE_ID = "phase4.message.id";
  private static final String KEY_AS4_REF_TO_MESSAGE_ID = "phase4.ref.to.message.id";
  private static final String KEY_AS4_MESSAGE_TIMESTAMP = "phase4.message.timestamp";
  private static final String KEY_IS_PING_MESSAGE = "phase4.is.ping.message";
  private static final String KEY_SOAP_BODY_PAYLOAD_NODE = "phase4.soap.body.first.child";
  private static final String KEY_SOAP_HEADER_ELEMENT_PROCESSING_SUCCESSFUL = "phase4.soap.header.element.processing.successful";
  private static final String KEY_CRYPTO_FACTORY_SIGN = "phase4.crypto-factory.sign";
  private static final String KEY_CRYPTO_FACTORY_CRYPT = "phase4.crypto-factory.crypt";

  private final OffsetDateTime m_aReceiptDT;
  private final ESoapVersion m_eSoapVersion;
  private final AS4ResourceHelper m_aResHelper;
  private final Locale m_aLocale;

  public AS4IncomingMessageState (@Nonnull final ESoapVersion eSoapVersion,
                                  @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                  @Nonnull final Locale aLocale)
  {
    m_aReceiptDT = MetaAS4Manager.getTimestampMgr ().getCurrentDateTime ();
    m_eSoapVersion = ValueEnforcer.notNull (eSoapVersion, "SOAPVersion");
    m_aResHelper = ValueEnforcer.notNull (aResHelper, "ResHelper");
    m_aLocale = ValueEnforcer.notNull (aLocale, "Locale");
  }

  @Nonnull
  public OffsetDateTime getReceiptDT ()
  {
    return m_aReceiptDT;
  }

  @Nonnull
  public ESoapVersion getSoapVersion ()
  {
    return m_eSoapVersion;
  }

  @Nonnull
  public AS4ResourceHelper getResourceHelper ()
  {
    return m_aResHelper;
  }

  @Nonnull
  public Locale getLocale ()
  {
    return m_aLocale;
  }

  @Nullable
  public Ebms3Messaging getMessaging ()
  {
    return getCastedValue (KEY_EBMS3_MESSAGING);
  }

  public void setMessaging (@Nullable final Ebms3Messaging aMessaging)
  {
    putIn (KEY_EBMS3_MESSAGING, aMessaging);
  }

  @Nullable
  public IPMode getPMode ()
  {
    return getCastedValue (KEY_PMODE);
  }

  /**
   * Set the PMode to be used. Called only from Ebms3 header processor
   *
   * @param aPMode
   *        PMode Config. May be <code>null</code>.
   */
  public void setPMode (@Nullable final IPMode aPMode)
  {
    putIn (KEY_PMODE, aPMode);
  }

  @Nullable
  public Document getOriginalSoapDocument ()
  {
    return getCastedValue (KEY_ORIGINAL_SOAP_DOCUMENT);
  }

  public void setOriginalSoapDocument (@Nullable final Document aDocument)
  {
    putIn (KEY_ORIGINAL_SOAP_DOCUMENT, aDocument);
  }

  @Nullable
  public ICommonsList <WSS4JAttachment> getOriginalAttachments ()
  {
    return getCastedValue (KEY_ORIGINAL_ATTACHMENT_LIST);
  }

  public void setOriginalAttachments (@Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    putIn (KEY_ORIGINAL_ATTACHMENT_LIST, aAttachments);
  }

  @Nullable
  public Document getDecryptedSoapDocument ()
  {
    return getCastedValue (KEY_DECRYPTED_SOAP_DOCUMENT);
  }

  public void setDecryptedSoapDocument (@Nullable final Document aDocument)
  {
    putIn (KEY_DECRYPTED_SOAP_DOCUMENT, aDocument);
  }

  @Nullable
  public ICommonsList <WSS4JAttachment> getDecryptedAttachments ()
  {
    return getCastedValue (KEY_DECRYPTED_ATTACHMENT_LIST);
  }

  public void setDecryptedAttachments (@Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    putIn (KEY_DECRYPTED_ATTACHMENT_LIST, aAttachments);
  }

  @Nullable
  public ICommonsMap <String, EAS4CompressionMode> getCompressedAttachmentIDs ()
  {
    return getCastedValue (KEY_COMPRESSED_ATTACHMENT_IDS);
  }

  public void setCompressedAttachmentIDs (@Nullable final ICommonsMap <String, EAS4CompressionMode> aIDs)
  {
    putIn (KEY_COMPRESSED_ATTACHMENT_IDS, aIDs);
  }

  @Nullable
  public IMPC getMPC ()
  {
    return getCastedValue (KEY_MPC);
  }

  public void setMPC (@Nullable final IMPC aMPC)
  {
    putIn (KEY_MPC, aMPC);
  }

  public boolean isSoapBodyPayloadPresent ()
  {
    return getAsBoolean (KEY_SOAP_BODY_PAYLOAD_PRESENT, false);
  }

  public void setSoapBodyPayloadPresent (final boolean bHasSoapBodyPayload)
  {
    putIn (KEY_SOAP_BODY_PAYLOAD_PRESENT, bHasSoapBodyPayload);
  }

  @Nullable
  public String getInitiatorID ()
  {
    return getAsString (KEY_INITIATOR_ID);
  }

  public void setInitiatorID (@Nullable final String sInitiatorID)
  {
    putIn (KEY_INITIATOR_ID, sInitiatorID);
  }

  @Nullable
  public String getResponderID ()
  {
    return getAsString (KEY_RESPONDER_ID);
  }

  public void setResponderID (@Nullable final String sResponderID)
  {
    putIn (KEY_RESPONDER_ID, sResponderID);
  }

  @Nullable
  public X509Certificate getUsedCertificate ()
  {
    return getCastedValue (KEY_USED_CERTIFICATE);
  }

  public void setUsedCertificate (@Nullable final X509Certificate aCert)
  {
    putIn (KEY_USED_CERTIFICATE, aCert);
  }

  @Nullable
  public PModeLeg getEffectivePModeLeg ()
  {
    return getCastedValue (KEY_EFFECTIVE_PMODE_LEG);
  }

  @CheckForSigned
  public int getEffectivePModeLegNumber ()
  {
    return getAsInt (KEY_EFFECTIVE_PMODE_LEG_NUMBER, -1);
  }

  public void setEffectivePModeLeg (@Nonnegative final int nLegNumber, @Nullable final PModeLeg aEffectiveLeg)
  {
    ValueEnforcer.isTrue (nLegNumber == 1 || nLegNumber == 2, "LegNumber must be 1 or 2");
    putIn (KEY_EFFECTIVE_PMODE_LEG, aEffectiveLeg);
    putIn (KEY_EFFECTIVE_PMODE_LEG_NUMBER, nLegNumber);
  }

  public int getSoapWSS4JSecurityActions ()
  {
    return getAsInt (KEY_WSS4J_SECURITY_ACTIONS, 0);
  }

  public void setSoapWSS4JSecurityActions (final int nSecurityActions)
  {
    putIn (KEY_WSS4J_SECURITY_ACTIONS, nSecurityActions);
  }

  public boolean isSoapSignatureChecked ()
  {
    return (getSoapWSS4JSecurityActions () & WSConstants.SIGN) == WSConstants.SIGN;
  }

  public boolean isSoapDecrypted ()
  {
    return (getSoapWSS4JSecurityActions () & WSConstants.ENCR) == WSConstants.ENCR;
  }

  @Nullable
  public Exception getSoapWSS4JException ()
  {
    return getCastedValue (KEY_WSS4J_EXCEPTION);
  }

  public void setSoapWSS4JException (@Nullable final Exception aException)
  {
    putIn (KEY_WSS4J_EXCEPTION, aException);
  }

  @Nullable
  public IAS4Profile getAS4Profile ()
  {
    return getCastedValue (KEY_PHASE4_PROFILE);
  }

  /**
   * Set the AS4 profile of the message.
   *
   * @param aProfile
   *        The internal AS4 profile. May be <code>null</code>.
   */
  public void setAS4Profile (@Nullable final IAS4Profile aProfile)
  {
    putIn (KEY_PHASE4_PROFILE, aProfile);
  }

  @Nullable
  public String getMessageID ()
  {
    return getAsString (KEY_AS4_MESSAGE_ID);
  }

  /**
   * Set the AS4 message ID of the current message.
   *
   * @param sMessageID
   *        The ID to be set. May be <code>null</code>.
   */
  public void setMessageID (@Nullable final String sMessageID)
  {
    final String sOldMessageID = getMessageID ();
    if (sOldMessageID != null && !sOldMessageID.equals (sMessageID))
      LOGGER.warn ("Overwriting the AS4 message ID from '" + sOldMessageID + "' to '" + sMessageID + "'");
    putIn (KEY_AS4_MESSAGE_ID, sMessageID);
  }

  @Nullable
  public String getRefToMessageID ()
  {
    return getAsString (KEY_AS4_REF_TO_MESSAGE_ID);
  }

  /**
   * Set the AS4 "reference to message ID" from the current message.
   *
   * @param sRefMessageID
   *        The ID to be set. May be <code>null</code>.
   * @since 1.2.0
   */
  public void setRefToMessageID (@Nullable final String sRefMessageID)
  {
    putIn (KEY_AS4_REF_TO_MESSAGE_ID, sRefMessageID);
  }

  @Nullable
  public XMLOffsetDateTime getMessageTimestamp ()
  {
    return getCastedValue (KEY_AS4_MESSAGE_TIMESTAMP);
  }

  /**
   * Set the AS4 message timestamp of the current message.
   *
   * @param aMessageTimestamp
   *        The timestamp to be set. May be <code>null</code>.
   * @since 1.2.0
   */
  public void setMessageTimestamp (@Nullable final XMLOffsetDateTime aMessageTimestamp)
  {
    putIn (KEY_AS4_MESSAGE_TIMESTAMP, aMessageTimestamp);
  }

  public boolean isPingMessage ()
  {
    return getAsBoolean (KEY_IS_PING_MESSAGE, false);
  }

  public void setPingMessage (final boolean bIsPingMessage)
  {
    putIn (KEY_IS_PING_MESSAGE, bIsPingMessage);
  }

  @Nullable
  public Node getSoapBodyPayloadNode ()
  {
    return getCastedValue (KEY_SOAP_BODY_PAYLOAD_NODE);
  }

  public void setSoapBodyPayloadNode (@Nullable final Node aPayloadNode)
  {
    putIn (KEY_SOAP_BODY_PAYLOAD_NODE, aPayloadNode);
  }

  public boolean isSoapHeaderElementProcessingSuccessful ()
  {
    return getAsBoolean (KEY_SOAP_HEADER_ELEMENT_PROCESSING_SUCCESSFUL, false);
  }

  public void setSoapHeaderElementProcessingSuccessful (final boolean bSuccess)
  {
    putIn (KEY_SOAP_HEADER_ELEMENT_PROCESSING_SUCCESSFUL, bSuccess);
  }

  @Nullable
  public IAS4CryptoFactory getCryptoFactorySign ()
  {
    return getCastedValue (KEY_CRYPTO_FACTORY_SIGN);
  }

  @Nullable
  public void setCryptoFactorySign (@Nullable final IAS4CryptoFactory aCryptoFactorySign)
  {
    putIn (KEY_CRYPTO_FACTORY_SIGN, aCryptoFactorySign);
  }

  @Nullable
  public IAS4CryptoFactory getCryptoFactoryCrypt ()
  {
    return getCastedValue (KEY_CRYPTO_FACTORY_CRYPT);
  }

  @Nullable
  public void setCryptoFactoryCrypt (@Nullable final IAS4CryptoFactory aCryptoFactoryCrypt)
  {
    putIn (KEY_CRYPTO_FACTORY_CRYPT, aCryptoFactoryCrypt);
  }

  @Override
  public boolean equals (final Object o)
  {
    // New fields, no change
    return super.equals (o);
  }

  @Override
  public int hashCode ()
  {
    // New fields, no change
    return super.hashCode ();
  }
}
