/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.cef;

import java.util.List;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
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
 * Validate certain requirements imposed by the CEF profile.
 *
 * @author bayerlma
 * @author Philip Helger
 */
public class CEFCompatibilityValidator implements IAS4ProfileValidator
{
  public static final boolean DEFAULT_EXPECT_FOUR_CORNER_MODEL = true;

  private boolean m_bExpectFourCornerModel = DEFAULT_EXPECT_FOUR_CORNER_MODEL;

  public CEFCompatibilityValidator ()
  {}

  public final boolean isExpectFourCornerModel ()
  {
    return m_bExpectFourCornerModel;
  }

  @Nonnull
  public final CEFCompatibilityValidator setExpectFourCornerModel (final boolean b)
  {
    m_bExpectFourCornerModel = b;
    return this;
  }

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
          if (sAddressProtocol.equalsIgnoreCase ("http") && GlobalDebug.isDebugMode ())
          {
            // Okay in debug mode only
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
      // certificate is in Partner/SMP - therefore not here :)
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
        if (!aPModeLegSecurity.getX509SignatureAlgorithm ().equals (ECryptoAlgorithmSign.RSA_SHA_256))
        {
          aErrorList.add (_createError (sFieldPrefix +
                                        "Security.X509SignatureAlgorithm must use the value '" +
                                        ECryptoAlgorithmSign.RSA_SHA_256.getID () +
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
        // Check for WSS - Version if there is one present
        if (!aPModeLegSecurity.getWSSVersion ().equals (EWSSVersion.WSS_111))
          aErrorList.add (_createError (sFieldPrefix +
                                        "Security.WSSVersion must use the value " +
                                        EWSSVersion.WSS_111 +
                                        " instead of " +
                                        aPModeLegSecurity.getWSSVersion ()));
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
          // set response required
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
      // Valid
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

    // Compression application/gzip ONLY
    // other possible states are absent or "" (No input)
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

      if (m_bExpectFourCornerModel)
      {
        // Check if originalSender and finalRecipient are present
        // Since these two properties are mandatory
        final Ebms3MessageProperties aMessageProperties = aUserMsg.getMessageProperties ();
        if (aMessageProperties == null)
          aErrorList.add (_createError ("MessageProperties is missing but 'originalSender' and 'finalRecipient' properties are required"));
        else
        {
          final List <Ebms3Property> aProps = aMessageProperties.getProperty ();
          if (aProps.isEmpty ())
            aErrorList.add (_createError ("MessageProperties/Property must not be empty"));
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
              aErrorList.add (_createError ("MessageProperties/Property '" +
                                            CAS4.ORIGINAL_SENDER +
                                            "' property is empty or not existant but mandatory"));
            if (StringHelper.hasNoText (sFinalRecipientC4))
              aErrorList.add (_createError ("MessageProperties/Property '" +
                                            CAS4.FINAL_RECIPIENT +
                                            "' property is empty or not existant but mandatory"));
          }
        }
      }
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
