/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.error.list.ErrorList;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.v3.ChangeV3;

import java.security.cert.X509Certificate;

/**
 * Generic AS4 profile validator
 *
 * @author bayerlma
 * @author Philip Helger
 */
public interface IAS4ProfileValidator
{
  /**
   * Validation method
   *
   * @param aPMode
   *        The PMode to be validated. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   */
  @ChangeV3 ("add parameter if for UserMessage or SignalMessage")
  default void validatePMode (@Nonnull final IPMode aPMode, @Nonnull final ErrorList aErrorList)
  {}

  /**
   * Validation method
   *
   * @param aUserMsg
   *        The message to use for comparison. May not be <code>null</code>.
   * @param aSigCert
   *        The signature certificate used to sign the message. Can be <code>null</code>.
   * @param aMessageMetadata
   *        Metadata of the message containing the TLS client certificate. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   */
  default void validateInitiatorIdentity(@Nonnull final Ebms3UserMessage aUserMsg,
                                         @Nullable X509Certificate aSigCert,
                                         @Nonnull IAS4IncomingMessageMetadata aMessageMetadata,
                                         @Nonnull final ErrorList aErrorList)
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
