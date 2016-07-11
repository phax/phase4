package com.helger.as4lib.error;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.ebms3header.Ebms3Description;
import com.helger.commons.text.display.IHasDisplayText;

public enum EEbmsError implements IEbmsError
{
  // ebMs Processing Errors - from OASIS ebXML Messaging Service
  EBMS_VALUE_NOT_RECOGNIZED ("EBMS:0001", EErrorSeverity.FAILURE, "ValueNotRecognized", EErrorText.VALUE_NOT_RECOGNIZED, EErrorCategory.CONTENT, "", "", null),
  EBMS_FEATURE_NOT_SUPPORTED ("EBMS:0002", EErrorSeverity.WARNING, "FeatureNotSupported", EErrorText.FEATURE_NOT_SUPPORTED, EErrorCategory.CONTENT, "", "", null),
  EBMS_VALUE_INCONSISTENT ("EBMS:0003", EErrorSeverity.FAILURE, "ValueInConsistent", EErrorText.VALUE_INCONSISTENT, EErrorCategory.CONTENT, "", "", null),
  EBMS_OTHER ("EBMS:0004", EErrorSeverity.FAILURE, "Other", EErrorText.OTHER, EErrorCategory.CONTENT, "", "", null),
  EBMS_CONNECTION_FAIlURE ("EBMS:0005", EErrorSeverity.WARNING, "ConnectionFailure", EErrorText.CONNECTION_FAIlURE, EErrorCategory.COMMUNICATION, "", "", null),
  EBMS_EMPTY_MESSAGE_PARTITION_CHANNEL ("EBMS:0006", EErrorSeverity.FAILURE, "EmptyMessagePartitionChannel", EErrorText.EMPTY_MESSAGE_PARTITION_CHANNEL, EErrorCategory.COMMUNICATION, "", "", null),
  EBMS_MIME_INCONSISTENCY ("EBMS:0007", EErrorSeverity.FAILURE, "MimeInconsistency", EErrorText.MIME_INCONSISTENCY, EErrorCategory.UNPACKAGING, "", "", null),
  EBMS_FEATURE_NOT_SUPPORTED_INCONSISTENT ("EBMS:0008", EErrorSeverity.FAILURE, "FeatureNotSupportedInconsistency", EErrorText.FEATURE_NOT_SUPPORTED_INCONSISTENT, EErrorCategory.UNPACKAGING, "", "", null),
  EBMS_INVALID_HEADER ("EBMS:0009", EErrorSeverity.FAILURE, "InvalidHeader", EErrorText.INVALID_HEADER, EErrorCategory.UNPACKAGING, "", "", null),
  EBMS_PROCESSING_MODE_MISMATCH ("EBMS:0010", EErrorSeverity.FAILURE, "ProcessingModeMismatch", EErrorText.PROCESSING_MODE_MISMATCH, EErrorCategory.PROCESSING, "", "", null),
  EBMS_EXTERNAL_PAYLOAD_ERROR ("EBMS:0011", EErrorSeverity.FAILURE, "ExternalPayloadError", EErrorText.EXTERNAL_PAYLOAD_ERROR, EErrorCategory.PROCESSING, "", "", null),
  // Security Processing Errors - from OASIS ebXML Messaging Service
  EBMS_FAILED_AUTHENTICATION ("EBMS:0101", EErrorSeverity.FAILURE, "FailedAuthentication", EErrorText.FAILED_AUTHENTICATION, EErrorCategory.PROCESSING, "", "", null),
  EBMS_FAILED_DECRYPTION ("EBMS:0102", EErrorSeverity.FAILURE, "FailedDecyrption", EErrorText.FAILED_DECRYPTION, EErrorCategory.PROCESSING, "", "", null),
  EBMS_POLICY_NONCOMPLIANCE ("EBMS:0103", EErrorSeverity.FAILURE, "PolicyNoncompliance", EErrorText.POLICY_NONCOMPLIANCE, EErrorCategory.PROCESSING, "", "", null),
  // Reliable Messaging Errors - from OASIS ebXML Messaging Service
  EBMS_DYSFUNCTIONAL_RELIABILITY ("EBMS:0201", EErrorSeverity.FAILURE, "DysfunctionalReliability", EErrorText.DYSFUNCTIONAL_RELIABILITY, EErrorCategory.PROCESSING, "", "", null),
  EBMS_DELIVERY_FAILURE ("EBMS:0202", EErrorSeverity.FAILURE, "DeliveryFailure", EErrorText.DELIVERY_FAILURE, EErrorCategory.COMMUNICATION, "", "", null),
  // Additional Feature Errors - from AS4 Profile of ebMs 3.0 Version 1.0
  EBMS_MISSING_RECEIPT ("EBMS:0301", EErrorSeverity.FAILURE, "MissingReceipt", EErrorText.MISSING_RECEIPT, EErrorCategory.COMMUNICATION, "", "", null),
  EBMS_INVALID_RECEIPT ("EBMS:0302", EErrorSeverity.FAILURE, "InvalidReceipt", EErrorText.INVALID_RECEIPT, EErrorCategory.COMMUNICATION, "", "", null),
  EBMS_DECOMPRESSION_FAILURE ("EBMS:0303", EErrorSeverity.FAILURE, "DecompressionFailure", EErrorText.DECOMPRESSION_FAILURE, EErrorCategory.COMMUNICATION, "", "", null);

  private final String m_sErrorCode;
  private final EErrorSeverity m_eSeverity;
  private final String m_sShortDescription;
  private final IHasDisplayText m_aErrorDetail;
  private final EErrorCategory m_eCategory;
  private final String m_sRefToMessageInError;
  private final String m_sOrigin;
  private final Ebms3Description m_aEbmsDescription;

  private EEbmsError (@Nonnull final String sErrorCode,
                       @Nonnull final EErrorSeverity eSeverity,
                       @Nonnull final String sShortDescription,
                       @Nonnull final IHasDisplayText aErrorDetail,
                       @Nonnull final EErrorCategory eCategory,
                       @Nullable final String sRefToMessageInError,
                       @Nullable final String sOrigin,
                       @Nullable final Ebms3Description aEbmsDescription)
  {
    m_sErrorCode = sErrorCode;
    m_eSeverity = eSeverity;
    m_sShortDescription = sShortDescription;
    m_aErrorDetail = aErrorDetail;
    m_eCategory = eCategory;
    m_sRefToMessageInError = sRefToMessageInError;
    m_sOrigin = sOrigin;
    m_aEbmsDescription = aEbmsDescription;
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

  @Nullable
  public String getRefToMessageInError ()
  {
    return m_sRefToMessageInError;
  }

  @Nullable
  public String getOrigin ()
  {
    return m_sOrigin;
  }

  @Nullable
  public Ebms3Description getDescription ()
  {
    return m_aEbmsDescription;
  }
}
