/*
 * Copyright (C) 2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.hredelivery;

import org.slf4j.Logger;
import org.w3c.dom.Element;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.util.Phase4Exception;
import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.executorset.IValidationExecutorSetRegistry;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.validity.IValidityDeterminator;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phive.xml.source.ValidationSourceXML;

import jakarta.annotation.Nonnull;

/**
 * This class contains the client side validation required for outgoing Peppol messages.
 *
 * @author Philip Helger
 * @since 4.0.1
 */
public final class Phase4HREdeliveryValidation
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (Phase4HREdeliveryValidation.class);

  private Phase4HREdeliveryValidation ()
  {}

  /**
   * Validate the passed DOM element using the provided VESID using the provided registry.
   *
   * @param aXML
   *        The XML element to be validated. May not be <code>null</code>.
   * @param aVESRegistry
   *        The VES registry the VESID is looked up in.
   * @param aVESID
   *        The {@link DVRCoordinate} to be used. Must be contained in the provided registry. May
   *        not be <code>null</code>.
   * @param aValidationResultHandler
   *        The validation result handler to be used. May not be <code>null</code>.
   * @throws Phase4Exception
   *         In case e.g. the validation failed. This usually implies, that the document will NOT be
   *         send out.
   */
  public static void validateOutgoingBusinessDocument (@Nonnull final Element aXML,
                                                       @Nonnull final IValidationExecutorSetRegistry <IValidationSourceXML> aVESRegistry,
                                                       @Nonnull final DVRCoordinate aVESID,
                                                       @Nonnull final IPhase4HREdeliveryValidationResultHandler aValidationResultHandler) throws Phase4Exception
  {
    ValueEnforcer.notNull (aXML, "XMLElement");
    ValueEnforcer.notNull (aVESRegistry, "VESRegistry");
    ValueEnforcer.notNull (aVESID, "VESID");
    ValueEnforcer.notNull (aValidationResultHandler, "ValidationResultHandler");

    final IValidationExecutorSet <IValidationSourceXML> aVES = aVESRegistry.getOfID (aVESID);
    if (aVES == null)
      throw new Phase4Exception ("The validation executor set ID " + aVESID.getAsSingleID () + " is unknown!")
                                                                                                              .setRetryFeasible (false);

    final ValidationResultList aValidationResult = ValidationExecutionManager.executeValidation (IValidityDeterminator.createDefault (),
                                                                                                 aVES,
                                                                                                 ValidationSourceXML.create (null,
                                                                                                                             aXML));
    if (aValidationResult.containsAtLeastOneError ())
    {
      aValidationResultHandler.onValidationErrors (aValidationResult);
      LOGGER.warn ("Continue to send AS4 message, although validation errors are contained!");
    }
    else
      aValidationResultHandler.onValidationSuccess (aValidationResult);
  }
}
