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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;

/**
 * JSON converter for objects of class {@link PModeParty}.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class PModePartyJsonConverter
{
  private static final String ID_TYPE = "IDType";
  private static final String ID_VALUE = "IDValue";
  private static final String ROLE = "Role";
  private static final String USER_NAME = "Username";
  private static final String PASSWORD = "Password";

  private PModePartyJsonConverter ()
  {}

  @Nonnull
  public static IJsonObject convertToJson (@Nonnull final PModeParty aValue)
  {
    final IJsonObject ret = new JsonObject ();
    if (aValue.hasIDType ())
      ret.add (ID_TYPE, aValue.getIDType ());
    ret.add (ID_VALUE, aValue.getIDValue ());
    ret.add (ROLE, aValue.getRole ());
    if (aValue.hasUserName ())
      ret.add (USER_NAME, aValue.getUserName ());
    if (aValue.hasPassword ())
      ret.add (PASSWORD, aValue.getPassword ());
    return ret;
  }

  @Nonnull
  public static PModeParty convertToNative (@Nonnull final IJsonObject aElement)
  {
    final String sIDType = aElement.getAsString (ID_TYPE);
    final String sIDValue = aElement.getAsString (ID_VALUE);
    final String sRole = aElement.getAsString (ROLE);
    final String sUserName = aElement.getAsString (USER_NAME);
    final String sPassword = aElement.getAsString (PASSWORD);
    return new PModeParty (sIDType, sIDValue, sRole, sUserName, sPassword);
  }
}
