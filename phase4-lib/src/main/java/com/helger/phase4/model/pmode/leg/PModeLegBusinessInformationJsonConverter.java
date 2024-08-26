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
package com.helger.phase4.model.pmode.leg;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;

/**
 * JSON converter for objects of class {@link PModeLegBusinessInformation}.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class PModeLegBusinessInformationJsonConverter
{
  private static final String SERVICE = "Service";
  private static final String SERVICE_TYPE = "ServiceType";
  private static final String ACTION = "Action";
  private static final String PROPERTIES = "Properties";
  private static final String PAYLOAD_PROFILE = "PayloadProfile";
  private static final String PAYLOAD_PROFILE_MAX_KB = "PayloadProfileMaxKB";
  private static final String MPCID = "MPCID";

  private PModeLegBusinessInformationJsonConverter ()
  {}

  @Nonnull
  public static IJsonObject convertToJson (@Nonnull final PModeLegBusinessInformation aValue)
  {
    final IJsonObject ret = new JsonObject ();
    if (aValue.hasService ())
      ret.add (SERVICE, aValue.getService ());
    if (aValue.hasServiceType ())
      ret.add (SERVICE_TYPE, aValue.getServiceType ());
    if (aValue.hasAction ())
      ret.add (ACTION, aValue.getAction ());
    if (aValue.properties ().isNotEmpty ())
      ret.addJson (PROPERTIES,
                   new JsonArray ().addAllMapped (aValue.properties ().values (),
                                                  PModePropertyJsonConverter::convertToJson));
    if (aValue.payloadProfiles ().isNotEmpty ())
      ret.addJson (PAYLOAD_PROFILE,
                   new JsonArray ().addAllMapped (aValue.payloadProfiles ().values (),
                                                  PModePayloadProfileJsonConverter::convertToJson));
    if (aValue.hasPayloadProfileMaxKB ())
      ret.add (PAYLOAD_PROFILE_MAX_KB, aValue.getPayloadProfileMaxKB ().longValue ());
    if (aValue.hasMPCID ())
      ret.add (MPCID, aValue.getMPCID ());
    return ret;
  }

  @Nonnull
  public static PModeLegBusinessInformation convertToNative (@Nonnull final IJsonObject aElement)
  {
    final String sService = aElement.getAsString (SERVICE);
    final String sServiceType = aElement.getAsString (SERVICE_TYPE);
    final String sAction = aElement.getAsString (ACTION);

    final ICommonsOrderedMap <String, PModeProperty> aProperties = new CommonsLinkedHashMap <> ();
    final IJsonArray aProps = aElement.getAsArray (PROPERTIES);
    if (aProps != null)
      for (final IJsonObject aPropertyElement : aProps.iteratorObjects ())
      {
        final PModeProperty aProperty = PModePropertyJsonConverter.convertToNative (aPropertyElement);
        aProperties.put (aProperty.getName (), aProperty);
      }

    final ICommonsOrderedMap <String, PModePayloadProfile> aPayloadProfiles = new CommonsLinkedHashMap <> ();
    final IJsonArray aPayloadProfs = aElement.getAsArray (PAYLOAD_PROFILE);
    if (aPayloadProfs != null)
      for (final IJsonObject aPayloadElement : aPayloadProfs.iteratorObjects ())
      {
        final PModePayloadProfile aPayloadProfile = PModePayloadProfileJsonConverter.convertToNative (aPayloadElement);
        aPayloadProfiles.put (aPayloadProfile.getName (), aPayloadProfile);
      }

    final Long aPayloadProfileMaxKB = aElement.getAsLongObj (PAYLOAD_PROFILE_MAX_KB);
    final String sMPCID = aElement.getAsString (MPCID);

    return new PModeLegBusinessInformation (sService,
                                            sServiceType,
                                            sAction,
                                            aProperties,
                                            aPayloadProfiles,
                                            aPayloadProfileMaxKB,
                                            sMPCID);
  }
}
