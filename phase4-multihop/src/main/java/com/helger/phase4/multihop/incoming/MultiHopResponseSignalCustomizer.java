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
package com.helger.phase4.multihop.incoming;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.w3c.dom.Document;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.incoming.IAS4ResponseSignalCustomizer;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.EAS4MessageType;
import com.helger.phase4.multihop.AS4MultiHopConfig;
import com.helger.phase4.multihop.CAS4MultiHop;
import com.helger.phase4.multihop.crypto.MultiHopSignatureCustomizer;
import com.helger.phase4.multihop.model.MultiHopRoutingInput;
import com.helger.phase4.multihop.model.RoutingInputInferrer;
import com.helger.phase4.multihop.soap.MultiHopSoapHelper;

/**
 * Adds the WS-Addressing headers and the <code>ebint:RoutingInput</code> to an outgoing Receipt or
 * Error, if - and only if - the User Message it answers arrived through an I-Cloud (R4).
 * <p>
 * Detection is purely based on the role/actor attribute of the incoming message, as AS4 Profile
 * section 4.2 requires - there is no configuration involved (D5, D6). A message that did not
 * arrive through an I-Cloud is therefore answered byte-identically to a phase4 without this
 * module (R5).
 * </p>
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public class MultiHopResponseSignalCustomizer implements IAS4ResponseSignalCustomizer
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MultiHopResponseSignalCustomizer.class);

  private final AS4MultiHopConfig m_aConfig;

  /**
   * Constructor using the default configuration.
   */
  public MultiHopResponseSignalCustomizer ()
  {
    this (AS4MultiHopConfig.getDefaultInstance ());
  }

  /**
   * Constructor.
   *
   * @param aConfig
   *        The configuration to be used. May not be <code>null</code>.
   */
  public MultiHopResponseSignalCustomizer (@NonNull final AS4MultiHopConfig aConfig)
  {
    ValueEnforcer.notNull (aConfig, "Config");
    m_aConfig = aConfig;
  }

  /**
   * @return The used configuration. Never <code>null</code>.
   */
  @NonNull
  public final AS4MultiHopConfig getConfig ()
  {
    return m_aConfig;
  }

  public void customizeResponseSignal (@NonNull final IAS4IncomingMessageState aIncomingState,
                                       @NonNull final EAS4MessageType eResponseType,
                                       @NonNull final ESoapVersion eResponseSoapVersion,
                                       @NonNull final Document aUnsignedResponseDoc,
                                       @Nullable final AS4SigningParams aResponseSigningParams)
  {
    // Only User Messages are answered with a routed signal
    final Ebms3UserMessage aUserMsg = aIncomingState.getEbmsUserMessage ();
    if (aUserMsg == null)
      return;

    // D6 - the incoming message decides, not any configuration
    if (!MultiHopSoapHelper.isTargetedToNextMSH (aIncomingState.getMessaging (), aIncomingState.getSoapVersion ()))
      return;

    final String sWsaAction;
    switch (eResponseType)
    {
      case RECEIPT:
        sWsaAction = CAS4MultiHop.WSA_ACTION_ONEWAY_RECEIPT;
        break;
      case ERROR_MESSAGE:
        sWsaAction = CAS4MultiHop.WSA_ACTION_ONEWAY_ERROR;
        break;
      default:
        // Nothing to do for other message types
        return;
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Adding the multi-hop routing headers to the outgoing " + eResponseType);

    // R6 - the inferred reverse RoutingInput
    final MultiHopRoutingInput aRoutingInput = RoutingInputInferrer.inferReverse (aUserMsg, eResponseType, m_aConfig);

    // R4, R7, R8
    final ICommonsList <String> aIDsToSign = MultiHopSoapHelper.addResponseAddressingHeaders (aUnsignedResponseDoc,
                                                                                               eResponseSoapVersion,
                                                                                               sWsaAction,
                                                                                               aRoutingInput);

    // R9 / D2 - have the added headers covered by the signature
    if (aResponseSigningParams != null && m_aConfig.isSignAddressingHeaders ())
      aResponseSigningParams.setWSSecSignatureCustomizer (new MultiHopSignatureCustomizer (aResponseSigningParams.getWSSecSignatureCustomizer (),
                                                                                            aIDsToSign));
  }
}
