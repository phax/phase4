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
package com.helger.phase4.model.pmode;

import java.time.LocalDateTime;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegJsonConverter;
import com.helger.photon.security.object.StubObject;
import com.helger.tenancy.IBusinessObject;

/**
 * JSON converter for objects of class {@link PMode}.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class PModeJsonConverter
{
  private static final String ATTR_ID = "id";
  private static final String ATTR_CREATIONLDT = "creationldt";
  private static final String ATTR_CREATIONUSERID = "creationuserid";
  private static final String ATTR_LASTMODLDT = "lastmodldt";
  private static final String ATTR_LASTMODUSERID = "lastmoduserid";
  private static final String ATTR_DELETIONLDT = "deletionldt";
  private static final String ATTR_DELETIONUSERID = "deletionuserid";
  private static final String ELEMENT_CUSTOM = "custom";
  private static final String VALUE = "value";

  private static final String ELEMENT_INITIATOR = "Initiator";
  private static final String ELEMENT_RESPONDER = "Responder";
  private static final String ATTR_AGREEMENT = "Agreement";
  private static final String ATTR_MEP = "MEP";
  private static final String ATTR_MEP_BINDING = "MEPBinding";
  private static final String ELEMENT_LEG1 = "Leg1";
  private static final String ELEMENT_LEG2 = "Leg2";
  private static final String ELEMENT_PAYLOADSERVICE = "PayloadServices";
  private static final String ELEMENT_RECEPETIONAWARENESS = "RecepetionAwareness";

  private PModeJsonConverter ()
  {}

  public static void setObjectFields (@Nonnull final IBusinessObject aValue, @Nonnull final IJsonObject aElement)
  {
    aElement.add (ATTR_ID, aValue.getID ());
    if (aValue.hasCreationDateTime ())
      aElement.add (ATTR_CREATIONLDT, PDTWebDateHelper.getAsStringXSD (aValue.getCreationDateTime ()));
    if (aValue.hasCreationUserID ())
      aElement.add (ATTR_CREATIONUSERID, aValue.getCreationUserID ());
    if (aValue.hasLastModificationDateTime ())
      aElement.add (ATTR_LASTMODLDT, PDTWebDateHelper.getAsStringXSD (aValue.getLastModificationDateTime ()));
    if (aValue.hasLastModificationUserID ())
      aElement.add (ATTR_LASTMODUSERID, aValue.getLastModificationUserID ());
    if (aValue.hasDeletionDateTime ())
      aElement.add (ATTR_DELETIONLDT, PDTWebDateHelper.getAsStringXSD (aValue.getDeletionDateTime ()));
    if (aValue.hasDeletionUserID ())
      aElement.add (ATTR_DELETIONUSERID, aValue.getDeletionUserID ());
    if (aValue.attrs ().isNotEmpty ())
    {
      final IJsonArray aCustomArray = new JsonArray ();
      for (final Map.Entry <String, String> aEntry : CollectionHelper.getSortedByKey (aValue.attrs ()).entrySet ())
      {
        final IJsonObject eCustom = new JsonObject ();
        eCustom.add (ATTR_ID, aEntry.getKey ());
        if (aEntry.getValue () != null)
          eCustom.add (VALUE, aEntry.getValue ());
        aCustomArray.add (eCustom);
      }
      aElement.addJson (ELEMENT_CUSTOM, aCustomArray);
    }
  }

  @Nonnull
  public static StubObject getStubObject (@Nonnull final IJsonObject aElement)
  {
    // ID
    final String sID = aElement.getAsString (ATTR_ID);

    // Creation
    final LocalDateTime aCreationLDT = PDTWebDateHelper.getLocalDateTimeFromXSD (aElement.getAsString (ATTR_CREATIONLDT));
    final String sCreationUserID = aElement.getAsString (ATTR_CREATIONUSERID);

    // Last modification
    final LocalDateTime aLastModificationLDT = PDTWebDateHelper.getLocalDateTimeFromXSD (aElement.getAsString (ATTR_LASTMODLDT));
    final String sLastModificationUserID = aElement.getAsString (ATTR_LASTMODUSERID);

    // Deletion
    final LocalDateTime aDeletionLDT = PDTWebDateHelper.getLocalDateTimeFromXSD (aElement.getAsString (ATTR_DELETIONLDT));
    final String sDeletionUserID = aElement.getAsString (ATTR_DELETIONUSERID);

    final ICommonsOrderedMap <String, String> aCustomAttrs = new CommonsLinkedHashMap <> ();
    final IJsonArray aCustom = aElement.getAsArray (ELEMENT_CUSTOM);
    if (aCustom != null)
      for (final IJsonObject eCustom : aCustom.iteratorObjects ())
        aCustomAttrs.put (eCustom.getAsString (ATTR_ID), eCustom.getAsString (VALUE));

    return new StubObject (sID,
                           aCreationLDT,
                           sCreationUserID,
                           aLastModificationLDT,
                           sLastModificationUserID,
                           aDeletionLDT,
                           sDeletionUserID,
                           aCustomAttrs);
  }

  @Nonnull
  public static IJsonObject convertToJson (@Nonnull final IPMode aValue)
  {
    final IJsonObject ret = new JsonObject ();
    setObjectFields (aValue, ret);
    if (aValue.hasInitiator ())
      ret.addJson (ELEMENT_INITIATOR, PModePartyJsonConverter.convertToJson (aValue.getInitiator ()));
    if (aValue.hasResponder ())
      ret.addJson (ELEMENT_RESPONDER, PModePartyJsonConverter.convertToJson (aValue.getResponder ()));
    if (aValue.hasAgreement ())
      ret.add (ATTR_AGREEMENT, aValue.getAgreement ());
    ret.add (ATTR_MEP, aValue.getMEPID ());
    ret.add (ATTR_MEP_BINDING, aValue.getMEPBindingID ());
    if (aValue.hasLeg1 ())
      ret.addJson (ELEMENT_LEG1, PModeLegJsonConverter.convertToJson (aValue.getLeg1 ()));
    if (aValue.hasLeg2 ())
      ret.addJson (ELEMENT_LEG2, PModeLegJsonConverter.convertToJson (aValue.getLeg2 ()));
    if (aValue.hasPayloadService ())
      ret.addJson (ELEMENT_PAYLOADSERVICE,
                   PModePayloadServiceJsonConverter.convertToJson (aValue.getPayloadService ()));
    if (aValue.hasReceptionAwareness ())
      ret.addJson (ELEMENT_RECEPETIONAWARENESS,
                   PModeReceptionAwarenessJsonConverter.convertToJson (aValue.getReceptionAwareness ()));
    return ret;
  }

  @Nonnull
  public static PMode convertToNative (@Nonnull final IJsonObject aElement)
  {
    final IJsonObject aInit = aElement.getAsObject (ELEMENT_INITIATOR);
    final PModeParty aInitiator = aInit == null ? null : PModePartyJsonConverter.convertToNative (aInit);

    final IJsonObject aResp = aElement.getAsObject (ELEMENT_RESPONDER);
    final PModeParty aResponder = aResp == null ? null : PModePartyJsonConverter.convertToNative (aResp);

    final String sAgreement = aElement.getAsString (ATTR_AGREEMENT);

    final String sMEP = aElement.getAsString (ATTR_MEP);
    final EMEP eMEP = EMEP.getFromIDOrNull (sMEP);
    if (eMEP == null)
      throw new IllegalStateException ("Failed to resolve MEP '" + sMEP + "'");

    final String sMEPBinding = aElement.getAsString (ATTR_MEP_BINDING);
    final EMEPBinding eMEPBinding = EMEPBinding.getFromIDOrNull (sMEPBinding);
    if (eMEPBinding == null)
      throw new IllegalStateException ("Failed to resolve MEPBinding '" + sMEPBinding + "'");

    final IJsonObject aL1 = aElement.getAsObject (ELEMENT_LEG1);
    final PModeLeg aLeg1 = aL1 == null ? null : PModeLegJsonConverter.convertToNative (aL1);

    final IJsonObject aL2 = aElement.getAsObject (ELEMENT_LEG2);
    final PModeLeg aLeg2 = aL2 == null ? null : PModeLegJsonConverter.convertToNative (aL2);

    final IJsonObject aPS = aElement.getAsObject (ELEMENT_PAYLOADSERVICE);
    final PModePayloadService aPayloadService = aPS == null ? null : PModePayloadServiceJsonConverter.convertToNative (
                                                                                                                       aPS);

    final IJsonObject aRA = aElement.getAsObject (ELEMENT_RECEPETIONAWARENESS);
    final PModeReceptionAwareness aReceptionAwareness = aRA == null ? null : PModeReceptionAwarenessJsonConverter
                                                                                                                 .convertToNative (aRA);

    return new PMode (getStubObject (aElement),
                      aInitiator,
                      aResponder,
                      sAgreement,
                      eMEP,
                      eMEPBinding,
                      aLeg1,
                      aLeg2,
                      aPayloadService,
                      aReceptionAwareness);
  }
}
