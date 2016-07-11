package com.helger.as4lib.error;

import javax.annotation.Nullable;

import com.helger.as4lib.ebms3header.Ebms3Description;

public interface IMutableEbmsError extends IEbmsError
{
  /**
   * Sets the value of the description property.
   *
   * @param value
   *        allowed object is {@link Ebms3Description }
   */
  void setDescription (@Nullable Ebms3Description value);

  /**
   * Sets the value of the errorDetail property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setErrorDetail (@Nullable String value);

  /**
   * Sets the value of the category property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setCategory (@Nullable String value);

  /**
   * Sets the value of the refToMessageInError property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setRefToMessageInError (@Nullable String value);

  /**
   * Sets the value of the errorCode property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setErrorCode (@Nullable String value);

  /**
   * Sets the value of the origin property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setOrigin (@Nullable String value);

  /**
   * Sets the value of the severity property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setSeverity (@Nullable String value);

  /**
   * Sets the value of the shortDescription property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setShortDescription (@Nullable String value);
}
