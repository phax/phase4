/**
 * Copyright (C) 2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.dynamicdiscovery;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;

/**
 * Implementation of {@link IAS4EndpointDetailProvider} that uses constant
 * values.
 *
 * @author Philip Helger
 * @since 0.10.6
 */
public class AS4EndpointDetailProviderConstant implements IAS4EndpointDetailProvider
{
  private final X509Certificate m_aReceiverCert;
  private final String m_sDestURL;

  public AS4EndpointDetailProviderConstant (@Nullable final X509Certificate aReceiverCert, @Nonnull @Nonempty final String sDestURL)
  {
    ValueEnforcer.notEmpty (sDestURL, "DestURL");
    m_aReceiverCert = aReceiverCert;
    m_sDestURL = sDestURL;
  }

  public void init (@Nonnull final IDocumentTypeIdentifier aDocTypeID,
                    @Nonnull final IProcessIdentifier aProcID,
                    @Nonnull final IParticipantIdentifier aReceiverID)
  {
    // Not needed for this implementation
  }

  @Nullable
  public X509Certificate getReceiverAPCertificate ()
  {
    return m_aReceiverCert;
  }

  @Nonnull
  @Nonempty
  public String getReceiverAPEndpointURL ()
  {
    return m_sDestURL;
  }
}
