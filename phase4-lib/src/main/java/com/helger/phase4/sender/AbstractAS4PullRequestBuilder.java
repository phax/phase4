/*
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.string.StringHelper;
import com.helger.phase4.client.AS4ClientPullRequestMessage;
import com.helger.phase4.client.IAS4UserMessageConsumer;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.Phase4Exception;

/**
 * Abstract builder base class for a Pull Request.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        The implementation type
 * @since 0.12.0
 */
@NotThreadSafe
public abstract class AbstractAS4PullRequestBuilder <IMPLTYPE extends AbstractAS4PullRequestBuilder <IMPLTYPE>> extends
                                                    AbstractAS4MessageBuilder <IMPLTYPE>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractAS4PullRequestBuilder.class);

  protected String m_sMPC;
  protected String m_sEndpointURL;
  protected IAS4UserMessageConsumer m_aUserMsgConsumer;

  /**
   * Create a new builder, with the following fields already set:<br>
   */
  public AbstractAS4PullRequestBuilder ()
  {}

  /**
   * Set the MPC to be used in the Pull Request.
   *
   * @param sMPC
   *        The MPC to use. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE mpc (@Nullable final String sMPC)
  {
    m_sMPC = sMPC;
    return thisAsT ();
  }

  /**
   * Set an receiver AS4 endpoint URL, independent of its usability.
   *
   * @param sEndointURL
   *        The endpoint URL to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE endpointURL (@Nullable final String sEndointURL)
  {
    m_sEndpointURL = sEndointURL;
    return thisAsT ();
  }

  /**
   * Set an optional Ebms3 User Message Consumer. This method is optional and
   * must not be called prior to sending.
   *
   * @param aUserMsgConsumer
   *        The optional User Message consumer. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE userMsgConsumer (@Nullable final IAS4UserMessageConsumer aUserMsgConsumer)
  {
    m_aUserMsgConsumer = aUserMsgConsumer;
    return thisAsT ();
  }

  @Override
  @OverridingMethodsMustInvokeSuper
  public boolean isEveryRequiredFieldSet ()
  {
    if (!super.isEveryRequiredFieldSet ())
      return false;

    if (StringHelper.hasNoText (m_sMPC))
    {
      LOGGER.warn ("The field 'MPC' is not set");
      return false;
    }

    if (StringHelper.hasNoText (m_sEndpointURL))
    {
      LOGGER.warn ("The field 'endpointURL' is not set");
      return false;
    }

    // m_aUserMsgConsumer is optional

    // All valid
    return true;
  }

  /**
   * This method applies all builder parameters onto the Pull Request, except
   * the attachments.
   *
   * @param aPullRequestMsg
   *        The Pull request the parameters should be applied to. May not be
   *        <code>null</code>.
   */
  protected final void applyToPullRequest (@Nonnull final AS4ClientPullRequestMessage aPullRequestMsg)
  {
    if (m_aHttpRetrySettings != null)
      aPullRequestMsg.httpRetrySettings ().assignFrom (m_aHttpRetrySettings);

    aPullRequestMsg.getHttpPoster ().setHttpClientFactory (m_aHttpClientFactory);

    // Otherwise Oxalis dies
    aPullRequestMsg.getHttpPoster ().setQuoteHttpHeaders (false);
    aPullRequestMsg.setSoapVersion (m_eSoapVersion);
    aPullRequestMsg.setSendingDateTimeOrNow (m_aSendingDateTime);
    // Set the keystore/truststore parameters
    aPullRequestMsg.setAS4CryptoFactory (m_aCryptoFactory);

    if (StringHelper.hasText (m_sMessageID))
      aPullRequestMsg.setMessageID (m_sMessageID);
    if (StringHelper.hasText (m_sRefToMessageID))
      aPullRequestMsg.setRefToMessageID (m_sRefToMessageID);

    aPullRequestMsg.setMPC (m_sMPC);
  }

  @Override
  protected final void mainSendMessage () throws Phase4Exception
  {
    // Temporary file manager
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      // Start building AS4 User Message
      final AS4ClientPullRequestMessage aPullRequestMsg = new AS4ClientPullRequestMessage (aResHelper);
      applyToPullRequest (aPullRequestMsg);

      // Main sending
      AS4BidirectionalClientHelper.sendAS4PullRequestAndReceiveAS4UserMessage (m_aCryptoFactory,
                                                                               pmodeResolver (),
                                                                               incomingAttachmentFactory (),
                                                                               incomingProfileSelector (),
                                                                               aPullRequestMsg,
                                                                               m_aLocale,
                                                                               m_sEndpointURL,
                                                                               m_aBuildMessageCallback,
                                                                               m_aOutgoingDumper,
                                                                               m_aIncomingDumper,
                                                                               m_aRetryCallback,
                                                                               m_aResponseConsumer,
                                                                               m_aUserMsgConsumer);
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
  }
}
