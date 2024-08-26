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
package com.helger.phase4.test.profile;

import java.util.List;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.error.IError;
import com.helger.commons.error.SingleError;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.ebms3header.Ebms3From;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3To;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.PModePayloadService;
import com.helger.phase4.model.pmode.PModeValidationException;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegProtocol;
import com.helger.phase4.profile.IAS4ProfileValidator;

/**
 * Validate certain requirements imposed by the Test profile.
 *
 * @author Philip Helger
 * @since 2.3.0
 */
public class TestProfileCompatibilityValidator implements IAS4ProfileValidator
{
  public TestProfileCompatibilityValidator ()
  {}

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
        if (sAddressProtocol.equalsIgnoreCase ("http") || sAddressProtocol.equalsIgnoreCase ("https"))
        {
          // Always okay
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
        aErrorList.add (_createError (sFieldPrefix + "AddressProtocol is missing"));
      }

      // Support any SOAP version
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
