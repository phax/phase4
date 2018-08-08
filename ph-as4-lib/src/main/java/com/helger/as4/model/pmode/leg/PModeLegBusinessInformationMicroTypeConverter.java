/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.model.pmode.leg;

import com.helger.as4.model.pmode.AbstractPModeMicroTypeConverter;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.MicroTypeConverter;

public class PModeLegBusinessInformationMicroTypeConverter extends
                                                           AbstractPModeMicroTypeConverter <PModeLegBusinessInformation>
{
  private static final String ATTR_SERVICE = "Service";
  private static final String ATTR_ACTION = "Action";
  private static final String ELEMENT_PROPERTIES = "Properties";
  private static final String ELEMENT_PAYLOAD_PROFILE = "PayloadProfile";
  private static final String ATTR_PAYLOAD_PROFILE_MAX_KB = "PayloadProfileMaxKB";
  private static final String ATTR_MPCID = "MPCID";

  public IMicroElement convertToMicroElement (final PModeLegBusinessInformation aValue,
                                              final String sNamespaceURI,
                                              final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_SERVICE, aValue.getService ());
    ret.setAttribute (ATTR_ACTION, aValue.getAction ());
    aValue.forAllProperties (x -> ret.appendChild (MicroTypeConverter.convertToMicroElement (x,
                                                                                             sNamespaceURI,
                                                                                             ELEMENT_PROPERTIES)));
    aValue.forAllPayloadProfiles (x -> ret.appendChild (MicroTypeConverter.convertToMicroElement (x,
                                                                                                  sNamespaceURI,
                                                                                                  ELEMENT_PAYLOAD_PROFILE)));
    if (aValue.getPayloadProfileMaxKB () != null)
      ret.setAttribute (ATTR_PAYLOAD_PROFILE_MAX_KB, aValue.getPayloadProfileMaxKB ().longValue ());
    ret.setAttribute (ATTR_MPCID, aValue.getMPCID ());
    return ret;
  }

  public PModeLegBusinessInformation convertToNative (final IMicroElement aElement)
  {
    final String sService = aElement.getAttributeValue (ATTR_SERVICE);
    final String sAction = aElement.getAttributeValue (ATTR_ACTION);

    final ICommonsOrderedMap <String, PModeProperty> aProperties = new CommonsLinkedHashMap <> ();
    for (final IMicroElement aPropertyElement : aElement.getAllChildElements (ELEMENT_PROPERTIES))
    {
      final PModeProperty aProperty = MicroTypeConverter.convertToNative (aPropertyElement, PModeProperty.class);
      aProperties.put (aProperty.getName (), aProperty);
    }

    final ICommonsOrderedMap <String, PModePayloadProfile> aPayloadProfiles = new CommonsLinkedHashMap <> ();
    for (final IMicroElement aPayloadElement : aElement.getAllChildElements (ELEMENT_PAYLOAD_PROFILE))
    {
      final PModePayloadProfile aPayloadProfile = MicroTypeConverter.convertToNative (aPayloadElement,
                                                                                      PModePayloadProfile.class);
      aPayloadProfiles.put (aPayloadProfile.getName (), aPayloadProfile);
    }

    final Long aPayloadProfileMaxKB = aElement.getAttributeValueWithConversion (ATTR_PAYLOAD_PROFILE_MAX_KB,
                                                                                Long.class);
    final String sMPCID = aElement.getAttributeValue (ATTR_MPCID);

    return new PModeLegBusinessInformation (sService,
                                            sAction,
                                            aProperties,
                                            aPayloadProfiles,
                                            aPayloadProfileMaxKB,
                                            sMPCID);
  }
}
