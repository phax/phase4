/*
 * Copyright (C) 2025-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.dbnalliance.server.api;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.phase4.dbnalliance.server.APConfig;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.api.IAPIExecutor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Abstract API executor class. Contains the check for the "X-Token" HTTP
 * header.
 *
 * @author Philip Helger
 */
public abstract class AbstractVerifyingAPIExecutor implements IAPIExecutor
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AbstractVerifyingAPIExecutor.class);

  protected abstract void verifiedInvokeAPI (@NonNull IAPIDescriptor aAPIDescriptor,
                                             @NonNull @Nonempty String sPath,
                                             @NonNull Map <String, String> aPathVariables,
                                             @NonNull IRequestWebScopeWithoutResponse aRequestScope,
                                             @NonNull UnifiedResponse aUnifiedResponse) throws Exception;

  public void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                         @NonNull @Nonempty final String sPath,
                         @NonNull final Map <String, String> aPathVariables,
                         @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                         @NonNull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sXToken = aRequestScope.headers ().getFirstHeaderValue ("X-Token");
    if (StringHelper.isEmpty (sXToken))
    {
      LOGGER.error ("The specific token header is missing");
      throw new APIParamException ("The specific token header is missing");
    }
    if (!sXToken.equals (APConfig.getPhase4ApiRequiredToken ()))
    {
      LOGGER.error ("The specified token value does not match the configured required token");
      throw new APIParamException ("The specified token value does not match the configured required token");
    }

    // Invoke
    verifiedInvokeAPI (aAPIDescriptor, sPath, aPathVariables, aRequestScope, aUnifiedResponse);
  }
}
