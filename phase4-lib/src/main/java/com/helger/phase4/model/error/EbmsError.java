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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.text.display.IHasDisplayText;

/**
 * Generic implementation of {@link IEbmsError} to represent other errors
 * besides {@link EEbmsError}.
 *
 * @author Philip Helger
 * @since 0.13.1
 */
@Immutable
public class EbmsError implements IEbmsError
{
  private final String m_sErrorCode;
  private final EEbmsErrorSeverity m_eSeverity;
  private final String m_sShortDescription;
  private final IHasDisplayText m_aDescription;
  private final EEbmsErrorCategory m_eCategory;

  public EbmsError (@Nonnull final String sErrorCode,
                    @Nonnull final EEbmsErrorSeverity eSeverity,
                    @Nonnull final String sShortDescription,
                    @Nonnull final IHasDisplayText aDescription,
                    @Nonnull final EEbmsErrorCategory eCategory)
  {
    ValueEnforcer.notNull (sErrorCode, "ErrorCode");
    ValueEnforcer.notNull (eSeverity, "Severity");
    ValueEnforcer.notNull (sShortDescription, "ShortDescription");
    ValueEnforcer.notNull (aDescription, "Description");
    ValueEnforcer.notNull (eCategory, "Category");
    m_sErrorCode = sErrorCode;
    m_eSeverity = eSeverity;
    m_sShortDescription = sShortDescription;
    m_aDescription = aDescription;
    m_eCategory = eCategory;
  }

  @Nonnull
  public String getErrorCode ()
  {
    return m_sErrorCode;
  }

  @Nonnull
  public EEbmsErrorSeverity getSeverity ()
  {
    return m_eSeverity;
  }

  @Nonnull
  public String getShortDescription ()
  {
    return m_sShortDescription;
  }

  @Nonnull
  public IHasDisplayText getDescription ()
  {
    return m_aDescription;
  }

  @Nonnull
  public EEbmsErrorCategory getCategory ()
  {
    return m_eCategory;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ErrorCode", m_sErrorCode)
                                       .append ("Severity", m_eSeverity)
                                       .append ("ShortDescription", m_sShortDescription)
                                       .append ("Description", m_aDescription)
                                       .append ("Category", m_eCategory)
                                       .getToString ();
  }
}
