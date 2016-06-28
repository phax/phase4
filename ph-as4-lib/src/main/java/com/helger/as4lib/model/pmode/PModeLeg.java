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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PModeLeg
{
  private final PModeLegProtocol m_aProtocol = new PModeLegProtocol ();
  private PModeLegBusinessInformation m_aBusinessInfo;
  private PModeLegErrorHandling m_aErrorHandling;
  private PModeLegReliability m_aReliability;
  private PModeLegSecurity m_aSecurity;

  @Nonnull
  public PModeLegProtocol getProtocol ()
  {
    return m_aProtocol;
  }

  @Nullable
  public PModeLegBusinessInformation getBusinessInfo ()
  {
    return m_aBusinessInfo;
  }

  @Nonnull
  public PModeLegBusinessInformation getOrCreateBusinessInfo ()
  {
    if (m_aBusinessInfo == null)
      m_aBusinessInfo = new PModeLegBusinessInformation ();
    return getBusinessInfo ();
  }

  @Nullable
  public PModeLegErrorHandling getErrorHandling ()
  {
    return m_aErrorHandling;
  }

  @Nonnull
  public PModeLegErrorHandling getOrCreateErrorHandling ()
  {
    if (m_aErrorHandling == null)
      m_aErrorHandling = new PModeLegErrorHandling ();
    return getErrorHandling ();
  }

  @Nullable
  public PModeLegReliability getReliability ()
  {
    return m_aReliability;
  }

  @Nonnull
  public PModeLegReliability getOrCreateReliability ()
  {
    if (m_aReliability == null)
      m_aReliability = new PModeLegReliability ();
    return getReliability ();
  }

  @Nullable
  public PModeLegSecurity getSecurity ()
  {
    return m_aSecurity;
  }

  @Nonnull
  public PModeLegSecurity getOrCreateSecurity ()
  {
    if (m_aSecurity == null)
      m_aSecurity = new PModeLegSecurity ();
    return getSecurity ();
  }
}
