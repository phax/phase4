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
package com.helger.as4lib.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;

/**
 * Predefines transport channel bindings.
 * 
 * @author Philip Helger
 */
public enum ETransportChannelBinding
{
  /**
   * maps an MEP User message to the 1st leg of an underlying 2-way transport
   * protocol, or of a 1-way protocol.
   */
  PUSH ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/push"),
  /**
   * maps an MEP User message to the second leg of an underlying two-way
   * transport protocol, as a result of an ebMS Pull Signal sent over the first
   * leg.
   */
  PULL ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pull"),
  /**
   * maps an exchange of two User messages respectively to the first and second
   * legs of a two-way underlying transport protocol.
   */
  SYNC ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sync");

  private final String m_sURI;

  private ETransportChannelBinding (@Nonnull @Nonempty final String sURI)
  {
    m_sURI = sURI;
  }

  @Nonnull
  @Nonempty
  public String getURI ()
  {
    return m_sURI;
  }

  @Nullable
  public static ETransportChannelBinding getFromURIOrNull (@Nullable final String sURI)
  {
    if (StringHelper.hasText (sURI))
      for (final ETransportChannelBinding e : values ())
        if (sURI.equals (e.getURI ()))
          return e;
    return null;
  }
}
