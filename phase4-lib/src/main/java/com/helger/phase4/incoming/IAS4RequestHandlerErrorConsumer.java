/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.incoming;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.model.message.AS4ErrorMessage;

import jakarta.annotation.Nonnull;

/**
 * A callback interface to be notified about returned errors.
 *
 * @author Philip Helger
 * @since 0.9.10
 */
public interface IAS4RequestHandlerErrorConsumer
{
  /**
   * Invoked when an AS4 error message is created. This doesn't mean that the response message is
   * also sent back - that can be configured in the P-Mode.
   *
   * @param aIncomingState
   *        The current message processing state. Never <code>null</code>.
   * @param aEbmsErrors
   *        The list of errors that occurred. Neither <code>null</code> nor empty. Never modify that
   *        list.
   * @param aAS4ErrorMsg
   *        The filled AS4 error message to be returned. Don't touch. Never <code>null</code>.
   */
  void onAS4ErrorMessage (@Nonnull IAS4IncomingMessageState aIncomingState,
                          @Nonnull @Nonempty ICommonsList <Ebms3Error> aEbmsErrors,
                          @Nonnull AS4ErrorMessage aAS4ErrorMsg);
}
