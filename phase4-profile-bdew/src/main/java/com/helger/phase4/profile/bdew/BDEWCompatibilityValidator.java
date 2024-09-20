/*
 * Copyright (C) 2023-2024 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
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
package com.helger.phase4.profile.bdew;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.error.IError;
import com.helger.commons.error.SingleError;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.ebms3header.Ebms3AgreementRef;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3From;
import com.helger.phase4.ebms3header.Ebms3PartyId;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3Service;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3To;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.model.pmode.PModePayloadService;
import com.helger.phase4.model.pmode.PModeReceptionAwareness;
import com.helger.phase4.model.pmode.PModeValidationException;
import com.helger.phase4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.phase4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.phase4.model.pmode.leg.PModeLegProtocol;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;
import com.helger.phase4.profile.IAS4ProfileValidator;
import com.helger.phase4.wss.EWSSVersion;

/**
 * Validate certain requirements imposed by the BDEW project.
 *
 * @author Gregor Scholtysik
 * @since 2.1.0
 */
public class BDEWCompatibilityValidator implements IAS4ProfileValidator
{

  public static final String EMT_MAK = "EMT.MAK";

  public BDEWCompatibilityValidator ()
  {}

  @Nonnull
  private static IError _createError (@Nonnull final String sMsg)
  {
    return SingleError.builderError ().errorText (sMsg).build ();
  }

  @Nonnull
  private static IError _createWarn (@Nonnull final String sMsg)
  {
    return SingleError.builderWarn ().errorText (sMsg).build ();
  }

  private static void _checkIfLegIsValid (@Nonnull final ErrorList aErrorList,
                                          @Nonnull final PModeLeg aPModeLeg,
                                          @Nonnull @Nonempty final String sFieldPrefix)
  {
    final PModeLegProtocol aLegProtocol = aPModeLeg.getProtocol ();
    if (aLegProtocol == null)
    {
      aErrorList.add (_createError (sFieldPrefix + "Protocol is missing"));
    }
    else
    {
      // PROTOCOL Address only https allowed
      final String sAddressProtocol = aLegProtocol.getAddressProtocol ();

      if (StringHelper.hasText (sAddressProtocol))
      {
        if (sAddressProtocol.equalsIgnoreCase ("https"))
        {
          // Always okay
        }
        else
        {
          // Other protocol
          aErrorList.add (_createError (sFieldPrefix + "AddressProtocol '" + sAddressProtocol + "' is unsupported"));
        }
      }
      else
      {
        // Empty address protocol
        if (false)
          aErrorList.add (_createError (sFieldPrefix + "AddressProtocol is missing"));
      }

      final PModeLegBusinessInformation aBusinessInfo = aPModeLeg.getBusinessInfo ();
      if (aBusinessInfo == null)
      {
        aErrorList.add (_createError (sFieldPrefix + "BusinessInfo is missing"));
      }
      else
      {
        final String sService = aBusinessInfo.getService ();
        if (sService == null || !BDEWPMode.containsService (sService))
        {
          aErrorList.add (_createError (sFieldPrefix + "BusinessInfo.Service '" + sService + "' is unsupported"));
        }

        final String sAction = aBusinessInfo.getAction ();
        if (sAction == null || !BDEWPMode.containsAction (sAction))
        {
          aErrorList.add (_createError (sFieldPrefix + "BusinessInfo.Action '" + sAction + "' is unsupported"));
        }
      }

      final ESoapVersion eSOAPVersion = aLegProtocol.getSoapVersion ();
      if (!eSOAPVersion.isAS4Default ())
      {
        aErrorList.add (_createError (sFieldPrefix +
                                      "SoapVersion '" +
                                      eSOAPVersion.getVersion () +
                                      "' is unsupported"));
      }
    }

    // Only check the security features if a Security Leg is currently present
    final PModeLegSecurity aPModeLegSecurity = aPModeLeg.getSecurity ();
    if (aPModeLegSecurity != null)
    {
      // Check Certificate
      if (false)
        if (aPModeLegSecurity.getX509SignatureCertificate () == null)
        {
          aErrorList.add (_createError (sFieldPrefix + "Security.X509SignatureCertificate is missing"));
        }

      // Check Signature Algorithm
      if (aPModeLegSecurity.getX509SignatureAlgorithm () == null)
      {
        aErrorList.add (_createError (sFieldPrefix + "Security.X509SignatureAlgorithm is missing"));
      }
      else
        if (!aPModeLegSecurity.getX509SignatureAlgorithm ().equals (ECryptoAlgorithmSign.ECDSA_SHA_256))
        {
          aErrorList.add (_createError (sFieldPrefix +
                                        "Security.X509SignatureAlgorithm must use the value '" +
                                        ECryptoAlgorithmSign.ECDSA_SHA_256.getID () +
                                        "'"));
        }

      // Check Hash Function
      if (aPModeLegSecurity.getX509SignatureHashFunction () == null)
      {
        aErrorList.add (_createError (sFieldPrefix + "Security.X509SignatureHashFunction is missing"));
      }
      else
        if (!aPModeLegSecurity.getX509SignatureHashFunction ().equals (ECryptoAlgorithmSignDigest.DIGEST_SHA_256))
        {
          aErrorList.add (_createError (sFieldPrefix +
                                        "Security.X509SignatureHashFunction must use the value '" +
                                        ECryptoAlgorithmSignDigest.DIGEST_SHA_256.getID () +
                                        "'"));
        }

      // Check Encrypt algorithm
      if (aPModeLegSecurity.getX509EncryptionAlgorithm () == null)
      {
        aErrorList.add (_createError (sFieldPrefix + "Security.X509EncryptionAlgorithm is missing"));
      }
      else
      {
        if (!aPModeLegSecurity.getX509EncryptionAlgorithm ().equals (ECryptoAlgorithmCrypt.AES_128_GCM))
        {
          aErrorList.add (_createError (sFieldPrefix +
                                        "Security.X509EncryptionAlgorithm must use the value '" +
                                        ECryptoAlgorithmCrypt.AES_128_GCM.getID () +
                                        "' instead of '" +
                                        aPModeLegSecurity.getX509EncryptionAlgorithm ().getID () +
                                        "'"));
        }
      }

      final Integer aEncryptionMinimumStrength = aPModeLegSecurity.getX509EncryptionMinimumStrength ();
      if (aEncryptionMinimumStrength == null || aEncryptionMinimumStrength.intValue () != 128)
      {
        aErrorList.add (_createError (sFieldPrefix +
                                      "Security.X509Encryption.MinimalStrength must be defined and set to 128"));
      }

      // Check WSS Version = 1.1.1
      if (aPModeLegSecurity.getWSSVersion () != null)
      {
        // Check for WSS - Version if there is one present
        if (!aPModeLegSecurity.getWSSVersion ().equals (EWSSVersion.WSS_111))
          aErrorList.add (_createError (sFieldPrefix +
                                        "Security.WSSVersion must use the value " +
                                        EWSSVersion.WSS_111 +
                                        " instead of " +
                                        aPModeLegSecurity.getWSSVersion ()));
      }

      if (aPModeLegSecurity.isUsernameTokenCreatedDefined () ||
          aPModeLegSecurity.isUsernameTokenDigestDefined () ||
          aPModeLegSecurity.isUsernameTokenNonceDefined () ||
          aPModeLegSecurity.hasUsernameTokenPassword () ||
          aPModeLegSecurity.hasUsernameTokenUsername ())
      {
        aErrorList.add (_createError (sFieldPrefix + "Username nor its part MUST NOT be set"));
      }

      // PModeAuthorize
      if (aPModeLegSecurity.isPModeAuthorizeDefined ())
      {
        if (aPModeLegSecurity.isPModeAuthorize ())
          aErrorList.add (_createError (sFieldPrefix + "Security.PModeAuthorize must be set to 'false'"));
      }
      else
      {
        aErrorList.add (_createError (sFieldPrefix + "Security.PModeAuthorize is missing"));
      }

      if (!aPModeLegSecurity.isSendReceiptDefined () || !aPModeLegSecurity.isSendReceipt ())
      {
        aErrorList.add (_createError (sFieldPrefix + "Security.SendReceipt must be defined and set to 'true'"));
      }
      else
      {
        // set response required
        if (!aPModeLegSecurity.isSendReceiptNonRepudiation ())
          aErrorList.add (_createError (sFieldPrefix + "SendReceiptNonRepudiation must be set to 'true'"));

        if (aPModeLegSecurity.getSendReceiptReplyPattern () != EPModeSendReceiptReplyPattern.RESPONSE)
          aErrorList.add (_createError (sFieldPrefix +
                                        "Security.SendReceiptReplyPattern must use the value " +
                                        EPModeSendReceiptReplyPattern.RESPONSE +
                                        " instead of " +
                                        aPModeLegSecurity.getSendReceiptReplyPattern ()));
      }
    }
    else
    {
      aErrorList.add (_createError (sFieldPrefix + "Security is missing"));
    }

    // Error Handling
    final PModeLegErrorHandling aErrorHandling = aPModeLeg.getErrorHandling ();
    if (aErrorHandling != null)
    {
      if (aErrorHandling.isReportAsResponseDefined ())
      {
        if (!aErrorHandling.isReportAsResponse ())
          aErrorList.add (_createError (sFieldPrefix + "ErrorHandling.Report.AsResponse must be 'true'"));
      }
      else
      {
        aErrorList.add (_createError (sFieldPrefix + "ErrorHandling.Report.AsResponse is missing"));
      }
      if (aErrorHandling.isReportProcessErrorNotifyConsumerDefined ())
      {
        if (!aErrorHandling.isReportProcessErrorNotifyConsumer ())
          aErrorList.add (_createWarn (sFieldPrefix +
                                       "ErrorHandling.Report.ProcessErrorNotifyConsumer should be 'true'"));
      }
      else
      {
        aErrorList.add (_createError (sFieldPrefix + "ErrorHandling.Report.ProcessErrorNotifyConsumer is missing"));
      }

      if (aErrorHandling.isReportProcessErrorNotifyProducerDefined ())
      {
        if (!aErrorHandling.isReportProcessErrorNotifyProducer ())
          aErrorList.add (_createWarn (sFieldPrefix +
                                       "ErrorHandling.Report.ProcessErrorNotifyProducer should be 'true'"));
      }
      else
      {
        aErrorList.add (_createError (sFieldPrefix + "ErrorHandling.Report.ProcessErrorNotifyProducer is missing"));
      }

      if (aErrorHandling.getReportSenderErrorsTo () != null &&
          aErrorHandling.getReportSenderErrorsTo ().addresses () != null &&
          aErrorHandling.getReportSenderErrorsTo ().addresses ().isNotEmpty ())
      {
        aErrorList.add (_createError (sFieldPrefix + "ReportSenderErrorsTo must not be set"));
      }
    }
    else
    {
      aErrorList.add (_createError (sFieldPrefix + "ErrorHandling is missing"));
    }
  }

  @Override
  public void validatePMode (@Nonnull final IPMode aPMode,
                             @Nonnull final ErrorList aErrorList,
                             @Nonnull final EAS4ProfileValidationMode eValidationMode)
  {
    ValueEnforcer.notNull (aPMode, "PMode");
    ValueEnforcer.notNull (aErrorList, "ErrorList");
    ValueEnforcer.notNull (eValidationMode, "ValidationMode");
    ValueEnforcer.isTrue (aErrorList.isEmpty (), () -> "Errors in global PMode validation: " + aErrorList);

    try
    {
      MetaAS4Manager.getPModeMgr ().validatePMode (aPMode);
    }
    catch (final PModeValidationException ex)
    {
      aErrorList.add (_createError (ex.getMessage ()));
    }

    final String sAgreement = aPMode.getAgreement ();
    if (sAgreement == null || !sAgreement.equals (BDEWPMode.DEFAULT_AGREEMENT_ID))
    {
      aErrorList.add (_createError ("PMode.Agreement must be set to '" + BDEWPMode.DEFAULT_AGREEMENT_ID + "'"));
    }

    final EMEP eMEP = aPMode.getMEP ();
    final EMEPBinding eMEPBinding = aPMode.getMEPBinding ();

    if (eMEP == EMEP.ONE_WAY && eMEPBinding == EMEPBinding.PUSH)
    {
      // Valid
    }
    else
    {
      aErrorList.add (_createError ("An invalid combination of PMode MEP (" +
                                    eMEP +
                                    ") and MEP binding (" +
                                    eMEPBinding +
                                    ") was specified, valid is only one-way/push."));
    }

    final PModeParty aInitiatorParty = aPMode.getInitiator ();
    if (aInitiatorParty != null && !aInitiatorParty.getRole ().equals (CAS4.DEFAULT_INITIATOR_URL))
    {
      aErrorList.add (_createError ("PMode.Initiator.Role must be set to '" + CAS4.DEFAULT_INITIATOR_URL + "'"));
    }

    final PModeParty aResponderParty = aPMode.getResponder ();
    if (aResponderParty != null && !aResponderParty.getRole ().equals (CAS4.DEFAULT_RESPONDER_URL))
    {
      aErrorList.add (_createError ("PMode.Responder.Role must be set to '" + CAS4.DEFAULT_RESPONDER_URL + "'"));
    }

    // Leg1 must be present
    final PModeLeg aPModeLeg1 = aPMode.getLeg1 ();
    if (aPModeLeg1 == null)
    {
      aErrorList.add (_createError ("PMode.Leg[1] is missing"));
    }
    else
    {
      _checkIfLegIsValid (aErrorList, aPModeLeg1, "PMode.Leg[1].");
    }

    if (aPMode.getLeg2 () != null)
    {
      aErrorList.add (_createError ("PMode.Leg[2] must not be present"));
    }

    final PModePayloadService aPayloadService = aPMode.getPayloadService ();
    if (aPayloadService != null)
    {
      final EAS4CompressionMode eCompressionMode = aPayloadService.getCompressionMode ();
      if (eCompressionMode != null)
      {
        if (!eCompressionMode.equals (EAS4CompressionMode.GZIP))
          aErrorList.add (_createError ("PMode.PayloadService.CompressionMode must be " +
                                        EAS4CompressionMode.GZIP +
                                        " instead of " +
                                        eCompressionMode));
      }
      else
      {
        aErrorList.add (_createError ("PMode.PayloadService.CompressionMode is missing"));
      }
    }

    // ReceptionAwareness
    final PModeReceptionAwareness aPModeReceptionAwareness = aPMode.getReceptionAwareness ();
    if (aPModeReceptionAwareness != null)
    {
      if (!aPModeReceptionAwareness.isReceptionAwarenessDefined () || !aPModeReceptionAwareness.isReceptionAwareness ())
      {
        aErrorList.add (_createError ("PMode[1].ReceptionAwareness must be defined and set to 'true'"));
      }
      else
      {
        if (!aPModeReceptionAwareness.isRetryDefined () || !aPModeReceptionAwareness.isRetry ())
        {
          aErrorList.add (_createError ("PMode[1].ReceptionAwareness.Retry must be defined and set to 'true'"));
        }

        if (!aPModeReceptionAwareness.isDuplicateDetectionDefined () ||
            !aPModeReceptionAwareness.isDuplicateDetection ())
        {
          aErrorList.add (_createError ("PMode[1].ReceptionAwareness.DuplicateDetection must be defined and set to 'true'"));
        }
      }
    }
  }

  @Override
  public void validateInitiatorIdentity (@Nonnull final Ebms3UserMessage aUserMsg,
                                         @Nullable final X509Certificate aSignatureCert,
                                         @Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                         @Nonnull final ErrorList aErrorList)
  {
    X509Certificate aTlsClientEndCert = null;
    if (aMessageMetadata.hasRemoteTlsCerts ())
    {
      aTlsClientEndCert = aMessageMetadata.remoteTlsCerts ().getFirstOrNull ();

      final X500Name aTlsName = new X500Name (aTlsClientEndCert.getSubjectX500Principal ().getName ());
      final RDN aTlsCnRDN = aTlsName.getRDNs (BCStyle.CN)[0];
      final String cn = IETFUtils.valueToString (aTlsCnRDN.getFirst ().getValue ());

      if (!cn.contains (EMT_MAK))
      {
        aErrorList.add (_createError ("TLS certificate '" +
                                      aTlsClientEndCert.getSubjectX500Principal () +
                                      "' is not an EMT/MAKO certificate"));
      }
    }

    if (aSignatureCert != null)
    {
      final X500Name aTlsName = new X500Name (aSignatureCert.getSubjectX500Principal ().getName ());
      final RDN aSigCnRDN = aTlsName.getRDNs (BCStyle.CN)[0];
      final String cn = IETFUtils.valueToString (aSigCnRDN.getFirst ().getValue ());

      if (!cn.contains (EMT_MAK))
      {
        aErrorList.add (_createError ("Signature certificate '" +
                                      aSignatureCert.getSubjectX500Principal () +
                                      "' is not an EMT/MAKO certificate"));
      }
    }

    final Ebms3PartyInfo aInitatorPartyInfo = aUserMsg.getPartyInfo ();
    if (aInitatorPartyInfo != null)
    {
      final Ebms3From aInitiator = aInitatorPartyInfo.getFrom ();
      if (aInitiator != null && aInitiator.hasPartyIdEntries ())
      {
        final Ebms3PartyId aInitiatorPartyID = aInitiator.getPartyIdAtIndex (0);
        if (aInitiatorPartyID != null)
        {
          final String sInitiatorID = aInitiatorPartyID.getValue ();
          if (sInitiatorID != null)
          {
            if (aSignatureCert != null)
            {
              final X500Name aSigName = new X500Name (aSignatureCert.getSubjectX500Principal ().getName ());
              final RDN aSigOuRDN = aSigName.getRDNs (BCStyle.OU)[0];
              final String sSigCertId = IETFUtils.valueToString (aSigOuRDN.getFirst ().getValue ());

              if (!sInitiatorID.equals (sSigCertId))
              {
                aErrorList.add (_createError ("ID of initiator party '" +
                                              sInitiatorID +
                                              "' does not match ID in signature certificate '" +
                                              sSigCertId +
                                              "'"));
              }
            }

            if (aTlsClientEndCert != null)
            {

              final X500Name aTlsName = new X500Name (aTlsClientEndCert.getSubjectX500Principal ().getName ());
              final RDN aTlsOuRDN = aTlsName.getRDNs (BCStyle.OU)[0];
              final String sTlsCertId = IETFUtils.valueToString (aTlsOuRDN.getFirst ().getValue ());

              if (!sInitiatorID.equals (sTlsCertId))
              {
                aErrorList.add (_createError ("ID of initiator party '" +
                                              sInitiatorID +
                                              "' does not match ID in client TLS certificate '" +
                                              sTlsCertId +
                                              "'"));
              }
            }
          }
        }
      }
    }
  }

  @Override
  public void validateUserMessage (@Nonnull final Ebms3UserMessage aUserMsg, @Nonnull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aUserMsg, "UserMsg");

    if (aUserMsg.getMessageInfo () == null)
    {
      aErrorList.add (_createError ("MessageInfo is missing"));
    }
    else
    {
      if (StringHelper.hasNoText (aUserMsg.getMessageInfo ().getMessageId ()))
        aErrorList.add (_createError ("MessageInfo/MessageId is missing"));

      if (StringHelper.hasText (aUserMsg.getMessageInfo ().getRefToMessageId ()))
        aErrorList.add (_createError ("MessageInfo/RefToMessageId must not be set"));

    }

    if (aUserMsg.getPartyInfo () == null)
    {
      aErrorList.add (_createError ("PartyInfo is missing"));
    }
    else
    {
      final Ebms3From aFrom = aUserMsg.getPartyInfo ().getFrom ();
      if (aFrom != null)
      {
        if (aFrom.getPartyIdCount () > 1)
        {
          aErrorList.add (_createError ("PartyInfo/From must contain no more than one PartyID"));
        }

        if (!CAS4.DEFAULT_INITIATOR_URL.equals (aFrom.getRole ()))
        {
          aErrorList.add (_createError ("PartyInfo/From/Role must be set to '" + CAS4.DEFAULT_INITIATOR_URL + "'"));
        }
      }
      else
      {
        aErrorList.add (_createError ("PartyInfo/From is missing"));
      }

      final Ebms3To aTo = aUserMsg.getPartyInfo ().getTo ();
      if (aTo != null)
      {
        if (aTo.getPartyIdCount () > 1)
        {
          aErrorList.add (_createError ("PartyInfo/To must contain no more than one PartyID"));
        }

        if (!CAS4.DEFAULT_RESPONDER_URL.equals (aTo.getRole ()))
        {
          aErrorList.add (_createError ("PartyInfo/To/Role must be set to '" + CAS4.DEFAULT_RESPONDER_URL + "'"));
        }
      }
      else
      {
        aErrorList.add (_createError ("PartyInfo/To is missing"));
      }
    }

    if (aUserMsg.getCollaborationInfo () == null)
    {
      aErrorList.add (_createError ("CollaborationInfo is missing"));
    }
    else
    {
      final Ebms3CollaborationInfo aCollaborationInfo = aUserMsg.getCollaborationInfo ();
      final Ebms3AgreementRef aAgreementRef = aCollaborationInfo.getAgreementRef ();
      if (aAgreementRef == null)
      {
        aErrorList.add (_createError ("CollaborationInfo/AgreementRef must be set!"));
      }
      else
      {
        if (StringHelper.hasNoText (aAgreementRef.getValue ()))
          aErrorList.add (_createError ("CollaborationInfo/AgreementRef value is missing"));
        if (!BDEWPMode.DEFAULT_AGREEMENT_ID.equals (aAgreementRef.getValue ()))
          aErrorList.add (_createError ("CollaborationInfo/AgreementRef value must equal " +
                                        BDEWPMode.DEFAULT_AGREEMENT_ID));
        if (aAgreementRef.getPmode () != null)
          aErrorList.add (_createError ("CollaborationInfo/PMode must not be set!"));
        if (aAgreementRef.getType () != null)
          aErrorList.add (_createError ("CollaborationInfo/Type must not be set!"));
      }

      final Ebms3Service aService = aCollaborationInfo.getService ();
      if (aService == null || StringHelper.hasNoText (aService.getValue ()))
      {
        aErrorList.add (_createError ("CollaborationInfo/Service is missing"));
      }
      else
      {
        if (!BDEWPMode.containsService (aService.getValue ()))
        {
          aErrorList.add (_createError ("CollaborationInfo/Service '" + aService.getValue () + "' is unsupported"));
        }
      }

      final String sAction = aCollaborationInfo.getAction ();
      if (StringHelper.hasNoText (sAction))
      {
        aErrorList.add (_createError ("CollaborationInfo/Action is missing"));
      }
      else
      {
        if (!BDEWPMode.containsAction (sAction))
        {
          aErrorList.add (_createError ("CollaborationInfo/Action '" + sAction + "' is unsupported"));
        }
      }
    }
  }

  @Override
  public void validateSignalMessage (@Nonnull final Ebms3SignalMessage aSignalMsg, @Nonnull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aSignalMsg, "SignalMsg");

    if (aSignalMsg.getMessageInfo () == null)
    {
      aErrorList.add (_createError ("MessageInfo is missing"));
    }
    else
    {
      if (StringHelper.hasNoText (aSignalMsg.getMessageInfo ().getMessageId ()))
        aErrorList.add (_createError ("MessageInfo/MessageId is missing"));
    }
  }
}
