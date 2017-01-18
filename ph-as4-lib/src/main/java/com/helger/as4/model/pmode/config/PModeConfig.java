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
package com.helger.as4.model.pmode.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.model.EMEP;
import com.helger.as4.model.ETransportChannelBinding;
import com.helger.as4.model.pmode.PModePayloadService;
import com.helger.as4.model.pmode.PModeReceptionAwareness;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.photon.basic.object.AbstractBaseObject;
import com.helger.photon.security.object.StubObject;

public class PModeConfig extends AbstractBaseObject implements IPModeConfig
{
  public static final ObjectType OT = new ObjectType ("as4.pmodeconfig");

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

  private PModeLeg m_aLeg1;
  private PModeLeg m_aLeg2;

  /**
   * PayloadService is only used in the AS4 - Profile, to mark the compression
   * type.
   */
  private PModePayloadService m_aPayloadService;

  private PModeReceptionAwareness m_aReceptionAwareness;

  public PModeConfig (@Nonnull @Nonempty final String sID)
  {
    this (StubObject.createForCurrentUserAndID (sID));
  }

  PModeConfig (@Nonnull final StubObject aStubObject)
  {
    super (aStubObject);
  }

  @Nonnull
  public ObjectType getObjectType ()
  {
    return OT;
  }

  @Nullable
  public String getAgreement ()
  {
    return m_sAgreement;
  }

  public void setAgreement (final String sAgreement)
  {
    m_sAgreement = sAgreement;
  }

  @Nullable
  public EMEP getMEP ()
  {
    return m_eMEP;
  }

  public void setMEP (@Nullable final EMEP eMEP)
  {
    m_eMEP = eMEP;
  }

  @Nullable
  public ETransportChannelBinding getMEPBinding ()
  {
    return m_eMEPBinding;
  }

  public void setMEPBinding (@Nullable final ETransportChannelBinding eMEPBinding)
  {
    m_eMEPBinding = eMEPBinding;
  }

  @Nullable
  public PModeLeg getLeg1 ()
  {
    return m_aLeg1;
  }

  public void setLeg1 (@Nullable final PModeLeg aLeg1)
  {
    m_aLeg1 = aLeg1;
  }

  @Nullable
  public PModeLeg getLeg2 ()
  {
    return m_aLeg2;
  }

  public void setLeg2 (@Nullable final PModeLeg aLeg2)
  {
    m_aLeg2 = aLeg2;
  }

  public PModePayloadService getPayloadService ()
  {
    return m_aPayloadService;
  }

  public void setPayloadService (@Nullable final PModePayloadService aPayloadService)
  {
    m_aPayloadService = aPayloadService;
  }

  public PModeReceptionAwareness getReceptionAwareness ()
  {
    return m_aReceptionAwareness;
  }

  public void setReceptionAwareness (@Nullable final PModeReceptionAwareness aPModeReceptionAwareness)
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
    final PModeConfig rhs = (PModeConfig) o;
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
                            .append ("Agreement", m_sAgreement)
                            .append ("MEP", m_eMEP)
                            .append ("MEPBinding", m_eMEPBinding)
                            .append ("Leg1", m_aLeg1)
                            .append ("Leg2", m_aLeg2)
                            .append ("PayloadService", m_aPayloadService)
                            .append ("ReceptionAwareness", m_aReceptionAwareness)
                            .toString ();
  }
}
