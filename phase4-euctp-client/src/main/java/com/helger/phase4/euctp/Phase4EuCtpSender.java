/*
 * Copyright (C) 2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.euctp;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * This class contains all the specifics to send AS4 messages with the euctp
 * profile. See <code>sendAS4Message</code> as the main method to trigger the
 * sending, with all potential customization.
 *
 * @author Ulrik Stehling
 */
@Immutable
public final class Phase4EuCtpSender
{
  private Phase4EuCtpSender ()
  {}

  /**
   * @return Create a new Builder for AS4 User Messages if the payload is
   *         present. Never <code>null</code>.
   */
  @Nonnull
  public static EuCtpUserMessageBuilder builderUserMessage ()
  {
    return new EuCtpUserMessageBuilder ();
  }

  /**
   * @return Create a new Builder for AS4 Pull Requests. Never
   *         <code>null</code>.
   * @since 3.0.0
   */
  @Nonnull
  public static EuCtpPullRequestBuilder builderPullRequest ()
  {
    return new EuCtpPullRequestBuilder ();
  }

  /**
   * The builder class for sending AS4 User Messages using EuCTP profile
   * specifics. Use {@link #sendMessage()} or
   * {@link #sendMessageAndCheckForReceipt()} to trigger the main transmission.
   *
   * @author Ulrik Stehling
   */
  public static class EuCtpUserMessageBuilder extends AbstractEuCtpUserMessageBuilder <EuCtpUserMessageBuilder>
  {
    public EuCtpUserMessageBuilder ()
    {
      super ();
    }
  }

  /**
   * The builder class for sending AS4 Pull Requests using EuCTP profile
   * specifics. Use {@link #sendMessage()} to trigger the main transmission.
   *
   * @author Philip Helger
   */
  public static class EuCtpPullRequestBuilder extends AbstractEuCtpPullRequestBuilder <EuCtpPullRequestBuilder>
  {
    public EuCtpPullRequestBuilder ()
    {
      super ();
    }
  }
}
