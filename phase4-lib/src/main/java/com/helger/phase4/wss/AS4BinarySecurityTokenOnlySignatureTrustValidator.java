/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.wss;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SignatureTrustValidator;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.base.array.ArrayHelper;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.util.Phase4Exception;

/**
 * Special {@link SignatureTrustValidator} implementation that allows only for BinarySecurityToken
 * for signature verification.
 *
 * @author Philip Helger
 * @since 4.2.6
 */
public class AS4BinarySecurityTokenOnlySignatureTrustValidator extends SignatureTrustValidator
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4BinarySecurityTokenOnlySignatureTrustValidator.class);

  @Override
  @OverridingMethodsMustInvokeSuper
  public Credential validate (@NonNull final Credential aCredential, @NonNull final RequestData aReqData)
                                                                                                          throws WSSecurityException
  {
    // Check that we have the full certificate available
    if (ArrayHelper.isEmpty (aCredential.getCertificates ()))
    {
      // No BST used -> reject
      throw new WSSecurityException (WSSecurityException.ErrorCode.FAILURE,
                                     new Phase4Exception ("Only BinarySecurityToken-based keys allowed for signature verification"));
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Verified that inbound message uses a BinarySecurityToken for signature verification");

    return super.validate (aCredential, aReqData);
  }
}
