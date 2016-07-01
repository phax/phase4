package com.helger.as4lib.error;

import com.helger.as4lib.ebms3header.Ebms3Description;

public enum EEbmsErrors implements IEbmsError
{
  // ebMs Processing Errors - from OASIS ebXML Messaging Service
  EBMS_VALUE_NOT_RECOGNIZED ("EBMS:0001", EErrorSeverity.FAILURE.getSeverity (), "ValueNotRecognized", EErrorText.VALUE_NOT_RECOGNIZED.getErrorText (), EErrorCategory.CONTENT.getsContent (), "", "", null),
  EBMS_FEATURE_NOT_SUPPORTED ("EBMS:0002", EErrorSeverity.WARNING.getSeverity (), "FeatureNotSupported", EErrorText.FEATURE_NOT_SUPPORTED.getErrorText (), EErrorCategory.CONTENT.getsContent (), "", "", null),
  EBMS_VALUE_INCONSISTENT ("EBMS:0003", EErrorSeverity.FAILURE.getSeverity (), "ValueInConsistent", EErrorText.VALUE_INCONSISTENT.getErrorText (), EErrorCategory.CONTENT.getsContent (), "", "", null),
  EBMS_OTHER ("EBMS:0004", EErrorSeverity.FAILURE.getSeverity (), "Other", EErrorText.OTHER.getErrorText (), EErrorCategory.CONTENT.getsContent (), "", "", null),
  EBMS_CONNECTION_FAIlURE ("EBMS:0005", EErrorSeverity.WARNING.getSeverity (), "ConnectionFailure", EErrorText.CONNECTION_FAIlURE.getErrorText (), EErrorCategory.COMMUNICATION.getsContent (), "", "", null),
  EBMS_EMPTY_MESSAGE_PARTITION_CHANNEL ("EBMS:0006", EErrorSeverity.FAILURE.getSeverity (), "EmptyMessagePartitionChannel", EErrorText.EMPTY_MESSAGE_PARTITION_CHANNEL.getErrorText (), EErrorCategory.COMMUNICATION.getsContent (), "", "", null),
  EBMS_MIME_INCONSISTENCY ("EBMS:0007", EErrorSeverity.FAILURE.getSeverity (), "MimeInconsistency", EErrorText.MIME_INCONSISTENCY.getErrorText (), EErrorCategory.UNPACKAGING.getsContent (), "", "", null),
  EBMS_FEATURE_NOT_SUPPORTED_INCONSISTENT ("EBMS:0008", EErrorSeverity.FAILURE.getSeverity (), "FeatureNotSupportedInconsistency", EErrorText.FEATURE_NOT_SUPPORTED_INCONSISTENT.getErrorText (), EErrorCategory.UNPACKAGING.getsContent (), "", "", null),
  EBMS_INVALID_HEADER ("EBMS:0009", EErrorSeverity.FAILURE.getSeverity (), "InvalidHeader", EErrorText.INVALID_HEADER.getErrorText (), EErrorCategory.UNPACKAGING.getsContent (), "", "", null),
  EBMS_PROCESSING_MODE_MISMATCH ("EBMS:0010", EErrorSeverity.FAILURE.getSeverity (), "ProcessingModeMismatch", EErrorText.PROCESSING_MODE_MISMATCH.getErrorText (), EErrorCategory.PROCESSING.getsContent (), "", "", null),
  EBMS_EXTERNAL_PAYLOAD_ERROR ("EBMS:0011", EErrorSeverity.FAILURE.getSeverity (), "ExternalPayloadError", EErrorText.EXTERNAL_PAYLOAD_ERROR.getErrorText (), EErrorCategory.PROCESSING.getsContent (), "", "", null),
  // Security Processing Errors - from OASIS ebXML Messaging Service
  EBMS_FAILED_AUTHENTICATION ("EBMS:0101", EErrorSeverity.FAILURE.getSeverity (), "FailedAuthentication", EErrorText.FAILED_AUTHENTICATION.getErrorText (), EErrorCategory.PROCESSING.getsContent (), "", "", null),
  EBMS_FAILED_DECRYPTION ("EBMS:0102", EErrorSeverity.FAILURE.getSeverity (), "FailedDecyrption", EErrorText.FAILED_DECRYPTION.getErrorText (), EErrorCategory.PROCESSING.getsContent (), "", "", null),
  EBMS_POLICY_NONCOMPLIANCE ("EBMS:0103", EErrorSeverity.FAILURE.getSeverity (), "PolicyNoncompliance", EErrorText.POLICY_NONCOMPLIANCE.getErrorText (), EErrorCategory.PROCESSING.getsContent (), "", "", null),
  // Reliable Messaging Errors - from OASIS ebXML Messaging Service
  EBMS_DYSFUNCTIONAL_RELIABILITY ("EBMS:0201", EErrorSeverity.FAILURE.getSeverity (), "DysfunctionalReliability", EErrorText.DYSFUNCTIONAL_RELIABILITY.getErrorText (), EErrorCategory.PROCESSING.getsContent (), "", "", null),
  EBMS_DELIVERY_FAILURE ("EBMS:0202", EErrorSeverity.FAILURE.getSeverity (), "DeliveryFailure", EErrorText.DELIVERY_FAILURE.getErrorText (), EErrorCategory.COMMUNICATION.getsContent (), "", "", null),
  // Additional Feature Errors - from AS4 Profile of ebMs 3.0 Version 1.0
  EBMS_MISSING_RECEIPT ("EBMS:0301", EErrorSeverity.FAILURE.getSeverity (), "MissingReceipt", EErrorText.MISSING_RECEIPT.getErrorText (), EErrorCategory.COMMUNICATION.getsContent (), "", "", null),
  EBMS_INVALID_RECEIPT ("EBMS:0302", EErrorSeverity.FAILURE.getSeverity (), "InvalidReceipt", EErrorText.INVALID_RECEIPT.getErrorText (), EErrorCategory.COMMUNICATION.getsContent (), "", "", null),
  EBMS_DECOMPRESSION_FAILURE ("EBMS:0303", EErrorSeverity.FAILURE.getSeverity (), "DecompressionFailure", EErrorText.DECOMPRESSION_FAILURE.getErrorText (), EErrorCategory.COMMUNICATION.getsContent (), "", "", null);

  private String m_sErrorCode;
  private String m_sSeverity;
  private String m_sShortDescription;
  private String m_sErrorDetail;
  private String m_sCategory;
  private String m_sRefToMessageInError;
  private String m_sOrigin;
  private Ebms3Description m_aEbmsDescription;

  private EEbmsErrors (final String sErrorCode,
                       final String sSeverity,
                       final String sShortDescription,
                       final String sErrorDetail,
                       final String sCategory,
                       final String sRefToMessageInError,
                       final String sOrigin,
                       final Ebms3Description aEbmsDescription)
  {
    m_sErrorCode = sErrorCode;
    m_sSeverity = sSeverity;
    m_sShortDescription = sShortDescription;
    m_sErrorDetail = sErrorDetail;
    m_sCategory = sCategory;
    m_sRefToMessageInError = sRefToMessageInError;
    m_sOrigin = sOrigin;
    m_aEbmsDescription = aEbmsDescription;
  }

  public Ebms3Description getDescription ()
  {
    return m_aEbmsDescription;
  }

  public void setDescription (final Ebms3Description value)
  {
    m_aEbmsDescription = value;
  }

  public String getErrorDetail ()
  {
    return m_sErrorDetail;
  }

  public void setErrorDetail (final String value)
  {
    m_sErrorDetail = value;
  }

  public String getCategory ()
  {
    return m_sCategory;
  }

  public void setCategory (final String value)
  {
    m_sCategory = value;
  }

  public String getRefToMessageInError ()
  {
    return m_sRefToMessageInError;
  }

  public void setRefToMessageInError (final String value)
  {
    m_sRefToMessageInError = value;
  }

  public String getErrorCode ()
  {
    return m_sErrorCode;
  }

  public void setErrorCode (final String value)
  {
    m_sErrorCode = value;
  }

  public String getOrigin ()
  {
    return m_sOrigin;
  }

  public void setOrigin (final String value)
  {
    m_sOrigin = value;
  }

  public String getSeverity ()
  {
    return m_sSeverity;
  }

  public void setSeverity (final String value)
  {
    m_sSeverity = value;
  }

  public String getShortDescription ()
  {
    return m_sShortDescription;
  }

  public void setShortDescription (final String value)
  {
    m_sShortDescription = value;
  }

}
