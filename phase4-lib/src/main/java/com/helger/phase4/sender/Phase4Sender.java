/**
 * Copyright (C) 2020 Philip Helger (www.helger.com)
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
import javax.annotation.concurrent.Immutable;

/**
 * This class contains all the settings necessary to send AS4 messages using the
 * builder pattern. See <code>Builder.sendMessage</code> as the main method to
 * trigger the sending, with all potential customization.
 *
 * @author Philip Helger
 * @since 0.10.0
 */
@Immutable
public final class Phase4Sender
{
  private Phase4Sender ()
  {}

  /**
   * @return Create a new Builder for generic AS4 user messages. Never
   *         <code>null</code>.
   */
  @Nonnull
  public static BuilderUserMessage builderUserMessage ()
  {
    return new BuilderUserMessage ();
  }

  /**
   * This sending builder enforces the creation of a MIME message by putting the
   * payload as a MIME part.
   *
   * @author Philip Helger
   */
  public static class BuilderUserMessage extends AbstractAS4UserMessageBuilderMIMEPayload <BuilderUserMessage>
  {
    /**
     * Create a new builder, with the some fields already set as outlined in
     * {@link AbstractAS4UserMessageBuilder#AbstractPhase4SenderBuilder()}
     */
    public BuilderUserMessage ()
    {}
  }
}
