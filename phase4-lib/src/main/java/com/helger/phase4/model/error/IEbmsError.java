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
package com.helger.phase4.model.error;

import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.text.display.IHasDisplayText;

/**
 * Base interface for a single EBMS error
 *
 * @author Philip Helger
 */
public interface IEbmsError
{
  /**
   * Gets the value of the errorCode property.
   *
   * @return possible object is {@link String }
   */
  @Nonnull
  String getErrorCode ();

  /**
   * Gets the value of the severity property.
   *
   * @return possible object is {@link String }
   */
  @Nonnull
  EEbmsErrorSeverity getSeverity ();

  /**
   * Gets the value of the shortDescription property.
   *
   * @return Short description. Never <code>null</code>.
   */
  @Nonnull
  String getShortDescription ();

  /**
   * Gets the value of the description property.
   *
   * @return The multilingual description.
   * @since 2.6.0
   */
  @Nonnull
  IHasDisplayText getDescription ();

  /**
   * Gets the value of the category property.
   *
   * @return possible object is {@link EEbmsErrorCategory }
   */
  @Nonnull
  EEbmsErrorCategory getCategory ();

  /**
   * Create a new {@link Ebms3ErrorBuilder} with the information of this
   * element. Sets description, category, errorCode, severity and
   * shortDescription.
   *
   * @param aContentLocale
   *        Content locale to use. May not be <code>null</code>.
   * @return Never <code>null</code>.
   * @since 2.6.0
   */
  @Nonnull
  default Ebms3ErrorBuilder errorBuilder (@Nonnull final Locale aContentLocale)
  {
    return new Ebms3ErrorBuilder (this, aContentLocale);
  }
}
