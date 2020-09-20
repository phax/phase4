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

import com.helger.commons.state.EMandatory;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;

/**
 * XML converter for objects of class {@link PModeProperty}.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class PModePropertyJsonConverter
{
  private static final String ATTR_NAME = "Name";
  private static final String ATTR_DESCRIPTION = "Description";
  private static final String ATTR_DATA_TYPE = "DataType";
  private static final String ATTR_MANDATORY = "Mandatory";

  private PModePropertyJsonConverter ()
  {}

  @Nonnull
  public static IJsonObject convertToJson (@Nonnull final PModeProperty aValue)
  {
    return new JsonObject ().add (ATTR_NAME, aValue.getName ())
                            .add (ATTR_DESCRIPTION, aValue.getDescription ())
                            .add (ATTR_DATA_TYPE, aValue.getDataType ())
                            .add (ATTR_MANDATORY, aValue.isMandatory ());
  }

  @Nonnull
  public static PModeProperty convertToNative (@Nonnull final IJsonObject aElement)
  {
    final String sName = aElement.getAsString (ATTR_NAME);
    final String sDescription = aElement.getAsString (ATTR_DESCRIPTION);
    final String sDataType = aElement.getAsString (ATTR_DATA_TYPE);
    final EMandatory eMandatory = EMandatory.valueOf (aElement.getAsBoolean (ATTR_MANDATORY,
                                                                             PModeProperty.DEFAULT_MANDATORY));

    return new PModeProperty (sName, sDescription, sDataType, eMandatory);
  }
}
