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

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.datetime.XMLOffsetDateTime;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.ebms3header.Ebms3PullRequest;
import com.helger.phase4.ebms3header.Ebms3Receipt;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.mpc.IMPC;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.profile.IAS4Profile;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * Read-only AS4 message state for incoming messages.<br/>
 * Old name before v3: <code>IAS4MessageState</code>
 *
 * @author Philip Helger
 */
public interface IAS4IncomingMessageState
{
  /**
   * @return Date and time when the receipt started. This must be set in the
   *         implementation and never <code>null</code>.
   */
  @Nonnull
  OffsetDateTime getReceiptDT ();

  /**
   * @return The SOAP version of the current request as specified in the
   *         constructor. Never <code>null</code>.
   * @since v0.9.8
   */
  @Nonnull
  ESoapVersion getSoapVersion ();

  /**
   * @return The resource manager as specified in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  AS4ResourceHelper getResourceHelper ();

  /**
   * @return The request locale to use. Never <code>null</code>.
   */
  @Nonnull
  Locale getLocale ();

  /**
   * @return The parent of the user message/signal message for further
   *         evaluation.
   */
  @Nullable
  Ebms3Messaging getMessaging ();

  /**
   * @return The EBMS user message. May be <code>null</code>.
   * @since v0.9.7
   */
  @Nullable
  default Ebms3UserMessage getEbmsUserMessage ()
  {
    final Ebms3Messaging aMessaging = getMessaging ();
    return aMessaging != null && aMessaging.hasUserMessageEntries () ? aMessaging.getUserMessageAtIndex (0) : null;
  }

  /**
   * @return The EBMS signal message. May be <code>null</code>.
   * @since v0.9.7
   */
  @Nullable
  default Ebms3SignalMessage getEbmsSignalMessage ()
  {
    final Ebms3Messaging aMessaging = getMessaging ();
    return aMessaging != null && aMessaging.hasSignalMessageEntries () ? aMessaging.getSignalMessageAtIndex (0) : null;
  }

  /**
   * @return The EBMS signal message error. May be <code>null</code>.
   * @since v0.9.7
   */
  @Nullable
  default Ebms3Error getEbmsError ()
  {
    final Ebms3SignalMessage aEbmsSignalMessage = getEbmsSignalMessage ();
    return aEbmsSignalMessage != null && aEbmsSignalMessage.hasErrorEntries () ? aEbmsSignalMessage.getErrorAtIndex (0)
                                                                               : null;
  }

  /**
   * @return The EBMS signal message pull request. May be <code>null</code>.
   * @since v0.9.7
   */
  @Nullable
  default Ebms3PullRequest getEbmsPullRequest ()
  {
    final Ebms3SignalMessage aEbmsSignalMessage = getEbmsSignalMessage ();
    return aEbmsSignalMessage != null ? aEbmsSignalMessage.getPullRequest () : null;
  }

  /**
   * @return The EBMS signal message receipt. May be <code>null</code>.
   * @since v0.9.7
   */
  @Nullable
  default Ebms3Receipt getEbmsReceipt ()
  {
    final Ebms3SignalMessage aEbmsSignalMessage = getEbmsSignalMessage ();
    return aEbmsSignalMessage != null ? aEbmsSignalMessage.getReceipt () : null;
  }

  /**
   * @return the PMode that is used with the current message. May be
   *         <code>null</code> if none was found.
   */
  @Nullable
  IPMode getPMode ();

  /**
   * @return get the original SOAP document, only the entire document no
   *         attachment. This might by encrypted.
   * @see #hasDecryptedSoapDocument()
   * @see #getDecryptedSoapDocument()
   * @since v0.9.8
   */
  @Nullable
  Document getOriginalSoapDocument ();

  /**
   * @return <code>true</code> if an original SOAP document is present,
   *         <code>false</code> if not.
   * @see #getOriginalSoapDocument()
   */
  default boolean hasOriginalSoapDocument ()
  {
    return getOriginalSoapDocument () != null;
  }

  /**
   * @return has saved the original attachment, can be encrypted or not depends
   *         if encryption is used or not.
   * @see #hasDecryptedAttachments()
   * @see #getDecryptedAttachments()
   */
  @Nullable
  ICommonsList <WSS4JAttachment> getOriginalAttachments ();

  /**
   * @return <code>true</code> if original attachments are present,
   *         <code>false</code> if not.
   * @see #getOriginalAttachments()
   */
  default boolean hasOriginalAttachments ()
  {
    final ICommonsList <WSS4JAttachment> aMap = getOriginalAttachments ();
    return aMap != null && aMap.isNotEmpty ();
  }

  /**
   * @return get the decrypted SOAP document, only the entire document no
   *         attachment
   * @see #hasOriginalSoapDocument()
   * @see #getOriginalSoapDocument()
   * @since v0.9.8
   */
  @Nullable
  Document getDecryptedSoapDocument ();

  /**
   * @return <code>true</code> of a decrypted SOAP document is present,
   *         <code>false</code> if not.
   * @see #getDecryptedSoapDocument()
   * @see #getOriginalSoapDocument()
   */
  default boolean hasDecryptedSoapDocument ()
  {
    return getDecryptedSoapDocument () != null;
  }

  /**
   * @return The effectively decrypted SOAP document.
   * @since 3.0.0
   * @see #getDecryptedSoapDocument()
   * @see #hasDecryptedSoapDocument()
   * @see #getOriginalSoapDocument()
   */
  @Nullable
  default Document getEffectiveDecryptedSoapDocument ()
  {
    Document ret = getDecryptedSoapDocument ();
    if (ret == null)
      ret = getOriginalSoapDocument ();
    return ret;
  }

  /**
   * @return Getting decrypted attachment, if there were encrypted attachments
   *         to begin with. May be <code>null</code>.
   * @see #getOriginalAttachments()
   */
  @Nullable
  ICommonsList <WSS4JAttachment> getDecryptedAttachments ();

  /**
   * @return <code>true</code> if a decrypted attachments are present,
   *         <code>false</code> if not.
   */
  default boolean hasDecryptedAttachments ()
  {
    final ICommonsList <WSS4JAttachment> aList = getDecryptedAttachments ();
    return aList != null && aList.isNotEmpty ();
  }

  /**
   * @return IDs from all compressed attachments and/or payload. May be
   *         <code>null</code>.
   */
  @Nullable
  ICommonsMap <String, EAS4CompressionMode> getCompressedAttachmentIDs ();

  default boolean hasCompressedAttachmentIDs ()
  {
    final ICommonsMap <String, EAS4CompressionMode> aMap = getCompressedAttachmentIDs ();
    return aMap != null && aMap.isNotEmpty ();
  }

  /**
   * @param sID
   *        id to look up´. May be <code>null</code>.
   * @return Looks up if a compression mode with the id sID exists and returns
   *         the mode else <code>null</code>
   */
  @Nullable
  default EAS4CompressionMode getAttachmentCompressionMode (@Nullable final String sID)
  {
    final ICommonsMap <String, EAS4CompressionMode> aIDs = getCompressedAttachmentIDs ();
    return aIDs == null ? null : aIDs.get (sID);
  }

  /**
   * @param sID
   *        the id to look up. May be <code>null</code>.
   * @return looks up if the compressed attachment contain the given ID
   */
  default boolean containsCompressedAttachmentID (@Nullable final String sID)
  {
    final ICommonsMap <String, EAS4CompressionMode> aIDs = getCompressedAttachmentIDs ();
    return aIDs != null && aIDs.containsKey (sID);
  }

  /**
   * @return the MPC that is used in the current message exchange. May be
   *         <code>null</code>.
   */
  @Nullable
  IMPC getMPC ();

  /**
   * @return <code>true</code> if an MPC is set, <code>false</code> if not.
   * @see #getMPC()
   */
  default boolean hasMPC ()
  {
    return getMPC () != null;
  }

  /**
   * @return <code>true</code> if a payload in the soap body is present, else
   *         <code>false</code>
   * @see #getSoapBodyPayloadNode()
   */
  boolean isSoapBodyPayloadPresent ();

  /**
   * @return initiator set in the usermessage if the incoming message is a
   *         UserMessage. This is the content of the FromParty/PartyId element
   *         and in Peppol reflects the C2. May be <code>null</code>.
   */
  @Nullable
  String getInitiatorID ();

  /**
   * @return <code>true</code> if an initiator ID was part of the UserMessage,
   *         <code>false</code> if not.
   * @see #getInitiatorID()
   */
  default boolean hasInitiatorID ()
  {
    return StringHelper.hasText (getInitiatorID ());
  }

  /**
   * @return responder set in the usermessage if the incoming message is a
   *         UserMessage. This is the content of the ToParty/PartyId element and
   *         in Peppol reflects the C3. May be <code>null</code>.
   */
  @Nullable
  String getResponderID ();

  /**
   * @return <code>true</code> if a responder ID was part of the UserMessage,
   *         <code>false</code> if not.
   * @see #getResponderID()
   */
  default boolean hasResponderID ()
  {
    return StringHelper.hasText (getResponderID ());
  }

  /**
   * @return The first provided certificate in the incoming message. Usually the
   *         certificate that was used for signing. May be <code>null</code>.
   * @see #hasUsedCertificate()
   */
  @Nullable
  X509Certificate getUsedCertificate ();

  /**
   * @return <code>true</code> if a certificate is provided, <code>false</code>
   *         if not.
   * @see #getUsedCertificate()
   */
  default boolean hasUsedCertificate ()
  {
    return getUsedCertificate () != null;
  }

  /**
   * @return The effective leg to use. May be leg 1 or leg 2 of the PMode. If no
   *         PMode was found, no PModeLeg is present.
   * @see #getPMode()
   */
  @Nullable
  PModeLeg getEffectivePModeLeg ();

  /**
   * @return 1 or 2, depending on the used leg. Any other value indicates
   *         "undefined". If no PMode was found, no PModeLeg is present.
   * @see #getEffectivePModeLeg()
   */
  @CheckForSigned
  int getEffectivePModeLegNumber ();

  /**
   * @return A bit set of the WSS4J security action tags found. 0 for nothing.
   *         See class org.apache.wss4j.dom.WSContants line 326ff for the
   *         constants.
   * @since v0.9.8
   */
  @Nonnegative
  int getSoapWSS4JSecurityActions ();

  /**
   * @return <code>true</code> if the incoming message was signed and the
   *         signature was verified, <code>false</code> otherwise.
   */
  boolean isSoapSignatureChecked ();

  /**
   * @return <code>true</code> if the incoming message was decrypted,
   *         <code>false</code> otherwise.
   */
  boolean isSoapDecrypted ();

  /**
   * @return An Exception that occurred during processing the incoming SOAP
   *         WS-Security handler. If this is a
   *         <code>org.apache.wss4j.common.ext.WSSecurityException</code>
   *         exception something happened on the security level.
   * @since 0.9.11
   */
  @Nullable
  Exception getSoapWSS4JException ();

  /**
   * @return <code>true</code> if a SOAP WSS4J exception is present,
   *         <code>false</code> if not.
   * @see #getSoapWSS4JException()
   */
  default boolean hasSoapWSS4JException ()
  {
    return getSoapWSS4JException () != null;
  }

  /**
   * @return The phase4 profile ID to be used. May be <code>null</code>.
   * @since v0.9.7
   */
  @Nullable
  default String getProfileID ()
  {
    final IAS4Profile aProfile = getAS4Profile ();
    return aProfile == null ? null : aProfile.getID ();
  }

  /**
   * @return The phase4 profile to be used. May be <code>null</code> in case it
   *         could not be determined.
   * @since v2.5.3
   */
  @Nullable
  IAS4Profile getAS4Profile ();

  /**
   * @return The AS4 message ID. Source is the <code>MessageInfo</code> element.
   *         May be <code>null</code>.
   * @since v0.9.7
   */
  @Nullable
  String getMessageID ();

  /**
   * @return The AS4 "reference to message ID". This value is optional in the
   *         headers. Source is the <code>MessageInfo</code> element. May be
   *         <code>null</code>.
   * @since v1.2.0
   */
  @Nullable
  String getRefToMessageID ();

  /**
   * @return The AS4 provided message timestamp. This value is mandatory in the
   *         source <code>MessageInfo</code> element. May be <code>null</code>.
   * @since v1.2.0
   */
  @Nullable
  XMLOffsetDateTime getMessageTimestamp ();

  /**
   * @return <code>true</code> if the incoming message is an AS4 ping message,
   *         <code>false</code> otherwise.
   * @since v0.9.7
   */
  boolean isPingMessage ();

  /**
   * @return The child of the SOAP Body node or <code>null</code>. That is
   *         always decrypted.
   * @since v0.9.8
   */
  @Nullable
  Node getSoapBodyPayloadNode ();

  /**
   * @return <code>true</code> if SOAP header element processing was successful,
   *         <code>false</code> if not. If not, than many other values are also
   *         not set and at least one error is in the error list.
   * @since v0.9.7
   */
  boolean isSoapHeaderElementProcessingSuccessful ();

  /**
   * @return The crypto factory that was used to verify an eventually contained
   *         signature. May be <code>null</code>. If this is
   *         non-<code>null</code> it is NO indicator, whether a message was
   *         signed or not.
   * @see #isSoapSignatureChecked()
   * @since 3.0.0-beta6
   */
  @Nullable
  IAS4CryptoFactory getCryptoFactorySign ();

  /**
   * @return The crypto factory that was used to decrypt an eventually encrypted
   *         message. May be <code>null</code>. If this is non-<code>null</code>
   *         it is NO indicator, whether a message was encrypted or not.
   * @see #isSoapDecrypted()
   * @since 3.0.0-beta6
   */
  @Nullable
  IAS4CryptoFactory getCryptoFactoryCrypt ();
}
