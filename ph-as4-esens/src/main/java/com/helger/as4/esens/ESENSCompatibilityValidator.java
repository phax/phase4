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
package com.helger.as4.esens;

import static org.junit.Assert.assertTrue;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.as4lib.model.pmode.EPModeSendReceiptReplyPattern;
import com.helger.as4lib.model.pmode.IPModeConfig;
import com.helger.as4lib.model.pmode.PModeLeg;
import com.helger.as4lib.model.pmode.PModeLegErrorHandling;
import com.helger.as4lib.model.pmode.PModeLegProtocol;
import com.helger.as4lib.model.pmode.PModeLegSecurity;
import com.helger.as4lib.model.pmode.PModePayloadService;
import com.helger.as4lib.model.profile.IAS4ProfileValidator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.wss.EWSSVersion;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.error.IError;
import com.helger.commons.error.SingleError;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.string.StringHelper;

/**
 * Validate certain requirements imposed by the e-SENS project.
 *
 * @author bayerlma
 */
final class ESENSCompatibilityValidator implements IAS4ProfileValidator
{
  @SuppressWarnings ("unused")
  private static final Logger s_aLogger = LoggerFactory.getLogger (ESENSCompatibilityValidator.class);

  public ESENSCompatibilityValidator ()
  {}

  @Nonnull
  private static IError _createError (@Nonnull final String sMsg)
  {
    return SingleError.builderError ().setErrorText (sMsg).build ();
  }

  public void validatePModeConfig (@Nonnull final IPModeConfig aPModeConfig, @Nonnull final ErrorList aErrorList)
  {

    MetaAS4Manager.getPModeConfigMgr ().validatePModeConfig (aPModeConfig, aErrorList);
    assertTrue (aErrorList.isEmpty ());

    if (!aPModeConfig.getMEP ().equals (EMEP.ONE_WAY) || aPModeConfig.getMEP ().equals (EMEP.TWO_WAY))
    {
      aErrorList.add (_createError ("A non valid PMode MEP was specified, valid or only one-way and two-way."));
    }

    if (!aPModeConfig.getMEPBinding ().equals (ETransportChannelBinding.PUSH) &&
        !aPModeConfig.getMEPBinding ().equals (ETransportChannelBinding.PUSH_AND_PULL))
    {
      aErrorList.add (_createError ("A non valid PMode MEP-Binding was specified, valid or only one-way and two-way."));
    }

    final PModeLeg aPModeLeg1 = aPModeConfig.getLeg1 ();
    if (aPModeLeg1 == null)
    {
      aErrorList.add (_createError ("PMode is missing Leg 1"));
    }
    else
    {
      final PModeLegProtocol aLeg1Protocol = aPModeLeg1.getProtocol ();
      if (aLeg1Protocol == null)
      {
        aErrorList.add (_createError ("PMode Leg 1 is missing Protocol"));
      }
      else
      {
        // PROTOCOL Address only https allowed
        final String sAddressProtocol = aLeg1Protocol.getAddressProtocol ();
        if (sAddressProtocol == null)
        {
          aErrorList.add (_createError ("PMode Leg 1 is missing AddressProtocol"));
        }
        else
          if (!sAddressProtocol.equalsIgnoreCase ("https"))
          {
            // Non https?
            aErrorList.add (_createError ("PMode Leg1 uses a non-standard AddressProtocol: " + sAddressProtocol));
          }

        final ESOAPVersion eSOAPVersion = aLeg1Protocol.getSOAPVersion ();
        if (eSOAPVersion == null)
        {
          aErrorList.add (_createError ("PMode Leg 1 is missing SOAPVersion"));
        }
        else
          if (!eSOAPVersion.isAS4Default ())
          {
            aErrorList.add (_createError ("PMode Leg1 uses a non-standard SOAP version: " +
                                          eSOAPVersion.getVersion ()));
          }
      }

      // Only check the security features if a Security Leg is currently present
      final PModeLegSecurity aPModeLegSecurity = aPModeLeg1.getSecurity ();
      if (aPModeLegSecurity != null)
      {

        // Check Certificate
        if (aPModeLegSecurity.getX509SignatureCertificate () == null)
        {
          aErrorList.add (_createError ("A signature certificate is required"));
        }

        // Check Signature Algorithm
        if (aPModeLegSecurity.getX509SignatureAlgorithm () == null)
        {
          aErrorList.add (_createError ("No signature algorithm is specified but is required"));
        }
        else
          if (!aPModeLegSecurity.getX509SignatureAlgorithm ().equals (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT))
          {
            aErrorList.add (_createError ("AS4 Profile only allows " +
                                          ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT.getID () +
                                          "as signing algorithm"));
          }

        // Check Hash Function
        if (aPModeLegSecurity.getX509SignatureHashFunction () == null)
        {
          aErrorList.add (_createError ("No hash function (Digest Algorithm) is specified but is required"));
        }
        else
          if (!aPModeLegSecurity.getX509SignatureHashFunction ()
                                .equals (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT))
          {
            aErrorList.add (_createError ("AS4 Profile only allows " +
                                          ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT.getID () +
                                          "as hash function"));
          }

        // Check Encrypt algorithm
        if (aPModeLegSecurity.getX509EncryptionAlgorithm () == null)
        {
          aErrorList.add (_createError ("No encryption algorithm is specified but is required"));
        }
        else
          if (!aPModeLegSecurity.getX509EncryptionAlgorithm ()
                                .equals (ECryptoAlgorithmCrypt.ENCRPYTION_ALGORITHM_DEFAULT))
          {
            aErrorList.add (_createError ("AS4 Profile only allows " +
                                          ECryptoAlgorithmCrypt.ENCRPYTION_ALGORITHM_DEFAULT.getID () +
                                          "as encryption algorithm"));
          }

        // Check WSS Version = 1.1.1
        if (aPModeLegSecurity.getWSSVersion () != null)
        {
          // Check for WSS - Version if there is one present
          if (!aPModeLegSecurity.getWSSVersion ().equals (EWSSVersion.WSS_111))
            aErrorList.add (_createError ("Wrong WSS Version " +
                                          aPModeLegSecurity.getWSSVersion () +
                                          " only " +
                                          EWSSVersion.WSS_111 +
                                          " is allowed."));
        }

        // PModeAuthorize
        if (aPModeLegSecurity.isPModeAuthorizeDefined ())
        {
          if (aPModeLegSecurity.isPModeAuthorize ())
          {
            aErrorList.add (_createError ("PMode Authorize has to be set to false"));
          }
        }
        else
        {
          aErrorList.add (_createError ("PMode Authorize is a mandatory parameter"));
        }

        // SEND RECEIPT TRUE/FALSE when false dont send receipts anymore
        if (aPModeLegSecurity.isSendReceiptDefined ())
        {
          if (aPModeLegSecurity.isSendReceipt ())
          {
            // set response required

            if (aPModeLegSecurity.getSendReceiptReplyPattern () != EPModeSendReceiptReplyPattern.RESPONSE)
            {
              aErrorList.add (_createError ("Only response is allowed as pattern"));
            }
          }
        }
      }

      // Error Handling
      final PModeLegErrorHandling aErrorHandling = aPModeLeg1.getErrorHandling ();
      if (aErrorHandling != null)
      {
        if (aErrorHandling.isReportAsResponseDefined ())
        {
          if (!aErrorHandling.isReportAsResponse ())
          {
            aErrorList.add (_createError ("PMode ReportAsResponse has to be True"));
          }
        }
        else
        {
          aErrorList.add (_createError ("ReportAsResponse is a mandatory PMode parameter"));
        }
        if (aErrorHandling.isReportProcessErrorNotifyConsumerDefined ())
        {
          if (!aErrorHandling.isReportProcessErrorNotifyConsumer ())
          {
            aErrorList.add (_createError ("PMode ReportProcessErrorNotifyConsumer has to be True"));
          }
        }
        else
        {
          aErrorList.add (_createError ("ReportProcessErrorNotifyConsumer is a mandatory PMode parameter"));
        }
        if (aErrorHandling.isReportDeliveryFailuresNotifyProducerDefined ())
        {
          if (!aErrorHandling.isReportDeliveryFailuresNotifyProducer ())
          {
            aErrorList.add (_createError ("PMode ReportDeliveryFailuresNotifyProducer has to be True"));
          }
        }
        else
        {
          aErrorList.add (_createError ("ReportDeliveryFailuresNotifyProducer is a mandatory PMode parameter"));
        }
      }
      else
      {
        aErrorList.add (_createError ("No ErrorHandling Parameter present but they are mandatory"));
      }

      // Compression application/gzip ONLY
      // other possible states are absent or "" (No input)
      final PModePayloadService aPayloadService = aPModeConfig.getPayloadService ();
      if (aPayloadService != null)
      {
        final EAS4CompressionMode eCompressionMode = aPayloadService.getCompressionMode ();
        if (eCompressionMode != null)
        {
          if (!eCompressionMode.equals (EAS4CompressionMode.GZIP))
            aErrorList.add (_createError ("Only GZIP Compression is allowed"));
        }
      }
    }
  }

  public void validateUserMessage (@Nonnull final Ebms3UserMessage aUserMsg, @Nonnull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aUserMsg, "UserMsg");

    if (aUserMsg.getMessageInfo () != null)
    {

      if (StringHelper.hasNoText (aUserMsg.getMessageInfo ().getMessageId ()))
      {
        aErrorList.add (_createError ("MessageID is missing but is mandatory!"));
      }
    }
    else
    {
      aErrorList.add (_createError ("MessageInfo is missing but is mandatory!"));
    }

    if (aUserMsg.getPartyInfo () != null)
    {
      if (aUserMsg.getPartyInfo ().getTo () != null)
      {
        if (aUserMsg.getPartyInfo ().getTo ().getPartyIdCount () > 1)
        {
          aErrorList.add (_createError ("Only 1 PartyID is allowed in PartyTo - part"));
        }
      }
      if (aUserMsg.getPartyInfo ().getFrom () != null)
      {
        if (aUserMsg.getPartyInfo ().getFrom ().getPartyIdCount () > 1)
        {
          aErrorList.add (_createError ("Only 1 PartyID is allowed in PartyFrom - part"));
        }
      }
    }
    else
    {
      aErrorList.add (_createError ("At least one PartyInfo element has to be present"));
    }
  }

  public void validateSignalMessage (@Nonnull final Ebms3SignalMessage aSignalMsg, @Nonnull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aSignalMsg, "SignalMsg");

    if (StringHelper.hasNoText (aSignalMsg.getMessageInfo ().getMessageId ()))
    {
      aErrorList.add (_createError ("MessageID is missing but is mandatory!"));
    }
  }
}
