package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;

import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.state.EMandatory;
import com.helger.commons.string.StringParser;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

public class PModePayloadProfileMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ATTR_MAX_SIZE_KB = "MaxSizeKB";
  private static final String ATTR_MIME_TYPE = "MimeType";
  private static final String ATTR_MANDATORY = "Mandatory";
  private static final String ATTR_NAME = "Name";
  private static final String ATTR_XSD_FILENAME = "XSDFilename";

  @Nonnull
  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModePayloadProfile aValue = (PModePayloadProfile) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    if (aValue.hasMaxSizeKB ())
      ret.setAttribute (ATTR_MAX_SIZE_KB, aValue.getMaxSizeKB ());
    ret.setAttribute (ATTR_MIME_TYPE, aValue.getMimeType ().getAsString ());
    ret.setAttribute (ATTR_MANDATORY, aValue.isMandatory ());
    ret.setAttribute (ATTR_NAME, aValue.getName ());
    ret.setAttribute (ATTR_XSD_FILENAME, aValue.getXSDFilename ());
    return ret;
  }

  @Nonnull
  public Object convertToNative (final IMicroElement aElement)
  {
    final String sName = aElement.getAttributeValue (ATTR_NAME);
    final IMimeType aMimeType = MimeTypeParser.parseMimeType (aElement.getAttributeValue (ATTR_MIME_TYPE));
    final String sXSDFilename = aElement.getAttributeValue (ATTR_XSD_FILENAME);
    final Integer aMaxSizeKB = aElement.getAttributeValueWithConversion (ATTR_MAX_SIZE_KB, Integer.class);
    final EMandatory eMandatory = EMandatory.valueOf (StringParser.parseBool (aElement.getAttributeValue (ATTR_MANDATORY),
                                                                              false));

    return new PModePayloadProfile (sName, aMimeType, sXSDFilename, aMaxSizeKB, eMandatory);
  }

}
