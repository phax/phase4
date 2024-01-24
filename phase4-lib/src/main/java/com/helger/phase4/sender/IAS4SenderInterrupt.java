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
package com.helger.phase4.sender;

import javax.annotation.Nonnull;

import com.helger.commons.state.EContinue;

/**
 * A specific helper interface with the sole purpose to be able to interrupt the
 * sending of a document after all checks are performed.
 *
 * @author Philip Helger
 * @since 0.13.0
 */
public interface IAS4SenderInterrupt
{
  /**
   * @return {@link EContinue#CONTINUE} to send the message,
   *         {@link EContinue#BREAK} to not send the message. May not be
   *         <code>null</code>.
   */
  @Nonnull
  EContinue canSendDocument ();
}
