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

import org.jspecify.annotations.NonNull;

import com.helger.phase4.util.Phase4Exception;
import com.helger.phive.api.result.ValidationResultList;

/**
 * Interface for handling validation errors
 *
 * @author Philip Helger
 * @since 4.0.2
 */
public interface IPhase4HREdeliveryValidationResultHandler
{
  /**
   * Invoked, if no validation error is present. This method is invoked if only warnings are
   * present.
   *
   * @param aValidationResult
   *        The full validation results. Never <code>null</code>.
   * @throws Phase4Exception
   *         Implementation dependent
   */
  void onValidationSuccess (@NonNull ValidationResultList aValidationResult) throws Phase4Exception;

  /**
   * Invoked, if at least one validation error is present.
   *
   * @param aValidationResult
   *        The full validation results. Never <code>null</code>.
   * @throws Phase4Exception
   *         Implementation dependent
   */
  void onValidationErrors (@NonNull ValidationResultList aValidationResult) throws Phase4Exception;
}
