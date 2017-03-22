/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.photon.basic.object.AbstractBaseObject;
import com.helger.photon.security.object.StubObject;

public class PMode extends AbstractBaseObject implements IPMode
{
  public static final ObjectType OT = new ObjectType ("as4.pmode");

  /**
   * 1.(PMode.Initiator and its subelements are optional if PMode.Responder is
   * present.) Qualifies the party initiating the MEP (see Section 2.2.3). A
   * user message initiating an MEP instance under this P-Mode must have its
   * <code>eb:Messaging/eb:UserMessage/eb:PartyInfo/eb:From</code> element
   * contain the same <code>PartyId</code> elements as the <code>PartyId</code>
   * elements defined in this parameter. Any user message sent to the initiator
   * must have its <code>eb:PartyInfo/eb:To</code> map to or be compatible with
   * this parameter.<br>
   * Role: Name of the role assumed by the party sending the first message of
   * this MEP. Either the message element
   * <code>eb:Messaging/eb:UserMessage/eb:PartyInfo/eb:From/eb:Role</code> or
   * the element
   * <code>eb:Messaging/eb:UserMessage/eb:PartyInfo/eb:To/eb:Role</code> of each
   * message in this MEP must have this value, depending on the direction of
   * message transfer.<br>
   * Authorization: Describe authorization information for messages sent by
   * Initiator. These parameters need to be matched by a
   * <code>wsse:UsernameToken</code> element in a message (in a security header
   * only intended for authorization) for this message to be processed
   * successfully on receiver side - here by Responder MSH.
   */
  private PModeParty m_aInitiator;

  /**
   * (PMode.Responder and its subelements are optional if PMode.Initiator is
   * present.) Qualifies the party responding to the initiator party in this
   * MEP. Any user message sent to the responder must have its
   * <code>eb:Messaging/eb:UserMessage/eb:PartyInfo/eb:To</code> element contain
   * the same <code>PartyId</code> elements as the <code>PartyId</code> elements
   * defined in this parameter.<br>
   * Role: Name of the role assumed by the party receiving the first message of
   * this MEP. Either the message element
   * <code>eb:Messaging/eb:UserMessage/eb:PartyInfo/eb:From/eb:Role</code> or
   * the element
   * <code>eb:Messaging/eb:UserMessage/eb:PartyInfo/eb:To/eb:Role</code> of each
   * message in this MEP must have this value, depending on the direction of
   * message transfer.<br>
   * Authorization: Describe authorization information for messages sent by
   * Responder. These parameters need to be matched by a
   * <code>wsse:UsernameToken</code> element in a message (in a security header
   * only intended for authorization) for this message to be processed
   * successfully on receiver side - here by Initiator MSH.
   */
  private PModeParty m_aResponder;

  /**
   * The reference to the agreement governing this message exchange (maps to
   * <code>eb:AgreementRef</code> in message header).
   */
  private String m_sAgreement;

  /** The type of ebMS MEP associated with this P-Mode. */
  private EMEP m_eMEP = EMEP.DEFAULT_EBMS;

  /**
   * The transport channel binding assigned to the MEP (push, pull, sync,
   * push-and-push, push-and-pull, pull-and-push, pull-and-pull, ...).
   */
  private EMEPBinding m_eMEPBinding = EMEPBinding.DEFAULT_EBMS;

  private PModeLeg m_aLeg1;
  private PModeLeg m_aLeg2;

  /**
   * PayloadService is only used in the AS4 - Profile, to mark the compression
   * type.
   */
  private PModePayloadService m_aPayloadService;

  private PModeReceptionAwareness m_aReceptionAwareness;

  public PMode (@Nonnull final PModeParty aInitiator,
                @Nonnull final PModeParty aResponder,
                @Nonnull final String sAgreement,
                @Nonnull final EMEP eMEP,
                @Nonnull final EMEPBinding eMEPBinding,
                @Nonnull final PModeLeg aLeg1,
                @Nullable final PModeLeg aLeg2,
                @Nullable final PModePayloadService aPayloadService,
                @Nullable final PModeReceptionAwareness aReceptionAwareness)
  {
    this (StubObject.createForCurrentUserAndID (ValueEnforcer.notNull (aInitiator, "Initiator").getID () +
                                                "-" +
                                                ValueEnforcer.notNull (aResponder, "Responder").getID ()),
          aInitiator,
          aResponder,
          sAgreement,
          eMEP,
          eMEPBinding,
          aLeg1,
          aLeg2,
          aPayloadService,
          aReceptionAwareness);
  }

  PMode (@Nonnull final StubObject aObject,
         @Nonnull final PModeParty aInitiator,
         @Nonnull final PModeParty aResponder,
         @Nonnull final String sAgreement,
         @Nonnull final EMEP eMEP,
         @Nonnull final EMEPBinding eMEPBinding,
         @Nonnull final PModeLeg aLeg1,
         @Nullable final PModeLeg aLeg2,
         @Nullable final PModePayloadService aPayloadService,
         @Nullable final PModeReceptionAwareness aReceptionAwareness)
  {
    super (aObject);
    setInitiator (aInitiator);
    setResponder (aResponder);
    setAgreement (sAgreement);
    setMEP (eMEP);
    setMEPBinding (eMEPBinding);
    setLeg1 (aLeg1);
    setLeg2 (aLeg2);
    setPayloadService (aPayloadService);
    setReceptionAwareness (aReceptionAwareness);
  }

  @Nonnull
  public ObjectType getObjectType ()
  {
    return OT;
  }

  @Nullable
  public PModeParty getInitiator ()
  {
    return m_aInitiator;
  }

  public final void setInitiator (@Nullable final PModeParty aInitiator)
  {
    m_aInitiator = aInitiator;
  }

  @Nullable
  public PModeParty getResponder ()
  {
    return m_aResponder;
  }

  public final void setResponder (@Nullable final PModeParty aResponder)
  {
    m_aResponder = aResponder;
  }

  @Nullable
  public String getAgreement ()
  {
    return m_sAgreement;
  }

  public final void setAgreement (final String sAgreement)
  {
    m_sAgreement = sAgreement;
  }

  @Nonnull
  public EMEP getMEP ()
  {
    return m_eMEP;
  }

  public final void setMEP (@Nonnull final EMEP eMEP)
  {
    ValueEnforcer.notNull (eMEP, "MEP");
    m_eMEP = eMEP;
  }

  @Nonnull
  public EMEPBinding getMEPBinding ()
  {
    return m_eMEPBinding;
  }

  public final void setMEPBinding (@Nonnull final EMEPBinding eMEPBinding)
  {
    ValueEnforcer.notNull (eMEPBinding, "MEPBinding");
    m_eMEPBinding = eMEPBinding;
  }

  @Nullable
  public PModeLeg getLeg1 ()
  {
    return m_aLeg1;
  }

  public final void setLeg1 (@Nullable final PModeLeg aLeg1)
  {
    m_aLeg1 = aLeg1;
  }

  @Nullable
  public PModeLeg getLeg2 ()
  {
    return m_aLeg2;
  }

  public final void setLeg2 (@Nullable final PModeLeg aLeg2)
  {
    m_aLeg2 = aLeg2;
  }

  public PModePayloadService getPayloadService ()
  {
    return m_aPayloadService;
  }

  public final void setPayloadService (@Nullable final PModePayloadService aPayloadService)
  {
    m_aPayloadService = aPayloadService;
  }

  public PModeReceptionAwareness getReceptionAwareness ()
  {
    return m_aReceptionAwareness;
  }

  public final void setReceptionAwareness (@Nullable final PModeReceptionAwareness aPModeReceptionAwareness)
  {
    m_aReceptionAwareness = aPModeReceptionAwareness;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PMode rhs = (PMode) o;
    return getID ().equals (rhs.getID ());
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (getID ()).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("Initiator", m_aInitiator)
                            .append ("Responder", m_aResponder)
                            .append ("Agreement", m_sAgreement)
                            .append ("MEP", m_eMEP)
                            .append ("MEPBinding", m_eMEPBinding)
                            .append ("Leg1", m_aLeg1)
                            .append ("Leg2", m_aLeg2)
                            .append ("PayloadService", m_aPayloadService)
                            .append ("ReceptionAwareness", m_aReceptionAwareness)
                            .getToString ();
  }
}
