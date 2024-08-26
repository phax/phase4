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

/**
 * JSON converter for {@link PModeLeg}.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class PModeLegJsonConverter
{
  private static final String PROTOCOL = "Protocol";
  private static final String BUSINESS_INFORMATION = "BusinessInfo";
  private static final String ERROR_HANDLING = "ErrorHandling";
  private static final String RELIABILITY = "Reliability";
  private static final String SECURITY = "Security";

  private PModeLegJsonConverter ()
  {}

  @Nonnull
  public static IJsonObject convertToJson (@Nonnull final PModeLeg aValue)
  {
    final IJsonObject ret = new JsonObject ();
    if (aValue.hasProtocol ())
      ret.addJson (PROTOCOL, PModeLegProtocolJsonConverter.convertToJson (aValue.getProtocol ()));
    if (aValue.hasBusinessInfo ())
      ret.addJson (BUSINESS_INFORMATION,
                   PModeLegBusinessInformationJsonConverter.convertToJson (aValue.getBusinessInfo ()));
    if (aValue.hasErrorHandling ())
      ret.addJson (ERROR_HANDLING, PModeLegErrorHandlingJsonConverter.convertToJson (aValue.getErrorHandling ()));
    if (aValue.hasReliability ())
      ret.addJson (RELIABILITY, PModeLegReliabilityJsonConverter.convertToJson (aValue.getReliability ()));
    if (aValue.hasSecurity ())
      ret.addJson (SECURITY, PModeLegSecurityJsonConverter.convertToJson (aValue.getSecurity ()));
    return ret;
  }

  @Nonnull
  public static PModeLeg convertToNative (@Nonnull final IJsonObject aElement)
  {
    final IJsonObject aProt = aElement.getAsObject (PROTOCOL);
    final PModeLegProtocol aProtocol = aProt == null ? null : PModeLegProtocolJsonConverter.convertToNative (aProt);

    final IJsonObject aBI = aElement.getAsObject (BUSINESS_INFORMATION);
    final PModeLegBusinessInformation aBusinessInformation = aBI == null ? null
                                                                         : PModeLegBusinessInformationJsonConverter.convertToNative (aBI);

    final IJsonObject aEH = aElement.getAsObject (ERROR_HANDLING);
    final PModeLegErrorHandling aErrorHandling = aEH == null ? null : PModeLegErrorHandlingJsonConverter
                                                                                                        .convertToNative (aEH);

    final IJsonObject aR = aElement.getAsObject (RELIABILITY);
    final PModeLegReliability aReliability = aR == null ? null : PModeLegReliabilityJsonConverter.convertToNative (aR);

    final IJsonObject aS = aElement.getAsObject (SECURITY);
    final PModeLegSecurity aSecurity = aS == null ? null : PModeLegSecurityJsonConverter.convertToNative (aS);

    return new PModeLeg (aProtocol, aBusinessInformation, aErrorHandling, aReliability, aSecurity);
  }
}
