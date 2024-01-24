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
package com.helger.phase4.model.pmode.leg;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;

/**
 * Contains the information for a single direction (leg) of a PMode
 *
 * @author Philip Helger
 */
@NotThreadSafe
@MustImplementEqualsAndHashcode
public class PModeLeg implements Serializable
{
  private PModeLegProtocol m_aProtocol;
  private PModeLegBusinessInformation m_aBusinessInfo;
  private PModeLegErrorHandling m_aErrorHandling;
  private PModeLegReliability m_aReliability;
  private PModeLegSecurity m_aSecurity;

  public PModeLeg ()
  {}

  public PModeLeg (@Nullable final PModeLegProtocol aProtocol,
                   @Nullable final PModeLegBusinessInformation aBusinessInfo,
                   @Nullable final PModeLegErrorHandling aErrorHandling,
                   @Nullable final PModeLegReliability aReliability,
                   @Nullable final PModeLegSecurity aSecurity)
  {
    setProtocol (aProtocol);
    setBusinessInfo (aBusinessInfo);
    setErrorHandling (aErrorHandling);
    setReliability (aReliability);
    setSecurity (aSecurity);
  }

  @Nullable
  public final PModeLegProtocol getProtocol ()
  {
    return m_aProtocol;
  }

  public final boolean hasProtocol ()
  {
    return m_aProtocol != null;
  }

  @Nonnull
  public final EChange setProtocol (@Nullable final PModeLegProtocol aProtocol)
  {
    if (EqualsHelper.equals (aProtocol, m_aProtocol))
      return EChange.UNCHANGED;
    m_aProtocol = aProtocol;
    return EChange.CHANGED;
  }

  @Nullable
  public final PModeLegBusinessInformation getBusinessInfo ()
  {
    return m_aBusinessInfo;
  }

  public final boolean hasBusinessInfo ()
  {
    return m_aBusinessInfo != null;
  }

  @Nonnull
  public final EChange setBusinessInfo (@Nullable final PModeLegBusinessInformation aBusinessInfo)
  {
    if (EqualsHelper.equals (aBusinessInfo, m_aBusinessInfo))
      return EChange.UNCHANGED;
    m_aBusinessInfo = aBusinessInfo;
    return EChange.CHANGED;
  }

  @Nullable
  public final PModeLegErrorHandling getErrorHandling ()
  {
    return m_aErrorHandling;
  }

  public final boolean hasErrorHandling ()
  {
    return m_aErrorHandling != null;
  }

  @Nonnull
  public final EChange setErrorHandling (@Nullable final PModeLegErrorHandling aErrorHandling)
  {
    if (EqualsHelper.equals (aErrorHandling, m_aErrorHandling))
      return EChange.UNCHANGED;
    m_aErrorHandling = aErrorHandling;
    return EChange.CHANGED;
  }

  @Nullable
  public final PModeLegReliability getReliability ()
  {
    return m_aReliability;
  }

  public final boolean hasReliability ()
  {
    return m_aReliability != null;
  }

  @Nonnull
  public final EChange setReliability (@Nullable final PModeLegReliability aReliability)
  {
    if (EqualsHelper.equals (aReliability, m_aReliability))
      return EChange.UNCHANGED;
    m_aReliability = aReliability;
    return EChange.CHANGED;
  }

  @Nullable
  public final PModeLegSecurity getSecurity ()
  {
    return m_aSecurity;
  }

  public final boolean hasSecurity ()
  {
    return m_aSecurity != null;
  }

  @Nonnull
  public final EChange setSecurity (@Nullable final PModeLegSecurity aSecurity)
  {
    if (EqualsHelper.equals (aSecurity, m_aSecurity))
      return EChange.UNCHANGED;
    m_aSecurity = aSecurity;
    return EChange.CHANGED;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModeLeg rhs = (PModeLeg) o;
    return EqualsHelper.equals (m_aProtocol, rhs.m_aProtocol) &&
           EqualsHelper.equals (m_aBusinessInfo, rhs.m_aBusinessInfo) &&
           EqualsHelper.equals (m_aErrorHandling, rhs.m_aErrorHandling) &&
           EqualsHelper.equals (m_aReliability, rhs.m_aReliability) &&
           EqualsHelper.equals (m_aSecurity, rhs.m_aSecurity);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aProtocol)
                                       .append (m_aBusinessInfo)
                                       .append (m_aErrorHandling)
                                       .append (m_aReliability)
                                       .append (m_aSecurity)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Protocol", m_aProtocol)
                                       .append ("BusinessInfo", m_aBusinessInfo)
                                       .append ("ErrorHandling", m_aErrorHandling)
                                       .append ("Reliability", m_aReliability)
                                       .append ("Security", m_aSecurity)
                                       .getToString ();
  }
}
