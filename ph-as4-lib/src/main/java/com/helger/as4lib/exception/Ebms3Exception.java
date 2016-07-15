package com.helger.as4lib.exception;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.error.EEbmsError;
import com.helger.commons.string.StringHelper;

public class Ebms3Exception extends Exception
{
  private final EEbmsError m_eError;
  private final String m_sAdditionalInformation;
  private final String m_sRefToMessageId;

  public Ebms3Exception (@Nonnull final EEbmsError eError,
                         @Nullable final String sAdditionalInformation,
                         @Nullable final String sRefToMessageId)
  {
    super (StringHelper.getImplodedNonEmpty (" - ",
                                             eError.getErrorCode (),
                                             eError.getShortDescription (),
                                             sAdditionalInformation));
    m_eError = eError;
    m_sAdditionalInformation = sAdditionalInformation;
    m_sRefToMessageId = sRefToMessageId;
  }

  @Nonnull
  public EEbmsError getError ()
  {
    return m_eError;
  }

  @Nullable
  public String getAdditionalInformation ()
  {
    return m_sAdditionalInformation;
  }

  @Nullable
  public String getRefToMessageID ()
  {
    return m_sRefToMessageId;
  }
}
