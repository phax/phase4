package com.helger.as4lib.error;

import java.io.Serializable;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.ebms3header.Ebms3Description;
import com.helger.as4lib.ebms3header.Ebms3Error;
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

  @Nonnull
  default Ebms3Error getAsEbms3Error (@Nonnull final Locale aContentLocale)
  {
    return getAsEbms3Error (aContentLocale, "", "", null);
  }

  @Nonnull
  default Ebms3Error getAsEbms3Error (@Nonnull final Locale aContentLocale,
                                      @Nullable final String sRefToMessageInError,
                                      @Nullable final String sOrigin,
                                      @Nullable final Ebms3Description aEbmsDescription)
  {
    final Ebms3Error aEbms3Error = new Ebms3Error ();
    aEbms3Error.setDescription (aEbmsDescription);
    aEbms3Error.setErrorDetail (getErrorDetail ().getDisplayText (aContentLocale));
    aEbms3Error.setErrorCode (getErrorCode ());
    aEbms3Error.setSeverity (getSeverity ().getSeverity ());
    aEbms3Error.setShortDescription (getShortDescription ());
    aEbms3Error.setCategory (getCategory ().getContent ());
    aEbms3Error.setRefToMessageInError (sRefToMessageInError);
    aEbms3Error.setOrigin (sOrigin);
    return aEbms3Error;
  }
}
