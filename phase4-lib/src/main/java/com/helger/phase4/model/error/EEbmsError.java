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
import javax.annotation.Nullable;

import com.helger.commons.error.IError;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.text.display.IHasDisplayText;

/**
 * Enumeration with all predefined EBMS errors based on the specs. Use
 * {@link IEbmsError} for a generic interface.
 *
 * @author Philip Helger
 */
public enum EEbmsError implements IEbmsError
{
  // ebMs Processing Errors - from OASIS ebXML Messaging Service
  EBMS_VALUE_NOT_RECOGNIZED ("EBMS:0001",
                             EEbmsErrorSeverity.FAILURE,
                             "ValueNotRecognized",
                             EEbmsErrorText.VALUE_NOT_RECOGNIZED,
                             EEbmsErrorCategory.CONTENT),
  EBMS_FEATURE_NOT_SUPPORTED ("EBMS:0002",
                              EEbmsErrorSeverity.WARNING,
                              "FeatureNotSupported",
                              EEbmsErrorText.FEATURE_NOT_SUPPORTED,
                              EEbmsErrorCategory.CONTENT),
  EBMS_VALUE_INCONSISTENT ("EBMS:0003",
                           EEbmsErrorSeverity.FAILURE,
                           "ValueInConsistent",
                           EEbmsErrorText.VALUE_INCONSISTENT,
                           EEbmsErrorCategory.CONTENT),
  EBMS_OTHER ("EBMS:0004", EEbmsErrorSeverity.FAILURE, "Other", EEbmsErrorText.OTHER, EEbmsErrorCategory.CONTENT),
  EBMS_CONNECTION_FAILURE ("EBMS:0005",
                           EEbmsErrorSeverity.WARNING,
                           "ConnectionFailure",
                           EEbmsErrorText.CONNECTION_FAILURE,
                           EEbmsErrorCategory.COMMUNICATION),
  EBMS_EMPTY_MESSAGE_PARTITION_CHANNEL ("EBMS:0006",
                                        EEbmsErrorSeverity.FAILURE,
                                        "EmptyMessagePartitionChannel",
                                        EEbmsErrorText.EMPTY_MESSAGE_PARTITION_CHANNEL,
                                        EEbmsErrorCategory.COMMUNICATION),
  EBMS_MIME_INCONSISTENCY ("EBMS:0007",
                           EEbmsErrorSeverity.FAILURE,
                           "MimeInconsistency",
                           EEbmsErrorText.MIME_INCONSISTENCY,
                           EEbmsErrorCategory.UNPACKAGING),
  EBMS_FEATURE_NOT_SUPPORTED_INCONSISTENCY ("EBMS:0008",
                                            EEbmsErrorSeverity.FAILURE,
                                            "FeatureNotSupportedInconsistency",
                                            EEbmsErrorText.FEATURE_NOT_SUPPORTED_INCONSISTENT,
                                            EEbmsErrorCategory.UNPACKAGING),
  EBMS_INVALID_HEADER ("EBMS:0009",
                       EEbmsErrorSeverity.FAILURE,
                       "InvalidHeader",
                       EEbmsErrorText.INVALID_HEADER,
                       EEbmsErrorCategory.UNPACKAGING),
  EBMS_PROCESSING_MODE_MISMATCH ("EBMS:0010",
                                 EEbmsErrorSeverity.FAILURE,
                                 "ProcessingModeMismatch",
                                 EEbmsErrorText.PROCESSING_MODE_MISMATCH,
                                 EEbmsErrorCategory.PROCESSING),
  EBMS_EXTERNAL_PAYLOAD_ERROR ("EBMS:0011",
                               EEbmsErrorSeverity.FAILURE,
                               "ExternalPayloadError",
                               EEbmsErrorText.EXTERNAL_PAYLOAD_ERROR,
                               EEbmsErrorCategory.PROCESSING),
  // Security Processing Errors - from OASIS ebXML Messaging Service
  EBMS_FAILED_AUTHENTICATION ("EBMS:0101",
                              EEbmsErrorSeverity.FAILURE,
                              "FailedAuthentication",
                              EEbmsErrorText.FAILED_AUTHENTICATION,
                              EEbmsErrorCategory.PROCESSING),
  EBMS_FAILED_DECRYPTION ("EBMS:0102",
                          EEbmsErrorSeverity.FAILURE,
                          "FailedDecryption",
                          EEbmsErrorText.FAILED_DECRYPTION,
                          EEbmsErrorCategory.PROCESSING),
  EBMS_POLICY_NONCOMPLIANCE ("EBMS:0103",
                             EEbmsErrorSeverity.FAILURE,
                             "PolicyNoncompliance",
                             EEbmsErrorText.POLICY_NONCOMPLIANCE,
                             EEbmsErrorCategory.PROCESSING),
  // Reliable Messaging Errors - from OASIS ebXML Messaging Service
  EBMS_DYSFUNCTIONAL_RELIABILITY ("EBMS:0201",
                                  EEbmsErrorSeverity.FAILURE,
                                  "DysfunctionalReliability",
                                  EEbmsErrorText.DYSFUNCTIONAL_RELIABILITY,
                                  EEbmsErrorCategory.PROCESSING),
  EBMS_DELIVERY_FAILURE ("EBMS:0202",
                         EEbmsErrorSeverity.FAILURE,
                         "DeliveryFailure",
                         EEbmsErrorText.DELIVERY_FAILURE,
                         EEbmsErrorCategory.COMMUNICATION),
  // Additional Feature Errors - from AS4 Profile of ebMs 3.0 Version 1.0
  EBMS_MISSING_RECEIPT ("EBMS:0301",
                        EEbmsErrorSeverity.FAILURE,
                        "MissingReceipt",
                        EEbmsErrorText.MISSING_RECEIPT,
                        EEbmsErrorCategory.COMMUNICATION),
  EBMS_INVALID_RECEIPT ("EBMS:0302",
                        EEbmsErrorSeverity.FAILURE,
                        "InvalidReceipt",
                        EEbmsErrorText.INVALID_RECEIPT,
                        EEbmsErrorCategory.COMMUNICATION),
  EBMS_DECOMPRESSION_FAILURE ("EBMS:0303",
                              EEbmsErrorSeverity.FAILURE,
                              "DecompressionFailure",
                              EEbmsErrorText.DECOMPRESSION_FAILURE,
                              EEbmsErrorCategory.COMMUNICATION);

  private final String m_sErrorCode;
  private final EEbmsErrorSeverity m_eSeverity;
  private final String m_sShortDescription;
  private final IHasDisplayText m_aDescription;
  private final EEbmsErrorCategory m_eCategory;

  EEbmsError (@Nonnull final String sErrorCode,
              @Nonnull final EEbmsErrorSeverity eSeverity,
              @Nonnull final String sShortDescription,
              @Nonnull final IHasDisplayText aDescription,
              @Nonnull final EEbmsErrorCategory eCategory)
  {
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

  @Nullable
  public static EEbmsError getFromErrorCodeOrNull (@Nullable final String sErrorCode)
  {
    if (StringHelper.hasNoText (sErrorCode))
      return null;
    return EnumHelper.findFirst (EEbmsError.class, x -> x.getErrorCode ().equals (sErrorCode));
  }

  @Nullable
  public static EEbmsError getFromIErrorOrNull (@Nullable final IError aError)
  {
    if (aError == null)
      return null;
    return getFromErrorCodeOrNull (aError.getErrorID ());
  }
}
