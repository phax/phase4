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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.soap.ESOAPVersion;
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
  private static final Logger s_aLogger = LoggerFactory.getLogger (PModeManager.class);

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

  public void validatePMode (@Nullable final IPMode aPMode)
  {
    // TODO FIXME XXX validatestuff

    if (aPMode == null)
    {
      throw new IllegalStateException ("PMode is null!");
    }

    // Needs ID
    if (aPMode.getID () == null)
    {
      throw new IllegalStateException ("No PMode ID present");
    }

    // MEPBINDING only push maybe push and pull
    if (aPMode.getMEPBinding () == null)
    {
      throw new IllegalStateException ("No PMode MEPBinding present. (Push, Pull, Sync)");
    }

    // MEP ONLY ONEWAY maybe twoway
    // TODO Check on specific MEP? or allow all
    if (aPMode.getMEP () == null)
    {
      throw new IllegalStateException ("No PMode MEP present");
    }

    final PModeParty aInitiator = aPMode.getInitiator ();
    if (aInitiator != null)
    {
      // INITIATOR PARTY_ID
      if (aInitiator.getIDValue () == null)
      {
        throw new IllegalStateException ("No PMode Initiator ID present");
      }

      // INITIATOR ROLE
      if (aInitiator.getRole () == null)
      {
        throw new IllegalStateException ("No PMode Initiator Role present");
      }
    }

    final PModeParty aResponder = aPMode.getResponder ();
    if (aResponder != null)
    {
      // RESPONDER PARTY_ID
      if (aResponder.getIDValue () == null)
      {
        throw new IllegalStateException ("No PMode Responder ID present");
      }

      // RESPONDER ROLE
      if (aResponder.getRole () == null)
      {
        throw new IllegalStateException ("No PMode Responder Role present");
      }
    }

    if (aResponder == null && aInitiator == null)
    {
      throw new IllegalStateException ("PMode is missing Initiator and/or Responder");
    }

    final PModeLeg aPModeLeg1 = aPMode.getLeg1 ();
    if (aPModeLeg1 == null)
    {
      throw new IllegalStateException ("PMode is missing Leg 1");
    }

    final PModeLegProtocol aLeg1Protocol = aPModeLeg1.getProtocol ();
    if (aLeg1Protocol == null)
    {
      throw new IllegalStateException ("PMode Leg 1 is missing Protocol");
    }

    // PROTOCOL Address only http allowed
    final String sAddressProtocol = aLeg1Protocol.getAddressProtocol ();
    if (sAddressProtocol == null)
    {
      throw new IllegalStateException ("PMode Leg 1 is missing AddressProtocol");
    }
    // Non https?
    if (!sAddressProtocol.equalsIgnoreCase ("https"))
    {
      s_aLogger.warn ("PMode Leg1 uses a non-standard AddressProtocol: " + sAddressProtocol);
    }

    // By default AS4 only allows SOAP 1.2 - since we're flexible, just emit a
    // warning
    final ESOAPVersion eSOAPVersion = aLeg1Protocol.getSOAPVersion ();
    if (eSOAPVersion == null)
    {
      throw new IllegalStateException ("PMode Leg 1 is missing SOAPVersion");
    }
    if (!eSOAPVersion.isAS4Default ())
    {
      s_aLogger.warn ("PMode Leg1 uses a non-standard SOAP version: " + eSOAPVersion.getVersion ());
    }

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

          if (aPModeLegSecurity.getSendReceiptReplyPattern () != EPModeSendReceiptReplyPattern.RESPONSE)
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

      // Check Hash Function
      if (aPModeLegSecurity.getX509SignatureHashFunction () == null)
      {
        throw new IllegalStateException ("No hash function (Digest Algorithm) is specified but is required");
      }

      // Check Encrypt algorithm
      if (aPModeLegSecurity.getX509EncryptionAlgorithm () == null)
      {
        throw new IllegalStateException ("No encryption algorithm is specified but is required");
      }

      // Check WSS Version = 1.1.1
      if (aPModeLegSecurity.getWSSVersion () != null)
      {
        // Check for WSS - Version if there is one present
        if (!aPModeLegSecurity.getWSSVersion ().equals (EWSSVersion.WSS_11))
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
