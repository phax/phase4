/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.smpclient.exception.SMPClientException;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;

/**
 * The default implementation of IPhase4PeppolEndpointDetailProvider using a
 * Peppol SMP Client to determine this information from an endpoint.
 *
 * @author Philip Helger
 */
public class Phase4PeppolEndpointDetailProviderSMP implements IPhase4PeppolEndpointDetailProvider
{
  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4PeppolEndpointDetailProviderSMP.class);
  // The transport profile to be used is fixed
  private static final ESMPTransportProfile TP = ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2;

  private final SMPClientReadOnly m_aSMPClient;
  private EndpointType m_aEndpoint;

  public Phase4PeppolEndpointDetailProviderSMP (@Nonnull final SMPClientReadOnly aSMPClient)
  {
    ValueEnforcer.notNull (aSMPClient, "SMPClient");
    m_aSMPClient = aSMPClient;
  }

  public void init (@Nonnull final IDocumentTypeIdentifier aDocTypeID,
                    @Nonnull final IProcessIdentifier aProcID,
                    @Nonnull final IParticipantIdentifier aReceiverID) throws Phase4PeppolException
  {
    ValueEnforcer.notNull (aDocTypeID, "DocTypeID");
    ValueEnforcer.notNull (aProcID, "ProcID");
    ValueEnforcer.notNull (aReceiverID, "ReceiverID");

    // Do the real SMP lookup only once
    if (m_aEndpoint == null)
    {
      // Perform SMP lookup
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Start performing SMP lookup (" +
                      aReceiverID.getURIEncoded () +
                      ", " +
                      aDocTypeID.getURIEncoded () +
                      ", " +
                      aProcID.getURIEncoded () +
                      ")");

      // Perform SMP lookup
      try
      {
        m_aEndpoint = m_aSMPClient.getEndpoint (aReceiverID, aDocTypeID, aProcID, TP);
        if (m_aEndpoint == null)
          throw new Phase4PeppolSMPException ("Failed to resolve SMP endpoint (" +
                                              aReceiverID.getURIEncoded () +
                                              ", " +
                                              aDocTypeID.getURIEncoded () +
                                              ", " +
                                              aProcID.getURIEncoded () +
                                              ", " +
                                              TP.getID () +
                                              ")");
      }
      catch (final SMPClientException ex)
      {
        throw new Phase4PeppolSMPException ("Failed to resolve SMP endpoint (" +
                                            aReceiverID.getURIEncoded () +
                                            ", " +
                                            aDocTypeID.getURIEncoded () +
                                            ", " +
                                            aProcID.getURIEncoded () +
                                            ", " +
                                            TP.getID () +
                                            ")",
                                            ex);
      }
    }
  }

  @Nullable
  public X509Certificate getReceiverAPCertificate () throws Phase4PeppolException
  {
    try
    {
      return SMPClientReadOnly.getEndpointCertificate (m_aEndpoint);
    }
    catch (final CertificateException ex)
    {
      throw new Phase4PeppolException ("Failed to extract AP certificate from SMP endpoint: " + m_aEndpoint, ex);
    }
  }

  @Nonnull
  @Nonempty
  public String getReceiverAPEndpointURL () throws Phase4PeppolException
  {
    final String sDestURL = SMPClientReadOnly.getEndpointAddress (m_aEndpoint);
    if (StringHelper.hasNoText (sDestURL))
      throw new Phase4PeppolException ("Failed to determine the destination URL from the SMP endpoint: " + m_aEndpoint);
    return null;
  }
}
