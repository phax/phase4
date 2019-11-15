/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol;

import java.util.Locale;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.bdve.execute.ValidationExecutionManager;
import com.helger.bdve.executorset.IValidationExecutorSet;
import com.helger.bdve.executorset.VESID;
import com.helger.bdve.executorset.ValidationExecutorSetRegistry;
import com.helger.bdve.peppol.PeppolValidation;
import com.helger.bdve.result.ValidationResultList;
import com.helger.bdve.source.ValidationSource;
import com.helger.commons.ValueEnforcer;

/**
 * This class contains the client side validation required for outgoing Peppol
 * messages.
 *
 * @author Philip Helger
 */
public final class Phase4PeppolValidation
{
  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4PeppolValidation.class);
  private static final ValidationExecutorSetRegistry VES_REGISTRY = new ValidationExecutorSetRegistry ();

  static
  {
    PeppolValidation.initStandard (VES_REGISTRY);
    PeppolValidation.initThirdParty (VES_REGISTRY);
  }

  private Phase4PeppolValidation ()
  {}

  public static void validateOutgoingBusinessDocument (@Nonnull final Element aXML,
                                                       @Nonnull final VESID aVESID,
                                                       @Nonnull final IPhase4PeppolValidatonResultHandler aValidationResultHandler) throws Phase4PeppolException
  {
    ValueEnforcer.notNull (aXML, "XMLElement");
    ValueEnforcer.notNull (aVESID, "VESID");
    ValueEnforcer.notNull (aValidationResultHandler, "ValidationResultHandler");

    final IValidationExecutorSet aVES = VES_REGISTRY.getOfID (aVESID);
    if (aVES == null)
      throw new Phase4PeppolException ("The validation executor set ID " +
                                              aVESID.getAsSingleID () +
                                              " is unknown!");

    final ValidationExecutionManager aVEM = aVES.createExecutionManager ();
    final ValidationResultList aValidationResult = aVEM.executeValidation (ValidationSource.create (null, aXML),
                                                                           (Locale) null);
    if (aValidationResult.containsAtLeastOneError ())
    {
      aValidationResultHandler.onValidationErrors (aValidationResult);
      LOGGER.warn ("Continue to send AS4 message, although validation errors are contained!");
    }
    else
      aValidationResultHandler.onValidationSuccess (aValidationResult);
  }
}
