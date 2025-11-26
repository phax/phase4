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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.util.Phase4Exception;
import com.helger.smpclient.bdxr1.BDXRClientReadOnly;
import com.helger.smpclient.bdxr1.IBDXRExtendedServiceMetadataProvider;
import com.helger.smpclient.exception.SMPClientBadRequestException;
import com.helger.smpclient.exception.SMPClientBadResponseException;
import com.helger.smpclient.exception.SMPClientException;
import com.helger.smpclient.exception.SMPClientUnauthorizedException;
import com.helger.xsds.bdxr.smp1.EndpointType;

/**
 * Implementation of {@link IAS4EndpointDetailProvider} using an OASIS BDXR SMP v1 Client to
 * determine this information from an endpoint.
 *
 * @author Philip Helger
 * @since 0.10.6
 */
public class AS4EndpointDetailProviderBDXR implements IAS4EndpointDetailProvider
{
  public static final ISMPTransportProfile DEFAULT_TRANSPORT_PROFILE = ESMPTransportProfile.TRANSPORT_PROFILE_BDXR_AS4;

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4EndpointDetailProviderBDXR.class);

  private final IBDXRExtendedServiceMetadataProvider m_aSMPClient;
  private ISMPTransportProfile m_aTP = DEFAULT_TRANSPORT_PROFILE;
  private EndpointType m_aEndpoint;

  public AS4EndpointDetailProviderBDXR (@NonNull final IBDXRExtendedServiceMetadataProvider aSMPClient)
  {
    ValueEnforcer.notNull (aSMPClient, "SMPClient");
    m_aSMPClient = aSMPClient;
  }

  /**
   * @return The service metadata provider passed in the constructor. Never <code>null</code>.
   */
  @NonNull
  public final IBDXRExtendedServiceMetadataProvider getServiceMetadataProvider ()
  {
    return m_aSMPClient;
  }

  /**
   * @return The transport profile to be used. Defaults to {@link #DEFAULT_TRANSPORT_PROFILE}.
   */
  @NonNull
  public final ISMPTransportProfile getTransportProfile ()
  {
    return m_aTP;
  }

  /**
   * Change the transport profile to be used. This only has an effect if it is called prior to
   * {@link #init(IDocumentTypeIdentifier, IProcessIdentifier, IParticipantIdentifier)}.
   *
   * @param aTP
   *        The transport profile to be used. May not be <code>null</code>.
   * @return this for chaining.
   */
  @NonNull
  public final AS4EndpointDetailProviderBDXR setTransportProfile (@NonNull final ISMPTransportProfile aTP)
  {
    ValueEnforcer.notNull (aTP, "TransportProfile");
    m_aTP = aTP;
    return this;
  }

  /**
   * @return The endpoint resolved. May only be non-<code>null</code> if
   *         {@link #init(IDocumentTypeIdentifier, IProcessIdentifier, IParticipantIdentifier)} was
   *         called.
   */
  @Nullable
  public final EndpointType getEndpoint ()
  {
    return m_aEndpoint;
  }

  public void init (@NonNull final IDocumentTypeIdentifier aDocTypeID,
                    @NonNull final IProcessIdentifier aProcID,
                    @NonNull final IParticipantIdentifier aReceiverID) throws Phase4Exception
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
        m_aEndpoint = m_aSMPClient.getEndpoint (aReceiverID, aDocTypeID, aProcID, m_aTP);
        if (m_aEndpoint == null)
          throw new Phase4SMPException ("Failed to resolve SMP endpoint (" +
                                        aReceiverID.getURIEncoded () +
                                        ", " +
                                        aDocTypeID.getURIEncoded () +
                                        ", " +
                                        aProcID.getURIEncoded () +
                                        ", " +
                                        m_aTP.getID () +
                                        ")");

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Successfully resolved SMP endpoint (" +
                        aReceiverID.getURIEncoded () +
                        ", " +
                        aDocTypeID.getURIEncoded () +
                        ", " +
                        aProcID.getURIEncoded () +
                        ", " +
                        m_aTP.getID () +
                        ")");
      }
      catch (final SMPClientException ex)
      {
        final boolean bRetryFeasible = ex instanceof SMPClientBadRequestException ||
          ex instanceof SMPClientBadResponseException ||
          ex instanceof SMPClientUnauthorizedException ||
          ex.getClass ().equals (SMPClientException.class);
        throw new Phase4SMPException ("Failed to resolve SMP endpoint (" +
                                      aReceiverID.getURIEncoded () +
                                      ", " +
                                      aDocTypeID.getURIEncoded () +
                                      ", " +
                                      aProcID.getURIEncoded () +
                                      ", " +
                                      m_aTP.getID () +
                                      ")",
                                      ex).setRetryFeasible (bRetryFeasible);
      }
    }
  }

  @Nullable
  public X509Certificate getReceiverAPCertificate () throws Phase4Exception
  {
    try
    {
      return BDXRClientReadOnly.getEndpointCertificate (m_aEndpoint);
    }
    catch (final CertificateException ex)
    {
      throw new Phase4Exception ("Failed to extract AP certificate from SMP endpoint: " + m_aEndpoint, ex)
                                                                                                          .setRetryFeasible (false);
    }
  }

  @NonNull
  @Nonempty
  public String getReceiverAPEndpointURL () throws Phase4Exception
  {
    final String sDestURL = BDXRClientReadOnly.getEndpointAddress (m_aEndpoint);
    if (StringHelper.isEmpty (sDestURL))
      throw new Phase4Exception ("Failed to determine the destination URL from the SMP endpoint: " + m_aEndpoint);
    return sDestURL;
  }

  @Nullable
  public String getReceiverTechnicalContact () throws Phase4Exception
  {
    return m_aEndpoint.getTechnicalContactUrl ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("SMPClient", m_aSMPClient)
                                       .append ("TransportProfile", m_aTP)
                                       .appendIfNotNull ("Endpoint", m_aEndpoint)
                                       .getToString ();
  }
}
