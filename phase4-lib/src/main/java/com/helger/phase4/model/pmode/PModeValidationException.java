package com.helger.phase4.model.pmode;

import javax.annotation.Nonnull;

/**
 * A special exception thrown in PMode validation.
 * 
 * @author Philip Helger
 * @since 0.9.6
 */
public class PModeValidationException extends Exception
{
  public PModeValidationException (@Nonnull final String sMsg)
  {
    super (sMsg);
  }
}
