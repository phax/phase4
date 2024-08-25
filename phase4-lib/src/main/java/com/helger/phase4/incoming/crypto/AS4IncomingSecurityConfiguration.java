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
package com.helger.phase4.incoming.crypto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.crypto.AS4CryptParams;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.IAS4DecryptParameterModifier;

/**
 * Default implementation of {@link IAS4IncomingSecurityConfiguration}.
 *
 * @author Philip Helger
 * @since 2.1.3
 */
@NotThreadSafe
public class AS4IncomingSecurityConfiguration implements IAS4IncomingSecurityConfiguration
{
  private AS4SigningParams m_aSigningParams;
  private AS4CryptParams m_aCryptParams;
  private IAS4DecryptParameterModifier m_aDecryptParameterModifier;

  public AS4IncomingSecurityConfiguration ()
  {}

  @Nullable
  public AS4SigningParams getSigningParams ()
  {
    return m_aSigningParams;
  }

  @Nonnull
  public AS4IncomingSecurityConfiguration setSigningParams (@Nullable final AS4SigningParams a)
  {
    m_aSigningParams = a;
    return this;
  }

  @Nullable
  public AS4CryptParams getCryptParams ()
  {
    return m_aCryptParams;
  }

  @Nonnull
  public AS4IncomingSecurityConfiguration setCryptParams (@Nullable final AS4CryptParams a)
  {
    m_aCryptParams = a;
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
    return new ToStringGenerator (null).append ("SigningParams", m_aSigningParams)
                                       .append ("CryptParams", m_aCryptParams)
                                       .append ("DecryptParameterModifier", m_aDecryptParameterModifier)
                                       .getToString ();
  }

  @Nonnull
  public static AS4IncomingSecurityConfiguration createDefaultInstance ()
  {
    // No SigningParams
    // No CryptParams
    // No DecryptParameterModifier
    return new AS4IncomingSecurityConfiguration ();
  }
}
