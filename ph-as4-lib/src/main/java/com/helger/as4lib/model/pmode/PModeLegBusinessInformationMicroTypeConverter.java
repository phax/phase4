package com.helger.as4lib.model.pmode;

import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsOrderedMap;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.MicroTypeConverter;

public class PModeLegBusinessInformationMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ELEMENT_PAYLOAD_PROFILE = "PayloadProfile";
  private static final String ELEMENT_PROPERTIES = "Properties";
  private static final String ATTR_PAYLOAD_PROFILE_MAX_KB = "PayloadProfileMaxKB";
  private static final String ATTR_ACTION = "Action";
  private static final String ATTR_MPCID = "MPCID";
  private static final String ATTR_SERVICE = "Service";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModeLegBusinessInformation aValue = (PModeLegBusinessInformation) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    for (final String sPayloadProfileKey : aValue.getPayloadProfile ().keySet ())
    {
      ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getProperties ().get (sPayloadProfileKey),
                                                                 sNamespaceURI,
                                                                 ELEMENT_PAYLOAD_PROFILE));
    }
    for (final String sPropertyKey : aValue.getProperties ().keySet ())
    {
      ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getProperties ().get (sPropertyKey),
                                                                 sNamespaceURI,
                                                                 ELEMENT_PROPERTIES));
    }
    ret.setAttribute (ATTR_PAYLOAD_PROFILE_MAX_KB, aValue.getPayloadProfileMaxKB ());
    ret.setAttribute (ATTR_ACTION, aValue.getAction ());
    ret.setAttribute (ATTR_MPCID, aValue.getMPCID ());
    ret.setAttribute (ATTR_SERVICE, aValue.getService ());
    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {
    final ICommonsOrderedMap <String, PModePayloadProfile> aPayloadProfiles = new CommonsLinkedHashMap<> ();
    for (final IMicroElement aPayloadElement : aElement.getAllChildElements (ELEMENT_PAYLOAD_PROFILE))
    {
      final PModePayloadProfile aPayloadProfile = MicroTypeConverter.convertToNative (aPayloadElement,
                                                                                      PModePayloadProfile.class);
      aPayloadProfiles.put (aPayloadProfile.getName (), aPayloadProfile);
    }

    final ICommonsOrderedMap <String, PModeProperty> aProperties = new CommonsLinkedHashMap<> ();
    for (final IMicroElement aPropertyElement : aElement.getAllChildElements (ELEMENT_PROPERTIES))
    {
      final PModeProperty aProperty = MicroTypeConverter.convertToNative (aPropertyElement, PModeProperty.class);
      aProperties.put (aProperty.getName (), aProperty);
    }

    final Integer aPayloadProfileMaxKB = aElement.getAttributeValueWithConversion (ATTR_PAYLOAD_PROFILE_MAX_KB,
                                                                                   Integer.class);
    final String sAction = aElement.getAttributeValue (ATTR_ACTION);
    final String sMPCID = aElement.getAttributeValue (ATTR_MPCID);
    final String sService = aElement.getAttributeValue (ATTR_SERVICE);
    return new PModeLegBusinessInformation (aPayloadProfiles,
                                            aProperties,
                                            aPayloadProfileMaxKB,
                                            sAction,
                                            sMPCID,
                                            sService);
  }

}
