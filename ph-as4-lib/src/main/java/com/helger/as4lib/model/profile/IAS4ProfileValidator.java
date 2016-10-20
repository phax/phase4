package com.helger.as4lib.model.profile;

import javax.annotation.Nonnull;

import com.helger.as4lib.model.pmode.IPMode;
import com.helger.commons.error.list.ErrorList;

public interface IAS4ProfileValidator
{
  void validatePMode (@Nonnull IPMode aPMode, @Nonnull ErrorList aErrorList);

}
