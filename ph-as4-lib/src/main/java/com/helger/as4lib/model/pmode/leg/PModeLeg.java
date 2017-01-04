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
package com.helger.as4lib.model.pmode.leg;

import javax.annotation.Nullable;

import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;

public class PModeLeg
{
  private final PModeLegProtocol m_aProtocol;
  private final PModeLegBusinessInformation m_aBusinessInfo;
  private final PModeLegErrorHandling m_aErrorHandling;
  private final PModeLegReliability m_aReliability;
  private final PModeLegSecurity m_aSecurity;

  public PModeLeg (@Nullable final PModeLegProtocol aProtocol,
                   @Nullable final PModeLegBusinessInformation aBusinessInfo,
                   @Nullable final PModeLegErrorHandling aErrorHandling,
                   @Nullable final PModeLegReliability aReliability,
                   @Nullable final PModeLegSecurity aSecurity)
  {
    m_aBusinessInfo = aBusinessInfo;
    m_aErrorHandling = aErrorHandling;
    m_aProtocol = aProtocol;
    m_aReliability = aReliability;
    m_aSecurity = aSecurity;
  }

  @Nullable
  public PModeLegProtocol getProtocol ()
  {
    return m_aProtocol;
  }

  @Nullable
  public PModeLegBusinessInformation getBusinessInfo ()
  {
    return m_aBusinessInfo;
  }

  @Nullable
  public PModeLegErrorHandling getErrorHandling ()
  {
    return m_aErrorHandling;
  }

  @Nullable
  public PModeLegReliability getReliability ()
  {
    return m_aReliability;
  }

  @Nullable
  public PModeLegSecurity getSecurity ()
  {
    return m_aSecurity;
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
}
