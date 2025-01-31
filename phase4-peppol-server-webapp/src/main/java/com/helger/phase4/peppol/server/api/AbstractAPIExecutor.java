/*
 * Copyright (C) 2020-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol.server.api;

import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.peppol.server.APConfig;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.api.IAPIExecutor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Abstract API executor class. Contains the token check.
 *
 * @author Philip Helger
 */
public abstract class AbstractAPIExecutor implements IAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractAPIExecutor.class);

  protected abstract void verifiedInvokeAPI (@Nonnull IAPIDescriptor aAPIDescriptor,
                                             @Nonnull @Nonempty String sPath,
                                             @Nonnull Map <String, String> aPathVariables,
                                             @Nonnull IRequestWebScopeWithoutResponse aRequestScope,
                                             @Nonnull UnifiedResponse aUnifiedResponse) throws Exception;

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sXToken = aRequestScope.headers ().getFirstHeaderValue ("X-Token");
    if (StringHelper.hasNoText (sXToken))
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
