/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4server.receive;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.w3c.dom.Document;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.attachment.WSS4JAttachment;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.model.mpc.IMPC;
import com.helger.as4lib.model.pmode.IPModeConfig;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.util.AS4ResourceManager;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.attr.MapBasedAttributeContainerAny;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.datetime.PDTFactory;

/**
 * This class keeps track of the status of an incoming message. It is basically
 * a String to any map.<br>
 * Keys starting with <code>as4.</code> are reserved for internal use.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class AS4MessageState extends MapBasedAttributeContainerAny <String>
{
  private static final String KEY_EBMS3_MESSAGING = "as4.ebms3.messaging";
  private static final String KEY_PMODE_CONFIG = "as4.pmode.config";
  private static final String KEY_MPC = "as4.mpc";
  private static final String KEY_ORIGINAL_ATTACHMENT_LIST = "as4.soap.attachmentlist";
  private static final String KEY_DECRYPTED_SOAP_DOCUMENT = "as4.soap.decrypted.document";
  private static final String KEY_DECRYPTED_ATTACHMENT_LIST = "as4.soap.decrypted.attachmentlist";
  private static final String KEY_COMPRESSED_ATTACHMENT_IDS = "as4.compressed.attachment.ids";
  private static final String KEY_SOAP_BODY_PAYLOAD_PRESENT = "as4.soap.body.payload.present";
  private static final String KEY_INITIATOR_ID = "as4.initiator.id";
  private static final String KEY_RESPONDER_ID = "as4.responder.id";
  private static final String KEY_USED_CERTIFICATE = "as4.used.certificate";

  private final LocalDateTime m_aReceiptDT;
  private final ESOAPVersion m_eSOAPVersion;
  private final AS4ResourceManager m_aResMgr;

  public AS4MessageState (@Nonnull final ESOAPVersion eSOAPVersion, @Nonnull final AS4ResourceManager aResMgr)
  {
    m_aReceiptDT = PDTFactory.getCurrentLocalDateTime ();
    m_eSOAPVersion = ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    m_aResMgr = ValueEnforcer.notNull (aResMgr, "ResMgr");
  }

  /**
   * @return Date and time when the receipt started.
   */
  @Nonnull
  public final LocalDateTime getReceiptDT ()
  {
    return m_aReceiptDT;
  }

  /**
   * @return The SOAP version of the current request as specified in the
   *         constructor. Never <code>null</code>.
   */
  @Nonnull
  public final ESOAPVersion getSOAPVersion ()
  {
    return m_eSOAPVersion;
  }

  /**
   * @return The resource manager as specified in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  public final AS4ResourceManager getResourceMgr ()
  {
    return m_aResMgr;
  }

  public void setMessaging (@Nullable final Ebms3Messaging aMessaging)
  {
    setAttribute (KEY_EBMS3_MESSAGING, aMessaging);
  }

  @Nullable
  public Ebms3Messaging getMessaging ()
  {
    return getCastedAttribute (KEY_EBMS3_MESSAGING);
  }

  public void setPModeConfig (@Nullable final IPModeConfig aPModeConfig)
  {
    setAttribute (KEY_PMODE_CONFIG, aPModeConfig);
  }

  @Nullable
  public IPModeConfig getPModeConfig ()
  {
    return getCastedAttribute (KEY_PMODE_CONFIG);
  }

  public void setOriginalAttachments (@Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    setAttribute (KEY_ORIGINAL_ATTACHMENT_LIST, aAttachments);
  }

  public boolean hasOriginalAttachments ()
  {
    final ICommonsList <WSS4JAttachment> aAttachments = getOriginalAttachments ();
    return aAttachments != null && aAttachments.isNotEmpty ();
  }

  @Nullable
  public ICommonsList <WSS4JAttachment> getOriginalAttachments ()
  {
    return getCastedAttribute (KEY_ORIGINAL_ATTACHMENT_LIST);
  }

  public void setDecryptedSOAPDocument (@Nullable final Document aDocument)
  {
    setAttribute (KEY_DECRYPTED_SOAP_DOCUMENT, aDocument);
  }

  public boolean hasDecryptedSOAPDocument ()
  {
    return containsAttribute (KEY_DECRYPTED_SOAP_DOCUMENT);
  }

  @Nullable
  public Document getDecryptedSOAPDocument ()
  {
    return getCastedAttribute (KEY_DECRYPTED_SOAP_DOCUMENT);
  }

  public void setDecryptedAttachments (@Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    setAttribute (KEY_DECRYPTED_ATTACHMENT_LIST, aAttachments);
  }

  public boolean hasDecryptedAttachments ()
  {
    final ICommonsList <WSS4JAttachment> aAttachments = getDecryptedAttachments ();
    return aAttachments != null && aAttachments.isNotEmpty ();
  }

  @Nullable
  public ICommonsList <WSS4JAttachment> getDecryptedAttachments ()
  {
    return getCastedAttribute (KEY_DECRYPTED_ATTACHMENT_LIST);
  }

  public void setCompressedAttachmentIDs (@Nullable final ICommonsMap <String, EAS4CompressionMode> aIDs)
  {
    setAttribute (KEY_COMPRESSED_ATTACHMENT_IDS, aIDs);
  }

  public boolean hasCompressedAttachmentIDs ()
  {
    return containsAttribute (KEY_COMPRESSED_ATTACHMENT_IDS);
  }

  @Nullable
  public ICommonsMap <String, EAS4CompressionMode> getCompressedAttachmentIDs ()
  {
    return getCastedAttribute (KEY_COMPRESSED_ATTACHMENT_IDS);
  }

  @Nullable
  public EAS4CompressionMode getAttachmentCompressionMode (@Nullable final String sID)
  {
    final ICommonsMap <String, EAS4CompressionMode> aIDs = getCompressedAttachmentIDs ();
    return aIDs == null ? null : aIDs.get (sID);
  }

  public boolean containsCompressedAttachmentID (@Nullable final String sID)
  {
    final ICommonsMap <String, EAS4CompressionMode> aIDs = getCompressedAttachmentIDs ();
    return aIDs != null && aIDs.containsKey (sID);
  }

  public void setMPC (@Nullable final IMPC aMPC)
  {
    setAttribute (KEY_MPC, aMPC);
  }

  @Nullable
  public IMPC getMPC ()
  {
    return getCastedAttribute (KEY_MPC);
  }

  public void setSoapBodyPayloadPresent (final boolean bHasSoapBodyPayload)
  {
    setAttribute (KEY_SOAP_BODY_PAYLOAD_PRESENT, bHasSoapBodyPayload);
  }

  public boolean isSoapBodyPayloadPresent ()
  {
    return getAttributeAsBoolean (KEY_SOAP_BODY_PAYLOAD_PRESENT, false);
  }

  public void setInitiatorID (@Nullable final String sInitiatorID)
  {
    setAttribute (KEY_INITIATOR_ID, sInitiatorID);
  }

  @Nullable
  public String getInitiatorID ()
  {
    return getAttributeAsString (KEY_INITIATOR_ID);
  }

  public void setResponderID (@Nullable final String sResponderID)
  {
    setAttribute (KEY_RESPONDER_ID, sResponderID);
  }

  @Nullable
  public String getResponderID ()
  {
    return getAttributeAsString (KEY_RESPONDER_ID);
  }

  public void setUsedCertificate (@Nullable final X509Certificate aCert)
  {
    setAttribute (KEY_USED_CERTIFICATE, aCert);
  }

  @Nullable
  public X509Certificate getUsedCertificate ()
  {
    return getCastedAttribute (KEY_USED_CERTIFICATE);
  }
}
