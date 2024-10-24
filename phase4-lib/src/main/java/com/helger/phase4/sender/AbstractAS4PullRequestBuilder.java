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
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.client.AS4ClientPullRequestMessage;
import com.helger.phase4.incoming.AS4IncomingReceiverConfiguration;
import com.helger.phase4.incoming.IAS4IncomingReceiverConfiguration;
import com.helger.phase4.incoming.IAS4SignalMessageConsumer;
import com.helger.phase4.incoming.IAS4UserMessageConsumer;
import com.helger.phase4.incoming.crypto.AS4IncomingSecurityConfiguration;
import com.helger.phase4.incoming.crypto.IAS4IncomingSecurityConfiguration;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.leg.PModeLeg;
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

  protected IPMode m_aPMode;
  protected String m_sPModeID;
  private boolean m_bUseLeg1 = true;

  protected String m_sMPC;
  protected String m_sEndpointURL;
  protected IAS4UserMessageConsumer m_aUserMsgConsumer;
  protected IAS4SignalMessageConsumer m_aSignalMsgConsumer;
  protected IAS4SignalMessageValidationResultHandler m_aSignalMsgValidationResultHdl;

  /**
   * Create a new builder, with the following fields already set:<br>
   */
  protected AbstractAS4PullRequestBuilder ()
  {
    // No defaults
  }

  /**
   * @return The currently set P-Mode. May be <code>null</code>.
   */
  @Nullable
  public final IPMode pmode ()
  {
    return m_aPMode;
  }

  /**
   * Set the PMode to be used. By default a generic PMode is used.
   *
   * @param aPMode
   *        The PMode to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE pmode (@Nullable final IPMode aPMode)
  {
    if (aPMode == null)
      LOGGER.warn ("A null PMode was supplied");
    m_aPMode = aPMode;
    return thisAsT ();
  }

  /**
   * @return the current PMode ID. May be <code>null</code>.
   * @see #pmodeID(String)
   * @since 3.0.0
   */
  @Nullable
  public final String pmodeID ()
  {
    return m_sPModeID;
  }

  /**
   * Set the optional PMode ID for packaging in the pull request.
   *
   * @param s
   *        PMode ID. May be <code>null</code>.
   * @return this for chaining
   * @since 3.0.0
   */
  @Nonnull
  public final IMPLTYPE pmodeID (@Nullable final String s)
  {
    m_sPModeID = s;
    return thisAsT ();
  }

  /**
   * @return <code>true</code> if PMode leg 1 is used, <code>false</code> if leg
   *         2 is used.
   * @since 3.0.0
   */
  public final boolean useLeg1 ()
  {
    return m_bUseLeg1;
  }

  /**
   * Determine whether to use leg 1 or leg 2 of the PMode.
   *
   * @param bUseLeg1
   *        <code>true</code> to use leg 1, <code>false</code> to use leg 2.
   * @return this for chaining
   * @since 2.7.8
   */
  @Nonnull
  public final IMPLTYPE useLeg1 (final boolean bUseLeg1)
  {
    m_bUseLeg1 = bUseLeg1;
    return thisAsT ();
  }

  /**
   * @return The currently set MPC. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String mpc ()
  {
    return m_sMPC;
  }

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
   * @return The receiver AS4 endpoint URL currently set. May be
   *         <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final String endpointURL ()
  {
    return m_sEndpointURL;
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
   * @return The optional Ebms3 User Message Consumer. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final IAS4UserMessageConsumer userMsgConsumer ()
  {
    return m_aUserMsgConsumer;
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

  /**
   * @return The optional Ebms3 Signal Message Consumer. May be
   *         <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  public final IAS4SignalMessageConsumer signalMsgConsumer ()
  {
    return m_aSignalMsgConsumer;
  }

  /**
   * Set an optional Ebms3 Signal Message Consumer. If this consumer is set, the
   * response is trying to be parsed as a Signal Message. This method is
   * optional and must not be called prior to sending.
   *
   * @param aSignalMsgConsumer
   *        The optional signal message consumer. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE signalMsgConsumer (@Nullable final IAS4SignalMessageConsumer aSignalMsgConsumer)
  {
    m_aSignalMsgConsumer = aSignalMsgConsumer;
    return thisAsT ();
  }

  @Override
  @Nonnull
  @OverridingMethodsMustInvokeSuper
  protected ESuccess finishFields () throws Phase4Exception
  {
    if (super.finishFields ().isFailure ())
      return ESuccess.FAILURE;

    if (m_aPMode == null && pmodeResolver () != null)
    {
      // Create a default PMode template
      m_aPMode = pmodeResolver ().findPMode (m_sPModeID, "s", "a", "i", "r", "a", null);
      if (m_aPMode == null)
        LOGGER.warn ("No PMode was provided, and the PMode Resolver delivered a null-PMode as well");
    }

    return ESuccess.SUCCESS;
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
    // m_aSignalMsgConsumer is optional

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
    if (m_aCustomHttpPoster != null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Using a custom IHttpPoster implementation: " + m_aCustomHttpPoster);
      aPullRequestMsg.setHttpPoster (m_aCustomHttpPoster);
    }
    else
    {
      aPullRequestMsg.getHttpPoster ().setHttpClientFactory (m_aHttpClientFactory);
      // Otherwise Oxalis dies
      aPullRequestMsg.getHttpPoster ().setQuoteHttpHeaders (false);
    }

    aPullRequestMsg.setSoapVersion (m_eSoapVersion);
    aPullRequestMsg.setSendingDateTime (m_aSendingDateTime);
    // Set the keystore/truststore parameters
    aPullRequestMsg.setCryptoFactorySign (m_aCryptoFactorySign);
    aPullRequestMsg.setCryptoFactoryCrypt (m_aCryptoFactoryCrypt);

    // Copy all values
    m_aCryptParams.cloneTo (aPullRequestMsg.cryptParams ());
    m_aSigningParams.cloneTo (aPullRequestMsg.signingParams ());

    if (m_aHttpRetrySettings != null)
      aPullRequestMsg.httpRetrySettings ().assignFrom (m_aHttpRetrySettings);

    if (StringHelper.hasText (m_sMessageID))
      aPullRequestMsg.setMessageID (m_sMessageID);
    if (StringHelper.hasText (m_sRefToMessageID))
      aPullRequestMsg.setRefToMessageID (m_sRefToMessageID);

    aPullRequestMsg.setMPC (m_sMPC);

    if (m_aPMode != null)
    {
      final PModeLeg aEffectiveLeg = m_bUseLeg1 ? m_aPMode.getLeg1 () : m_aPMode.getLeg2 ();
      aPullRequestMsg.setValuesFromPMode (m_aPMode, aEffectiveLeg);
    }
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

      if (m_aSendingDTConsumer != null)
      {
        try
        {
          // Eventually this call will determine the sendingDateTime if none is
          // set yet
          m_aSendingDTConsumer.onEffectiveSendingDateTime (aPullRequestMsg.ensureSendingDateTime ()
                                                                          .getSendingDateTime ());
        }
        catch (final Exception ex)
        {
          LOGGER.error ("Failed to invoke IAS4SendingDateTimeConsumer", ex);
        }
      }

      // Create on demand with all necessary parameters
      final IAS4IncomingSecurityConfiguration aIncomingSecurityConfiguration = new AS4IncomingSecurityConfiguration ().setSigningParams (m_aSigningParams.getClone ())
                                                                                                                      .setCryptParams (m_aCryptParams.getClone ())
                                                                                                                      .setDecryptParameterModifier (m_aDecryptParameterModifier);

      // Use defaults
      final IAS4IncomingReceiverConfiguration aIncomingReceiverConfiguration = new AS4IncomingReceiverConfiguration ();

      // Main sending
      AS4BidirectionalClientHelper.sendAS4PullRequestAndReceiveAS4UserOrSignalMessage (m_aCryptoFactorySign,
                                                                                       m_aCryptoFactoryCrypt,
                                                                                       pmodeResolver (),
                                                                                       incomingAttachmentFactory (),
                                                                                       incomingProfileSelector (),
                                                                                       aPullRequestMsg,
                                                                                       m_aLocale,
                                                                                       m_sEndpointURL,
                                                                                       m_aBuildMessageCallback,
                                                                                       m_aOutgoingDumper,
                                                                                       m_aIncomingDumper,
                                                                                       aIncomingSecurityConfiguration,
                                                                                       aIncomingReceiverConfiguration,
                                                                                       m_aRetryCallback,
                                                                                       m_aResponseConsumer,
                                                                                       m_aUserMsgConsumer,
                                                                                       m_aSignalMsgConsumer,
                                                                                       m_aSignalMsgValidationResultHdl,
                                                                                       m_aPMode);
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
