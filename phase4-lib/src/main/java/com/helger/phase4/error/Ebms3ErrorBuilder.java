/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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
package com.helger.phase4.error;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.builder.IBuilder;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.ebms3header.Ebms3Description;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.messaging.domain.MessageHelperMethods;

/**
 * Builder class for {@link Ebms3Error}
 *
 * @author Philip Helger
 * @since 2.6.0
 */
public class Ebms3ErrorBuilder implements IBuilder <Ebms3Error>
{
  private Ebms3Description m_aDescription;
  private String m_sErrorDetail;
  private EEbmsErrorCategory m_eCategory;
  private String m_sRefToMessageInError;
  private String m_sErrorCode;
  private String m_sOrigin;
  private EEbmsErrorSeverity m_eSeverity;
  private String m_sShortDescription;

  public Ebms3ErrorBuilder ()
  {}

  @Nonnull
  public Ebms3ErrorBuilder description (@Nullable final String s, @Nullable final Locale aLocale)
  {
    return description (StringHelper.hasNoText (s) || aLocale == null ? null : MessageHelperMethods
                                                                                                   .createEbms3Description (aLocale,
                                                                                                                            s));
  }

  @Nonnull
  public Ebms3ErrorBuilder description (@Nullable final Ebms3Description a)
  {
    m_aDescription = a;
    return this;
  }

  @Nonnull
  public Ebms3ErrorBuilder errorDetail (@Nullable final String s)
  {
    m_sErrorDetail = s;
    return this;
  }

  @Nonnull
  public Ebms3ErrorBuilder category (@Nullable final EEbmsErrorCategory e)
  {
    m_eCategory = e;
    return this;
  }

  @Nonnull
  public Ebms3ErrorBuilder refToMessageInError (@Nullable final String s)
  {
    m_sRefToMessageInError = s;
    return this;
  }

  @Nonnull
  public Ebms3ErrorBuilder errorCode (@Nullable final String s)
  {
    m_sErrorCode = s;
    return this;
  }

  @Nonnull
  public Ebms3ErrorBuilder origin (@Nullable final String s)
  {
    m_sOrigin = s;
    return this;
  }

  @Nonnull
  public Ebms3ErrorBuilder severity (@Nullable final EEbmsErrorSeverity e)
  {
    m_eSeverity = e;
    return this;
  }

  @Nonnull
  public Ebms3ErrorBuilder shortDescription (@Nullable final String s)
  {
    m_sShortDescription = s;
    return this;
  }

  @Nonnull
  public Ebms3Error build ()
  {
    if (m_eSeverity == null)
      throw new IllegalStateException ("Severity is required");
    if (StringHelper.hasNoText (m_sErrorCode))
      throw new IllegalStateException ("Error Code is required");

    final Ebms3Error aEbms3Error = new Ebms3Error ();
    // Default to shortDescription if none provided
    aEbms3Error.setDescription (m_aDescription);
    aEbms3Error.setErrorDetail (m_sErrorDetail);
    if (m_eCategory != null)
      aEbms3Error.setCategory (m_eCategory.getDisplayName ());
    aEbms3Error.setRefToMessageInError (m_sRefToMessageInError);
    aEbms3Error.setErrorCode (m_sErrorCode);
    aEbms3Error.setOrigin (m_sOrigin);
    aEbms3Error.setSeverity (m_eSeverity.getSeverity ());
    aEbms3Error.setShortDescription (m_sShortDescription);
    return aEbms3Error;
  }
}
