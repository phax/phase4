/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.sender;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.multihop.AS4MultiHopConfig;
import com.helger.phase4.multihop.CAS4MultiHop;
import com.helger.phase4.multihop.incoming.MultiHopSentMessageStore;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilder;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.sender.IAS4RawResponseConsumer;

/**
 * Configures an existing AS4 User Message builder for sending through an I-Cloud.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@Immutable
public final class AS4MultiHopSender
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4MultiHopSender.class);

  private AS4MultiHopSender ()
  {}

  /**
   * Configure the provided builder for multi-hop sending.
   * <ol>
   * <li>Wraps an existing build message callback so that the <code>nextmsh</code> role/actor
   * attribute is set (R1)</li>
   * <li>Sets the explicit PMode and the AgreementRef PMode ID without the <code>.init</code> /
   * <code>.resp</code> suffix (R11). The PMode must be set explicitly, because
   * {@code AbstractAS4UserMessageBuilder} uses the same field for the PMode lookup and for
   * <code>AgreementRef/@pmode</code></li>
   * <li>Remembers the message ID so that an asynchronously arriving signal can be related back to
   * the PMode</li>
   * <li>Installs a raw response consumer that records the HTTP outcome for
   * {@link #interpret(EAS4UserMessageSendResult, MultiHopSendOutcomeHolder)}</li>
   * </ol>
   * <p>
   * <b>Note:</b> the builder must already have a non-empty <code>agreementRef</code> value,
   * otherwise no <code>eb:AgreementRef</code> element is created at all and the PMode ID never
   * reaches the wire.
   * </p>
   *
   * @param <T>
   *        The builder implementation type
   * @param aBuilder
   *        The builder to configure. May not be <code>null</code>.
   * @param aPMode
   *        The PMode to use. May not be <code>null</code>. Its ID may carry the
   *        <code>.init</code> / <code>.resp</code> suffix.
   * @param aOutcomeHolder
   *        The holder that receives the raw HTTP outcome. May not be <code>null</code>.
   * @param aCfg
   *        The multi-hop configuration. May not be <code>null</code>.
   * @param aStore
   *        The store remembering sent messages. May not be <code>null</code>.
   * @return The passed builder for chaining. Never <code>null</code>.
   */
  @NonNull
  public static <T extends AbstractAS4UserMessageBuilder <T>> T configure (@NonNull final T aBuilder,
                                                                           @NonNull final IPMode aPMode,
                                                                           @NonNull final MultiHopSendOutcomeHolder aOutcomeHolder,
                                                                           @NonNull final AS4MultiHopConfig aCfg,
                                                                           @NonNull final MultiHopSentMessageStore aStore)
  {
    ValueEnforcer.notNull (aBuilder, "Builder");
    ValueEnforcer.notNull (aPMode, "PMode");
    ValueEnforcer.notNull (aOutcomeHolder, "OutcomeHolder");
    ValueEnforcer.notNull (aCfg, "Config");
    ValueEnforcer.notNull (aStore, "Store");

    final String sPModeID = aPMode.getID ();
    ValueEnforcer.notEmpty (sPModeID, "PMode.ID");

    if (StringHelper.isEmpty (aBuilder.agreementRef ()))
      throw new IllegalStateException ("An AgreementRef value must be set on the builder before calling configure(), " +
                                       "because phase4 only emits eb:AgreementRef - and hence the @pmode attribute - " +
                                       "if the AgreementRef value is non-empty");

    // (1) role/actor attribute
    final boolean bActive = aCfg.isAddActorOrRoleAttribute (sPModeID);
    if (!bActive)
      LOGGER.warn ("Multi-Hop sending is not enabled for PMode ID '" +
                   sPModeID +
                   "' - the message will be sent without the nextmsh role attribute");
    aBuilder.buildMessageCallback (new MultiHopBuildMessageCallback (aBuilder.buildMessageCallback (), bActive));

    // (2) explicit PMode, then the stripped ID for AgreementRef/@pmode (R11)
    aBuilder.pmode (aPMode);
    aBuilder.pmodeID (CAS4MultiHop.getAgreementPModeID (sPModeID));

    // (3) remember the message ID for the asynchronous signal
    final String sMessageID = aBuilder.messageID ();
    if (StringHelper.isNotEmpty (sMessageID))
      aStore.rememberSentMessage (sMessageID, sPModeID);

    // (4) record the raw HTTP outcome (D10)
    final IAS4RawResponseConsumer aOldRRC = aBuilder.rawResponseConsumer ();
    aBuilder.rawResponseConsumer (aResponseMsg -> {
      if (aOldRRC != null)
        aOldRRC.handleResponse (aResponseMsg);

      final int nStatusCode = aResponseMsg.hasResponseStatusLine () ? aResponseMsg.getResponseStatusLine ()
                                                                                  .getStatusCode ()
                                                                    : com.helger.phase4.CAS4.HTTP_STATUS_UNDEFINED;
      final boolean bHasContent = aResponseMsg.hasResponseContent () && aResponseMsg.getResponseContent ().length > 0;
      aOutcomeHolder.setResponse (nStatusCode, bHasContent);

      // The message ID is only known reliably after building
      final String sSentMessageID = aResponseMsg.getMessageID ();
      if (StringHelper.isNotEmpty (sSentMessageID))
        aStore.rememberSentMessage (sSentMessageID, sPModeID);
    });

    return aBuilder;
  }

  /**
   * Interpret the send result of a multi-hop send. D10 - an HTTP 2xx with an empty body from the
   * edge intermediary means "accepted, the Receipt will arrive asynchronously" and is therefore
   * <b>not</b> a failure, even though phase4 itself reports
   * {@link EAS4UserMessageSendResult#NO_SIGNAL_MESSAGE_RECEIVED}.
   *
   * @param eResult
   *        The result reported by phase4. May be <code>null</code>.
   * @param aOutcomeHolder
   *        The holder filled by the raw response consumer installed in
   *        {@link #configure(AbstractAS4UserMessageBuilder, IPMode, MultiHopSendOutcomeHolder, AS4MultiHopConfig, MultiHopSentMessageStore)}.
   *        May not be <code>null</code>.
   * @return The multi-hop outcome. Never <code>null</code>.
   */
  @NonNull
  public static EAS4MultiHopSendOutcome interpret (@Nullable final EAS4UserMessageSendResult eResult,
                                                   @NonNull final MultiHopSendOutcomeHolder aOutcomeHolder)
  {
    ValueEnforcer.notNull (aOutcomeHolder, "OutcomeHolder");

    if (eResult == EAS4UserMessageSendResult.SUCCESS)
      return EAS4MultiHopSendOutcome.SIGNAL_RECEIVED_SYNC;

    if (eResult == EAS4UserMessageSendResult.NO_SIGNAL_MESSAGE_RECEIVED && aOutcomeHolder.isEmpty2xxResponse ())
    {
      LOGGER.info ("The I-Cloud accepted the AS4 User Message with HTTP " +
                   aOutcomeHolder.getHttpStatusCode () +
                   " and an empty body - the Receipt is expected to arrive asynchronously");
      return EAS4MultiHopSendOutcome.ACCEPTED_BY_ICLOUD_ASYNC;
    }

    return EAS4MultiHopSendOutcome.FAILED;
  }

  /**
   * @param sPModeID
   *        The PMode ID. May neither be <code>null</code> nor empty.
   * @return The PMode ID to be used in <code>eb:AgreementRef/@pmode</code>, i.e. without the
   *         <code>.init</code> / <code>.resp</code> suffix. R11.
   */
  @Nullable
  public static String getAgreementPModeID (@NonNull @Nonempty final String sPModeID)
  {
    return CAS4MultiHop.getAgreementPModeID (sPModeID);
  }
}
