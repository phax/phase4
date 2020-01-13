/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import com.helger.bdve.result.ValidationResultList;

/**
 * The default implementation of {@link IPhase4PeppolValidatonResultHandler}.
 *
 * @author Philip Helger
 * @since 0.9.7
 */
public class Phase4PeppolValidatonResultHandler implements IPhase4PeppolValidatonResultHandler
{
  public void onValidationSuccess (@Nonnull final ValidationResultList aValidationResult)
  {
    // empty
  }

  public void onValidationErrors (@Nonnull final ValidationResultList aValidationResult) throws Phase4PeppolValidationException
  {
    throw new Phase4PeppolValidationException (aValidationResult);
  }
}
