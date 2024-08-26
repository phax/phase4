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
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.builder.IBuilder;
import com.helger.commons.error.level.IErrorLevel;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.ebms3header.Ebms3Description;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.model.message.MessageHelperMethods;

/**
 * Builder class for {@link Ebms3Error}
 *
 * @author Philip Helger
 * @since 2.6.0
 */
@NotThreadSafe
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

  /**
   * Create an empty builder.
   */
  public Ebms3ErrorBuilder ()
  {}

  /**
   * Create a new builder setting {@link #description(Ebms3Description)},
   * {@link #category(EEbmsErrorCategory)}, {@link #errorCode(String)},
   * {@link #severity(EEbmsErrorSeverity)} and {@link #shortDescription(String)}
   *
   * @param aError
   *        The source error. May not be <code>null</code>.
   * @param aContentLocale
   *        The locale to be used to resolve error texts.
   */
  public Ebms3ErrorBuilder (@Nonnull final IEbmsError aError, @Nonnull final Locale aContentLocale)
  {
    // Default to shortDescription if none provided
    description (StringHelper.getNotNull (aError.getDescription ().getDisplayText (aContentLocale),
                                          aError.getShortDescription ()), aContentLocale);
    category (aError.getCategory ());
    errorCode (aError.getErrorCode ());
    severity (aError.getSeverity ());
    shortDescription (aError.getShortDescription ());
  }

  @Nonnull
  public Ebms3ErrorBuilder description (@Nullable final String s, @Nullable final Locale aLocale)
  {
    return description (StringHelper.hasNoText (s) ? null : MessageHelperMethods.createEbms3Description (aLocale == null
                                                                                                                         ? Locale.US
                                                                                                                         : aLocale,
                                                                                                         s));
  }

  @Nonnull
  public Ebms3ErrorBuilder description (@Nullable final Ebms3Description a)
  {
    m_aDescription = a;
    return this;
  }

  @Nonnull
  public Ebms3ErrorBuilder errorDetail (@Nullable final String s, @Nullable final Throwable t)
  {
    // Be able to allow disabling sending stack traces (see #225)
    final Throwable aLogT = AS4Configuration.isIncludeStackTraceInErrorMessages () ? t : null;
    return errorDetail (StringHelper.getConcatenatedOnDemand (s,
                                                              ": ",
                                                              aLogT == null ? "" : "Technical details: " +
                                                                                   StringHelper.getConcatenatedOnDemand (aLogT.getClass ()
                                                                                                                              .getName (),
                                                                                                                         " - ",
                                                                                                                         aLogT.getMessage ())));
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
  public Ebms3ErrorBuilder severity (@Nullable final IErrorLevel a)
  {
    return severity (EEbmsErrorSeverity.getFromErrorLevelOrNull (a));
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

    // This OPTIONAL element provides a narrative description of the error in
    // the language defined by the xml:lang attribute. The content of this
    // element is left to implementation-specific decisions.
    aEbms3Error.setDescription (m_aDescription);

    // This OPTIONAL element provides additional details about the context in
    // which the error occurred. For example, it may be an exception trace.
    aEbms3Error.setErrorDetail (m_sErrorDetail);

    if (m_eCategory != null)
    {
      // This OPTIONAL attribute identifies the type of error related to a
      // particular origin. For example: Content, Packaging, UnPackaging,
      // Communication, InternalProcess.
      aEbms3Error.setCategory (m_eCategory.getDisplayName ());
    }

    // This OPTIONAL attribute indicates the MessageId of the message in error,
    // for which this error is raised.
    aEbms3Error.setRefToMessageInError (m_sRefToMessageInError);

    // This REQUIRED attribute is a unique identifier for the type of error.
    aEbms3Error.setErrorCode (m_sErrorCode);

    // This OPTIONAL attribute identifies the functional module within which the
    // error occurred. This module could be the the ebMS Module, the Reliability
    // Module, or the Security Module. Possible values for this attribute
    // include "ebMS", "reliability", and "security". The use of other modules,
    // and thus their corresponding @origin values, may be specified elsewhere,
    // such as in a forthcoming Part 2 of this specification.
    aEbms3Error.setOrigin (m_sOrigin);

    // This REQUIRED attribute indicates the severity of the error. Valid values
    // are: warning, failure.
    aEbms3Error.setSeverity (m_eSeverity.getSeverity ());

    // This OPTIONAL attribute provides a short description of the error that
    // can be reported in a log, in order to facilitate readability.
    aEbms3Error.setShortDescription (m_sShortDescription);

    return aEbms3Error;
  }
}
