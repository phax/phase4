/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as4lib.model.profile;

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.model.pmode.config.IPModeConfig;
import com.helger.commons.error.list.ErrorList;

/**
 * Generic profile validator
 *
 * @author bayerlma
 */
public interface IAS4ProfileValidator extends Serializable
{
  /**
   * Validation method
   *
   * @param aPModeConfig
   *        The PMode config to be validated. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   */
  default void validatePModeConfig (@Nonnull final IPModeConfig aPModeConfig, @Nonnull final ErrorList aErrorList)
  {}

  /**
   * Validation method
   *
   * @param aUserMsg
   *        The message to be validated. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   */
  default void validateUserMessage (@Nonnull final Ebms3UserMessage aUserMsg, @Nonnull final ErrorList aErrorList)
  {}

  /**
   * Validation method
   *
   * @param aSignalMsg
   *        The message to be validated. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   */
  default void validateSignalMessage (@Nonnull final Ebms3SignalMessage aSignalMsg, @Nonnull final ErrorList aErrorList)
  {}
}
