/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.messaging.http;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.array.ArrayHelper;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringHex;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.security.certificate.CertificateHelper;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

/**
 * Immutable domain object with the parameters of the TLS connection that was used to transmit a
 * single AS4 message. Instances are created for outgoing connections from the {@link SSLSession} of
 * the underlying Apache HttpClient connection, and for incoming connections from the TLS attributes
 * the Servlet container provides.
 * <p>
 * Not all fields are available in all cases:
 * <ul>
 * <li>The protocol version is not part of the Servlet specification, so it is only present for
 * outgoing connections.</li>
 * <li>The key size is only provided by the Servlet container, so it is only present for incoming
 * connections.</li>
 * <li>The peer certificates are only filled for outgoing connections. For an incoming request, the
 * certificates the client presented are available via
 * <code>IAS4IncomingMessageMetadata.remoteTlsClientCerts()</code> and are therefore not repeated
 * here.</li>
 * </ul>
 *
 * @author Philip Helger
 * @since 4.8.0
 */
@Immutable
public class AS4TlsConnectionDetails
{
  public static final String JSON_PROTOCOL = "protocol";
  public static final String JSON_CIPHER_SUITE = "cipherSuite";
  public static final String JSON_KEY_SIZE = "keySize";
  public static final String JSON_SESSION_ID = "sessionId";
  public static final String JSON_PEER_CERTS = "peerCerts";

  /** Constant indicating, that the TLS key size is unknown */
  public static final int KEY_SIZE_UNDEFINED = -1;

  /** The protocol name of the JSSE "no connection" placeholder session */
  private static final String PROTOCOL_NONE = "NONE";
  /** The cipher suite name of the JSSE "no connection" placeholder session */
  private static final String CIPHER_SUITE_NONE = "SSL_NULL_WITH_NULL_NULL";

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4TlsConnectionDetails.class);

  private final String m_sProtocol;
  private final String m_sCipherSuite;
  private final int m_nKeySize;
  private final String m_sSessionID;
  private final ICommonsList <X509Certificate> m_aPeerCerts;

  /**
   * Constructor
   *
   * @param sProtocol
   *        The negotiated TLS protocol version - e.g. <code>TLSv1.3</code>. May be
   *        <code>null</code>.
   * @param sCipherSuite
   *        The negotiated TLS cipher suite - e.g. <code>TLS_AES_256_GCM_SHA384</code>. May be
   *        <code>null</code>.
   * @param nKeySize
   *        The bit size of the algorithm in use. Use {@link #KEY_SIZE_UNDEFINED} if unknown.
   * @param sSessionID
   *        The hex encoded TLS session ID. May be <code>null</code>.
   * @param aPeerCerts
   *        The certificate chain the remote peer presented during the handshake (index 0 =
   *        leaf/server certificate). May be <code>null</code>.
   */
  public AS4TlsConnectionDetails (@Nullable final String sProtocol,
                                  @Nullable final String sCipherSuite,
                                  final int nKeySize,
                                  @Nullable final String sSessionID,
                                  @Nullable final ICommonsList <X509Certificate> aPeerCerts)
  {
    m_sProtocol = sProtocol;
    m_sCipherSuite = sCipherSuite;
    m_nKeySize = nKeySize < 0 ? KEY_SIZE_UNDEFINED : nKeySize;
    m_sSessionID = sSessionID;
    m_aPeerCerts = aPeerCerts == null || aPeerCerts.isEmpty () ? null : aPeerCerts;
  }

  /**
   * @return The negotiated TLS protocol version - e.g. <code>TLSv1.3</code>. May be
   *         <code>null</code>, especially for incoming requests, because the Servlet specification
   *         has no attribute for it.
   */
  @Nullable
  public final String getProtocol ()
  {
    return m_sProtocol;
  }

  public final boolean hasProtocol ()
  {
    return StringHelper.isNotEmpty (m_sProtocol);
  }

  /**
   * @return The negotiated TLS cipher suite - e.g. <code>TLS_AES_256_GCM_SHA384</code>. May be
   *         <code>null</code>.
   */
  @Nullable
  public final String getCipherSuite ()
  {
    return m_sCipherSuite;
  }

  public final boolean hasCipherSuite ()
  {
    return StringHelper.isNotEmpty (m_sCipherSuite);
  }

  /**
   * @return The bit size of the algorithm in use, or {@link #KEY_SIZE_UNDEFINED} if unknown. This
   *         is only provided by the Servlet container, so it is undefined for outgoing connections.
   */
  @CheckForSigned
  public final int getKeySize ()
  {
    return m_nKeySize;
  }

  public final boolean hasKeySize ()
  {
    return m_nKeySize > 0;
  }

  /**
   * @return The hex encoded TLS session ID. May be <code>null</code>. It can be used to correlate
   *         this connection with the log files of the other side.
   */
  @Nullable
  public final String getSessionID ()
  {
    return m_sSessionID;
  }

  public final boolean hasSessionID ()
  {
    return StringHelper.isNotEmpty (m_sSessionID);
  }

  /**
   * @return The certificate chain the remote peer presented during the handshake (index 0 =
   *         leaf/server certificate). May be <code>null</code>. This is only filled for outgoing
   *         connections.
   */
  @Nullable
  @ReturnsMutableObject
  public final ICommonsList <X509Certificate> peerCerts ()
  {
    return m_aPeerCerts;
  }

  public final boolean hasPeerCerts ()
  {
    return m_aPeerCerts != null;
  }

  /**
   * @return <code>true</code> if not a single piece of information is contained in this object,
   *         <code>false</code> otherwise.
   */
  public final boolean isEmpty ()
  {
    return !hasProtocol () && !hasCipherSuite () && !hasKeySize () && !hasSessionID () && !hasPeerCerts ();
  }

  /**
   * @return The TLS connection details as a JSON object, including the peer certificates. Only
   *         elements that are present, are contained. Never <code>null</code>.
   * @see #getAsJsonObject(boolean)
   */
  @NonNull
  public IJsonObject getAsJsonObject ()
  {
    return getAsJsonObject (true);
  }

  /**
   * @param bIncludePeerCerts
   *        <code>true</code> to include the PEM encoded peer certificates, <code>false</code> to
   *        emit the negotiated connection parameters only. The certificates are pretty large, so
   *        they should be omitted where the size of the result matters.
   * @return The TLS connection details as a JSON object. Only elements that are present, are
   *         contained. Never <code>null</code>.
   */
  @NonNull
  public IJsonObject getAsJsonObject (final boolean bIncludePeerCerts)
  {
    final IJsonObject ret = new JsonObject ();
    if (hasProtocol ())
      ret.add (JSON_PROTOCOL, m_sProtocol);
    if (hasCipherSuite ())
      ret.add (JSON_CIPHER_SUITE, m_sCipherSuite);
    if (hasKeySize ())
      ret.add (JSON_KEY_SIZE, m_nKeySize);
    if (hasSessionID ())
      ret.add (JSON_SESSION_ID, m_sSessionID);
    if (bIncludePeerCerts && hasPeerCerts ())
    {
      final IJsonArray aCerts = new JsonArray ();
      for (final X509Certificate aCert : m_aPeerCerts)
        aCerts.add (CertificateHelper.getPEMEncodedCertificate (aCert));
      ret.add (JSON_PEER_CERTS, aCerts);
    }
    return ret;
  }

  /**
   * @param sNamespaceURI
   *        The namespace URI to be used. May be <code>null</code>.
   * @param sTagName
   *        The tag name to use for the created element. May neither be <code>null</code> nor empty.
   * @return The TLS connection details as a MicroDOM element, including the peer certificates.
   *         Only elements that are present, are contained. Never <code>null</code>.
   * @see #getAsMicroElement(String, String, boolean)
   */
  @NonNull
  public IMicroElement getAsMicroElement (@Nullable final String sNamespaceURI,
                                          @NonNull @Nonempty final String sTagName)
  {
    return getAsMicroElement (sNamespaceURI, sTagName, true);
  }

  /**
   * @param sNamespaceURI
   *        The namespace URI to be used. May be <code>null</code>.
   * @param sTagName
   *        The tag name to use for the created element. May neither be <code>null</code> nor empty.
   * @param bIncludePeerCerts
   *        <code>true</code> to include the PEM encoded peer certificates, <code>false</code> to
   *        emit the negotiated connection parameters only. The certificates are pretty large, so
   *        they should be omitted where the size of the result matters.
   * @return The TLS connection details as a MicroDOM element. Only elements that are present, are
   *         contained. Never <code>null</code>.
   */
  @NonNull
  public IMicroElement getAsMicroElement (@Nullable final String sNamespaceURI,
                                          @NonNull @Nonempty final String sTagName,
                                          final boolean bIncludePeerCerts)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    if (hasProtocol ())
      ret.addElementNS (sNamespaceURI, "Protocol").addText (m_sProtocol);
    if (hasCipherSuite ())
      ret.addElementNS (sNamespaceURI, "CipherSuite").addText (m_sCipherSuite);
    if (hasKeySize ())
      ret.addElementNS (sNamespaceURI, "KeySize").addText (m_nKeySize);
    if (hasSessionID ())
      ret.addElementNS (sNamespaceURI, "SessionID").addText (m_sSessionID);
    if (bIncludePeerCerts && hasPeerCerts ())
    {
      final IMicroElement aCerts = ret.addElementNS (sNamespaceURI, "PeerCerts");
      for (final X509Certificate aCert : m_aPeerCerts)
        aCerts.addElementNS (sNamespaceURI, "Cert").addText (CertificateHelper.getPEMEncodedCertificate (aCert));
    }
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Protocol", m_sProtocol)
                                       .append ("CipherSuite", m_sCipherSuite)
                                       .append ("KeySize", m_nKeySize)
                                       .append ("SessionID", m_sSessionID)
                                       .append ("PeerCerts", m_aPeerCerts)
                                       .getToString ();
  }

  @Nullable
  private static String _getNonPlaceholder (@Nullable final String sValue, @NonNull final String sPlaceholder)
  {
    // The JSSE placeholder session for "no connection" uses dedicated names that carry no
    // information
    return StringHelper.isEmpty (sValue) || sPlaceholder.equals (sValue) ? null : sValue;
  }

  /**
   * Create the TLS connection details from the {@link SSLSession} of an outgoing connection.
   *
   * @param aSSLSession
   *        The SSL session to read from. May be <code>null</code>, e.g. for a plain HTTP
   *        connection.
   * @return <code>null</code> if no SSL session was provided or if it contains no usable
   *         information.
   */
  @Nullable
  public static AS4TlsConnectionDetails createFromSSLSession (@Nullable final SSLSession aSSLSession)
  {
    if (aSSLSession == null)
      return null;

    final String sProtocol = _getNonPlaceholder (aSSLSession.getProtocol (), PROTOCOL_NONE);
    final String sCipherSuite = _getNonPlaceholder (aSSLSession.getCipherSuite (), CIPHER_SUITE_NONE);

    final byte [] aSessionID = aSSLSession.getId ();
    final String sSessionID = ArrayHelper.isEmpty (aSessionID) ? null : StringHex.getHexEncoded (aSessionID);

    ICommonsList <X509Certificate> aPeerCerts = null;
    try
    {
      final Certificate [] aCerts = aSSLSession.getPeerCertificates ();
      if (ArrayHelper.isNotEmpty (aCerts))
      {
        aPeerCerts = new CommonsArrayList <> (aCerts.length);
        for (final Certificate aCert : aCerts)
          if (aCert instanceof X509Certificate)
            aPeerCerts.add ((X509Certificate) aCert);
      }
    }
    catch (final SSLPeerUnverifiedException ex)
    {
      // Happens e.g. for anonymous cipher suites - not an error
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("The remote TLS peer is not verified, so no peer certificates are available: " +
                      ex.getMessage ());
    }

    final AS4TlsConnectionDetails ret = new AS4TlsConnectionDetails (sProtocol,
                                                                     sCipherSuite,
                                                                     KEY_SIZE_UNDEFINED,
                                                                     sSessionID,
                                                                     aPeerCerts);
    return ret.isEmpty () ? null : ret;
  }
}
