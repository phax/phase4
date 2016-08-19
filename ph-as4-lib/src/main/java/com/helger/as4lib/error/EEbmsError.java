package com.helger.as4lib.error;

import javax.annotation.Nonnull;

import com.helger.commons.text.display.IHasDisplayText;

public enum EEbmsError implements IEbmsError
{
  // ebMs Processing Errors - from OASIS ebXML Messaging Service
  EBMS_VALUE_NOT_RECOGNIZED ("EBMS:0001",
                             EErrorSeverity.FAILURE,
                             "ValueNotRecognized",
                             EErrorText.VALUE_NOT_RECOGNIZED,
                             EErrorCategory.CONTENT),
  EBMS_FEATURE_NOT_SUPPORTED ("EBMS:0002",
                              EErrorSeverity.WARNING,
                              "FeatureNotSupported",
                              EErrorText.FEATURE_NOT_SUPPORTED,
                              EErrorCategory.CONTENT),
  EBMS_VALUE_INCONSISTENT ("EBMS:0003",
                           EErrorSeverity.FAILURE,
                           "ValueInConsistent",
                           EErrorText.VALUE_INCONSISTENT,
                           EErrorCategory.CONTENT),
  EBMS_OTHER ("EBMS:0004", EErrorSeverity.FAILURE, "Other", EErrorText.OTHER, EErrorCategory.CONTENT),
  EBMS_CONNECTION_FAIlURE ("EBMS:0005",
                           EErrorSeverity.WARNING,
                           "ConnectionFailure",
                           EErrorText.CONNECTION_FAIlURE,
                           EErrorCategory.COMMUNICATION),
  EBMS_EMPTY_MESSAGE_PARTITION_CHANNEL ("EBMS:0006",
                                        EErrorSeverity.FAILURE,
                                        "EmptyMessagePartitionChannel",
                                        EErrorText.EMPTY_MESSAGE_PARTITION_CHANNEL,
                                        EErrorCategory.COMMUNICATION),
  EBMS_MIME_INCONSISTENCY ("EBMS:0007",
                           EErrorSeverity.FAILURE,
                           "MimeInconsistency",
                           EErrorText.MIME_INCONSISTENCY,
                           EErrorCategory.UNPACKAGING),
  EBMS_FEATURE_NOT_SUPPORTED_INCONSISTENT ("EBMS:0008",
                                           EErrorSeverity.FAILURE,
                                           "FeatureNotSupportedInconsistency",
                                           EErrorText.FEATURE_NOT_SUPPORTED_INCONSISTENT,
                                           EErrorCategory.UNPACKAGING),
  EBMS_INVALID_HEADER ("EBMS:0009",
                       EErrorSeverity.FAILURE,
                       "InvalidHeader",
                       EErrorText.INVALID_HEADER,
                       EErrorCategory.UNPACKAGING),
  EBMS_PROCESSING_MODE_MISMATCH ("EBMS:0010",
                                 EErrorSeverity.FAILURE,
                                 "ProcessingModeMismatch",
                                 EErrorText.PROCESSING_MODE_MISMATCH,
                                 EErrorCategory.PROCESSING),
  EBMS_EXTERNAL_PAYLOAD_ERROR ("EBMS:0011",
                               EErrorSeverity.FAILURE,
                               "ExternalPayloadError",
                               EErrorText.EXTERNAL_PAYLOAD_ERROR,
                               EErrorCategory.PROCESSING),
  // Security Processing Errors - from OASIS ebXML Messaging Service
  EBMS_FAILED_AUTHENTICATION ("EBMS:0101",
                              EErrorSeverity.FAILURE,
                              "FailedAuthentication",
                              EErrorText.FAILED_AUTHENTICATION,
                              EErrorCategory.PROCESSING),
  EBMS_FAILED_DECRYPTION ("EBMS:0102",
                          EErrorSeverity.FAILURE,
                          "FailedDecyrption",
                          EErrorText.FAILED_DECRYPTION,
                          EErrorCategory.PROCESSING),
  EBMS_POLICY_NONCOMPLIANCE ("EBMS:0103",
                             EErrorSeverity.FAILURE,
                             "PolicyNoncompliance",
                             EErrorText.POLICY_NONCOMPLIANCE,
                             EErrorCategory.PROCESSING),
  // Reliable Messaging Errors - from OASIS ebXML Messaging Service
  EBMS_DYSFUNCTIONAL_RELIABILITY ("EBMS:0201",
                                  EErrorSeverity.FAILURE,
                                  "DysfunctionalReliability",
                                  EErrorText.DYSFUNCTIONAL_RELIABILITY,
                                  EErrorCategory.PROCESSING),
  EBMS_DELIVERY_FAILURE ("EBMS:0202",
                         EErrorSeverity.FAILURE,
                         "DeliveryFailure",
                         EErrorText.DELIVERY_FAILURE,
                         EErrorCategory.COMMUNICATION),
  // Additional Feature Errors - from AS4 Profile of ebMs 3.0 Version 1.0
  EBMS_MISSING_RECEIPT ("EBMS:0301",
                        EErrorSeverity.FAILURE,
                        "MissingReceipt",
                        EErrorText.MISSING_RECEIPT,
                        EErrorCategory.COMMUNICATION),
  EBMS_INVALID_RECEIPT ("EBMS:0302",
                        EErrorSeverity.FAILURE,
                        "InvalidReceipt",
                        EErrorText.INVALID_RECEIPT,
                        EErrorCategory.COMMUNICATION),
  EBMS_DECOMPRESSION_FAILURE ("EBMS:0303",
                              EErrorSeverity.FAILURE,
                              "DecompressionFailure",
                              EErrorText.DECOMPRESSION_FAILURE,
                              EErrorCategory.COMMUNICATION);

  private final String m_sErrorCode;
  private final EErrorSeverity m_eSeverity;
  private final String m_sShortDescription;
  private final IHasDisplayText m_aErrorDetail;
  private final EErrorCategory m_eCategory;

  private EEbmsError (@Nonnull final String sErrorCode,
                      @Nonnull final EErrorSeverity eSeverity,
                      @Nonnull final String sShortDescription,
                      @Nonnull final IHasDisplayText aErrorDetail,
                      @Nonnull final EErrorCategory eCategory)
  {
    m_sErrorCode = sErrorCode;
    m_eSeverity = eSeverity;
    m_sShortDescription = sShortDescription;
    m_aErrorDetail = aErrorDetail;
    m_eCategory = eCategory;
  }

  @Nonnull
  public String getErrorCode ()
  {
    return m_sErrorCode;
  }

  @Nonnull
  public EErrorSeverity getSeverity ()
  {
    return m_eSeverity;
  }

  @Nonnull
  public String getShortDescription ()
  {
    return m_sShortDescription;
  }

  @Nonnull
  public IHasDisplayText getErrorDetail ()
  {
    return m_aErrorDetail;
  }

  @Nonnull
  public EErrorCategory getCategory ()
  {
    return m_eCategory;
  }
}
