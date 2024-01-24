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

import com.helger.commons.ValueEnforcer;
import com.helger.phive.api.result.ValidationResultList;

/**
 * Special {@link Phase4PeppolException} exception for validation errors.
 *
 * @author Philip Helger
 */
public class Phase4PeppolValidationException extends Phase4PeppolException
{
  private final ValidationResultList m_aValidationResult;

  /**
   * @param aValidationResult
   *        The validation result list that usually contains at least one error.
   */
  public Phase4PeppolValidationException (@Nonnull final ValidationResultList aValidationResult)
  {
    super ("Error validating business document");
    m_aValidationResult = ValueEnforcer.notNull (aValidationResult, "ValidationResult");
  }

  /**
   * @return The validation results as provided in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  public final ValidationResultList getValidationResult ()
  {
    return m_aValidationResult;
  }
}
