/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.servlet;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Locale;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.collection.attr.IAttributeContainer;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.ebms3header.Ebms3PullRequest;
import com.helger.phase4.ebms3header.Ebms3Receipt;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.model.mpc.IMPC;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.soap.ESoapVersion;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * Read-only AS4 message state for incoming messages.
 *
 * @author Philip Helger
 */
public interface IAS4MessageState extends IAttributeContainer <String, Object>
{
  /**
   * @return Date and time when the receipt started. This must be set in the
   *         implementation and never <code>null</code>.
   */
  @Nonnull
  LocalDateTime getReceiptDT ();

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
   * @return the PMode that is used with the current message
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

  default boolean hasOriginalSoapDocument ()
  {
    return getOriginalSoapDocument () != null;
  }

  /**
   * @return has saved the original attachment, can be encrypted or not depends
   *         if encryption is used or not
   */
  @Nullable
  ICommonsList <WSS4JAttachment> getOriginalAttachments ();

  default boolean hasOriginalAttachments ()
  {
    final ICommonsList <WSS4JAttachment> aAttachments = getOriginalAttachments ();
    return aAttachments != null && aAttachments.isNotEmpty ();
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

  default boolean hasDecryptedSoapDocument ()
  {
    return getDecryptedSoapDocument () != null;
  }

  /**
   * @return getting decrypted attachment, if there were encrypted attachments
   *         to begin with
   */
  @Nullable
  ICommonsList <WSS4JAttachment> getDecryptedAttachments ();

  default boolean hasDecryptedAttachments ()
  {
    final ICommonsList <WSS4JAttachment> aAttachments = getDecryptedAttachments ();
    return aAttachments != null && aAttachments.isNotEmpty ();
  }

  /**
   * @return IDs from all compressed attachments and/or payload
   */
  @Nullable
  ICommonsMap <String, EAS4CompressionMode> getCompressedAttachmentIDs ();

  default boolean hasCompressedAttachmentIDs ()
  {
    return getCompressedAttachmentIDs () != null;
  }

  /**
   * @param sID
   *        id to look up
   * @return Looks up if a compression mode with the id sID exists and returns
   *         the mode else null
   */
  @Nullable
  default EAS4CompressionMode getAttachmentCompressionMode (@Nullable final String sID)
  {
    final ICommonsMap <String, EAS4CompressionMode> aIDs = getCompressedAttachmentIDs ();
    return aIDs == null ? null : aIDs.get (sID);
  }

  /**
   * @param sID
   *        the id to look up
   * @return looks up if the compressed attachment contain the given ID
   */
  default boolean containsCompressedAttachmentID (@Nullable final String sID)
  {
    final ICommonsMap <String, EAS4CompressionMode> aIDs = getCompressedAttachmentIDs ();
    return aIDs != null && aIDs.containsKey (sID);
  }

  /**
   * @return the MPC that is used in the current message exchange
   */
  @Nullable
  IMPC getMPC ();

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
   *         usermessage
   */
  @Nullable
  String getInitiatorID ();

  default boolean hasInitiatorID ()
  {
    return StringHelper.hasText (getInitiatorID ());
  }

  /**
   * @return responder set in the usermessage if the incoming message is a
   *         usermessage
   */
  @Nullable
  String getResponderID ();

  default boolean hasResponderID ()
  {
    return StringHelper.hasText (getResponderID ());
  }

  /**
   * @return The first provided certificate in the incoming message. May be
   *         <code>null</code>. Usually the certificate that was used for
   *         signing.
   */
  @Nullable
  X509Certificate getUsedCertificate ();

  default boolean hasUsedCertificate ()
  {
    return getUsedCertificate () != null;
  }

  /**
   * @return The effective leg to use. May be leg 1 or leg 2 of the PMode.
   * @see #getPMode()
   */
  @Nullable
  PModeLeg getEffectivePModeLeg ();

  /**
   * @return 1 or 2, depending on the used leg. Any other value indicates
   *         "undefined".
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
   * @return The phase4 profile ID to be used. May be <code>null</code>.
   * @since v0.9.7
   */
  @Nullable
  String getProfileID ();

  /**
   * @return The AS4 message ID. May be <code>null</code>.
   * @since v0.9.7
   */
  @Nullable
  String getMessageID ();

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
   *         <code>false</code> if not.
   * @since v0.9.7
   */
  boolean isSoapHeaderElementProcessingSuccessful ();
}
