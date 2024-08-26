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
  private static final String NAME = "Name";
  private static final String DESCRIPTION = "Description";
  private static final String DATA_TYPE = "DataType";
  private static final String MANDATORY = "Mandatory";

  private PModePropertyJsonConverter ()
  {}

  @Nonnull
  public static IJsonObject convertToJson (@Nonnull final PModeProperty aValue)
  {
    final IJsonObject ret = new JsonObject ();
    ret.add (NAME, aValue.getName ());
    if (aValue.hasDescription ())
      ret.add (DESCRIPTION, aValue.getDescription ());
    ret.add (DATA_TYPE, aValue.getDataType ());
    ret.add (MANDATORY, aValue.isMandatory ());
    return ret;
  }

  @Nonnull
  public static PModeProperty convertToNative (@Nonnull final IJsonObject aElement)
  {
    final String sName = aElement.getAsString (NAME);
    final String sDescription = aElement.getAsString (DESCRIPTION);
    final String sDataType = aElement.getAsString (DATA_TYPE);
    final EMandatory eMandatory = EMandatory.valueOf (aElement.getAsBoolean (MANDATORY,
                                                                             PModeProperty.DEFAULT_MANDATORY));

    return new PModeProperty (sName, sDescription, sDataType, eMandatory);
  }
}
