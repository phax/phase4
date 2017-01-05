/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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

import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.state.EMandatory;
import com.helger.commons.string.StringParser;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

public class PModePayloadProfileMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ATTR_NAME = "Name";
  private static final String ATTR_MIME_TYPE = "MimeType";
  private static final String ATTR_XSD_FILENAME = "XSDFilename";
  private static final String ATTR_MAX_SIZE_KB = "MaxSizeKB";
  private static final String ATTR_MANDATORY = "Mandatory";

  @Nonnull
  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModePayloadProfile aValue = (PModePayloadProfile) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);

    ret.setAttribute (ATTR_NAME, aValue.getName ());
    ret.setAttribute (ATTR_MIME_TYPE, aValue.getMimeType ().getAsString ());
    ret.setAttribute (ATTR_XSD_FILENAME, aValue.getXSDFilename ());
    if (aValue.hasMaxSizeKB ())
      ret.setAttribute (ATTR_MAX_SIZE_KB, aValue.getMaxSizeKB ());
    ret.setAttribute (ATTR_MANDATORY, aValue.isMandatory ());
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
