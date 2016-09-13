package com.helger.as4server.receive.soap;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.crypto.AS4CryptoFactory;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.model.pmode.IPMode;
import com.helger.as4lib.model.pmode.PModeLeg;
import com.helger.as4lib.model.pmode.PModeLegProtocol;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.wss.EWSSVersion;
import com.helger.as4server.receive.AS4MessageState;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.error.SingleError;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.state.ESuccess;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.write.XMLWriter;

public class SOAPHeaderElementProcessorWSS4J implements ISOAPHeaderElementProcessor
{
  private static final Logger LOG = LoggerFactory.getLogger (SOAPHeaderElementProcessorWSS4J.class);

  @Nonnull
  public ESuccess processHeaderElement (@Nonnull final Document aSOAPDoc,
                                        @Nonnull final Element aSecurityNode,
                                        @Nonnull final AS4MessageState aState,
                                        @Nonnull final ErrorList aErrorList)
  {
    final Ebms3Messaging aMessaging = aState.getMessaging ();
    if (aMessaging == null)
      throw new IllegalStateException ("No Ebms3Messaging present in state");
    final IPMode aPMode = aState.getPMode ();
    if (aPMode == null)
      throw new IllegalStateException ("No PMode present in state");

    if (aPMode.getMEP () == null || aPMode.getMEPBinding () == null)
      throw new IllegalStateException ("PMode is incomplete: " + aPMode);

    // Check if pmode contains a protocol and if the message complies
    final PModeLeg aPModeLeg1 = aPMode.getLeg1 ();
    if (aPModeLeg1 == null)
    {
      aErrorList.add (SingleError.builderError ().setErrorText ("PMode is missing Leg 1").build ());
      return ESuccess.FAILURE;
    }

    // Check protocol
    {
      final PModeLegProtocol aProtocol = aPModeLeg1.getProtocol ();
      if (aProtocol == null || !"http".equals (aProtocol.getAddressProtocol ()))
      {
        aErrorList.add (SingleError.builderError ()
                                   .setErrorText ("PMode Leg uses unsupported protocol '" +
                                                  aProtocol.getAddressProtocol () +
                                                  "'")
                                   .build ());
        return ESuccess.FAILURE;
      }

      // Check SOAP - Version
      final ESOAPVersion ePModeSoapVersion = aProtocol.getSOAPVersion ();
      if (!aState.getSOAPVersion ().equals (ePModeSoapVersion))
      {
        aErrorList.add (SingleError.builderError ()
                                   .setErrorText ("Error processing the PMode, the SOAP Version (" +
                                                  ePModeSoapVersion +
                                                  ") is incorrect.")
                                   .build ());
        return ESuccess.FAILURE;
      }
    }

    final Ebms3UserMessage aUserMessage = CollectionHelper.getAtIndex (aMessaging.getUserMessage (), 0);
    if (aUserMessage != null)
    {
      final Ebms3PartyInfo aPartyInfo = aUserMessage.getPartyInfo ();
      if (aPartyInfo != null)
      {
        // Initiator is optional for push
        if (aPMode.getInitiator () == null)
        {
          if (aPMode.getMEPBinding ().isPull ())
          {
            aErrorList.add (SingleError.builderError ()
                                       .setErrorText ("Initiator is required for PULL message")
                                       .build ());
            return ESuccess.FAILURE;
          }
        }
        else
        {
          if (aPartyInfo.getFrom () != null && aPartyInfo.getFrom ().getPartyId () != null)
          {
            // Check if PartyID is correct for Initiator
            final String sInitiatorID = aPMode.getInitiator ().getIDValue ();
            if (CollectionHelper.containsNone (aPartyInfo.getFrom ().getPartyId (),
                                               aID -> aID.getValue ().equals (sInitiatorID)))
            {
              aErrorList.add (SingleError.builderError ()
                                         .setErrorText ("Error processing the PMode, the Initiator/Sender PartyID is incorrect. Expected '" +
                                                        sInitiatorID +
                                                        "'")
                                         .build ());
              return ESuccess.FAILURE;
            }
          }
          else
          {
            aErrorList.add (SingleError.builderError ()
                                       .setErrorText ("Error processing the usermessage, initiator part is present. But from PartyInfo is invalid.")
                                       .build ());
            return ESuccess.FAILURE;
          }
        }

        // Response is optional for pull
        if (aPMode.getResponder () == null)
        {
          if (aPMode.getMEPBinding ().isPush ())
          {
            aErrorList.add (SingleError.builderError ()
                                       .setErrorText ("Responder is required for PUSH message")
                                       .build ());
            return ESuccess.FAILURE;
          }
        }
        else
        {
          if (aPartyInfo.getTo () != null && aPartyInfo.getTo ().getPartyId () != null)
          {
            // Check if PartyID is correct for Responder
            final String sResponderID = aPMode.getResponder ().getIDValue ();
            if (CollectionHelper.containsNone (aPartyInfo.getTo ().getPartyId (),
                                               aID -> aID.getValue ().equals (sResponderID)))
            {
              aErrorList.add (SingleError.builderError ()
                                         .setErrorText ("Error processing the PMode, the Responder PartyID is incorrect. Expected '" +
                                                        sResponderID +
                                                        "'")
                                         .build ());
              return ESuccess.FAILURE;
            }
          }
          else
          {
            aErrorList.add (SingleError.builderError ()
                                       .setErrorText ("Error processing the usermessage, to-PartyInfo is invalid.")
                                       .build ());
            return ESuccess.FAILURE;
          }
        }
      }
    }

    // Does security - legpart checks if not <code>null</code>
    if (aPModeLeg1.getSecurity () != null)
    {
      // TODO delete sysout
      LOG.info (XMLWriter.getXMLString (aSecurityNode));

      // Get Signature Algorithm
      Element aSignedNode = XMLHelper.getFirstChildElementOfName (aSecurityNode, CAS4.DS_NS, "Signature");
      if (aSignedNode != null)
      {
        aSignedNode = XMLHelper.getFirstChildElementOfName (aSignedNode, CAS4.DS_NS, "SignedInfo");
        final Element aSignatureAlgorithm = XMLHelper.getFirstChildElementOfName (aSignedNode,
                                                                                  CAS4.DS_NS,
                                                                                  "SignatureMethod");
        String sAlgorithm = aSignatureAlgorithm == null ? null : aSignatureAlgorithm.getAttribute ("Algorithm");
        if (ECryptoAlgorithmSign.getFromURIOrNull (sAlgorithm) == null)
        {
          aErrorList.add (SingleError.builderError ()
                                     .setErrorText ("Error processing the Security Header, your signing algorithm '" +
                                                    sAlgorithm +
                                                    "' is incorrect. Expected one of the following '" +
                                                    Arrays.asList (ECryptoAlgorithmSign.values ()) +
                                                    "' algorithms")
                                     .build ());
          return ESuccess.FAILURE;
        }

        // Get Signature Digest Algorithm
        aSignedNode = XMLHelper.getFirstChildElementOfName (aSignedNode, CAS4.DS_NS, "Reference");
        aSignedNode = XMLHelper.getFirstChildElementOfName (aSignedNode, CAS4.DS_NS, "DigestMethod");
        sAlgorithm = aSignedNode == null ? null : aSignedNode.getAttribute ("Algorithm");
        if (ECryptoAlgorithmSignDigest.getFromURIOrNull (sAlgorithm) == null)
        {
          aErrorList.add (SingleError.builderError ()
                                     .setErrorText ("Error processing the Security Header, your signing digest algorithm is incorrect. Expected one of the following'" +
                                                    Arrays.asList (ECryptoAlgorithmSignDigest.values ()) +
                                                    "' algorithms")
                                     .build ());
          return ESuccess.FAILURE;
        }
      }

      // Encrypted header TODO need to check BodyPayload for right or wrong
      // Algorithm
      final Element aEncryptedNode = XMLHelper.getFirstChildElementOfName (aSecurityNode, CAS4.XENC_NS, "EncryptedKey");
      if (aEncryptedNode != null)
      {
        // Encrypted checks
        LOG.info ("encrypted checks");

      }

      // Checks the WSSVersion
      if (EWSSVersion.getFromVersionOrNull (aPModeLeg1.getSecurity ().getWSSVersion ()) == null)
      {
        aErrorList.add (SingleError.builderError ()
                                   .setErrorText ("Error processing the PMode, the WSS - Version," +
                                                  aPModeLeg1.getSecurity ().getWSSVersion () +
                                                  " is incorrect")
                                   .build ());
        return ESuccess.FAILURE;
      }

      // De - Signing and Decryption
      final WSSecurityEngine aSecurityEngine = new WSSecurityEngine ();

      List <WSSecurityEngineResult> aResults = null;

      try
      {

        final KeyStoreCallbackHandler aKeyStoreCallback = new KeyStoreCallbackHandler ();
        // Upon success, the SOAP document contains the decrypted content
        // afterwards!
        aResults = aSecurityEngine.processSecurityHeader (aSOAPDoc,
                                                          null,
                                                          aKeyStoreCallback,
                                                          AS4CryptoFactory.createCrypto ())
                                  .getResults ();
        // TODO maybe not needed since you cant check Digest algorithm OR
        // encrypt algorithm
        aResults.forEach (x -> x.forEach ( (k, v) -> LOG.info (k + "=" + v)));

        aState.setDecryptedSOAPDocument (aSOAPDoc);

        // TODO save DOc somewhere? or what should happen with it
        // aSecurityEngine.processSecurityHeader (aSOAPDoc, null,
        // aKeyStoreCallback, AS4CryptoFactory.createCrypto ());
        // System.out.println ("Decryption Result ");
        // System.out.println (XMLUtils.prettyDocumentToString (aSOAPDoc));
      }
      catch (final Exception e)
      {
        // Decryption failed
        aErrorList.add (SingleError.builderError ()
                                   .setErrorText ("Error processing the WSSSecurity Header: " +
                                                  e.getLocalizedMessage ())
                                   .setLinkedException (e)
                                   .build ());
        return ESuccess.FAILURE;
      }
    }
    return ESuccess.SUCCESS;
  }
}
