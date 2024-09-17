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
package com.helger.phase4.peppol;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.executorset.IValidationExecutorSetRegistry;
import com.helger.phive.api.executorset.ValidationExecutorSetRegistry;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.validity.IValidityDeterminator;
import com.helger.phive.peppol.PeppolValidation;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phive.xml.source.ValidationSourceXML;

/**
 * This class contains the client side validation required for outgoing Peppol
 * messages.
 *
 * @author Philip Helger
 */
public final class Phase4PeppolValidation
{
  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4PeppolValidation.class);
  // Note to myself: don't create a getter for this registry to avoid outside
  // modification
  private static final IValidationExecutorSetRegistry <IValidationSourceXML> VES_REGISTRY = createDefaultRegistry ();

  private Phase4PeppolValidation ()
  {}

  /**
   * @return Get the existing default VES registry. Handle with care.
   * @since 1.3.1
   */
  @Nonnull
  @ReturnsMutableObject
  public static IValidationExecutorSetRegistry <IValidationSourceXML> getDefaultRegistry ()
  {
    return VES_REGISTRY;
  }

  /**
   * @return A new {@link ValidationExecutorSetRegistry} initialized with the
   *         Peppol rules only.
   * @since 0.10.1
   * @see PeppolValidation
   */
  @Nonnull
  @ReturnsMutableCopy
  public static ValidationExecutorSetRegistry <IValidationSourceXML> createDefaultRegistry ()
  {
    final ValidationExecutorSetRegistry <IValidationSourceXML> ret = new ValidationExecutorSetRegistry <> ();
    PeppolValidation.initStandard (ret);
    return ret;
  }

  /**
   * Validate the passed DOM element using the provided VESID using the default
   * registry.
   *
   * @param aXML
   *        The XML element to be validated. May not be <code>null</code>.
   * @param aVESID
   *        The {@link DVRCoordinate} to be used. Must be contained in the
   *        default registry. May not be <code>null</code>.
   * @param aValidationResultHandler
   *        The validation result handler to be used. May not be
   *        <code>null</code>.
   * @throws Phase4PeppolException
   *         In case e.g. the validation failed. This usually implies, that the
   *         document will NOT be send out.
   * @see #validateOutgoingBusinessDocument(Element,
   *      IValidationExecutorSetRegistry, DVRCoordinate,
   *      IPhase4PeppolValidationResultHandler)
   */
  public static void validateOutgoingBusinessDocument (@Nonnull final Element aXML,
                                                       @Nonnull final DVRCoordinate aVESID,
                                                       @Nonnull final IPhase4PeppolValidationResultHandler aValidationResultHandler) throws Phase4PeppolException
  {
    validateOutgoingBusinessDocument (aXML, VES_REGISTRY, aVESID, aValidationResultHandler);
  }

  /**
   * Validate the passed DOM element using the provided VESID using the provided
   * registry.
   *
   * @param aXML
   *        The XML element to be validated. May not be <code>null</code>.
   * @param aVESRegistry
   *        The VES registry the VESID is looked up in.
   * @param aVESID
   *        The {@link DVRCoordinate} to be used. Must be contained in the
   *        provided registry. May not be <code>null</code>.
   * @param aValidationResultHandler
   *        The validation result handler to be used. May not be
   *        <code>null</code>.
   * @throws Phase4PeppolException
   *         In case e.g. the validation failed. This usually implies, that the
   *         document will NOT be send out.
   * @since 0.10.1
   */
  public static void validateOutgoingBusinessDocument (@Nonnull final Element aXML,
                                                       @Nonnull final IValidationExecutorSetRegistry <IValidationSourceXML> aVESRegistry,
                                                       @Nonnull final DVRCoordinate aVESID,
                                                       @Nonnull final IPhase4PeppolValidationResultHandler aValidationResultHandler) throws Phase4PeppolException
  {
    ValueEnforcer.notNull (aXML, "XMLElement");
    ValueEnforcer.notNull (aVESRegistry, "VESRegistry");
    ValueEnforcer.notNull (aVESID, "VESID");
    ValueEnforcer.notNull (aValidationResultHandler, "ValidationResultHandler");

    final IValidationExecutorSet <IValidationSourceXML> aVES = aVESRegistry.getOfID (aVESID);
    if (aVES == null)
      throw new Phase4PeppolException ("The validation executor set ID " + aVESID.getAsSingleID () + " is unknown!");

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
