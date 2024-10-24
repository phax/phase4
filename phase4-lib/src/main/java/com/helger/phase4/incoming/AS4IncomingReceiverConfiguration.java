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
package com.helger.phase4.incoming;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.config.AS4Configuration;

/**
 * Default implementation of {@link IAS4IncomingReceiverConfiguration}.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
@NotThreadSafe
public class AS4IncomingReceiverConfiguration implements IAS4IncomingReceiverConfiguration
{
  private String m_sReceiverEndpointAddress;

  public AS4IncomingReceiverConfiguration ()
  {
    // Set default value from configuration
    setReceiverEndpointAddress (AS4Configuration.getThisEndpointAddress ());
  }

  @Nullable
  public final String getReceiverEndpointAddress ()
  {
    return m_sReceiverEndpointAddress;
  }

  @Nonnull
  public final AS4IncomingReceiverConfiguration setReceiverEndpointAddress (@Nullable final String s)
  {
    m_sReceiverEndpointAddress = s;
    return this;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ReceiverEndpointAddress", m_sReceiverEndpointAddress).getToString ();
  }
}
