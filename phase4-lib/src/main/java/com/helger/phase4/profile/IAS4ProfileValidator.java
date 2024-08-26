/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.error.list.ErrorList;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.model.pmode.IPMode;

/**
 * Generic AS4 profile validator
 *
 * @author bayerlma
 * @author Philip Helger
 */
public interface IAS4ProfileValidator
{
  enum EAS4ProfileValidationMode
  {
    USER_MESSAGE,
    SIGNAL_MESSAGE;
  }

  /**
   * Validation a PMode
   *
   * @param aPMode
   *        The PMode to be validated. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   * @param eValidationMode
   *        The validation mode to use. May not be <code>null</code>. Since
   *        v3.0.0
   */
  default void validatePMode (@Nonnull final IPMode aPMode,
                              @Nonnull final ErrorList aErrorList,
                              @Nonnull final EAS4ProfileValidationMode eValidationMode)
  {}

  /**
   * Validation the initiator identity
   *
   * @param aUserMsg
   *        The message to use for comparison. May not be <code>null</code>.
   * @param aSigCert
   *        The signature certificate used to sign the message. Can be
   *        <code>null</code>.
   * @param aMessageMetadata
   *        Metadata of the message optionally containing the TLS client
   *        certificate used. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   * @since 2.5.0
   */
  default void validateInitiatorIdentity (@Nonnull final Ebms3UserMessage aUserMsg,
                                          @Nullable final X509Certificate aSigCert,
                                          @Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                          @Nonnull final ErrorList aErrorList)
  {}

  /**
   * Validation a UserMessage
   *
   * @param aUserMsg
   *        The message to be validated. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   */
  default void validateUserMessage (@Nonnull final Ebms3UserMessage aUserMsg, @Nonnull final ErrorList aErrorList)
  {}

  /**
   * Validation a SignalMessage
   *
   * @param aSignalMsg
   *        The message to be validated. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   */
  default void validateSignalMessage (@Nonnull final Ebms3SignalMessage aSignalMsg, @Nonnull final ErrorList aErrorList)
  {}
}
