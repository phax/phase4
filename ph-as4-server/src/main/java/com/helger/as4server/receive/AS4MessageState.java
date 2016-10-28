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

import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.w3c.dom.Document;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.model.mpc.IMPC;
import com.helger.as4lib.model.pmode.IPMode;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.attr.MapBasedAttributeContainerAny;
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
  private static final String KEY_PMODE = "as4.pmode";
  private static final String KEY_MPC = "as4.mps";
  private static final String KEY_DECRYPTED_SOAP_DOCUMENT = "as4.soap.decrypted.document";
  private static final String KEY_COMPRESSED_ATTACHMENT_IDS = "as4.compressed.attachment.ids";

  private final LocalDateTime m_aReceiptDT;
  private final ESOAPVersion m_eSOAPVersion;

  public AS4MessageState (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_aReceiptDT = PDTFactory.getCurrentLocalDateTime ();
    m_eSOAPVersion = ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
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

  public void setMessaging (@Nullable final Ebms3Messaging aMessaging)
  {
    setAttribute (KEY_EBMS3_MESSAGING, aMessaging);
  }

  @Nullable
  public Ebms3Messaging getMessaging ()
  {
    return getCastedAttribute (KEY_EBMS3_MESSAGING);
  }

  public void setPMode (@Nullable final IPMode aPMode)
  {
    setAttribute (KEY_PMODE, aPMode);
  }

  @Nullable
  public IPMode getPMode ()
  {
    return getCastedAttribute (KEY_PMODE);
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
}
