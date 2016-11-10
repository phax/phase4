/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.photon.basic.object.AbstractBaseObject;
import com.helger.photon.security.object.StubObject;

// TODO remove public setters, needed for testing pmode in servlet
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
   * this parameter.<br />
   * Role: Name of the role assumed by the party sending the first message of
   * this MEP. Either the message element
   * <code>eb:Messaging/eb:UserMessage/eb:PartyInfo/eb:From/eb:Role</code> or
   * the element
   * <code>eb:Messaging/eb:UserMessage/eb:PartyInfo/eb:To/eb:Role</code> of each
   * message in this MEP must have this value, depending on the direction of
   * message transfer.<br/>
   * Authorization: Describe authorization information for messages sent by
   * Initiator. These parameters need to be matched by a
   * <code>wsse:UsernameToken</code> element in a message (in a security header
   * only intended for authorization) for this message to be processed
   * successfully on receiver side � here by Responder MSH.
   */
  private PModeParty m_aInitiator;

  /**
   * (PMode.Responder and its subelements are optional if PMode.Initiator is
   * present.) Qualifies the party responding to the initiator party in this
   * MEP. Any user message sent to the responder must have its
   * <code>eb:Messaging/eb:UserMessage/eb:PartyInfo/eb:To</code> element contain
   * the same <code>PartyId</code> elements as the <code>PartyId</code> elements
   * defined in this parameter.<br />
   * Role: Name of the role assumed by the party receiving the first message of
   * this MEP. Either the message element
   * <code>eb:Messaging/eb:UserMessage/eb:PartyInfo/eb:From/eb:Role</code> or
   * the element
   * <code>eb:Messaging/eb:UserMessage/eb:PartyInfo/eb:To/eb:Role</code> of each
   * message in this MEP must have this value, depending on the direction of
   * message transfer.<br/>
   * Authorization: Describe authorization information for messages sent by
   * Responder. These parameters need to be matched by a
   * <code>wsse:UsernameToken</code> element in a message (in a security header
   * only intended for authorization) for this message to be processed
   * successfully on receiver side � here by Initiator MSH.
   */
  private PModeParty m_aResponder;

  private final IPModeConfig m_aConfig;

  @Deprecated
  public PMode (@Nonnull final IPModeConfig aConfig)
  {
    this (null, null, aConfig);
  }

  public PMode (@Nullable final PModeParty aInitiator,
                @Nullable final PModeParty aResponder,
                @Nonnull final IPModeConfig aConfig)
  {
    this (StubObject.createForCurrentUser (), aInitiator, aResponder, aConfig);
  }

  PMode (@Nonnull final StubObject aStubObject,
         @Nullable final PModeParty aInitiator,
         @Nullable final PModeParty aResponder,
         @Nonnull final IPModeConfig aConfig)
  {
    super (aStubObject);
    setInitiator (aInitiator);
    setResponder (aResponder);
    m_aConfig = ValueEnforcer.notNull (aConfig, "PModeConfig");
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

  public void setInitiator (@Nullable final PModeParty aInitiator)
  {
    m_aInitiator = aInitiator;
  }

  @Nullable
  public PModeParty getResponder ()
  {
    return m_aResponder;
  }

  public void setResponder (@Nullable final PModeParty aResponder)
  {
    m_aResponder = aResponder;
  }

  @Nullable
  public IPModeConfig getConfig ()
  {
    return m_aConfig;
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
                            .append ("Config", m_aConfig)
                            .toString ();
  }
}
