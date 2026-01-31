/*
 * Copyright (C) 2025-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.hredelivery;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.rt.StackTraceHelper;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.IJsonWriterSettings;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phase4.CAS4Version;
import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.marshaller.Ebms3SignalMessageMarshaller;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.security.certificate.CertificateHelper;
import com.helger.security.certificate.ECertificateCheckResult;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.IXMLWriterSettings;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * This class contains the structured information about what happens on HR eDelivery sending.
 *
 * @author Philip Helger
 * @since 4.0.2
 */
@NotThreadSafe
public class Phase4HREdeliverySendingReport
{
  // State params
  private final OffsetDateTime m_aCurrentDateTimeUTC;
  private final String m_sSMLDNSZone;

  // Special issues only available if an SBDH is provided
  private Exception m_aSBDHParseException;

  // Input params
  private IParticipantIdentifier m_aSenderID;
  private IParticipantIdentifier m_aReceiverID;
  private IDocumentTypeIdentifier m_aDocTypeID;
  private IProcessIdentifier m_aProcessID;
  private String m_sSenderPartyID;
  private String m_sTransportProfileID;

  // SBDH details
  private String m_sSBDHInstanceIdentifier;

  // SMP lookup results
  private String m_sC3EndpointURL;
  private X509Certificate m_aC3Cert;
  private String m_sC3CertSubjectCN;
  private String m_sC3CertSubjectO;
  private OffsetDateTime m_aC3CertCheckDT;
  private ECertificateCheckResult m_eC3CertCheckResult;
  private String m_sC3TechnicalContact;

  // AS4 params
  private String m_sAS4MessageID;
  private String m_sAS4ConversationID;
  private OffsetDateTime m_aAS4SendingDT;

  // AS4 response details
  private AS4ClientSentMessage <byte []> m_aRawHttpResponse;
  private EAS4UserMessageSendResult m_eAS4SendingResult;
  private Exception m_aAS4SendingException;
  private Ebms3SignalMessage m_aAS4ReceivedSignalMsg;
  private boolean m_bAS4ResponseError = false;
  private ICommonsList <Ebms3Error> m_aAS4ResponseErrors;

  private long m_nOverallDurationMillis = -1;
  private boolean m_bSendingSuccess = false;
  private boolean m_bOverallSuccess = false;

  public Phase4HREdeliverySendingReport (@NonNull final ISMLInfo aSMLInfo)
  {
    m_aCurrentDateTimeUTC = PDTFactory.getCurrentOffsetDateTimeMillisOnlyUTC ();
    m_sSMLDNSZone = aSMLInfo.getDNSZone ();
    m_sTransportProfileID = ESMPTransportProfile.TRANSPORT_PROFILE_ERACUN_AS4_V1.getID ();
  }

  /**
   * @return The date and time for which the sending report is created. Never <code>null</code>.
   * @since 4.2.0
   */
  @NonNull
  public OffsetDateTime getCurrentDateTimeUTC ()
  {
    return m_aCurrentDateTimeUTC;
  }

  /**
   * @return The SML DNS zone on which the message exchange takes place. Neither <code>null</code>
   *         nor empty.
   * @since 4.2.0
   */
  @NonNull
  @Nonempty
  public String getSMLDNSZone ()
  {
    return m_sSMLDNSZone;
  }

  /**
   * @return The specific exception that occurred during parsing of a provided SBDH. May be
   *         <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public Exception getSBDHParseException ()
  {
    return m_aSBDHParseException;
  }

  public boolean hasSBDHParseException ()
  {
    return m_aSBDHParseException != null;
  }

  /**
   * Remember any specific exception that occurred during parsing of a provided SBDH.
   *
   * @param e
   *        The exception that was caught. May be <code>null</code>.
   */
  public void setSBDHParseException (@Nullable final Exception e)
  {
    m_aSBDHParseException = e;
  }

  /**
   * @return The sending participant identifier. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public IParticipantIdentifier getSenderID ()
  {
    return m_aSenderID;
  }

  public boolean hasSenderID ()
  {
    return m_aSenderID != null;
  }

  /**
   * Remember the senders HR eDelivery Participant ID (C1 ID).
   *
   * @param a
   *        HR eDelivery Participant ID. May be <code>null</code>.
   */
  public void setSenderID (@Nullable final IParticipantIdentifier a)
  {
    m_aSenderID = a;
  }

  /**
   * @return The receiving participant identifier. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public IParticipantIdentifier getReceiverID ()
  {
    return m_aReceiverID;
  }

  public boolean hasReceiverID ()
  {
    return m_aReceiverID != null;
  }

  /**
   * Remember the receivers HR eDelivery Participant ID (C4 ID).
   *
   * @param a
   *        HR eDelivery Participant ID. May be <code>null</code>.
   */
  public void setReceiverID (@Nullable final IParticipantIdentifier a)
  {
    m_aReceiverID = a;
  }

  /**
   * @return The exchanged document type identifier. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public IDocumentTypeIdentifier getDocTypeID ()
  {
    return m_aDocTypeID;
  }

  public boolean hasDocTypeID ()
  {
    return m_aDocTypeID != null;
  }

  /**
   * Remember the HR Document Type ID that was exchanged.
   *
   * @param a
   *        Document Type ID. May be <code>null</code>.
   */
  public void setDocTypeID (@Nullable final IDocumentTypeIdentifier a)
  {
    m_aDocTypeID = a;
  }

  /**
   * @return The exchanged process identifier. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public IProcessIdentifier getProcessID ()
  {
    return m_aProcessID;
  }

  public boolean hasProcessID ()
  {
    return m_aProcessID != null;
  }

  /**
   * Remember the HR Process ID that was exchanged.
   *
   * @param a
   *        Process ID. May be <code>null</code>.
   */
  public void setProcessID (@Nullable final IProcessIdentifier a)
  {
    m_aProcessID = a;
  }

  /**
   * @return The sender party (C3) ID. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public String getSenderPartyID ()
  {
    return m_sSenderPartyID;
  }

  public boolean hasSenderPartyID ()
  {
    return StringHelper.isNotEmpty (m_sSenderPartyID);
  }

  /**
   * Remember the sender party ID (the ID of C3).
   *
   * @param s
   *        Sender party ID. May be <code>null</code>.
   */
  public void setSenderPartyID (@Nullable final String s)
  {
    m_sSenderPartyID = s;
  }

  /**
   * @return The transport profile ID. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public String getTransportProfileID ()
  {
    return m_sTransportProfileID;
  }

  public boolean hasTransportProfileID ()
  {
    return StringHelper.isNotEmpty (m_sTransportProfileID);
  }

  /**
   * Remember the transport profile ID.
   *
   * @param s
   *        Transport profile ID. May be <code>null</code>.
   */
  public void setTransportProfileID (@Nullable final String s)
  {
    m_sTransportProfileID = s;
  }

  /**
   * @return The SBDH Instance Identifier. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public String getSBDHInstanceIdentifier ()
  {
    return m_sSBDHInstanceIdentifier;
  }

  public boolean hasSBDHInstanceIdentifier ()
  {
    return StringHelper.isNotEmpty (m_sSBDHInstanceIdentifier);
  }

  /**
   * Remember the SBDH Instance Identifier. That is the identifier, that uniquely identifies the
   * specific transmission and is referred to by MLR and MLS.
   *
   * @param s
   *        SBDH instance identifier. May be <code>null</code>.
   */
  public void setSBDHInstanceIdentifier (@Nullable final String s)
  {
    m_sSBDHInstanceIdentifier = s;
  }

  /**
   * @return The AP endpoint URL of C3 determined by the SMP lookup. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public String getC3EndpointURL ()
  {
    return m_sC3EndpointURL;
  }

  public boolean hasC3EndpointURL ()
  {
    return StringHelper.isNotEmpty (m_sC3EndpointURL);
  }

  /**
   * Remember the AP endpoint URL of C3 determined by the SMP lookup.
   *
   * @param s
   *        C3 endpoint URL. May be <code>null</code>.
   */
  public void setC3EndpointURL (@Nullable final String s)
  {
    m_sC3EndpointURL = s;
  }

  /**
   * @return The public HR eDelivery AP certificate of C3 determined by the SMP lookup. May be
   *         <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public X509Certificate getC3Cert ()
  {
    return m_aC3Cert;
  }

  public boolean hasC3Cert ()
  {
    return m_aC3Cert != null;
  }

  public boolean hasC3CertSubjectCN ()
  {
    return StringHelper.isNotEmpty (m_sC3CertSubjectCN);
  }

  public boolean hasC3CertSubjectO ()
  {
    return StringHelper.isNotEmpty (m_sC3CertSubjectO);
  }

  /**
   * Remember the public HR eDelivery AP certificate of C3 determined by the SMP lookup.
   *
   * @param a
   *        C3 public HR eDelivery AP certificate. May be <code>null</code>.
   */
  public void setC3Cert (@Nullable final X509Certificate a)
  {
    m_aC3Cert = a;
    m_sC3CertSubjectCN = CertificateHelper.getSubjectCN (a);
    m_sC3CertSubjectO = CertificateHelper.getSubjectO (a);
  }

  /**
   * @return The date and time, when the HR eDelivery AP certificate of C3, as retrieved from the
   *         SMP, was checked for revocation. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public OffsetDateTime getC3CertCheckDT ()
  {
    return m_aC3CertCheckDT;
  }

  public boolean hasC3CertCheckDT ()
  {
    return m_aC3CertCheckDT != null;
  }

  /**
   * Remember the date and time, when the HR eDelivery AP certificate of C3, as retrieved from the
   * SMP, was checked for revocation.
   *
   * @param a
   *        The HR eDelivery AP Certificate check date time. May be <code>null</code>.
   */
  public void setC3CertCheckDT (@Nullable final OffsetDateTime a)
  {
    m_aC3CertCheckDT = a;
  }

  /**
   * @return The result of checking the HR eDeliery AP certificate of C3, as retrieved from the SMP,
   *         for validity. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public ECertificateCheckResult getC3CertCheckResult ()
  {
    return m_eC3CertCheckResult;
  }

  public boolean hasC3CertCheckResult ()
  {
    return m_eC3CertCheckResult != null;
  }

  /**
   * Remember the result of checking the HR eDelivery AP certificate of C3, as retrieved from the
   * SMP, for validity.
   *
   * @param e
   *        The HR eDelivery AP Certificate check result. May be <code>null</code>.
   */
  public void setC3CertCheckResult (@Nullable final ECertificateCheckResult e)
  {
    m_eC3CertCheckResult = e;
  }

  /**
   * @return The technical contact information retrieved from the SMP endpoint. May be
   *         <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public String getC3TechnicalContact ()
  {
    return m_sC3TechnicalContact;
  }

  public boolean hasC3TechnicalContact ()
  {
    return StringHelper.isNotEmpty (m_sC3TechnicalContact);
  }

  /**
   * Remember the technical contact information retrieved from the SMP endpoint. This might be
   * helpful to quickly find support.
   *
   * @param s
   *        The technical contact URL to use. May be <code>null</code>.
   */
  public void setC3TechnicalContact (@Nullable final String s)
  {
    m_sC3TechnicalContact = s;
  }

  /**
   * @return The AS4 Message ID used to send out the message. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public String getAS4MessageID ()
  {
    return m_sAS4MessageID;
  }

  public boolean hasAS4MessageID ()
  {
    return StringHelper.isNotEmpty (m_sAS4MessageID);
  }

  /**
   * Remember the AS4 Message ID used to send out the message.
   *
   * @param s
   *        The AS4 Message ID. May be <code>null</code>.
   */
  public void setAS4MessageID (@Nullable final String s)
  {
    m_sAS4MessageID = s;
  }

  /**
   * @return The AS4 Conversation ID used to send out the message. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public String getAS4ConversationID ()
  {
    return m_sAS4ConversationID;
  }

  public boolean hasAS4ConversationID ()
  {
    return StringHelper.isNotEmpty (m_sAS4ConversationID);
  }

  /**
   * Remember the AS4 Conversation ID used to send out the message.
   *
   * @param s
   *        The AS4 Conversation ID. May be <code>null</code>.
   */
  public void setAS4ConversationID (@Nullable final String s)
  {
    m_sAS4ConversationID = s;
  }

  /**
   * @return The AS4 sending date time. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public OffsetDateTime getAS4SendingDT ()
  {
    return m_aAS4SendingDT;
  }

  public boolean hasAS4SendingDT ()
  {
    return m_aAS4SendingDT != null;
  }

  /**
   * Remember the AS4 sending date time, as the correlation basis for MLS.
   *
   * @param a
   *        The sending date time.
   */
  public void setAS4SendingDT (@Nullable final OffsetDateTime a)
  {
    // Make sure to use only millisecond precision for correct XSD rendering
    m_aAS4SendingDT = a == null ? null : PDTFactory.getWithMillisOnly (a);
  }

  /**
   * @return The raw HTTP response message received from the other side. This is only emitted in
   *         case of non-success. May be <code>null</code>.
   * @since 4.2.3
   */
  @Nullable
  public AS4ClientSentMessage <byte []> getRawHttpResponse ()
  {
    return m_aRawHttpResponse;
  }

  /**
   * @return <code>true</code> if a raw response is present, <code>false</code> if not.
   * @since 4.2.3
   */
  public boolean hasRawHttpResponse ()
  {
    return m_aRawHttpResponse != null;
  }

  /**
   * Set the raw HTTP response message received from the other side. This is only emitted in case of
   * non-success.
   *
   * @param aRawHttpResponse
   *        The raw response to use. May be <code>null</code> to explicit state that it should not
   *        be part of the sending report.
   * @since 4.2.3
   */
  public void setRawHttpResponse (@Nullable final AS4ClientSentMessage <byte []> aRawHttpResponse)
  {
    m_aRawHttpResponse = aRawHttpResponse;
  }

  /**
   * @return The overall AS4 sending result. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public EAS4UserMessageSendResult getAS4SendingResult ()
  {
    return m_eAS4SendingResult;
  }

  public boolean hasAS4SendingResult ()
  {
    return m_eAS4SendingResult != null;
  }

  public boolean hasUnsuccessfulAS4SendingResult ()
  {
    return m_eAS4SendingResult != null && m_eAS4SendingResult.isFailure ();
  }

  /**
   * Remember the overall AS4 sending result.
   *
   * @param e
   *        The AS4 sending result. May be <code>null</code>.
   */
  public void setAS4SendingResult (@Nullable final EAS4UserMessageSendResult e)
  {
    m_eAS4SendingResult = e;
  }

  /**
   * @return The exception that eventually occurred on AS4 sending. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public Exception getAS4SendingException ()
  {
    return m_aAS4SendingException;
  }

  public boolean hasAS4SendingException ()
  {
    return m_aAS4SendingException != null;
  }

  /**
   * Remember any exception that eventually occurred on AS4 sending.
   *
   * @param e
   *        The exception from AS4 sending. May be <code>null</code>.
   */
  public void setAS4SendingException (@Nullable final Exception e)
  {
    m_aAS4SendingException = e;
  }

  /**
   * @return The synchronously received AS4 Signal Message from C3. May be <code>null</code>.
   * @since 4.2.0
   */
  @Nullable
  public Ebms3SignalMessage getAS4ReceivedSignalMsg ()
  {
    return m_aAS4ReceivedSignalMsg;
  }

  /**
   * @return The list of AS4 EBMS errors received from C3. May be <code>null</code> or empty.
   * @since 4.2.0
   */
  @Nullable
  @ReturnsMutableObject
  public ICommonsList <Ebms3Error> getAS4ReceivedErrors ()
  {
    return m_aAS4ResponseErrors;
  }

  public boolean hasAS4ReceivedSignalMsg ()
  {
    return m_aAS4ReceivedSignalMsg != null;
  }

  public boolean hasAS4ResponseErrors ()
  {
    return m_aAS4ResponseErrors != null && m_aAS4ResponseErrors.isNotEmpty ();
  }

  /**
   * Remember the synchronously received AS4 Signal Message from C3.
   *
   * @param a
   *        The parsed AS4 Signal Message. May be <code>null</code>.
   */
  public void setAS4ReceivedSignalMsg (@Nullable final Ebms3SignalMessage a)
  {
    m_aAS4ReceivedSignalMsg = a;
    if (a != null)
    {
      if (a.hasErrorEntries ())
      {
        m_aAS4ResponseErrors = new CommonsArrayList <> (a.getError ());
        m_bAS4ResponseError = true;
      }
      else
        m_bAS4ResponseError = false;
    }
    else
    {
      m_aAS4ResponseErrors = null;
      m_bAS4ResponseError = false;
    }
  }

  /**
   * @return The overall duration it took to perform the lookup and sending process.
   * @since 4.2.0
   */
  public long getOverallDurationMillis ()
  {
    return m_nOverallDurationMillis;
  }

  /**
   * Remember the overall duration it took to perform the lookup and sending process.
   *
   * @param n
   *        The overall milliseconds needed. Must be &ge; 0.
   */
  public void setOverallDurationMillis (@Nonnegative final long n)
  {
    m_nOverallDurationMillis = n;
  }

  /**
   * @return <code>true</code> if sending was done successful.
   * @since 4.2.0
   */
  public boolean isSendingSuccess ()
  {
    return m_bSendingSuccess;
  }

  /**
   * Remember the overall sending success.
   *
   * @param b
   *        <code>true</code> on success, <code>false</code> on failure.
   */
  public void setSendingSuccess (final boolean b)
  {
    m_bSendingSuccess = b;
  }

  /**
   * @return <code>true</code> if sending and all follow up activities was done successful.
   * @since 4.2.0
   */
  public boolean isOverallSuccess ()
  {
    return m_bOverallSuccess;
  }

  /**
   * Remember the overall success. This may differ from the sending success, if e.g. sending
   * succeeded but storing the record for HR eDelivery reporting failed.
   *
   * @param b
   *        <code>true</code> on success, <code>false</code> on failure.
   */
  public void setOverallSuccess (final boolean b)
  {
    m_bOverallSuccess = b;
  }

  /**
   * Get the whole report as one big JSON structure. Only elements that were provided, are contained
   * in the report.
   *
   * @return The sending report as a JSON object. May not be <code>null</code>.
   */
  @NonNull
  public IJsonObject getAsJsonObject ()
  {
    // Function to convert Exception to JSON
    final Function <Exception, IJsonObject> fEx = ex -> new JsonObject ().add ("class", ex.getClass ().getName ())
                                                                         .add ("message", ex.getMessage ())
                                                                         .add ("stackTrace",
                                                                               StackTraceHelper.getStackAsString (ex));

    final IJsonObject aJson = new JsonObject ();
    aJson.add ("currentDateTimeUTC", PDTWebDateHelper.getAsStringXSD (m_aCurrentDateTimeUTC));
    aJson.add ("phase4Version", CAS4Version.BUILD_VERSION);
    aJson.add ("smlDnsZone", m_sSMLDNSZone);

    if (hasSBDHParseException ())
      aJson.add ("sbdhParsingException", fEx.apply (m_aSBDHParseException));

    if (hasSenderID ())
      aJson.add ("senderId", m_aSenderID.getURIEncoded ());
    if (hasReceiverID ())
      aJson.add ("receiverId", m_aReceiverID.getURIEncoded ());
    if (hasDocTypeID ())
      aJson.add ("docTypeId", m_aDocTypeID.getURIEncoded ());
    if (hasProcessID ())
      aJson.add ("processId", m_aProcessID.getURIEncoded ());
    if (hasSenderPartyID ())
      aJson.add ("senderPartyId", m_sSenderPartyID);
    if (hasTransportProfileID ())
      aJson.add ("transportProfileId", m_sTransportProfileID);

    if (hasSBDHInstanceIdentifier ())
      aJson.add ("sbdhInstanceIdentifier", m_sSBDHInstanceIdentifier);

    if (hasC3EndpointURL ())
      aJson.add ("c3EndpointUrl", m_sC3EndpointURL);
    if (hasC3Cert ())
      aJson.add ("c3Cert", CertificateHelper.getPEMEncodedCertificate (m_aC3Cert));
    if (hasC3CertSubjectCN ())
      aJson.add ("c3CertSubjectCN", m_sC3CertSubjectCN);
    if (hasC3CertSubjectO ())
      aJson.add ("c3CertSubjectO", m_sC3CertSubjectO);
    if (hasC3CertCheckDT ())
      aJson.add ("c3CertCheckDT", PDTWebDateHelper.getAsStringXSD (m_aC3CertCheckDT));
    if (hasC3CertCheckResult ())
      aJson.add ("c3CertCheckResult", m_eC3CertCheckResult.name ());
    if (hasC3TechnicalContact ())
      aJson.add ("c3TechnicalContact", m_sC3TechnicalContact);

    if (hasAS4MessageID ())
      aJson.add ("as4MessageId", m_sAS4MessageID);
    if (hasAS4ConversationID ())
      aJson.add ("as4ConversationId", m_sAS4ConversationID);
    if (hasAS4SendingDT ())
      aJson.add ("as4SendingDateTime", PDTWebDateHelper.getAsStringXSD (m_aAS4SendingDT));

    // Don't render Raw response in case of success, as the Signal Message is contained anyway
    if (hasRawHttpResponse () && hasUnsuccessfulAS4SendingResult ())
    {
      final IJsonObject aRawHttpResponse = new JsonObject ();
      if (m_aRawHttpResponse.hasResponseStatusLine ())
        aRawHttpResponse.add ("statusLine", m_aRawHttpResponse.getResponseStatusLine ().toString ());

      {
        final IJsonArray aHeaders = new JsonArray ();
        for (final var aHeader : m_aRawHttpResponse.getResponseHeaders ())
        {
          final IJsonObject aJsonHeader = new JsonObject ().add ("name", aHeader.getKey ());
          switch (aHeader.getValue ().size ())
          {
            case 0:
              break;
            case 1:
              aJsonHeader.add ("value", aHeader.getValue ().getFirstOrNull ());
              break;
            default:
              aJsonHeader.add ("value", new JsonArray ().addAll (aHeader.getValue ()));
              break;
          }
          aHeaders.add (aJsonHeader);
        }
        aRawHttpResponse.add ("headers", aHeaders);
      }

      if (m_aRawHttpResponse.hasResponseContent ())
      {
        // Does not clone
        final byte [] aBytes = m_aRawHttpResponse.getResponseContent ();
        aRawHttpResponse.add ("contentLength", aBytes.length);
        aRawHttpResponse.add ("content", new String (aBytes, StandardCharsets.UTF_8));
      }
      else
        aRawHttpResponse.add ("contentLength", 0);

      aJson.add ("rawHttpResponse", aRawHttpResponse);
    }

    if (hasAS4SendingResult ())
      aJson.add ("sendingResult", m_eAS4SendingResult.name ());
    if (hasAS4SendingException ())
      aJson.add ("sendingException", fEx.apply (m_aAS4SendingException));
    if (hasAS4ReceivedSignalMsg ())
      aJson.add ("as4ReceivedSignalMsg", new Ebms3SignalMessageMarshaller ().getAsString (m_aAS4ReceivedSignalMsg));
    aJson.add ("as4ResponseError", m_bAS4ResponseError);
    if (hasAS4ResponseErrors ())
    {
      final IJsonArray aErrors = new JsonArray ();
      for (final Ebms3Error aError : m_aAS4ResponseErrors)
      {
        final IJsonObject aErrorDetails = new JsonObject ();
        if (aError.getDescription () != null)
          aErrorDetails.add ("description", aError.getDescriptionValue ());
        if (aError.getErrorDetail () != null)
          aErrorDetails.add ("errorDetails", aError.getErrorDetail ());
        if (aError.getCategory () != null)
          aErrorDetails.add ("category", aError.getCategory ());
        if (aError.getRefToMessageInError () != null)
          aErrorDetails.add ("refToMessageInError", aError.getRefToMessageInError ());
        if (aError.getErrorCode () != null)
          aErrorDetails.add ("errorCode", aError.getErrorCode ());
        if (aError.getOrigin () != null)
          aErrorDetails.add ("origin", aError.getOrigin ());
        if (aError.getSeverity () != null)
          aErrorDetails.add ("severity", aError.getSeverity ());
        if (aError.getShortDescription () != null)
          aErrorDetails.add ("shortDescription", aError.getShortDescription ());
        aErrors.add (aErrorDetails);
      }
      aJson.add ("as4ResponseErrors", aErrors);
    }

    aJson.add ("overallDurationMillis", m_nOverallDurationMillis);
    aJson.add ("sendingSuccess", m_bSendingSuccess);
    aJson.add ("overallSuccess", m_bOverallSuccess);
    return aJson;
  }

  /**
   * @return The JSON representation of the sending report, as a formatted string. Never
   *         <code>null</code>.
   */
  @NonNull
  public String getAsJsonString ()
  {
    return getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);
  }

  /**
   * @param aJWS
   *        The JSON writer settings to use. May not be <code>null</code>.
   * @return The JSON representation of the sending report, as a string. Never <code>null</code>.
   */
  @NonNull
  public String getAsJsonString (@NonNull final IJsonWriterSettings aJWS)
  {
    return getAsJsonObject ().getAsJsonString (aJWS);
  }

  /**
   * Get the sending report as a MicroDOM element. Only elements that were provided, are contained
   * in the report.
   *
   * @param sNamespaceURI
   *        The namespace URI to be used. May be <code>null</code>.
   * @param sTagName
   *        The tag name to use for the root element. May neither be <code>null</code> nor empty.
   * @return The created micro element and never <code>null</code>.
   */
  @NonNull
  public IMicroElement getAsMicroElement (@Nullable final String sNamespaceURI,
                                          @NonNull @Nonempty final String sTagName)
  {
    final BiFunction <Exception, String, IMicroElement> fEx = (ex, tag) -> {
      final IMicroElement ret = new MicroElement (sNamespaceURI, tag);
      ret.addElementNS (sNamespaceURI, "Class").addText (ex.getClass ().getName ());
      ret.addElementNS (sNamespaceURI, "Message").addText (ex.getMessage ());
      ret.addElementNS (sNamespaceURI, "StackTrace").addText (StackTraceHelper.getStackAsString (ex));
      return ret;
    };

    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.addElementNS (sNamespaceURI, "CurrentDateTimeUTC")
       .addText (PDTWebDateHelper.getAsStringXSD (m_aCurrentDateTimeUTC));
    ret.addElementNS (sNamespaceURI, "phase4Version").addText (CAS4Version.BUILD_VERSION);
    ret.addElementNS (sNamespaceURI, "SMLDNSZone").addText (m_sSMLDNSZone);

    if (hasSBDHParseException ())
      ret.addChild (fEx.apply (m_aSBDHParseException, "SBDHParsingException"));

    if (hasSenderID ())
      ret.addElementNS (sNamespaceURI, "SenderID").addText (m_aSenderID.getURIEncoded ());
    if (hasReceiverID ())
      ret.addElementNS (sNamespaceURI, "ReceiverID").addText (m_aReceiverID.getURIEncoded ());
    if (hasDocTypeID ())
      ret.addElementNS (sNamespaceURI, "DocTypeID").addText (m_aDocTypeID.getURIEncoded ());
    if (hasProcessID ())
      ret.addElementNS (sNamespaceURI, "ProcessID").addText (m_aProcessID.getURIEncoded ());
    if (hasSenderPartyID ())
      ret.addElementNS (sNamespaceURI, "SenderPartyID").addText (m_sSenderPartyID);
    if (hasTransportProfileID ())
      ret.addElementNS (sNamespaceURI, "TransportProfileID").addText (m_sTransportProfileID);

    if (hasSBDHInstanceIdentifier ())
      ret.addElementNS (sNamespaceURI, "SBDHInstanceIdentifier").addText (m_sSBDHInstanceIdentifier);

    if (hasC3EndpointURL ())
      ret.addElementNS (sNamespaceURI, "C3EndpointUrl").addText (m_sC3EndpointURL);
    if (hasC3Cert ())
      ret.addElementNS (sNamespaceURI, "C3Cert").addText (CertificateHelper.getPEMEncodedCertificate (m_aC3Cert));
    if (hasC3CertSubjectCN ())
      ret.addElementNS (sNamespaceURI, "C3CertSubjectCN").addText (m_sC3CertSubjectCN);
    if (hasC3CertSubjectO ())
      ret.addElementNS (sNamespaceURI, "C3CertSubjectO").addText (m_sC3CertSubjectO);
    if (hasC3CertCheckDT ())
      ret.addElementNS (sNamespaceURI, "C3CertCheckDT").addText (PDTWebDateHelper.getAsStringXSD (m_aC3CertCheckDT));
    if (hasC3CertCheckResult ())
      ret.addElementNS (sNamespaceURI, "C3CertCheckResult").addText (m_eC3CertCheckResult.name ());
    if (hasC3TechnicalContact ())
      ret.addElementNS (sNamespaceURI, "C3TechnicalContact").addText (m_sC3TechnicalContact);

    if (hasAS4MessageID ())
      ret.addElementNS (sNamespaceURI, "AS4MessageId").addText (m_sAS4MessageID);
    if (hasAS4ConversationID ())
      ret.addElementNS (sNamespaceURI, "AS4ConversationId").addText (m_sAS4ConversationID);
    if (hasAS4SendingDT ())
      ret.addElementNS (sNamespaceURI, "AS4SendingDateTime")
         .addText (PDTWebDateHelper.getAsStringXSD (m_aAS4SendingDT));

    // Don't render Raw response in case of success, as the Signal Message is contained anyway
    if (hasRawHttpResponse () && hasUnsuccessfulAS4SendingResult ())
    {
      final IMicroElement aRawHttpResponse = ret.addElementNS (sNamespaceURI, "RawHttpResponse");
      if (m_aRawHttpResponse.hasResponseStatusLine ())
        aRawHttpResponse.addElementNS (sNamespaceURI, "StatusLine")
                        .addText (m_aRawHttpResponse.getResponseStatusLine ().toString ());

      {
        final IMicroElement aHeaders = aRawHttpResponse.addElementNS (sNamespaceURI, "Headers");
        for (final var aHeader : m_aRawHttpResponse.getResponseHeaders ())
        {
          final IMicroElement aHeaderEl = aHeaders.addElementNS (sNamespaceURI, "Header");
          aHeaderEl.setAttribute ("name", aHeader.getKey ());

          for (final String sValue : aHeader.getValue ())
            aHeaderEl.addElementNS (sNamespaceURI, "Value").addText (sValue);
        }
      }

      if (m_aRawHttpResponse.hasResponseContent ())
      {
        // Does not clone
        final byte [] aBytes = m_aRawHttpResponse.getResponseContent ();
        aRawHttpResponse.addElementNS (sNamespaceURI, "ContentLength").addText (aBytes.length);
        aRawHttpResponse.addElementNS (sNamespaceURI, "Content").addText (new String (aBytes, StandardCharsets.UTF_8));
      }
      else
        aRawHttpResponse.addElementNS (sNamespaceURI, "ContentLength").addText (0);
    }

    if (hasAS4SendingResult ())
      ret.addElementNS (sNamespaceURI, "AS4SendingResult").addText (m_eAS4SendingResult.name ());
    if (hasAS4SendingException ())
      ret.addChild (fEx.apply (m_aAS4SendingException, "AS4SendingException"));
    if (hasAS4ReceivedSignalMsg ())
      ret.addElementNS (sNamespaceURI, "AS4ReceivedSignalMsg")
         .addChild (new Ebms3SignalMessageMarshaller ().getAsMicroElement (m_aAS4ReceivedSignalMsg));
    ret.addElementNS (sNamespaceURI, "AS4ResponseError").addText (m_bAS4ResponseError);
    if (hasAS4ResponseErrors ())
    {
      final IMicroElement aErrors = ret.addElementNS (sNamespaceURI, "AS4ResponseErrors");

      for (final Ebms3Error aError : m_aAS4ResponseErrors)
      {
        final IMicroElement aItem = aErrors.addElementNS (sNamespaceURI, "Item");
        if (aError.getDescription () != null)
          aItem.addElementNS (sNamespaceURI, "Description").addText (aError.getDescriptionValue ());
        if (aError.getErrorDetail () != null)
          aItem.addElementNS (sNamespaceURI, "ErrorDetails").addText (aError.getErrorDetail ());
        if (aError.getCategory () != null)
          aItem.addElementNS (sNamespaceURI, "Category").addText (aError.getCategory ());
        if (aError.getRefToMessageInError () != null)
          aItem.addElementNS (sNamespaceURI, "RefToMessageInError").addText (aError.getRefToMessageInError ());
        if (aError.getErrorCode () != null)
          aItem.addElementNS (sNamespaceURI, "ErrorCode").addText (aError.getErrorCode ());
        if (aError.getOrigin () != null)
          aItem.addElementNS (sNamespaceURI, "Origin").addText (aError.getOrigin ());
        if (aError.getSeverity () != null)
          aItem.addElementNS (sNamespaceURI, "Severity").addText (aError.getSeverity ());
        if (aError.getShortDescription () != null)
          aItem.addElementNS (sNamespaceURI, "ShortDescription").addText (aError.getShortDescription ());
      }
    }

    ret.addElementNS (sNamespaceURI, "OverallDurationMillis").addText (m_nOverallDurationMillis);
    ret.addElementNS (sNamespaceURI, "SendingSuccess").addText (m_bSendingSuccess);
    ret.addElementNS (sNamespaceURI, "OverallSuccess").addText (m_bOverallSuccess);
    return ret;
  }

  @NonNull
  public String getAsXMLString ()
  {
    return getAsXMLString (null, new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN));
  }

  @NonNull
  public String getAsXMLString (@Nullable final String sNamespaceURI, @NonNull final IXMLWriterSettings aXWS)
  {
    return MicroWriter.getNodeAsString (getAsMicroElement (sNamespaceURI, "HREDeliverySendingReport"), aXWS);
  }
}
