/**
 * Copyright (C) 2019-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.peppol;

import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.error.IError;
import com.helger.commons.error.SingleError;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.ebms3header.Ebms3From;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3Property;
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
 * Validate certain requirements imposed by the Peppol project.
 *
 * @author Philip Helger
 */
public class PeppolCompatibilityValidator implements IAS4ProfileValidator
{
  @SuppressWarnings ("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolCompatibilityValidator.class);

  public PeppolCompatibilityValidator ()
  {}

  @Nonnull
  private static IError _createError (@Nonnull final String sMsg)
  {
    return SingleError.builderError ().setErrorText (sMsg).build ();
  }

  private void _checkIfLegIsValid (@Nonnull final IPMode aPMode,
                                   @Nonnull final ErrorList aErrorList,
                                   @Nonnull final PModeLeg aPModeLeg)
  {
    final PModeLegProtocol aLegProtocol = aPModeLeg.getProtocol ();
    if (aLegProtocol == null)
    {
      aErrorList.add (_createError ("PMode Leg 1 is missing Protocol"));
    }
    else
    {
      // PROTOCOL Address only https allowed
      final String sAddressProtocol = aLegProtocol.getAddressProtocol ();
      if (sAddressProtocol != null)
        if (sAddressProtocol.equalsIgnoreCase ("https"))
        {
          // Always okay
        }
        else
          if (sAddressProtocol.equalsIgnoreCase ("http") && GlobalDebug.isDebugMode ())
          {
            // Okay in debug mode only
          }
          else
          {
            // Other protocol
            aErrorList.add (_createError ("PMode Leg1 uses a non-standard AddressProtocol: " + sAddressProtocol));
          }

      final ESoapVersion eSOAPVersion = aLegProtocol.getSoapVersion ();
      if (!eSOAPVersion.isAS4Default ())
      {
        aErrorList.add (_createError ("PMode Leg1 uses a non-standard SOAP version: " + eSOAPVersion.getVersion ()));
      }
    }

    // Only check the security features if a Security Leg is currently present
    final PModeLegSecurity aPModeLegSecurity = aPModeLeg.getSecurity ();
    if (aPModeLegSecurity != null)
    {
      // Check Certificate
      // TODO certificate is in Partner - therefore not here :)
      if (false)
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
        if (!aPModeLegSecurity.getX509SignatureAlgorithm ().equals (ECryptoAlgorithmSign.RSA_SHA_256))
        {
          aErrorList.add (_createError ("AS4 Profile only allows " +
                                        ECryptoAlgorithmSign.RSA_SHA_256.getID () +
                                        " as signing algorithm"));
        }

      // Check Hash Function
      if (aPModeLegSecurity.getX509SignatureHashFunction () == null)
      {
        aErrorList.add (_createError ("No hash function (Digest Algorithm) is specified but is required"));
      }
      else
        if (!aPModeLegSecurity.getX509SignatureHashFunction ().equals (ECryptoAlgorithmSignDigest.DIGEST_SHA_256))
        {
          aErrorList.add (_createError ("AS4 Profile only allows " +
                                        ECryptoAlgorithmSignDigest.DIGEST_SHA_256.getID () +
                                        " as hash function"));
        }

      // Check Encrypt algorithm
      if (aPModeLegSecurity.getX509EncryptionAlgorithm () == null)
      {
        aErrorList.add (_createError ("No encryption algorithm is specified but is required"));
      }
      else
        if (!aPModeLegSecurity.getX509EncryptionAlgorithm ().equals (ECryptoAlgorithmCrypt.AES_128_GCM))
        {
          aErrorList.add (_createError ("AS4 Profile only allows " +
                                        ECryptoAlgorithmCrypt.AES_128_GCM.getID () +
                                        " as encryption algorithm"));
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
    final PModeLegErrorHandling aErrorHandling = aPModeLeg.getErrorHandling ();
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
    final PModePayloadService aPayloadService = aPMode.getPayloadService ();
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

  public void validatePMode (@Nonnull final IPMode aPMode, @Nonnull final ErrorList aErrorList)
  {
    assert aErrorList.isEmpty () : "Errors in global PMode validation: " + aErrorList.toString ();

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
                                    ") was specified, only one-way/push is valid."));
    }

    // Leg1 must be present
    final PModeLeg aPModeLeg1 = aPMode.getLeg1 ();
    if (aPModeLeg1 == null)
    {
      aErrorList.add (_createError ("PMode is missing Leg 1"));
    }
    else
    {
      _checkIfLegIsValid (aPMode, aErrorList, aPModeLeg1);

      if (aPMode.getLeg2 () != null)
      {
        aErrorList.add (_createError ("PMode should never have Leg 2"));
      }
    }
  }

  public void validateUserMessage (@Nonnull final Ebms3UserMessage aUserMsg, @Nonnull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aUserMsg, "UserMsg");

    if (aUserMsg.getMessageInfo () != null)
    {
      if (StringHelper.hasNoText (aUserMsg.getMessageInfo ().getMessageId ()))
        aErrorList.add (_createError ("MessageID is missing but is mandatory!"));

      {
        // Check if originalSender and finalRecipient are present
        // Since these two properties are mandatory
        final Ebms3MessageProperties aMessageProperties = aUserMsg.getMessageProperties ();
        if (aMessageProperties == null)
          aErrorList.add (_createError ("No Message Properties present but originalSender and finalRecipient have to be present"));
        else
        {
          final List <Ebms3Property> aProps = aMessageProperties.getProperty ();
          if (aProps.isEmpty ())
            aErrorList.add (_createError ("Message Property element present but no properties"));
          else
          {
            String sOriginalSenderC1 = null;
            String sFinalRecipientC4 = null;

            for (final Ebms3Property sProperty : aProps)
            {
              if (sProperty.getName ().equals (CAS4.ORIGINAL_SENDER))
                sOriginalSenderC1 = sProperty.getValue ();
              else
                if (sProperty.getName ().equals (CAS4.FINAL_RECIPIENT))
                  sFinalRecipientC4 = sProperty.getValue ();
            }

            if (StringHelper.hasNoText (sOriginalSenderC1))
              aErrorList.add (_createError ("'" +
                                            CAS4.ORIGINAL_SENDER +
                                            "' property is empty or not existant but mandatory"));
            if (StringHelper.hasNoText (sFinalRecipientC4))
              aErrorList.add (_createError ("'" +
                                            CAS4.FINAL_RECIPIENT +
                                            "' property is empty or not existant but mandatory"));
          }
        }
      }
    }
    else
    {
      aErrorList.add (_createError ("MessageInfo is missing but is mandatory!"));
    }

    if (aUserMsg.getPartyInfo () == null)
    {
      aErrorList.add (_createError ("At least one PartyInfo element has to be present"));
    }
    else
    {
      final Ebms3To aTo = aUserMsg.getPartyInfo ().getTo ();
      if (aTo != null)
      {
        if (aTo.getPartyIdCount () > 1)
          aErrorList.add (_createError ("Only 1 PartyID is allowed in PartyInfo/To - part"));
        else
          if (aTo.getPartyIdCount () == 1)
          {
            if (!PeppolPMode.DEFAULT_PARTY_TYPE_ID.equals (aTo.getPartyIdAtIndex (0).getType ()))
            {
              aErrorList.add (_createError ("The PartyInfo/To - part needs to have the type '" +
                                            PeppolPMode.DEFAULT_PARTY_TYPE_ID +
                                            "'"));
            }
          }
      }

      final Ebms3From aFrom = aUserMsg.getPartyInfo ().getFrom ();
      if (aFrom != null)
      {
        if (aFrom.getPartyIdCount () > 1)
          aErrorList.add (_createError ("Only 1 PartyID is allowed in PartyInfo/From - part"));
        else
          if (aFrom.getPartyIdCount () == 1)
          {
            if (!PeppolPMode.DEFAULT_PARTY_TYPE_ID.equals (aFrom.getPartyIdAtIndex (0).getType ()))
            {
              aErrorList.add (_createError ("The PartyInfo/From - part needs to have the type '" +
                                            PeppolPMode.DEFAULT_PARTY_TYPE_ID +
                                            "'"));
            }
          }
      }
    }
  }

  public void validateSignalMessage (@Nonnull final Ebms3SignalMessage aSignalMsg, @Nonnull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aSignalMsg, "SignalMsg");

    if (aSignalMsg.getMessageInfo () != null)
    {
      if (StringHelper.hasNoText (aSignalMsg.getMessageInfo ().getMessageId ()))
        aErrorList.add (_createError ("MessageID is missing but is mandatory!"));
    }
  }
}
