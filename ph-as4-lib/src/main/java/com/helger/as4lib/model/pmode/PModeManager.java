/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.wss.EWSSVersion;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.photon.basic.app.dao.impl.AbstractMapBasedWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.audit.AuditHelper;
import com.helger.photon.security.object.ObjectHelper;

public class PModeManager extends AbstractMapBasedWALDAO <IPMode, PMode>
{
  public PModeManager (@Nullable final String sFilename) throws DAOException
  {
    super (PMode.class, sFilename);
  }

  @Nonnull
  public IPMode createPMode (@Nonnull final PMode aPMode)
  {
    ValueEnforcer.notNull (aPMode, "PMode");

    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aPMode);
    });
    AuditHelper.onAuditCreateSuccess (PMode.OT, aPMode.getID ());

    return aPMode;
  }

  @Nonnull
  public EChange updatePMode (@Nonnull final IPMode aPMode)
  {
    ValueEnforcer.notNull (aPMode, "PMode");
    final PMode aRealPMode = getOfID (aPMode.getID ());
    if (aRealPMode == null)
    {
      AuditHelper.onAuditModifyFailure (PMode.OT, aPMode.getID (), "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      ObjectHelper.setLastModificationNow (aRealPMode);
      internalUpdateItem (aRealPMode);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (PMode.OT, "all", aRealPMode.getID ());

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange markPModeDeleted (@Nullable final String sPModeID)
  {
    final PMode aDeletedPMode = getOfID (sPModeID);
    if (aDeletedPMode == null)
    {
      AuditHelper.onAuditDeleteFailure (PMode.OT, "no-such-object-id", sPModeID);
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      if (ObjectHelper.setDeletionNow (aDeletedPMode).isUnchanged ())
      {
        AuditHelper.onAuditDeleteFailure (PMode.OT, "already-deleted", sPModeID);
        return EChange.UNCHANGED;
      }
      internalMarkItemDeleted (aDeletedPMode);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (PMode.OT, sPModeID);

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deletePMode (@Nullable final String sPModeID)
  {
    final PMode aDeletedPMode = getOfID (sPModeID);
    if (aDeletedPMode == null)
    {
      AuditHelper.onAuditDeleteFailure (PMode.OT, "no-such-object-id", sPModeID);
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      internalDeleteItem (sPModeID);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (PMode.OT, sPModeID);

    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IPMode> getAllPModes ()
  {
    return getAll ();
  }

  @Nullable
  public IPMode getPModeOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  public void validatePMode (@Nonnull final IPMode aPMode)
  {
    // TODO FIXME XXX validatestuff

    if (aPMode == null)
    {
      throw new IllegalStateException ("No PMode is null!");
    }

    // Needs ID
    if (aPMode.getID () == null)
    {
      throw new IllegalStateException ("No PModeID present");
    }

    // MEPBINDING only push maybe push and pull
    if (aPMode.getMEPBinding () == null)
    {
      throw new IllegalStateException ("No MEPBinding present. (Push, Pull, Sync)");
    }

    // MEP ONLY ONEWAY maybe twoway
    // TODO Check on specific MEP? or allow all
    if (aPMode.getMEP () == null)
    {
      throw new IllegalStateException ("No MEP present");
    }

    final PModeParty aInitiator = aPMode.getInitiator ();
    if (aInitiator != null)
    {
      // INITIATOR PARTY_ID
      if (aInitiator.getIDValue () == null)
      {
        throw new IllegalStateException ("No Initiator PartyID present");
      }

      // INITIATOR ROLE
      if (aInitiator.getRole () == null)
      {
        throw new IllegalStateException ("No Initiator Party Role present");
      }
    }

    final PModeParty aResponder = aPMode.getResponder ();
    if (aResponder != null)
    {
      // RESPONDER PARTY_ID
      if (aResponder.getIDValue () == null)
      {
        throw new IllegalStateException ("No Responder PartyID present");
      }

      // RESPONDER ROLE
      if (aResponder.getRole () == null)
      {
        throw new IllegalStateException ("No Responder Party Role present");
      }
    }

    if (aResponder == null && aInitiator == null)
    {
      throw new IllegalStateException ("There has to be atleast one of the following: Responder or Initiator present");
    }

    final PModeLeg aPModeLeg1 = aPMode.getLeg1 ();
    if (aPModeLeg1 == null)
    {
      throw new IllegalStateException ("PMode is missing Leg 1");
    }

    if (aPModeLeg1.getProtocol () == null)
    {
      throw new IllegalStateException ("PMode is missing Leg 1, Protocol is missing");
    }

    // PROTOCOL Address only http allowed
    if (aPModeLeg1.getProtocol ().getAddressProtocol () == null ||
        !aPModeLeg1.getProtocol ().getAddressProtocol ().toLowerCase ().equals ("http"))
    {
      throw new IllegalStateException ("Only the address protocol 'http' is allowed");
    }

    // SOAP VERSION = 1.2 TODO AS4 Specific if we implement SOAP 1.1 Gets
    // blocked
    // TODO also as4 specific PModeAuthorize needs to be false
    // if (aPMode.getLeg1 ().getProtocol ().getSOAPVersion () ==
    // ESOAPVersion.AS4_DEFAULT)
    // {
    // throw new IllegalStateException ("No Responder Party Role present");
    // }

    // BUSINESS INFO SERVICE

    // BUSINESS INFO ACTION

    // SEND RECEIPT TRUE/FALSE when false dont send receipts anymore
    final PModeLegSecurity aPModeLegSecurity = aPModeLeg1.getSecurity ();
    if (aPModeLegSecurity != null)
    {
      if (aPModeLegSecurity.isSendReceiptDefined ())
      {
        if (aPModeLegSecurity.isSendReceipt ())
        {
          // set response required

          if (aPModeLegSecurity.getSendReceiptReplyPattern () == null ||
              !aPModeLegSecurity.getSendReceiptReplyPattern ().toLowerCase ().equals ("response"))
          {
            throw new IllegalStateException ("Only response is allowed as pattern");
          }

          // Send NonRepudiation => Only activate able when Send Receipt true
          // and
          // only when Sign on True and Message Signed

        }
      }

      // TODO XXX Ask Philipp should it be allowed that a pmode has no
      // WSSecurity
      // Check Certificate
      if (aPModeLegSecurity.getX509SignatureCertificate () == null)
      {
        throw new IllegalStateException ("A signature certificate is required");
      }

      // Check Signature Algorithm
      if (aPModeLegSecurity.getX509SignatureAlgorithm () == null)
      {
        throw new IllegalStateException ("No signature algorithm is specified but is required");
      }
      ECryptoAlgorithmSign.getFromIDOrThrow (aPModeLegSecurity.getX509SignatureAlgorithm ());

      // Check Hash Function
      if (aPModeLegSecurity.getX509SignatureHashFunction () == null)
      {
        throw new IllegalStateException ("No hash function (Digest Algorithm) is specified but is required");
      }
      ECryptoAlgorithmSignDigest.getFromIDOrThrow (aPModeLegSecurity.getX509SignatureHashFunction ());

      // Check Encrypt algorithm
      if (aPModeLegSecurity.getX509EncryptionAlgorithm () == null)
      {
        throw new IllegalStateException ("No encryption algorithm is specified but is required");
      }
      ECryptoAlgorithmCrypt.getFromIDOrThrow (aPModeLegSecurity.getX509EncryptionAlgorithm ());

      // Check WSS Version = 1.1.1
      if (aPModeLegSecurity.getWSSVersion () != null)
      {
        // Check for WSS - Version if there is one present
        if (!aPModeLegSecurity.getWSSVersion ().equals (EWSSVersion.WSS_11.getVersion ()))
          throw new IllegalStateException ("No WSS Version is defined but required");
      }
    }

    // Error Handling
    final PModeLegErrorHandling aErrorHandling = aPModeLeg1.getErrorHandling ();
    if (aErrorHandling != null)
    {
      if (aErrorHandling.isReportAsResponseDefined ())
        if (aErrorHandling.isReportAsResponse ())
        {
          // TODO AS4 Profile says true
        }
      if (aErrorHandling.isReportProcessErrorNotifyConsumerDefined ())
        if (aErrorHandling.isReportProcessErrorNotifyConsumer ())
        {
          // TODO AS4 Profile says true
        }
      if (aErrorHandling.isReportDeliveryFailuresNotifyProducerDefined ())
        if (aErrorHandling.isReportDeliveryFailuresNotifyProducer ())
        {
          // TODO AS4 Profile says true
        }
    }
    else
    {
      // Disable Error Responses
    }

    // Compression application/gzip ONLY // other possible states are absent or
    // "" (No input)
    final PModePayloadService aPayloadService = aPMode.getPayloadService ();
    if (aPayloadService != null)
    {
      final EAS4CompressionMode aCompressionMode = aPayloadService.getCompressionMode ();
      if (aCompressionMode != null)
      {
        if (!aCompressionMode.equals (""))
        {
          if (!aCompressionMode.equals (EAS4CompressionMode.GZIP))
            throw new IllegalStateException ("Only GZIP Compression is allowed");
        }
      }
    }
    else
    {
      // TODO no compression allowed
    }
  }

  public void validateAllPModes ()
  {
    for (final IPMode aPMode : getAll ())
      validatePMode (aPMode);
  }
}
