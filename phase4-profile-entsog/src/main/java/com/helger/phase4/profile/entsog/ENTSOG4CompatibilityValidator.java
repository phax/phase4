/*
 * Copyright (C) 2015-2021 Pavel Rotek
 * pavel[dot]rotek[at]gmail[dot]com
 *
 * Copyright (C) 2021-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.entsog;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.ebms3header.Ebms3AgreementRef;
import com.helger.phase4.ebms3header.Ebms3From;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3To;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.PModePayloadService;
import com.helger.phase4.model.pmode.PModeValidationException;
import com.helger.phase4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.phase4.model.pmode.leg.PModeLegProtocol;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;
import com.helger.phase4.profile.IAS4ProfileValidator;
import com.helger.phase4.wss.EWSSVersion;

/**
 * Validate certain requirements imposed by the ENTSOG AS4 v4.0 profile. Supports both the primary
 * EdDSA/X25519 profile and the alternative ECDSA/ECDH-ES profile.
 *
 * @author Philip Helger
 * @since 4.4.2
 */
public class ENTSOG4CompatibilityValidator implements IAS4ProfileValidator
{
  private boolean m_bAllowECDSA;

  public ENTSOG4CompatibilityValidator ()
  {}

  /**
   * @return <code>true</code> if ECDSA signing (alternative EC profile) is also accepted,
   *         <code>false</code> if only EdDSA/Ed25519 (primary profile) is accepted.
   */
  public final boolean isAllowECDSA ()
  {
    return m_bAllowECDSA;
  }

  /**
   * Set whether ECDSA signing is also accepted as an alternative to EdDSA/Ed25519.
   *
   * @param b
   *        <code>true</code> to allow ECDSA, <code>false</code> to only allow EdDSA.
   * @return this for chaining
   */
  @NonNull
  public final ENTSOG4CompatibilityValidator setAllowECDSA (final boolean b)
  {
    m_bAllowECDSA = b;
    return this;
  }

  @NonNull
  private static IError _createError (@NonNull final String sMsg)
  {
    return SingleError.builderError ().errorText (sMsg).build ();
  }

  @NonNull
  private static IError _createWarn (@NonNull final String sMsg)
  {
    return SingleError.builderWarn ().errorText (sMsg).build ();
  }

  private void _checkIfLegIsValid (@NonNull final ErrorList aErrorList,
                                   @NonNull final PModeLeg aPModeLeg,
                                   @NonNull @Nonempty final String sFieldPrefix)
  {
    final PModeLegProtocol aLegProtocol = aPModeLeg.getProtocol ();
    if (aLegProtocol == null)
    {
      aErrorList.add (_createError (sFieldPrefix + "Protocol is missing"));
    }
    else
    {
      // PROTOCOL Address - HTTPS required, HTTP accepted with warning
      final String sAddressProtocol = aLegProtocol.getAddressProtocol ();
      if (StringHelper.isNotEmpty (sAddressProtocol))
      {
        if (sAddressProtocol.equalsIgnoreCase ("https"))
        {
          // Always okay - TLS is mandatory for ENTSOG 4.0
        }
        else
          if (sAddressProtocol.equalsIgnoreCase ("http"))
          {
            aErrorList.add (_createWarn (sFieldPrefix +
                                         "AddressProtocol 'http' is used but ENTSOG 4.0 requires TLS (https)"));
          }
          else
          {
            aErrorList.add (_createError (sFieldPrefix + "AddressProtocol '" + sAddressProtocol + "' is unsupported"));
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
      // Check Signature Algorithm - must be EdDSA Ed25519 (or optionally ECDSA)
      if (aPModeLegSecurity.getX509SignatureAlgorithm () == null)
      {
        aErrorList.add (_createError (sFieldPrefix + "Security.X509SignatureAlgorithm is missing"));
      }
      else
      {
        final ECryptoAlgorithmSign eSignAlgo = aPModeLegSecurity.getX509SignatureAlgorithm ();
        if (eSignAlgo.equals (ECryptoAlgorithmSign.EDDSA_ED25519))
        {
          // Primary profile - always valid
        }
        else
          if (m_bAllowECDSA &&
            (eSignAlgo.equals (ECryptoAlgorithmSign.ECDSA_SHA_256) ||
              eSignAlgo.equals (ECryptoAlgorithmSign.ECDSA_SHA_384) ||
              eSignAlgo.equals (ECryptoAlgorithmSign.ECDSA_SHA_512)))
          {
            // Alternative EC Profile - valid when ECDSA is allowed
          }
          else
          {
            aErrorList.add (_createError (sFieldPrefix +
                                          "Security.X509SignatureAlgorithm must use the value '" +
                                          ECryptoAlgorithmSign.EDDSA_ED25519.getID () +
                                          "'" +
                                          (m_bAllowECDSA ? " or an ECDSA algorithm" : "") +
                                          " instead of '" +
                                          eSignAlgo.getID () +
                                          "'"));
          }
      }

      // Check Hash Function - must be SHA-256
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

      // Check Encrypt algorithm - must be AES-128-GCM (no CBC)
      if (aPModeLegSecurity.getX509EncryptionAlgorithm () == null)
      {
        aErrorList.add (_createError (sFieldPrefix + "Security.X509EncryptionAlgorithm is missing"));
      }
      else
        if (!aPModeLegSecurity.getX509EncryptionAlgorithm ().equals (ECryptoAlgorithmCrypt.AES_128_GCM))
        {
          aErrorList.add (_createError (sFieldPrefix +
                                        "Security.X509EncryptionAlgorithm must use the value '" +
                                        ECryptoAlgorithmCrypt.AES_128_GCM.getID () +
                                        "' instead of '" +
                                        aPModeLegSecurity.getX509EncryptionAlgorithm ().getID () +
                                        "'"));
        }

      // Check WSS Version = 1.1.1
      if (aPModeLegSecurity.getWSSVersion () != null)
      {
        if (!aPModeLegSecurity.getWSSVersion ().equals (EWSSVersion.WSS_111))
          aErrorList.add (_createError (sFieldPrefix +
                                        "Security.WSSVersion must use the value " +
                                        EWSSVersion.WSS_111 +
                                        " instead of " +
                                        aPModeLegSecurity.getWSSVersion ()));
      }

      // No Username Tokens in ENTSOG 4.0
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

      // SEND RECEIPT TRUE/FALSE when false don't send receipts anymore
      if (aPModeLegSecurity.isSendReceiptDefined ())
      {
        if (aPModeLegSecurity.isSendReceipt ())
        {
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
  public void validatePMode (@NonNull final IPMode aPMode,
                             @NonNull final ErrorList aErrorList,
                             @NonNull final EAS4ProfileValidationMode eValidationMode)
  {
    ValueEnforcer.notNull (aPMode, "PMode");
    ValueEnforcer.notNull (aErrorList, "ErrorList");
    ValueEnforcer.notNull (eValidationMode, "ValidationMode");
    ValueEnforcer.isTrue (aErrorList.isEmpty (), () -> "Errors in global PMode validation: " + aErrorList.toString ());

    try
    {
      MetaAS4Manager.getPModeMgr ().validatePMode (aPMode);
    }
    catch (final PModeValidationException ex)
    {
      aErrorList.add (_createError (ex.getMessage ()));
    }

    final EMEP eMEP = aPMode.getMEP ();
    final EMEPBinding eMEPBinding = aPMode.getMEPBinding ();

    if ((eMEP == EMEP.ONE_WAY && eMEPBinding == EMEPBinding.PUSH) ||
      (eMEP == EMEP.TWO_WAY && eMEPBinding == EMEPBinding.PUSH_PUSH))
    {
      // Valid - ENTSOG 4.0 supports one-way/push and two-way/push-push
    }
    else
    {
      aErrorList.add (_createError ("An invalid combination of PMode MEP (" +
                                    eMEP +
                                    ") and MEP binding (" +
                                    eMEPBinding +
                                    ") was specified, valid are only one-way/push and two-way/push-push."));
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

    if (eMEP.isTwoWay ())
    {
      final PModeLeg aPModeLeg2 = aPMode.getLeg2 ();
      if (aPModeLeg2 == null)
      {
        aErrorList.add (_createError ("PMode.Leg[2] is missing as it specified as TWO-WAY"));
      }
      else
      {
        _checkIfLegIsValid (aErrorList, aPModeLeg2, "PMode.Leg[2].");
      }
    }

    // Compression - GZIP is RECOMMENDED (SHOULD) in ENTSOG 4.0, not mandatory
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
    }
  }

  @Override
  public void validateUserMessage (@NonNull final Ebms3UserMessage aUserMsg, @NonNull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aUserMsg, "UserMsg");

    if (aUserMsg.getMessageInfo () == null)
    {
      aErrorList.add (_createError ("MessageInfo is missing"));
    }
    else
    {
      if (StringHelper.isEmpty (aUserMsg.getMessageInfo ().getMessageId ()))
        aErrorList.add (_createError ("MessageInfo/MessageId is missing"));

      if (StringHelper.isNotEmpty (aUserMsg.getMessageInfo ().getRefToMessageId ()))
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
          aErrorList.add (_createError ("PartyInfo/From must contain no more than one PartyID"));
      }

      final Ebms3To aTo = aUserMsg.getPartyInfo ().getTo ();
      if (aTo != null)
      {
        if (aTo.getPartyIdCount () > 1)
          aErrorList.add (_createError ("PartyInfo/To must contain no more than one PartyID"));
      }
    }

    if (aUserMsg.getCollaborationInfo () == null)
    {
      aErrorList.add (_createError ("CollaborationInfo is missing"));
    }
    else
    {
      final Ebms3AgreementRef aAgreementRef = aUserMsg.getCollaborationInfo ().getAgreementRef ();
      if (StringHelper.isEmpty (aAgreementRef.getValue ()))
        aErrorList.add (_createError ("CollaborationInfo/AgreementRef value is missing"));
      if (aAgreementRef.getPmode () != null)
        aErrorList.add (_createError ("CollaborationInfo/PMode has not to be set!"));
    }
  }

  @Override
  public void validateSignalMessage (@NonNull final Ebms3SignalMessage aSignalMsg, @NonNull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aSignalMsg, "SignalMsg");

    if (aSignalMsg.getMessageInfo () == null)
    {
      aErrorList.add (_createError ("MessageInfo is missing"));
    }
    else
    {
      if (StringHelper.isEmpty (aSignalMsg.getMessageInfo ().getMessageId ()))
        aErrorList.add (_createError ("MessageInfo/MessageId is missing"));
    }
  }
}
