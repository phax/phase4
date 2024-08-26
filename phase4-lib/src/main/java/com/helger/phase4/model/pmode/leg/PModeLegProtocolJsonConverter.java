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

import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.phase4.model.ESoapVersion;

/**
 * JSON converter for objects of class {@link PModeLegProtocol}.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class PModeLegProtocolJsonConverter
{
  private static final String ADDRESS = "Address";
  private static final String SOAP_VERSION = "SoapVersion";

  private PModeLegProtocolJsonConverter ()
  {}

  @Nonnull
  public static IJsonObject convertToJson (@Nonnull final PModeLegProtocol aValue)
  {
    final IJsonObject ret = new JsonObject ();
    if (aValue.hasAddress ())
      ret.add (ADDRESS, aValue.getAddress ());
    ret.add (SOAP_VERSION, aValue.getSoapVersion ().getVersion ());
    return ret;
  }

  @Nonnull
  public static PModeLegProtocol convertToNative (@Nonnull final IJsonObject aElement)
  {
    final String sAddress = aElement.getAsString (ADDRESS);

    final String sSoapVersion = aElement.getAsString (SOAP_VERSION);
    final ESoapVersion eSoapVersion = ESoapVersion.getFromVersionOrNull (sSoapVersion);
    if (eSoapVersion == null)
      throw new IllegalStateException ("Failed to resolve SOAP version '" + sSoapVersion + "'");

    return new PModeLegProtocol (sAddress, eSoapVersion);
  }
}
