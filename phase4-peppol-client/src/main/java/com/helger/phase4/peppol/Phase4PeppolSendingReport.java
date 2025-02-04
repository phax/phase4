/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol;

import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.string.StringHelper;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.IJsonWriterSettings;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.utils.EPeppolCertificateCheckResult;
import com.helger.peppol.utils.PeppolCertificateHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.marshaller.Ebms3SignalMessageMarshaller;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.security.certificate.CertificateHelper;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.IXMLWriterSettings;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * This class contains the structured information about what happens on Peppol
 * sending.
 *
 * @author Philip Helger
 * @since 3.0.5
 */
public final class Phase4PeppolSendingReport
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
  private String m_sCountryC1;
  private String m_sSenderPartyID;

  // SBDH details
  private String m_sSBDHInstanceIdentifier;

  // SMP lookup results
  private String m_sC3EndpointURL;
  private X509Certificate m_aC3Cert;
  private String m_sC3CertSubjectCN;
  private OffsetDateTime m_aC3CertCheckDT;
  private EPeppolCertificateCheckResult m_eC3CertCheckResult;

  // AS4 params
  private String m_sAS4MessageID;
  private String m_sAS4ConversationID;

  // AS4 response details
  private EAS4UserMessageSendResult m_eAS4SendingResult;
  private Exception m_aAS4SendingException;
  private Ebms3SignalMessage m_aAS4ReceivedSignalMsg;
  private boolean m_bAS4ResponseError = false;
  private ICommonsList <Ebms3Error> m_aAS4ResponseErrors;

  private long m_nOverallDurationMillis = -1;
  private boolean m_bSendingSuccess = false;
  private boolean m_bOverallSuccess = false;

  public Phase4PeppolSendingReport (@Nonnull final ISMLInfo aSMLInfo)
  {
    m_aCurrentDateTimeUTC = PDTFactory.getCurrentOffsetDateTimeUTC ();
    m_sSMLDNSZone = aSMLInfo.getDNSZone ();
  }

  public boolean hasSBDHParseException ()
  {
    return m_aSBDHParseException != null;
  }

  public void setSBDHParseException (@Nullable final Exception e)
  {
    m_aSBDHParseException = e;
  }

  public boolean hasSenderID ()
  {
    return m_aSenderID != null;
  }

  public void setSenderID (@Nullable final IParticipantIdentifier a)
  {
    m_aSenderID = a;
  }

  public boolean hasReceiverID ()
  {
    return m_aReceiverID != null;
  }

  public void setReceiverID (@Nullable final IParticipantIdentifier a)
  {
    m_aReceiverID = a;
  }

  public boolean hasDocTypeID ()
  {
    return m_aDocTypeID != null;
  }

  public void setDocTypeID (@Nullable final IDocumentTypeIdentifier a)
  {
    m_aDocTypeID = a;
  }

  public boolean hasProcessID ()
  {
    return m_aProcessID != null;
  }

  public void setProcessID (@Nullable final IProcessIdentifier a)
  {
    m_aProcessID = a;
  }

  public boolean hasCountryC1 ()
  {
    return StringHelper.hasText (m_sCountryC1);
  }

  public void setCountryC1 (@Nullable final String s)
  {
    m_sCountryC1 = s;
  }

  public boolean hasSenderPartyID ()
  {
    return StringHelper.hasText (m_sSenderPartyID);
  }

  public void setSenderPartyID (@Nullable final String s)
  {
    m_sSenderPartyID = s;
  }

  public boolean hasSBDHInstanceIdentifier ()
  {
    return StringHelper.hasText (m_sSBDHInstanceIdentifier);
  }

  public void setSBDHInstanceIdentifier (@Nullable final String s)
  {
    m_sSBDHInstanceIdentifier = s;
  }

  public boolean hasC3EndpointURL ()
  {
    return StringHelper.hasText (m_sC3EndpointURL);
  }

  public void setC3EndpointURL (@Nullable final String s)
  {
    m_sC3EndpointURL = s;
  }

  public boolean hasC3Cert ()
  {
    return m_aC3Cert != null;
  }

  public boolean hasC3CertSubjectCN ()
  {
    return StringHelper.hasText (m_sC3CertSubjectCN);
  }

  public void setC3Cert (@Nullable final X509Certificate a)
  {
    m_aC3Cert = a;
    m_sC3CertSubjectCN = PeppolCertificateHelper.getSubjectCN (a);
  }

  public boolean hasC3CertCheckDT ()
  {
    return m_aC3CertCheckDT != null;
  }

  public void setC3CertCheckDT (@Nullable final OffsetDateTime a)
  {
    m_aC3CertCheckDT = a;
  }

  public boolean hasC3CertCheckResult ()
  {
    return m_eC3CertCheckResult != null;
  }

  public void setC3CertCheckResult (@Nullable final EPeppolCertificateCheckResult e)
  {
    m_eC3CertCheckResult = e;
  }

  public boolean hasAS4MessageID ()
  {
    return StringHelper.hasText (m_sAS4MessageID);
  }

  public void setAS4MessageID (@Nullable final String s)
  {
    m_sAS4MessageID = s;
  }

  public boolean hasAS4ConversationID ()
  {
    return StringHelper.hasText (m_sAS4ConversationID);
  }

  public void setAS4ConversationID (@Nullable final String s)
  {
    m_sAS4ConversationID = s;
  }

  public boolean hasAS4ReceivedSignalMsg ()
  {
    return m_aAS4ReceivedSignalMsg != null;
  }

  public boolean hasAS4ResponseErrors ()
  {
    return m_aAS4ResponseErrors != null && m_aAS4ResponseErrors.isNotEmpty ();
  }

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

  public boolean hasAS4SendingResult ()
  {
    return m_eAS4SendingResult != null;
  }

  public void setAS4SendingResult (@Nullable final EAS4UserMessageSendResult e)
  {
    m_eAS4SendingResult = e;
  }

  public boolean hasAS4SendingException ()
  {
    return m_aAS4SendingException != null;
  }

  public void setAS4SendingException (@Nullable final Exception e)
  {
    m_aAS4SendingException = e;
  }

  public void setOverallDurationMillis (final long n)
  {
    m_nOverallDurationMillis = n;
  }

  public void setSendingSuccess (final boolean b)
  {
    m_bSendingSuccess = b;
  }

  public void setOverallSuccess (final boolean b)
  {
    m_bOverallSuccess = b;
  }

  @Nonnull
  public IJsonObject getAsJsonObject ()
  {
    final Function <Exception, IJsonObject> fEx = ex -> new JsonObject ().add ("class", ex.getClass ().getName ())
                                                                         .add ("message", ex.getMessage ())
                                                                         .add ("stackTrace",
                                                                               StackTraceHelper.getStackAsString (ex));

    final IJsonObject aJson = new JsonObject ();
    aJson.add ("currentDateTimeUTC", PDTWebDateHelper.getAsStringXSD (m_aCurrentDateTimeUTC));
    aJson.add ("smlDnsZone", m_sSMLDNSZone);

    if (hasSBDHParseException ())
      aJson.addJson ("sbdhParsingException", fEx.apply (m_aSBDHParseException));

    if (hasSenderID ())
      aJson.add ("senderId", m_aSenderID.getURIEncoded ());
    if (hasReceiverID ())
      aJson.add ("receiverId", m_aReceiverID.getURIEncoded ());
    if (hasDocTypeID ())
      aJson.add ("docTypeId", m_aDocTypeID.getURIEncoded ());
    if (hasProcessID ())
      aJson.add ("processId", m_aProcessID.getURIEncoded ());
    if (hasCountryC1 ())
      aJson.add ("countryC1", m_sCountryC1);
    if (hasSenderPartyID ())
      aJson.add ("senderPartyId", m_sSenderPartyID);

    if (hasSBDHInstanceIdentifier ())
      aJson.add ("sbdhInstanceIdentifier", m_sSBDHInstanceIdentifier);

    if (hasC3EndpointURL ())
      aJson.add ("c3EndpointUrl", m_sC3EndpointURL);
    if (hasC3Cert ())
      aJson.add ("c3Cert", CertificateHelper.getPEMEncodedCertificate (m_aC3Cert));
    if (hasC3CertSubjectCN ())
      aJson.add ("c3CertSubjectCN", m_sC3CertSubjectCN);
    if (hasC3CertCheckDT ())
      aJson.add ("c3CertCheckDT", PDTWebDateHelper.getAsStringXSD (m_aC3CertCheckDT));
    if (hasC3CertCheckResult ())
      aJson.add ("c3CertCheckResult", m_eC3CertCheckResult.name ());

    if (hasAS4MessageID ())
      aJson.add ("as4MessageId", m_sAS4MessageID);
    if (hasAS4ConversationID ())
      aJson.add ("as4ConversationId", m_sAS4ConversationID);

    if (hasAS4SendingResult ())
      aJson.add ("sendingResult", m_eAS4SendingResult.name ());
    if (hasAS4SendingException ())
      aJson.addJson ("sendingException", fEx.apply (m_aAS4SendingException));
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

  @Nonnull
  public String getAsJsonString ()
  {
    return getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);
  }

  @Nonnull
  public String getAsJsonString (@Nonnull final IJsonWriterSettings aJWS)
  {
    return getAsJsonObject ().getAsJsonString (aJWS);
  }

  @Nonnull
  public IMicroElement getAsMicroElement (@Nullable final String sNamespaceURI, @Nonnull final String sTagName)
  {
    final BiFunction <Exception, String, IMicroElement> fEx = (ex, tag) -> {
      final IMicroElement ret = new MicroElement (sNamespaceURI, tag);
      ret.appendElement (sNamespaceURI, "Class").appendText (ex.getClass ().getName ());
      ret.appendElement (sNamespaceURI, "Message").appendText (ex.getMessage ());
      ret.appendElement (sNamespaceURI, "StackTrace").appendText (StackTraceHelper.getStackAsString (ex));
      return ret;
    };

    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.appendElement (sNamespaceURI, "CurrentDateTimeUTC")
       .appendText (PDTWebDateHelper.getAsStringXSD (m_aCurrentDateTimeUTC));
    ret.appendElement (sNamespaceURI, "SMLDNSZone").appendText (m_sSMLDNSZone);

    if (hasSBDHParseException ())
      ret.appendChild (fEx.apply (m_aSBDHParseException, "SBDHParsingException"));

    if (hasSenderID ())
      ret.appendElement (sNamespaceURI, "SenderID").appendText (m_aSenderID.getURIEncoded ());
    if (hasReceiverID ())
      ret.appendElement (sNamespaceURI, "ReceiverID").appendText (m_aReceiverID.getURIEncoded ());
    if (hasDocTypeID ())
      ret.appendElement (sNamespaceURI, "DocTypeID").appendText (m_aDocTypeID.getURIEncoded ());
    if (hasProcessID ())
      ret.appendElement (sNamespaceURI, "ProcessID").appendText (m_aProcessID.getURIEncoded ());
    if (hasCountryC1 ())
      ret.appendElement (sNamespaceURI, "CountryC1").appendText (m_sCountryC1);
    if (hasSenderPartyID ())
      ret.appendElement (sNamespaceURI, "SenderPartyID").appendText (m_sSenderPartyID);

    if (hasSBDHInstanceIdentifier ())
      ret.appendElement (sNamespaceURI, "SBDHInstanceIdentifier").appendText (m_sSBDHInstanceIdentifier);

    if (hasC3EndpointURL ())
      ret.appendElement (sNamespaceURI, "C3EndpointUrl").appendText (m_sC3EndpointURL);
    if (hasC3Cert ())
      ret.appendElement (sNamespaceURI, "C3Cert").appendText (CertificateHelper.getPEMEncodedCertificate (m_aC3Cert));
    if (hasC3CertSubjectCN ())
      ret.appendElement (sNamespaceURI, "C3CertSubjectCN").appendText (m_sC3CertSubjectCN);
    if (hasC3CertCheckDT ())
      ret.appendElement (sNamespaceURI, "C3CertCheckDT")
         .appendText (PDTWebDateHelper.getAsStringXSD (m_aC3CertCheckDT));
    if (hasC3CertCheckResult ())
      ret.appendElement (sNamespaceURI, "C3CertCheckResult").appendText (m_eC3CertCheckResult.name ());

    if (hasAS4MessageID ())
      ret.appendElement (sNamespaceURI, "AS4MessageId").appendText (m_sAS4MessageID);
    if (hasAS4ConversationID ())
      ret.appendElement (sNamespaceURI, "AS4ConversationId").appendText (m_sAS4ConversationID);

    if (hasAS4SendingResult ())
      ret.appendElement (sNamespaceURI, "AS4SendingResult").appendText (m_eAS4SendingResult.name ());
    if (hasAS4SendingException ())
      ret.appendChild (fEx.apply (m_aAS4SendingException, "AS4SendingException"));
    if (hasAS4ReceivedSignalMsg ())
      ret.appendElement (sNamespaceURI, "AS4ReceivedSignalMsg")
         .appendChild (new Ebms3SignalMessageMarshaller ().getAsMicroElement (m_aAS4ReceivedSignalMsg));
    ret.appendElement (sNamespaceURI, "AS4ResponseError").appendText (m_bAS4ResponseError);
    if (hasAS4ResponseErrors ())
    {
      final IMicroElement aErrors = ret.appendElement (sNamespaceURI, "AS4ResponseErrors");

      for (final Ebms3Error aError : m_aAS4ResponseErrors)
      {
        final IMicroElement aItem = aErrors.appendElement (sNamespaceURI, "Item");
        if (aError.getDescription () != null)
          aItem.appendElement (sNamespaceURI, "Description").appendText (aError.getDescriptionValue ());
        if (aError.getErrorDetail () != null)
          aItem.appendElement (sNamespaceURI, "ErrorDetails").appendText (aError.getErrorDetail ());
        if (aError.getCategory () != null)
          aItem.appendElement (sNamespaceURI, "Category").appendText (aError.getCategory ());
        if (aError.getRefToMessageInError () != null)
          aItem.appendElement (sNamespaceURI, "RefToMessageInError").appendText (aError.getRefToMessageInError ());
        if (aError.getErrorCode () != null)
          aItem.appendElement (sNamespaceURI, "ErrorCode").appendText (aError.getErrorCode ());
        if (aError.getOrigin () != null)
          aItem.appendElement (sNamespaceURI, "Origin").appendText (aError.getOrigin ());
        if (aError.getSeverity () != null)
          aItem.appendElement (sNamespaceURI, "Severity").appendText (aError.getSeverity ());
        if (aError.getShortDescription () != null)
          aItem.appendElement (sNamespaceURI, "ShortDescription").appendText (aError.getShortDescription ());
      }
    }

    ret.appendElement (sNamespaceURI, "OverallDurationMillis").appendText (m_nOverallDurationMillis);
    ret.appendElement (sNamespaceURI, "SendingSuccess").appendText (m_bSendingSuccess);
    ret.appendElement (sNamespaceURI, "OverallSuccess").appendText (m_bOverallSuccess);
    return ret;
  }

  @Nonnull
  public String getAsXMLString ()
  {
    return getAsXMLString (null, new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN));
  }

  @Nonnull
  public String getAsXMLString (@Nullable final String sNamespaceURI, @Nonnull final IXMLWriterSettings aXWS)
  {
    return MicroWriter.getNodeAsString (getAsMicroElement (sNamespaceURI, "PeppolSendingReport"), aXWS);
  }
}
