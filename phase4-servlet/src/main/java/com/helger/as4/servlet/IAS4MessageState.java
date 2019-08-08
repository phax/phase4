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
package com.helger.as4.servlet;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Locale;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;

import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.model.mpc.IMPC;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceHelper;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.commons.collection.attr.IAttributeContainer;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.string.StringHelper;

/**
 * Read-only AS4 message state.
 *
 * @author Philip Helger
 */
public interface IAS4MessageState extends IAttributeContainer <String, Object>
{
  /**
   * @return Date and time when the receipt started. This is constantly set in
   *         the constructor and never <code>null</code>.
   */
  @Nonnull
  LocalDateTime getReceiptDT ();

  /**
   * @return The SOAP version of the current request as specified in the
   *         constructor. Never <code>null</code>.
   */
  @Nonnull
  ESOAPVersion getSOAPVersion ();

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
   * @return The parent of the usermessage/signal message for further
   *         evaluation.
   */
  @Nullable
  Ebms3Messaging getMessaging ();

  @Nullable
  default String getRefToMessageID ()
  {
    Ebms3MessageInfo aMsgInfo = null;
    final Ebms3Messaging aMessaging = getMessaging ();
    if (aMessaging != null)
      if (aMessaging.hasUserMessageEntries ())
        aMsgInfo = aMessaging.getUserMessageAtIndex (0).getMessageInfo ();
      else
        if (aMessaging.hasSignalMessageEntries ())
          aMsgInfo = aMessaging.getSignalMessageAtIndex (0).getMessageInfo ();
    return aMsgInfo != null ? aMsgInfo.getMessageId () : "";
  }

  /**
   * @return the PMode that is used with the current message
   */
  @Nullable
  IPMode getPMode ();

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
   */
  @Nullable
  Document getDecryptedSOAPDocument ();

  default boolean hasDecryptedSOAPDocument ()
  {
    return getDecryptedSOAPDocument () != null;
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
   * @return true if a payload in the soap body is present, else false
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
   * @return <code>true</code> if the incoming message was signed and the
   *         signature was verified, <code>false</code> otherwise.
   */
  boolean isSoapSignatureChecked ();

  /**
   * @return <code>true</code> if the incoming message was decrypted,
   *         <code>false</code> otherwise.
   */
  boolean isSoapDecrypted ();
}
