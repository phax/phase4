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
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.state.ESuccess;
import com.helger.phase4.attachment.Phase4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.Phase4Exception;

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
  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4Sender.class);

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
  public static class BuilderUserMessage extends AbstractAS4UserMessageBuilder <BuilderUserMessage>
  {
    private Phase4OutgoingAttachment m_aPayload;

    /**
     * Create a new builder, with the some fields already set as outlined in
     * {@link AbstractAS4UserMessageBuilder#AbstractPhase4SenderBuilder()}
     */
    public BuilderUserMessage ()
    {}

    /**
     * Set the payload to be send out.
     *
     * @param aBuilder
     *        The payload builder to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public BuilderUserMessage payload (@Nullable final Phase4OutgoingAttachment.Builder aBuilder)
    {
      return payload (aBuilder == null ? null : aBuilder.build ());
    }

    /**
     * Set the payload to be send out.
     *
     * @param aPayload
     *        The payload to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public BuilderUserMessage payload (@Nullable final Phase4OutgoingAttachment aPayload)
    {
      m_aPayload = aPayload;
      return this;
    }

    @Override
    public boolean isEveryRequiredFieldSet ()
    {
      if (!super.isEveryRequiredFieldSet ())
        return false;

      if (m_aPayload == null)
        return false;
      // m_aAttachments may be empty

      // All valid
      return true;
    }

    @Override
    @Nonnull
    public ESuccess sendMessage () throws Phase4Exception
    {
      if (!isEveryRequiredFieldSet ())
      {
        LOGGER.error ("At least one mandatory field is not set and therefore the AS4 message cannot be send.");
        return ESuccess.FAILURE;
      }

      // Temporary file manager
      try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
      {
        // Start building AS4 User Message
        final AS4ClientUserMessage aUserMsg = new AS4ClientUserMessage (aResHelper);
        applyToUserMessage (aUserMsg);

        // No payload - only one attachment
        aUserMsg.setPayload (null);

        // Add main attachment
        aUserMsg.addAttachment (WSS4JAttachment.createOutgoingFileAttachment (m_aPayload, aResHelper));

        // Add other attachments
        for (final Phase4OutgoingAttachment aAttachment : m_aAttachments)
          aUserMsg.addAttachment (WSS4JAttachment.createOutgoingFileAttachment (aAttachment, aResHelper));

        // Main sending
        AS4BidirectionalClientHelper.sendAS4AndReceiveAS4 (m_aCryptoFactory,
                                                           m_aPModeResolver,
                                                           m_aIAF,
                                                           aUserMsg,
                                                           m_aLocale,
                                                           m_sEndointURL,
                                                           m_aBuildMessageCallback,
                                                           m_aOutgoingDumper,
                                                           m_aIncomingDumper,
                                                           m_aRetryCallback,
                                                           m_aResponseConsumer,
                                                           m_aSignalMsgConsumer);
      }
      catch (final Phase4Exception ex)
      {
        // Re-throw
        throw ex;
      }
      catch (final Exception ex)
      {
        // wrap
        throw new Phase4Exception ("Wrapped Phase4Exception", ex);
      }
      return ESuccess.SUCCESS;
    }
  }
}
