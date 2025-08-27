/*
 * Copyright (C) 2020-2025 Philip Helger (www.helger.com)
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

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.util.Phase4Exception;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Implementation of {@link IAS4EndpointDetailProvider} that uses constant values.
 *
 * @author Philip Helger
 * @since 0.10.6
 */
public class AS4EndpointDetailProviderConstant implements IAS4EndpointDetailProvider
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4EndpointDetailProviderConstant.class);

  private final X509Certificate m_aReceiverCert;
  private final String m_sDestURL;
  private final String m_sTechnicalContact;

  public AS4EndpointDetailProviderConstant (@Nullable final X509Certificate aReceiverCert,
                                            @Nonnull @Nonempty final String sDestURL)
  {
    this (aReceiverCert, sDestURL, (String) null);
  }

  public AS4EndpointDetailProviderConstant (@Nullable final X509Certificate aReceiverCert,
                                            @Nonnull @Nonempty final String sDestURL,
                                            @Nullable final String sTechnicalContact)
  {
    ValueEnforcer.notEmpty (sDestURL, "DestURL");
    m_aReceiverCert = aReceiverCert;
    m_sDestURL = sDestURL;
    m_sTechnicalContact = sTechnicalContact;

    if (aReceiverCert != null)
    {
      // Note: this is informational only. If the certificate is expired and you
      // don't want that, you need check that before
      try
      {
        aReceiverCert.checkValidity ();
      }
      catch (final CertificateExpiredException ex)
      {
        LOGGER.warn ("The provided Endpoint certificate is already expired. Please use a different one: " +
                     ex.getMessage ());
      }
      catch (final CertificateNotYetValidException ex)
      {
        LOGGER.warn ("The provided Endpoint certificate is not yet valid. Please use a different one: " +
                     ex.getMessage ());
      }
    }
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

  @Nullable
  public String getReceiverTechnicalContact () throws Phase4Exception
  {
    return m_sTechnicalContact;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ReceiverCert", m_aReceiverCert)
                                       .append ("DestURL", m_sDestURL)
                                       .appendIfNotNull ("TechnicalContact", m_sTechnicalContact)
                                       .getToString ();
  }
}
