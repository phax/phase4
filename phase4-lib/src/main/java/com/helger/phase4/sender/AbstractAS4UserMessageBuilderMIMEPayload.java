package com.helger.phase4.sender;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.state.ESuccess;
import com.helger.phase4.attachment.Phase4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.Phase4Exception;

/**
 * Abstract builder base class for a user messages that put the payload in a
 * MIME part.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        The implementation type
 * @since 0.10.0
 */
public abstract class AbstractAS4UserMessageBuilderMIMEPayload <IMPLTYPE extends AbstractAS4UserMessageBuilderMIMEPayload <IMPLTYPE>>
                                                               extends
                                                               AbstractAS4UserMessageBuilder <IMPLTYPE>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractAS4UserMessageBuilderMIMEPayload.class);

  private Phase4OutgoingAttachment m_aPayload;

  /**
   * Create a new builder, with the some fields already set as outlined in
   * {@link AbstractAS4UserMessageBuilder#AbstractPhase4SenderBuilder()}
   */
  public AbstractAS4UserMessageBuilderMIMEPayload ()
  {}

  /**
   * Set the payload to be send out.
   *
   * @param aBuilder
   *        The payload builder to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE payload (@Nullable final Phase4OutgoingAttachment.Builder aBuilder)
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
  public final IMPLTYPE payload (@Nullable final Phase4OutgoingAttachment aPayload)
  {
    m_aPayload = aPayload;
    return thisAsT ();
  }

  @Override
  public boolean isEveryRequiredFieldSet ()
  {
    if (!super.isEveryRequiredFieldSet ())
      return false;

    if (m_aPayload == null)
      return false;

    // All valid
    return true;
  }

  /**
   * Internal method that is invoked before the required field check is
   * performed. Override to set additional dynamically created fields if
   * necessary.<br>
   * Don't add message properties in here, because if the required fields check
   * fails than this method would be called again.
   *
   * @return {@link ESuccess} - never <code>null</code>. Returning failure here
   *         stops sending the message.
   * @throws Phase4Exception
   *         if something goes wrong
   */
  @OverrideOnDemand
  protected ESuccess finishFields () throws Phase4Exception
  {
    return ESuccess.SUCCESS;
  }

  /**
   * Internal method that is invoked after the required fields are checked but
   * before sending takes place. This is e.g. the perfect place to add custom
   * message properties.
   *
   * @throws Phase4Exception
   *         if something goes wrong
   */
  protected void customizeBeforeSending () throws Phase4Exception
  {}

  @Override
  @Nonnull
  public ESuccess sendMessage () throws Phase4Exception
  {
    // Pre required field check
    if (finishFields ().isFailure ())
      return ESuccess.FAILURE;

    if (!isEveryRequiredFieldSet ())
    {
      LOGGER.error ("At least one mandatory field is not set and therefore the AS4 UserMessage cannot be send.");
      return ESuccess.FAILURE;
    }

    // Post required field check
    customizeBeforeSending ();

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
