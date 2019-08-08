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
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;

import org.w3c.dom.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.attr.AttributeContainerAny;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.datetime.PDTFactory;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.model.mpc.IMPC;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.soap.ESOAPVersion;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * This class keeps track of the status of an incoming message. It is basically
 * a String to any map.<br>
 * Keys starting with <code>as4.</code> are reserved for internal use.<br>
 * Instances of this object are only modified in the SOAP header handlers.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class AS4MessageState extends AttributeContainerAny <String> implements IAS4MessageState
{
  private static final String KEY_EBMS3_MESSAGING = "as4.ebms3.messaging";
  private static final String KEY_PMODE = "as4.pmode.config";
  private static final String KEY_MPC = "as4.mpc";
  private static final String KEY_ORIGINAL_ATTACHMENT_LIST = "as4.soap.attachmentlist";
  private static final String KEY_DECRYPTED_SOAP_DOCUMENT = "as4.soap.decrypted.document";
  private static final String KEY_DECRYPTED_ATTACHMENT_LIST = "as4.soap.decrypted.attachmentlist";
  private static final String KEY_COMPRESSED_ATTACHMENT_IDS = "as4.compressed.attachment.ids";
  private static final String KEY_SOAP_BODY_PAYLOAD_PRESENT = "as4.soap.body.payload.present";
  private static final String KEY_INITIATOR_ID = "as4.initiator.id";
  private static final String KEY_RESPONDER_ID = "as4.responder.id";
  private static final String KEY_USED_CERTIFICATE = "as4.used.certificate";
  private static final String KEY_EFFECTIVE_PMODE_LEG = "as4.pmode.effective.leg";
  private static final String KEY_EFFECTIVE_PMODE_LEG_NUMBER = "as4.pmode.effective.leg.number";
  private static final String KEY_SOAP_CHECKED_SIGNATURE = "as4.soap.signature.checked";
  private static final String KEY_SOAP_DECRYPTED = "as4.soap.decrypted";

  private final LocalDateTime m_aReceiptDT;
  private final ESOAPVersion m_eSOAPVersion;
  private final AS4ResourceHelper m_aResHelper;
  private final Locale m_aLocale;

  public AS4MessageState (@Nonnull final ESOAPVersion eSOAPVersion,
                          @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                          @Nonnull final Locale aLocale)
  {
    m_aReceiptDT = PDTFactory.getCurrentLocalDateTime ();
    m_eSOAPVersion = ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    m_aResHelper = ValueEnforcer.notNull (aResHelper, "ResHelper");
    m_aLocale = ValueEnforcer.notNull (aLocale, "Locale");
  }

  @Nonnull
  public final LocalDateTime getReceiptDT ()
  {
    return m_aReceiptDT;
  }

  @Nonnull
  public final ESOAPVersion getSOAPVersion ()
  {
    return m_eSOAPVersion;
  }

  @Nonnull
  public final AS4ResourceHelper getResourceHelper ()
  {
    return m_aResHelper;
  }

  @Nonnull
  public final Locale getLocale ()
  {
    return m_aLocale;
  }

  public void setMessaging (@Nullable final Ebms3Messaging aMessaging)
  {
    putIn (KEY_EBMS3_MESSAGING, aMessaging);
  }

  @Nullable
  public Ebms3Messaging getMessaging ()
  {
    return getCastedValue (KEY_EBMS3_MESSAGING);
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
  public IPMode getPMode ()
  {
    return getCastedValue (KEY_PMODE);
  }

  public void setOriginalAttachments (@Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    putIn (KEY_ORIGINAL_ATTACHMENT_LIST, aAttachments);
  }

  @Nullable
  public ICommonsList <WSS4JAttachment> getOriginalAttachments ()
  {
    return getCastedValue (KEY_ORIGINAL_ATTACHMENT_LIST);
  }

  public void setDecryptedSOAPDocument (@Nullable final Document aDocument)
  {
    putIn (KEY_DECRYPTED_SOAP_DOCUMENT, aDocument);
  }

  @Nullable
  public Document getDecryptedSOAPDocument ()
  {
    return getCastedValue (KEY_DECRYPTED_SOAP_DOCUMENT);
  }

  public void setDecryptedAttachments (@Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    putIn (KEY_DECRYPTED_ATTACHMENT_LIST, aAttachments);
  }

  @Nullable
  public ICommonsList <WSS4JAttachment> getDecryptedAttachments ()
  {
    return getCastedValue (KEY_DECRYPTED_ATTACHMENT_LIST);
  }

  public void setCompressedAttachmentIDs (@Nullable final ICommonsMap <String, EAS4CompressionMode> aIDs)
  {
    putIn (KEY_COMPRESSED_ATTACHMENT_IDS, aIDs);
  }

  @Nullable
  public ICommonsMap <String, EAS4CompressionMode> getCompressedAttachmentIDs ()
  {
    return getCastedValue (KEY_COMPRESSED_ATTACHMENT_IDS);
  }

  public void setMPC (@Nullable final IMPC aMPC)
  {
    putIn (KEY_MPC, aMPC);
  }

  @Nullable
  public IMPC getMPC ()
  {
    return getCastedValue (KEY_MPC);
  }

  public void setSoapBodyPayloadPresent (final boolean bHasSoapBodyPayload)
  {
    putIn (KEY_SOAP_BODY_PAYLOAD_PRESENT, bHasSoapBodyPayload);
  }

  public boolean isSoapBodyPayloadPresent ()
  {
    return getAsBoolean (KEY_SOAP_BODY_PAYLOAD_PRESENT, false);
  }

  public void setInitiatorID (@Nullable final String sInitiatorID)
  {
    putIn (KEY_INITIATOR_ID, sInitiatorID);
  }

  @Nullable
  public String getInitiatorID ()
  {
    return getAsString (KEY_INITIATOR_ID);
  }

  public void setResponderID (@Nullable final String sResponderID)
  {
    putIn (KEY_RESPONDER_ID, sResponderID);
  }

  @Nullable
  public String getResponderID ()
  {
    return getAsString (KEY_RESPONDER_ID);
  }

  public void setUsedCertificate (@Nullable final X509Certificate aCert)
  {
    putIn (KEY_USED_CERTIFICATE, aCert);
  }

  @Nullable
  public X509Certificate getUsedCertificate ()
  {
    return getCastedValue (KEY_USED_CERTIFICATE);
  }

  public void setEffectivePModeLeg (@Nonnegative final int nLegNumber, @Nullable final PModeLeg aEffectiveLeg)
  {
    ValueEnforcer.isTrue (nLegNumber == 1 || nLegNumber == 2, "LegNumber must be 1 or 2");
    putIn (KEY_EFFECTIVE_PMODE_LEG, aEffectiveLeg);
    putIn (KEY_EFFECTIVE_PMODE_LEG_NUMBER, nLegNumber);
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

  public void setSoapSignatureChecked (final boolean bSignatureChecked)
  {
    putIn (KEY_SOAP_CHECKED_SIGNATURE, bSignatureChecked);
  }

  /**
   * @return <code>true</code> if the incoming message was signed and the
   *         signature was verified, <code>false</code> otherwise.
   */
  public boolean isSoapSignatureChecked ()
  {
    return getAsBoolean (KEY_SOAP_CHECKED_SIGNATURE, false);
  }

  public void setSoapDecrypted (final boolean bDecrypted)
  {
    putIn (KEY_SOAP_DECRYPTED, bDecrypted);
  }

  /**
   * @return <code>true</code> if the incoming message was decrypted,
   *         <code>false</code> otherwise.
   */
  public boolean isSoapDecrypted ()
  {
    return getAsBoolean (KEY_SOAP_DECRYPTED, false);
  }
}
