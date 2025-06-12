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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.util.Phase4Exception;
import com.helger.smpclient.exception.SMPClientBadRequestException;
import com.helger.smpclient.exception.SMPClientBadResponseException;
import com.helger.smpclient.exception.SMPClientException;
import com.helger.smpclient.exception.SMPClientUnauthorizedException;
import com.helger.smpclient.peppol.ISMPExtendedServiceMetadataProvider;
import com.helger.smpclient.peppol.ISMPServiceGroupProvider;
import com.helger.smpclient.peppol.ISMPServiceMetadataProvider;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.xsds.peppol.smp1.EndpointType;
import com.helger.xsds.peppol.smp1.SignedServiceMetadataType;

/**
 * Implementation of {@link IAS4EndpointDetailProvider} using a Peppol SMP Client to determine this
 * information from an endpoint.
 *
 * @author Philip Helger
 * @since 0.10.6
 */
public class AS4EndpointDetailProviderPeppol implements IAS4EndpointDetailProvider
{
  public static final ISMPTransportProfile DEFAULT_TRANSPORT_PROFILE = ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2;

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4EndpointDetailProviderPeppol.class);

  private final ISMPServiceGroupProvider m_aServiceGroupProvider;
  private final ISMPExtendedServiceMetadataProvider m_aServiceMetadataProvider;
  private ISMPTransportProfile m_aTP = DEFAULT_TRANSPORT_PROFILE;
  private EndpointType m_aEndpoint;

  public AS4EndpointDetailProviderPeppol (@Nonnull final ISMPServiceGroupProvider aServiceGroupProvider,
                                          @Nonnull final ISMPExtendedServiceMetadataProvider aServiceMetadataProvider)
  {
    ValueEnforcer.notNull (aServiceGroupProvider, "ServiceGroupProvider");
    ValueEnforcer.notNull (aServiceMetadataProvider, "ServiceMetadataProvider");
    m_aServiceGroupProvider = aServiceGroupProvider;
    m_aServiceMetadataProvider = aServiceMetadataProvider;
  }

  /**
   * @return The service group provider passed in the constructor. Never <code>null</code>.
   */
  @Nonnull
  public final ISMPServiceGroupProvider getServiceGroupProvider ()
  {
    return m_aServiceGroupProvider;
  }

  /**
   * @return The service metadata provider passed in the constructor. Never <code>null</code>.
   */
  @Nonnull
  public final ISMPServiceMetadataProvider getServiceMetadataProvider ()
  {
    return m_aServiceMetadataProvider;
  }

  /**
   * @return The transport profile to be used. Defaults to {@link #DEFAULT_TRANSPORT_PROFILE}.
   */
  @Nonnull
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
  @Nonnull
  public final AS4EndpointDetailProviderPeppol setTransportProfile (@Nonnull final ISMPTransportProfile aTP)
  {
    ValueEnforcer.notNull (aTP, "TransportProfile");
    m_aTP = aTP;
    return this;
  }

  /**
   * @return The endpoint resolved. May only be non-<code>null</code> after
   *         {@link #init(IDocumentTypeIdentifier, IProcessIdentifier, IParticipantIdentifier)} was
   *         called.
   */
  @Nullable
  public final EndpointType getEndpoint ()
  {
    return m_aEndpoint;
  }

  @Nullable
  @OverrideOnDemand
  protected SignedServiceMetadataType resolvedSchemeSpecificServiceMetadata (@Nonnull final IParticipantIdentifier aReceiverID,
                                                                             @Nonnull final IDocumentTypeIdentifier aDocTypeID) throws SMPClientException
  {
    // Resolve per scheme
    return m_aServiceMetadataProvider.getSchemeSpecificServiceMetadataOrNull (aReceiverID, aDocTypeID);
  }

  public void init (@Nonnull final IDocumentTypeIdentifier aDocTypeID,
                    @Nonnull final IProcessIdentifier aProcID,
                    @Nonnull final IParticipantIdentifier aReceiverID) throws Phase4Exception
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
                      ", " +
                      m_aTP.getID () +
                      ")");

      // Perform SMP lookup
      try
      {
        final boolean bWildcard;
        final SignedServiceMetadataType aSSM;

        // PFUOI 4.3.0
        aSSM = resolvedSchemeSpecificServiceMetadata (aReceiverID, aDocTypeID);
        // This is just a heuristic, because it is only evaluated in logging
        bWildcard = aSSM != null &&
                    PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_PEPPOL_DOCTYPE_WILDCARD.equals (aDocTypeID.getScheme ()) &&
                    aDocTypeID.getValue ().indexOf (PeppolIdentifierHelper.DOCUMENT_TYPE_WILDCARD_INDICATOR) >= 0;

        if (aSSM != null)
        {
          m_aEndpoint = SMPClientReadOnly.getEndpointAt (aSSM.getServiceMetadata (),
                                                         aProcID,
                                                         m_aTP,
                                                         PDTFactory.getCurrentLocalDateTime ());
        }

        if (m_aEndpoint == null)
        {
          throw new Phase4SMPException ("Failed to resolve SMP endpoint (" +
                                        aReceiverID.getURIEncoded () +
                                        ", " +
                                        aDocTypeID.getURIEncoded () +
                                        ", " +
                                        aProcID.getURIEncoded () +
                                        ", " +
                                        m_aTP.getID () +
                                        ")" +
                                        (bWildcard ? " [wildcard]" : " [static]"));
        }

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Successfully resolved SMP endpoint (" +
                        aReceiverID.getURIEncoded () +
                        ", " +
                        aDocTypeID.getURIEncoded () +
                        ", " +
                        aProcID.getURIEncoded () +
                        ", " +
                        m_aTP.getID () +
                        ")" +
                        (bWildcard ? " [wildcard]" : " [static]"));
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
      return SMPClientReadOnly.getEndpointCertificate (m_aEndpoint);
    }
    catch (final CertificateException ex)
    {
      throw new Phase4Exception ("Failed to extract AP certificate from SMP endpoint: " + m_aEndpoint, ex)
                                                                                                          .setRetryFeasible (false);
    }
  }

  @Nonnull
  @Nonempty
  public String getReceiverAPEndpointURL () throws Phase4Exception
  {
    final String sDestURL = SMPClientReadOnly.getEndpointAddress (m_aEndpoint);
    if (StringHelper.hasNoText (sDestURL))
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
    return new ToStringGenerator (null).append ("ServiceGroupProvider", m_aServiceGroupProvider)
                                       .append ("ServiceMetadataProvider", m_aServiceMetadataProvider)
                                       .append ("TransportProfile", m_aTP)
                                       .appendIfNotNull ("Endpoint", m_aEndpoint)
                                       .getToString ();
  }

  /**
   * Create a new {@link AS4EndpointDetailProviderPeppol} based on the provided SMP client.
   *
   * @param aSMPClient
   *        The SMP client to use. May not be <code>null</code>
   * @return Never <code>null</code>.
   * @since 2.8.1
   */
  @Nonnull
  public static AS4EndpointDetailProviderPeppol create (@Nonnull final SMPClientReadOnly aSMPClient)
  {
    return new AS4EndpointDetailProviderPeppol (aSMPClient, aSMPClient);
  }
}
