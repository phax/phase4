/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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

import javax.annotation.Nullable;

import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;

public class PMode
{
  /**
   * (optional) The identifier for the P-Mode, e.g. the name of the business
   * transaction: PurchaseOrderFromACME. This identifier is user-defined and
   * optional, for the convenience of P-Mode management. It must uniquely
   * identify the P-Mode among all P-Modes deployed on the same MSH, and may be
   * absent if the P-Mode is identified by other means, e.g. embedded in a
   * larger structure that is itself identified, or has parameter values
   * distinct from other P-Modes used on the same MSH. If the ID is specified,
   * the <code>AgreementRef/@pmode</code> attribute value is also expected to be
   * set in associated messages.
   */
  private String m_sID;

  /**
   * The reference to the agreement governing this message exchange (maps to
   * <code>eb:AgreementRef</code> in message header).
   */
  private String m_sAgreement;

  /** The type of ebMS MEP associated with this P-Mode. */
  private EMEP m_eMEP;

  /**
   * The transport channel binding assigned to the MEP (push, pull, sync,
   * push-and-push, push-and-pull, pull-and-push, pull-and-pull, ...).
   */
  private ETransportChannelBinding m_eMEPBinding;

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

  private PModeLeg m_aLeg1;
  private PModeLeg m_aLeg2;

  @Nullable
  public String getID ()
  {
    return m_sID;
  }

  void setID (final String sID)
  {
    m_sID = sID;
  }

  @Nullable
  public String getAgreement ()
  {
    return m_sAgreement;
  }

  void setAgreement (final String sAgreement)
  {
    m_sAgreement = sAgreement;
  }

  @Nullable
  public EMEP getMEP ()
  {
    return m_eMEP;
  }

  void setMEP (@Nullable final EMEP eMEP)
  {
    m_eMEP = eMEP;
  }

  @Nullable
  public ETransportChannelBinding getMEPBinding ()
  {
    return m_eMEPBinding;
  }

  void setMEPBinding (@Nullable final ETransportChannelBinding eMEPBinding)
  {
    m_eMEPBinding = eMEPBinding;
  }

  @Nullable
  public PModeParty getInitiator ()
  {
    return m_aInitiator;
  }

  void setInitiator (@Nullable final PModeParty aInitiator)
  {
    m_aInitiator = aInitiator;
  }

  @Nullable
  public PModeParty getResponder ()
  {
    return m_aResponder;
  }

  void setResponder (@Nullable final PModeParty aResponder)
  {
    m_aResponder = aResponder;
  }

  @Nullable
  public PModeLeg getLeg1 ()
  {
    return m_aLeg1;
  }

  void setLeg1 (@Nullable final PModeLeg aLeg1)
  {
    m_aLeg1 = aLeg1;
  }

  @Nullable
  public PModeLeg getLeg2 ()
  {
    return m_aLeg2;
  }

  void setLeg2 (@Nullable final PModeLeg aLeg2)
  {
    m_aLeg2 = aLeg2;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PMode rhs = (PMode) o;
    return EqualsHelper.equals (m_sID, rhs.m_sID) &&
           EqualsHelper.equals (m_sAgreement, rhs.m_sAgreement) &&
           EqualsHelper.equals (m_eMEP, rhs.m_eMEP) &&
           EqualsHelper.equals (m_eMEPBinding, rhs.m_eMEPBinding) &&
           EqualsHelper.equals (m_aInitiator, rhs.m_aInitiator) &&
           EqualsHelper.equals (m_aResponder, rhs.m_aResponder) &&
           EqualsHelper.equals (m_aLeg1, rhs.m_aLeg1) &&
           EqualsHelper.equals (m_aLeg2, rhs.m_aLeg2);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sID)
                                       .append (m_sAgreement)
                                       .append (m_eMEP)
                                       .append (m_eMEPBinding)
                                       .append (m_aInitiator)
                                       .append (m_aResponder)
                                       .append (m_aLeg1)
                                       .append (m_aLeg2)
                                       .getHashCode ();
  }
}
