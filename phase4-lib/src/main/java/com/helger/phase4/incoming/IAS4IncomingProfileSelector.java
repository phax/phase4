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
package com.helger.phase4.incoming;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Callback interface to determine the used AS4 profile of an incoming message.
 *
 * @author Philip Helger
 * @since 0.13.0
 */
public interface IAS4IncomingProfileSelector
{
  boolean DEFAULT_VALIDATE_AGAINST_PROFILE = true;

  /**
   * Try to determine the AS4 profile to be used for an incoming message. This
   * method is only called after the SOAP headers were processed successfully.
   *
   * @param aIncomingState
   *        The message state of processing. Never <code>null</code>.
   * @return The AS4 profile ID or <code>null</code> if none was found.
   */
  @Nullable
  String getAS4ProfileID (@Nonnull IAS4IncomingMessageState aIncomingState);

  /**
   * Configure if the profile validation rules should be applied or not. Usually
   * this is recommended, but there might be edge cases where it is good to be
   * able to deactivate them.
   *
   * @return <code>true</code> if profile validation should be performed,
   *         <code>false</code> if not. The default is <code>true</code>.
   * @since 0.13.1
   */
  boolean validateAgainstProfile ();
}
