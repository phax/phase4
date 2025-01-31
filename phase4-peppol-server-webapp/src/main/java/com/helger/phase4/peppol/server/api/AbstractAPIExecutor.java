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
