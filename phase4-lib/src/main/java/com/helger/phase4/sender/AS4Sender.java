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
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * This class contains all the settings necessary to send AS4 messages using the
 * builder pattern. See <code>Builder.sendMessage</code> and
 * <code>Builder.sendMessage</code> as the main methods to trigger the sending,
 * with all potential customization.<br>
 * Please note that this sender DOES NOT apply any profile specific settings.
 *
 * @author Philip Helger
 * @since 0.10.0
 */
@Immutable
public final class AS4Sender
{
  private AS4Sender ()
  {}

  /**
   * @return Create a new Builder for generic AS4 User Messages. Never
   *         <code>null</code>.
   */
  @Nonnull
  public static BuilderUserMessage builderUserMessage ()
  {
    return new BuilderUserMessage ();
  }

  /**
   * @return Create a new Builder for generic AS4 Pull Requests. Never
   *         <code>null</code>.
   * @since 0.12.0
   */
  @Nonnull
  public static BuilderPullRequest builderPullRequest ()
  {
    return new BuilderPullRequest ();
  }

  /**
   * This sending builder enforces the creation of a MIME message by putting the
   * payload as a MIME part.
   *
   * @author Philip Helger
   */
  @NotThreadSafe
  public static class BuilderUserMessage extends AbstractAS4UserMessageBuilderMIMEPayload <BuilderUserMessage>
  {
    /**
     * Create a new builder, with some fields already set as outlined in
     * {@link AbstractAS4UserMessageBuilderMIMEPayload#AbstractAS4UserMessageBuilderMIMEPayload()}
     */
    public BuilderUserMessage ()
    {
      // Use no specific AS4 profile ID
    }
  }

  /**
   * The default PullRequest builder.
   *
   * @author Philip Helger
   * @since 0.12.0
   */
  @NotThreadSafe
  public static class BuilderPullRequest extends AbstractAS4PullRequestBuilder <BuilderPullRequest>
  {
    /**
     * Create a new builder, with the some fields already set as outlined in
     * {@link AbstractAS4PullRequestBuilder#AbstractAS4PullRequestBuilder()}
     */
    public BuilderPullRequest ()
    {}
  }
}
