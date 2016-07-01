package com.helger.as4lib.error;

import javax.annotation.Nullable;

import com.helger.as4lib.ebms3header.Ebms3Description;

public interface IEbmsError
{
  /**
   * Gets the value of the description property.
   *
   * @return possible object is {@link Ebms3Description }
   */
  @Nullable
  public Ebms3Description getDescription ();

  /**
   * Sets the value of the description property.
   *
   * @param value
   *        allowed object is {@link Ebms3Description }
   */
  public void setDescription (@Nullable final Ebms3Description value);

  /**
   * Gets the value of the errorDetail property.
   *
   * @return possible object is {@link String }
   */
  @Nullable
  public String getErrorDetail ();

  /**
   * Sets the value of the errorDetail property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  public void setErrorDetail (@Nullable final String value);

  /**
   * Gets the value of the category property.
   *
   * @return possible object is {@link String }
   */
  @Nullable
  public String getCategory ();

  /**
   * Sets the value of the category property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  public void setCategory (@Nullable final String value);

  /**
   * Gets the value of the refToMessageInError property.
   *
   * @return possible object is {@link String }
   */
  @Nullable
  public String getRefToMessageInError ();

  /**
   * Sets the value of the refToMessageInError property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  public void setRefToMessageInError (@Nullable final String value);

  /**
   * Gets the value of the errorCode property.
   *
   * @return possible object is {@link String }
   */
  @Nullable
  public String getErrorCode ();

  /**
   * Sets the value of the errorCode property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  public void setErrorCode (@Nullable final String value);

  /**
   * Gets the value of the origin property.
   *
   * @return possible object is {@link String }
   */
  @Nullable
  public String getOrigin ();

  /**
   * Sets the value of the origin property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  public void setOrigin (@Nullable final String value);

  /**
   * Gets the value of the severity property.
   *
   * @return possible object is {@link String }
   */
  @Nullable
  public String getSeverity ();

  /**
   * Sets the value of the severity property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  public void setSeverity (@Nullable final String value);

  /**
   * Gets the value of the shortDescription property.
   *
   * @return possible object is {@link String }
   */
  @Nullable
  public String getShortDescription ();

  /**
   * Sets the value of the shortDescription property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  public void setShortDescription (@Nullable final String value);
}
