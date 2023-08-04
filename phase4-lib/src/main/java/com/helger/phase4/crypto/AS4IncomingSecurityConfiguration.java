/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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
package com.helger.phase4.crypto;

import java.security.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.string.ToStringGenerator;

/**
 * Default implementation of {@link IAS4IncomingSecurityConfiguration}.
 *
 * @author Philip Helger
 * @since 2.1.3
 */
@NotThreadSafe
public class AS4IncomingSecurityConfiguration implements IAS4IncomingSecurityConfiguration
{
  private Provider m_aSecurityProviderSign;
  private Provider m_aSecurityProviderCrypt;
  private IAS4DecryptParameterModifier m_aDecryptParameterModifier;

  public AS4IncomingSecurityConfiguration ()
  {}

  @Nullable
  public Provider getSecurityProviderSign ()
  {
    return m_aSecurityProviderSign;
  }

  @Nonnull
  public AS4IncomingSecurityConfiguration setSecurityProviderSign (@Nullable final Provider a)
  {
    m_aSecurityProviderSign = a;
    return this;
  }

  @Nullable
  public Provider getSecurityProviderCrypt ()
  {
    return m_aSecurityProviderCrypt;
  }

  @Nonnull
  public AS4IncomingSecurityConfiguration setSecurityProviderCrypt (@Nullable final Provider a)
  {
    m_aSecurityProviderCrypt = a;
    return this;
  }

  @Nullable
  public IAS4DecryptParameterModifier getDecryptParameterModifier ()
  {
    return m_aDecryptParameterModifier;
  }

  @Nonnull
  public AS4IncomingSecurityConfiguration setDecryptParameterModifier (@Nullable final IAS4DecryptParameterModifier a)
  {
    m_aDecryptParameterModifier = a;
    return this;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("SecurityProviderSign", m_aSecurityProviderSign)
                                       .append ("SecurityProviderCrypt", m_aSecurityProviderCrypt)
                                       .append ("DecryptParameterModifier", m_aDecryptParameterModifier)
                                       .getToString ();
  }

  @Nonnull
  public static AS4IncomingSecurityConfiguration createDefaultInstance ()
  {
    // No SecurityProviderSign
    // No SecurityProviderCrypt
    // No RequestDataModifier
    return new AS4IncomingSecurityConfiguration ();
  }
}
