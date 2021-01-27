/**
 * Copyright (C) 2015-2021 Pavel Rotek (www.helger.com)
 * pavel[dot]rotek[at]gmail[dot]com
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

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.error.IError;
import com.helger.commons.error.SingleError;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.string.StringHelper;
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
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.PModePayloadService;
import com.helger.phase4.model.pmode.PModeValidationException;
import com.helger.phase4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.phase4.model.pmode.leg.PModeLegProtocol;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;
import com.helger.phase4.profile.IAS4ProfileValidator;
import com.helger.phase4.soap.ESoapVersion;
import com.helger.phase4.wss.EWSSVersion;

/**
 * Validate certain requirements imposed by the ENTSOG project.
 *
 * @author Pavel Rotek
 */
public class ENTSOGCompatibilityValidator implements IAS4ProfileValidator {

  public ENTSOGCompatibilityValidator() {
  }

  @Nonnull
  private static IError _createError(@Nonnull final String sMsg) {
    return SingleError.builderError().setErrorText(sMsg).build();
  }

  private void _checkIfLegIsValid(@Nonnull final IPMode aPMode,
                                  @Nonnull final ErrorList aErrorList,
                                  @Nonnull final PModeLeg aPModeLeg) {
    final PModeLegProtocol aLegProtocol = aPModeLeg.getProtocol();
    if (aLegProtocol == null) {
      aErrorList.add(_createError("PMode Leg 1 is missing Protocol"));
    } else {
      // PROTOCOL Address only https allowed
      final String sAddressProtocol = aLegProtocol.getAddressProtocol();

      // FIXME only https allowed http only for development mode!!!!
      if (sAddressProtocol == null
          || !(sAddressProtocol.equalsIgnoreCase("https") || sAddressProtocol.equalsIgnoreCase("http"))) {
        aErrorList.add(_createError("PMode Leg1 uses a non-standard AddressProtocol: " + sAddressProtocol));
      }
      final ESoapVersion eSOAPVersion = aLegProtocol.getSoapVersion();
      if (!eSOAPVersion.isAS4Default()) {
        aErrorList.add(_createError("PMode Leg1 uses a non-standard SOAP version: " + eSOAPVersion.getVersion()));
      }
    }

    // Only check the security features if a Security Leg is currently present
    final PModeLegSecurity aPModeLegSecurity = aPModeLeg.getSecurity();
    if (aPModeLegSecurity != null) {
      if (aPModeLegSecurity.getX509SignatureCertificate() == null) {
        aErrorList.add(_createError("A signature certificate is required"));
      }

      // Check Signature Algorithm
      if (aPModeLegSecurity.getX509SignatureAlgorithm() == null) {
        aErrorList.add(_createError("No signature algorithm is specified but is required"));
      } else if (!aPModeLegSecurity.getX509SignatureAlgorithm().equals(ECryptoAlgorithmSign.RSA_SHA_256)) {
        aErrorList.add(_createError("AS4 Profile only allows " + ECryptoAlgorithmSign.RSA_SHA_256.getID()
            + " as signing algorithm"));
      }

      // Check Hash Function
      if (aPModeLegSecurity.getX509SignatureHashFunction() == null) {
        aErrorList.add(_createError("No hash function (Digest Algorithm) is specified but is required"));
      } else if (!aPModeLegSecurity.getX509SignatureHashFunction().equals(ECryptoAlgorithmSignDigest.DIGEST_SHA_256)) {
        aErrorList.add(_createError("AS4 Profile only allows " + ECryptoAlgorithmSignDigest.DIGEST_SHA_256.getID()
            + " as hash function"));
      }

      // Check Encrypt algorithm
      if (aPModeLegSecurity.getX509EncryptionAlgorithm() == null) {
        aErrorList.add(_createError("No encryption algorithm is specified but is required"));
      } else if (!aPModeLegSecurity.getX509EncryptionAlgorithm().equals(ECryptoAlgorithmCrypt.AES_128_GCM)) {
        aErrorList.add(_createError("AS4 Profile only allows " + ECryptoAlgorithmCrypt.AES_128_GCM.getID()
            + " as encryption algorithm"));
      }

      // Check WSS Version = 1.1.1
      if (aPModeLegSecurity.getWSSVersion() != null) {
        // Check for WSS - Version if there is one present
        if (!aPModeLegSecurity.getWSSVersion().equals(EWSSVersion.WSS_111))
          aErrorList.add(_createError("Wrong WSS Version " + aPModeLegSecurity.getWSSVersion() + " only "
              + EWSSVersion.WSS_111 + " is allowed."));
      }

      if (aPModeLegSecurity.isUsernameTokenCreatedDefined() || aPModeLegSecurity.isUsernameTokenDigestDefined()
          || aPModeLegSecurity.isUsernameTokenNonceDefined() || aPModeLegSecurity.hasUsernameTokenPassword()
          || aPModeLegSecurity.hasUsernameTokenUsername()) {
        aErrorList.add(_createError("Username nor it's part MUST NOT be set"));
      }

      // PModeAuthorize
      if (aPModeLegSecurity.isPModeAuthorizeDefined()) {
        if (aPModeLegSecurity.isPModeAuthorize()) {
          aErrorList.add(_createError("PMode Authorize has to be set to false"));
        }
      } else {
        aErrorList.add(_createError("PMode Authorize is a mandatory parameter"));
      }

      // SEND RECEIPT TRUE/FALSE when false dont send receipts anymore
      if (aPModeLegSecurity.isSendReceiptDefined()) {
        if (aPModeLegSecurity.isSendReceipt()) {
          // set response required

          if (!aPModeLegSecurity.isSendReceiptNonRepudiation()) {
            aErrorList.add(_createError("Receipt non repudation has to be true"));
          }

          if (aPModeLegSecurity.getSendReceiptReplyPattern() != EPModeSendReceiptReplyPattern.RESPONSE) {
            aErrorList.add(_createError("Only response is allowed as pattern"));
          }
        }
      }

    }

    // Error Handling
    final PModeLegErrorHandling aErrorHandling = aPModeLeg.getErrorHandling();
    if (aErrorHandling != null) {
      if (aErrorHandling.isReportAsResponseDefined()) {
        if (!aErrorHandling.isReportAsResponse()) {
          aErrorList.add(_createError("PMode ReportAsResponse has to be True"));
        }
      } else {
        aErrorList.add(_createError("ReportAsResponse is a mandatory PMode parameter"));
      }
      if (aErrorHandling.isReportProcessErrorNotifyConsumerDefined()) {
        if (!aErrorHandling.isReportProcessErrorNotifyConsumer()) {
          aErrorList.add(_createError("PMode ReportProcessErrorNotifyConsumer has to be True"));
        }
      } else {
        aErrorList.add(_createError("ReportProcessErrorNotifyConsumer is a mandatory PMode parameter"));
      }
      if (aErrorHandling.isReportDeliveryFailuresNotifyProducerDefined()) {
        if (!aErrorHandling.isReportDeliveryFailuresNotifyProducer()) {
          aErrorList.add(_createError("PMode ReportDeliveryFailuresNotifyProducer has to be True"));
        }
      } else {
        aErrorList.add(_createError("ReportDeliveryFailuresNotifyProducer is a mandatory PMode parameter"));
      }

      if (aErrorHandling.getReportSenderErrorsTo()!=null && aErrorHandling.getReportSenderErrorsTo().addresses()!=null && aErrorHandling.getReportSenderErrorsTo().addresses().isNotEmpty()) {
        aErrorList.add(_createError("ReportSenderErrorsTo has not to be set"));
      }

    } else {
      aErrorList.add(_createError("No ErrorHandling Parameter present but they are mandatory"));
    }

  }

  public void validatePMode(@Nonnull final IPMode aPMode, @Nonnull final ErrorList aErrorList) {
    assert aErrorList.isEmpty() : "Errors in global PMode validation: " + aErrorList.toString();

    try {
      MetaAS4Manager.getPModeMgr().validatePMode(aPMode);
    } catch (final PModeValidationException ex) {
      aErrorList.add(_createError(ex.getMessage()));
    }

    final EMEP eMEP = aPMode.getMEP();
    final EMEPBinding eMEPBinding = aPMode.getMEPBinding();

    if ((eMEP == EMEP.ONE_WAY && eMEPBinding == EMEPBinding.PUSH)
        || (eMEP == EMEP.TWO_WAY && eMEPBinding == EMEPBinding.PUSH_PUSH)) {
      // Valid
    } else {
      aErrorList.add(_createError("An invalid combination of PMode MEP (" + eMEP + ") and MEP binding (" + eMEPBinding
          + ") was specified, valid are only one-way/push and two-way/push-push."));
    }

    // Leg1 must be present
    final PModeLeg aPModeLeg1 = aPMode.getLeg1();
    if (aPModeLeg1 == null) {
      aErrorList.add(_createError("PMode is missing Leg 1"));
    } else {
      _checkIfLegIsValid(aPMode, aErrorList, aPModeLeg1);

      if (eMEP.isTwoWay()) {
        final PModeLeg aPModeLeg2 = aPMode.getLeg2();
        if (aPModeLeg2 == null) {
          aErrorList.add(_createError("PMode is missing Leg 2 and is specified as TWO-WAY"));
        } else {
          _checkIfLegIsValid(aPMode, aErrorList, aPModeLeg2);
        }
      }
    }

    final PModeLeg aPModeLeg2 = aPMode.getLeg2();
    if (aPModeLeg2 != null) {
      aErrorList.add(_createError("PMode can contain only leg 1"));
    }

    // OK
    final PModePayloadService aPayloadService = aPMode.getPayloadService();
    if (aPayloadService != null) {
      final EAS4CompressionMode eCompressionMode = aPayloadService.getCompressionMode();
      if (eCompressionMode != null) {
        if (!eCompressionMode.equals(EAS4CompressionMode.GZIP))
          aErrorList.add(_createError("Only GZIP Compression is allowed"));
      } else {
        aErrorList.add(_createError("GZIP Compression is invalid"));
      }
    } else {
      aErrorList.add(_createError("Payload service is required"));
    }

  }

  public void validateUserMessage(@Nonnull final Ebms3UserMessage aUserMsg, @Nonnull final ErrorList aErrorList) {
    ValueEnforcer.notNull(aUserMsg, "UserMsg");

    if (aUserMsg.getCollaborationInfo() != null) {
      Ebms3AgreementRef agreementRef = aUserMsg.getCollaborationInfo().getAgreementRef();
      if (StringHelper.hasNoText(agreementRef.getValue())) {
        aErrorList.add(_createError("AgreementRef value is missing but is mandatory!"));
      }
      if (agreementRef.getPmode() != null) {
        aErrorList.add(_createError("PMode has not to be set!"));
      }
    } else {
      aErrorList.add(_createError("No CollaborationInfo found!"));
    }

    if (aUserMsg.getMessageInfo() != null) {
      if (StringHelper.hasNoText(aUserMsg.getMessageInfo().getMessageId()))
        aErrorList.add(_createError("MessageID is missing but is mandatory!"));

      if (StringHelper.hasText(aUserMsg.getMessageInfo().getRefToMessageId()))
        aErrorList.add(_createError("RefToMessageID MUST NOT be present!"));

    } else {
      aErrorList.add(_createError("MessageInfo is missing but is mandatory!"));
    }

    if (aUserMsg.getPartyInfo() == null) {
      aErrorList.add(_createError("At least one PartyInfo element has to be present"));
    } else {
      final Ebms3To aTo = aUserMsg.getPartyInfo().getTo();
      if (aTo != null) {
        if (aTo.getPartyIdCount() > 1)
          aErrorList.add(_createError("Only 1 PartyID is allowed in PartyInfo/To - part"));
      }

      final Ebms3From aFrom = aUserMsg.getPartyInfo().getFrom();
      if (aFrom != null) {
        if (aFrom.getPartyIdCount() > 1)
          aErrorList.add(_createError("Only 1 PartyID is allowed in PartyInfo/From - part"));
      }
    }
  }

  public void validateSignalMessage(@Nonnull final Ebms3SignalMessage aSignalMsg, @Nonnull final ErrorList aErrorList) {
    ValueEnforcer.notNull(aSignalMsg, "SignalMsg");

    if (aSignalMsg.getMessageInfo() != null) {
      if (StringHelper.hasNoText(aSignalMsg.getMessageInfo().getMessageId()))
        aErrorList.add(_createError("MessageID is missing but is mandatory!"));
    }
  }
}
