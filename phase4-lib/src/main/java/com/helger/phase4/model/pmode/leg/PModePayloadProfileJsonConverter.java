/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.state.EMandatory;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;

/**
 * JSON converter for objects of class {@link PModePayloadProfile}.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class PModePayloadProfileJsonConverter
{
  private static final String ATTR_NAME = "Name";
  private static final String ATTR_MIME_TYPE = "MimeType";
  private static final String ATTR_XSD_FILENAME = "XSDFilename";
  private static final String ATTR_MAX_SIZE_KB = "MaxSizeKB";
  private static final String ATTR_MANDATORY = "Mandatory";

  private PModePayloadProfileJsonConverter ()
  {}

  @Nonnull
  public static IJsonObject convertToJson (@Nonnull final PModePayloadProfile aValue)
  {
    final IJsonObject ret = new JsonObject ();
    ret.add (ATTR_NAME, aValue.getName ());
    ret.add (ATTR_MIME_TYPE, aValue.getMimeType ().getAsString ());
    ret.add (ATTR_XSD_FILENAME, aValue.getXSDFilename ());
    if (aValue.hasMaxSizeKB ())
      ret.add (ATTR_MAX_SIZE_KB, aValue.getMaxSizeKB ().intValue ());
    ret.add (ATTR_MANDATORY, aValue.isMandatory ());
    return ret;
  }

  @Nonnull
  public static PModePayloadProfile convertToNative (final IJsonObject aElement)
  {
    final String sName = aElement.getAsString (ATTR_NAME);
    final IMimeType aMimeType = MimeTypeParser.parseMimeType (aElement.getAsString (ATTR_MIME_TYPE));
    final String sXSDFilename = aElement.getAsString (ATTR_XSD_FILENAME);
    final Integer aMaxSizeKB = aElement.getAsIntObj (ATTR_MAX_SIZE_KB);
    final EMandatory eMandatory = EMandatory.valueOf (aElement.getAsBoolean (ATTR_MANDATORY,
                                                                             PModePayloadProfile.DEFAULT_MANDATORY));

    return new PModePayloadProfile (sName, aMimeType, sXSDFilename, aMaxSizeKB, eMandatory);
  }
}
