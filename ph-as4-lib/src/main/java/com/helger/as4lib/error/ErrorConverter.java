package com.helger.as4lib.error;

import java.util.Locale;

import com.helger.as4lib.ebms3header.Ebms3Error;

/**
 * Converts EEbmsError into Ebms3Error to send it. Since EEbmsError is an enum
 * and not the xml - specified type we need to convert.
 *
 * @author bayerlma
 */
public class ErrorConverter
{
  // TODO Switch locale to dynamic version
  // TODO add Ebms3Description
  public static Ebms3Error convertEnumToEbms3Error (final EEbmsError aEbmsError)
  {
    final Ebms3Error aEbms3Error = new Ebms3Error ();
    aEbms3Error.setErrorCode (aEbmsError.getErrorCode ());
    aEbms3Error.setSeverity (aEbmsError.getSeverity ().getSeverity ());
    aEbms3Error.setShortDescription (aEbmsError.getShortDescription ());
    aEbms3Error.setErrorDetail (aEbmsError.getErrorDetail ().getDisplayText (Locale.getDefault ()));
    aEbms3Error.setCategory (aEbmsError.getCategory ().getContent ());
    aEbms3Error.setRefToMessageInError (null);
    aEbms3Error.setOrigin (null);
    return aEbms3Error;
  }

}
