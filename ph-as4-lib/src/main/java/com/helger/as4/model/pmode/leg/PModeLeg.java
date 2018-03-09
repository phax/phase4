/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.model.pmode.leg;

import java.io.Serializable;

import javax.annotation.Nullable;

import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;

public class PModeLeg implements Serializable
{
  private PModeLegProtocol m_aProtocol;
  private PModeLegBusinessInformation m_aBusinessInfo;
  private PModeLegErrorHandling m_aErrorHandling;
  private PModeLegReliability m_aReliability;
  private PModeLegSecurity m_aSecurity;

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
  public PModeLegProtocol getProtocol ()
  {
    return m_aProtocol;
  }

  public final void setProtocol (@Nullable final PModeLegProtocol aProtocol)
  {
    m_aProtocol = aProtocol;
  }

  @Nullable
  public PModeLegBusinessInformation getBusinessInfo ()
  {
    return m_aBusinessInfo;
  }

  public final void setBusinessInfo (@Nullable final PModeLegBusinessInformation aBusinessInfo)
  {
    m_aBusinessInfo = aBusinessInfo;
  }

  @Nullable
  public PModeLegErrorHandling getErrorHandling ()
  {
    return m_aErrorHandling;
  }

  public final void setErrorHandling (@Nullable final PModeLegErrorHandling aErrorHandling)
  {
    m_aErrorHandling = aErrorHandling;
  }

  @Nullable
  public PModeLegReliability getReliability ()
  {
    return m_aReliability;
  }

  public final void setReliability (@Nullable final PModeLegReliability aReliability)
  {
    m_aReliability = aReliability;
  }

  @Nullable
  public PModeLegSecurity getSecurity ()
  {
    return m_aSecurity;
  }

  public final void setSecurity (@Nullable final PModeLegSecurity aSecurity)
  {
    m_aSecurity = aSecurity;
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
