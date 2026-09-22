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
package com.helger.phase4.multihop.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.phase4.CAS4;
import com.helger.phase4.ebms3header.Ebms3AgreementRef;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3From;
import com.helger.phase4.ebms3header.Ebms3PartyId;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3Service;
import com.helger.phase4.ebms3header.Ebms3To;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.model.message.EAS4MessageType;
import com.helger.phase4.multihop.AS4MultiHopConfig;
import com.helger.phase4.multihop.CAS4MultiHop;

/**
 * Creates the "inferred RoutingInput for the reverse path" of a Receipt or Error, based on the
 * User Message it relates to. See AS4 Profile section 4.4 and ebMS3 Part 2 section 2.6.2 case 4.
 * <p>
 * Implementation notes:
 * </p>
 * <ul>
 * <li>From and To are swapped including their <code>Role</code></li>
 * <li>The Action gets a configurable suffix appended, the MPC the normative
 * <code>.receipt</code> / <code>.error</code> suffix</li>
 * <li><code>MessageInfo</code>, <code>MessageProperties</code> and <code>PayloadInfo</code> are
 * deliberately omitted</li>
 * <li>The source object is never modified - everything is deep copied</li>
 * </ul>
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@Immutable
public final class RoutingInputInferrer
{
  private RoutingInputInferrer ()
  {}

  @Nullable
  private static Ebms3To _fromToTo (@Nullable final Ebms3From aSrc)
  {
    if (aSrc == null)
      return null;

    final Ebms3To ret = new Ebms3To ();
    for (final Ebms3PartyId aPartyID : aSrc.getPartyId ())
      ret.addPartyId (aPartyID.clone ());
    ret.setRole (aSrc.getRole ());
    return ret;
  }

  @Nullable
  private static Ebms3From _toToFrom (@Nullable final Ebms3To aSrc)
  {
    if (aSrc == null)
      return null;

    final Ebms3From ret = new Ebms3From ();
    for (final Ebms3PartyId aPartyID : aSrc.getPartyId ())
      ret.addPartyId (aPartyID.clone ());
    ret.setRole (aSrc.getRole ());
    return ret;
  }

  /**
   * Swap From and To of the provided PartyInfo. R6.
   */
  @Nullable
  private static Ebms3PartyInfo _createSwappedPartyInfo (@Nullable final Ebms3PartyInfo aSrc)
  {
    if (aSrc == null)
      return null;

    final Ebms3PartyInfo ret = new Ebms3PartyInfo ();
    // The original "To" becomes the new "From" and vice versa
    ret.setFrom (_toToFrom (aSrc.getTo ()));
    ret.setTo (_fromToTo (aSrc.getFrom ()));
    return ret;
  }

  /**
   * Copy AgreementRef, Service and ConversationId, and append the suffix to the Action. D4.
   */
  @Nullable
  private static Ebms3CollaborationInfo _createReverseCollaborationInfo (@Nullable final Ebms3CollaborationInfo aSrc,
                                                                         @NonNull final String sActionSuffix)
  {
    if (aSrc == null)
      return null;

    final Ebms3CollaborationInfo ret = new Ebms3CollaborationInfo ();

    final Ebms3AgreementRef aAgreementRef = aSrc.getAgreementRef ();
    if (aAgreementRef != null)
      ret.setAgreementRef (aAgreementRef.clone ());

    final Ebms3Service aService = aSrc.getService ();
    if (aService != null)
      ret.setService (aService.clone ());

    ret.setAction (StringHelper.getNotNull (aSrc.getAction ()) + sActionSuffix);
    ret.setConversationId (aSrc.getConversationId ());
    return ret;
  }

  /**
   * Create the inferred reverse RoutingInput of a signal message.
   *
   * @param aUserMsg
   *        The User Message the signal relates to. May not be <code>null</code> and is never
   *        modified.
   * @param eSignalType
   *        The type of signal message. Must be either {@link EAS4MessageType#RECEIPT} or
   *        {@link EAS4MessageType#ERROR_MESSAGE}.
   * @param aCfg
   *        The configuration providing the Action suffixes. May not be <code>null</code>.
   * @return The newly created RoutingInput. Never <code>null</code>. The SOAP version dependent
   *         attributes are NOT yet set - use
   *         {@link MultiHopRoutingInput#setStandardAttributes(com.helger.phase4.model.ESoapVersion, String)}
   *         for that.
   */
  @NonNull
  public static MultiHopRoutingInput inferReverse (@NonNull final Ebms3UserMessage aUserMsg,
                                                   @NonNull final EAS4MessageType eSignalType,
                                                   @NonNull final AS4MultiHopConfig aCfg)
  {
    ValueEnforcer.notNull (aUserMsg, "UserMessage");
    ValueEnforcer.notNull (eSignalType, "SignalType");
    ValueEnforcer.notNull (aCfg, "Config");

    final String sActionSuffix;
    final String sMPCSuffix;
    switch (eSignalType)
    {
      case RECEIPT:
        sActionSuffix = aCfg.getReverseActionSuffixReceipt ();
        sMPCSuffix = CAS4MultiHop.MPC_SUFFIX_RECEIPT;
        break;
      case ERROR_MESSAGE:
        sActionSuffix = aCfg.getReverseActionSuffixError ();
        sMPCSuffix = CAS4MultiHop.MPC_SUFFIX_ERROR;
        break;
      default:
        throw new IllegalArgumentException ("Only Receipt and Error are supported, but got " + eSignalType);
    }

    final MultiHopRoutingUserMessage aUM = new MultiHopRoutingUserMessage ();
    // D4 - MessageInfo, MessageProperties and PayloadInfo are omitted on purpose
    aUM.setPartyInfo (_createSwappedPartyInfo (aUserMsg.getPartyInfo ()));
    aUM.setCollaborationInfo (_createReverseCollaborationInfo (aUserMsg.getCollaborationInfo (), sActionSuffix));

    // R6 - fall back to the default MPC if the User Message defines none
    final String sSrcMPC = StringHelper.isNotEmpty (aUserMsg.getMpc ()) ? aUserMsg.getMpc () : CAS4.DEFAULT_MPC_ID;
    aUM.setMpc (sSrcMPC + sMPCSuffix);

    final MultiHopRoutingInput ret = new MultiHopRoutingInput ();
    ret.setUserMessage (aUM);
    return ret;
  }
}
