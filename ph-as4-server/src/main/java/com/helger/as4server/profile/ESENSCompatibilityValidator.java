package com.helger.as4server.profile;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.as4lib.model.pmode.EPModeSendReceiptReplyPattern;
import com.helger.as4lib.model.pmode.IPMode;
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

/**
 * Validate certain requirements imposed by the e-SENS project.
 *
 * @author bayerlma
 */
public class ESENSCompatibilityValidator implements IAS4ProfileValidator
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ESENSCompatibilityValidator.class);

  public ESENSCompatibilityValidator ()
  {}

  @Nonnull
  private static IError _createError (@Nonnull final String sMsg)
  {
    return SingleError.builderError ().setErrorText (sMsg).build ();
  }

  public void validatePMode (@Nonnull final IPMode aPMode, @Nonnull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aPMode, "PMode");
    assert MetaAS4Manager.getPModeMgr ().validatePMode (aPMode).isSuccess ();

    if (!aPMode.getMEP ().equals (EMEP.ONE_WAY) || aPMode.getMEP ().equals (EMEP.TWO_WAY))
    {
      aErrorList.add (_createError ("A non valid PMode MEP was specified, valid or only one-way and two-way."));
    }

    if (!aPMode.getMEPBinding ().equals (ETransportChannelBinding.PUSH) &&
        !aPMode.getMEPBinding ().equals (ETransportChannelBinding.PUSH_AND_PULL))
    {
      aErrorList.add (_createError ("A non valid PMode MEP-Binding was specified, valid or only one-way and two-way."));
    }

    final PModeLeg aPModeLeg1 = aPMode.getLeg1 ();
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
        // PROTOCOL Address only http allowed
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

      // BUSINESS INFO SERVICE

      // BUSINESS INFO ACTION

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
          if (!aPModeLegSecurity.isPModeAuthorize ())
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

            // TODO Send NonRepudiation => Only activate able when Send Receipt
            // true and only when Sign on True and Message Signed
            // Needs to interact with the implementation
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
          if (aErrorHandling.isReportProcessErrorNotifyConsumer ())
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
          if (aErrorHandling.isReportDeliveryFailuresNotifyProducer ())
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
      else
      {
        // TODO no compression allowed in the implementation
      }
    }
  }
}
