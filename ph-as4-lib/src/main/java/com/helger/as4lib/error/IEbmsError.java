package com.helger.as4lib.error;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.ebms3header.Ebms3Description;
import com.helger.commons.text.display.IHasDisplayText;

public interface IEbmsError extends Serializable
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
  EErrorSeverity getSeverity ();

  /**
   * Gets the value of the shortDescription property.
   *
   * @return possible object is {@link String }
   */
  @Nonnull
  String getShortDescription ();

  /**
   * Gets the value of the errorDetail property.
   *
   * @return possible object is {@link String }
   */
  @Nonnull
  IHasDisplayText getErrorDetail ();

  /**
   * Gets the value of the category property.
   *
   * @return possible object is {@link EErrorCategory }
   */
  @Nonnull
  EErrorCategory getCategory ();

  /**
   * Gets the value of the refToMessageInError property.
   *
   * @return possible object is {@link String }
   */
  @Nullable
  String getRefToMessageInError ();

  /**
   * Gets the value of the origin property.
   *
   * @return possible object is {@link String }
   */
  @Nullable
  String getOrigin ();

  /**
   * Gets the value of the description property.
   *
   * @return possible object is {@link Ebms3Description }
   */
  @Nullable
  Ebms3Description getDescription ();
}
