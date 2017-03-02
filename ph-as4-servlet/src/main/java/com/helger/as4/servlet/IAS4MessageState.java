package com.helger.as4.servlet;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;

import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.model.mpc.IMPC;
import com.helger.as4.model.pmode.config.IPModeConfig;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.commons.collection.attr.IAttributeContainer;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsMap;

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
  AS4ResourceManager getResourceMgr ();

  @Nullable
  Ebms3Messaging getMessaging ();

  @Nullable
  IPModeConfig getPModeConfig ();

  @Nullable
  ICommonsList <WSS4JAttachment> getOriginalAttachments ();

  default boolean hasOriginalAttachments ()
  {
    final ICommonsList <WSS4JAttachment> aAttachments = getOriginalAttachments ();
    return aAttachments != null && aAttachments.isNotEmpty ();
  }

  @Nullable
  Document getDecryptedSOAPDocument ();

  default boolean hasDecryptedSOAPDocument ()
  {
    return getDecryptedSOAPDocument () != null;
  }

  @Nullable
  ICommonsList <WSS4JAttachment> getDecryptedAttachments ();

  default boolean hasDecryptedAttachments ()
  {
    final ICommonsList <WSS4JAttachment> aAttachments = getDecryptedAttachments ();
    return aAttachments != null && aAttachments.isNotEmpty ();
  }

  @Nullable
  ICommonsMap <String, EAS4CompressionMode> getCompressedAttachmentIDs ();

  default boolean hasCompressedAttachmentIDs ()
  {
    return getCompressedAttachmentIDs () != null;
  }

  @Nullable
  default EAS4CompressionMode getAttachmentCompressionMode (@Nullable final String sID)
  {
    final ICommonsMap <String, EAS4CompressionMode> aIDs = getCompressedAttachmentIDs ();
    return aIDs == null ? null : aIDs.get (sID);
  }

  default boolean containsCompressedAttachmentID (@Nullable final String sID)
  {
    final ICommonsMap <String, EAS4CompressionMode> aIDs = getCompressedAttachmentIDs ();
    return aIDs != null && aIDs.containsKey (sID);
  }

  @Nullable
  IMPC getMPC ();

  boolean isSoapBodyPayloadPresent ();

  @Nullable
  String getInitiatorID ();

  @Nullable
  String getResponderID ();

  @Nullable
  X509Certificate getUsedCertificate ();

  /**
   * @return The effective leg to use. May be leg 1 or leg 2 of the PMode.
   * @see #getPModeConfig()
   */
  @Nullable
  PModeLeg getEffectivePModeLeg ();

  @CheckForSigned
  int getEffectivePModeLegNumber ();
}
