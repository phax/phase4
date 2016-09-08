package com.helger.as4server.receive.soap;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.w3c.dom.Element;

import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.model.pmode.PModeLeg;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.wss.EWSSVersion;
import com.helger.as4server.receive.AS4MessageState;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.errorlist.IErrorBase;
import com.helger.commons.errorlist.SingleError;
import com.helger.commons.state.ESuccess;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.write.XMLWriter;

public class SOAPHeaderElementProcessorWSS4J implements ISOAPHeaderElementProcessor
{
  @Nonnull
  public ESuccess processHeaderElement (@Nonnull final Element aSecurityNode,
                                        @Nonnull final AS4MessageState aState,
                                        @Nonnull final List <? super IErrorBase <?>> aErrorList)
  {
    final Ebms3Messaging aMessaging = aState.getMessaging ();
    if (aMessaging == null)
      throw new IllegalStateException ("No Ebms3Messaging present in state");
    final PMode aPMode = aState.getPMode ();
    if (aPMode == null)
      throw new IllegalStateException ("No PMode present in state");

    // Check if pmode contains a protocol and if the message complies
    final PModeLeg aPModeLeg = aPMode.getLeg1 ();
    if (aPModeLeg == null)
    {
      aErrorList.add (SingleError.createError ("PMode is missing Leg 1"));
      return ESuccess.FAILURE;
    }
    // Protocol mostly SOAP - Version
    if (aPModeLeg.getProtocol () == null)
    {
      aErrorList.add (SingleError.createError ("PMode Leg 1 is missing protocol section"));
      return ESuccess.FAILURE;
    }

    // Check SOAP - Version
    final ESOAPVersion ePModeSoapVersion = aPModeLeg.getProtocol ().getSOAPVersion ();
    if (!aState.getSOAPVersion ().equals (ePModeSoapVersion))
    {
      aErrorList.add (SingleError.createError ("Error processing the PMode, the SOAP - Version (" +
                                               ePModeSoapVersion +
                                               ") is incorrect."));
      return ESuccess.FAILURE;
    }

    final Ebms3UserMessage aUserMessage = CollectionHelper.getAtIndex (aMessaging.getUserMessage (), 0);
    if (aUserMessage != null)
    {
      final Ebms3PartyInfo aPartyInfo = aUserMessage.getPartyInfo ();
      if (aPartyInfo != null)
      {
        if (aPMode.getInitiator () != null)
        {
          if (aPartyInfo.getFrom () != null)
          {
            if (aPartyInfo.getFrom ().getPartyId () != null)
            {
              // Check if PartyID is correct for Initiator
              final String sInitiatorID = aPMode.getInitiator ().getIDValue ();
              if (CollectionHelper.containsNone (aPartyInfo.getFrom ().getPartyId (),
                                                 aID -> aID.getValue ().equals (sInitiatorID)))
              {
                aErrorList.add (SingleError.createError ("Error processing the PMode, the Initiator/Sender PartyID is incorrect. Expected '" +
                                                         sInitiatorID +
                                                         "'"));
                return ESuccess.FAILURE;
              }
            }
          }
          else
          {
            aErrorList.add (SingleError.createError ("Error processing the usermessage, initiator partyID is not present. It is required."));
            return ESuccess.FAILURE;
          }
        }
        else
        {
          aErrorList.add (SingleError.createError ("Error processing the usermessage, no initiator is present. PMode " +
                                                   aPMode.getID () +
                                                   " requires a initiator."));
          return ESuccess.FAILURE;
        }
        if (aPMode.getResponder () != null)
        {
          if (aPartyInfo.getTo () != null)
          {
            if (aPartyInfo.getTo ().getPartyId () != null)
            {
              // Check if PartyID is correct for Responder
              final String sResponderID = aPMode.getResponder ().getIDValue ();
              if (CollectionHelper.containsNone (aPartyInfo.getTo ().getPartyId (),
                                                 aID -> aID.getValue ().equals (sResponderID)))
              {
                aErrorList.add (SingleError.createError ("Error processing the PMode, the Responder PartyID is incorrect. Expected '" +
                                                         sResponderID +
                                                         "'"));
                return ESuccess.FAILURE;
              }
            }
            else
            {
              aErrorList.add (SingleError.createError ("Error processing the usermessage, initiator partyID is not present. It is required."));
              return ESuccess.FAILURE;
            }
          }
          else
          {
            aErrorList.add (SingleError.createError ("Error processing the usermessage, no partyInfo is present. Min Occurs 1 is expected."));
            return ESuccess.FAILURE;
          }
        }
        else
        {
          aErrorList.add (SingleError.createError ("Error processing the usermessage, no responder is present. PMode " +
                                                   aPMode.getID () +
                                                   " requires a responder."));
          return ESuccess.FAILURE;
        }
      }
    }

    // Does security - legpart checks if not <code>null</code>
    if (aPModeLeg.getSecurity () != null)
    {
      // TODO delete sysout
      System.out.println (XMLWriter.getXMLString (aSecurityNode));

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
          aErrorList.add (SingleError.createError ("Error processing the Security Header, your signing algorithm '" +
                                                   sAlgorithm +
                                                   "' is incorrect. Expected one of the following '" +
                                                   Arrays.asList (ECryptoAlgorithmSign.values ()) +
                                                   "' algorithms"));
          return ESuccess.FAILURE;
        }

        // Get Signature Digest Algorithm
        aSignedNode = XMLHelper.getFirstChildElementOfName (aSignedNode, CAS4.DS_NS, "Reference");
        aSignedNode = XMLHelper.getFirstChildElementOfName (aSignedNode, CAS4.DS_NS, "DigestMethod");
        sAlgorithm = aSignedNode == null ? null : aSignedNode.getAttribute ("Algorithm");
        if (ECryptoAlgorithmSignDigest.getFromURIOrNull (sAlgorithm) == null)
        {
          aErrorList.add (SingleError.createError ("Error processing the Security Header, your signing digest algorithm is incorrect. Expected one of the following'" +
                                                   Arrays.asList (ECryptoAlgorithmSignDigest.values ()) +
                                                   "' algorithms"));
          return ESuccess.FAILURE;
        }
      }

      // Encrypted header TODO need to check BodyPayload for right or wrong
      // Algorithm
      final Element aEncryptedNode = XMLHelper.getFirstChildElementOfName (aSecurityNode, CAS4.XENC_NS, "EncryptedKey");
      if (aEncryptedNode != null)
      {
        // Encrypted checks
        System.out.println ("encrypted checks");

      }

      // Checks the WSSVersion
      if (EWSSVersion.getFromVersionOrNull (aPModeLeg.getSecurity ().getWSSVersion ()) == null)
      {
        aErrorList.add (SingleError.createError ("Error processing the PMode, the WSS - Version," +
                                                 aPModeLeg.getSecurity ().getWSSVersion () +
                                                 " is incorrect"));
        return ESuccess.FAILURE;
      }
    }

    return ESuccess.SUCCESS;
  }
}
