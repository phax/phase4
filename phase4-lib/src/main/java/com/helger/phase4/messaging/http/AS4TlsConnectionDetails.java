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

import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
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
 * Immutable domain object with the parameters of the TLS layer of the connection that was used to
 * transmit a single AS4 message. Instances are created for outgoing connections from the
 * {@link SSLSession} of the underlying Apache HttpClient connection, and for incoming connections
 * from the TLS attributes the Servlet container provides.
 * <p>
 * Not all fields are available in all cases:
 * <ul>
 * <li>The protocol version, the requested server names (SNI) and the principals are not part of the
 * Servlet specification, so they are only present for outgoing connections.</li>
 * <li>The key size is only provided by the Servlet container, so it is only present for incoming
 * connections.</li>
 * <li>The peer and the local certificates are only filled for outgoing connections. For an incoming
 * request, the certificates the client presented are available via
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
  public static final String JSON_REQUESTED_SERVER_NAMES = "requestedServerNames";
  public static final String JSON_PEER_PRINCIPAL = "peerPrincipal";
  public static final String JSON_PEER_CERTS = "peerCerts";
  public static final String JSON_LOCAL_PRINCIPAL = "localPrincipal";
  public static final String JSON_LOCAL_CERTS = "localCerts";

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
  private final ICommonsList <String> m_aRequestedServerNames;
  private final String m_sPeerPrincipal;
  private final ICommonsList <X509Certificate> m_aPeerCerts;
  private final String m_sLocalPrincipal;
  private final ICommonsList <X509Certificate> m_aLocalCerts;

  @Nullable
  private static <T> ICommonsList <T> _nullIfEmpty (@Nullable final ICommonsList <T> aList)
  {
    return aList == null || aList.isEmpty () ? null : aList;
  }

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
   * @param aRequestedServerNames
   *        The server names that were requested via the TLS SNI extension. May be
   *        <code>null</code>.
   * @param sPeerPrincipal
   *        The name of the principal of the remote peer. May be <code>null</code>.
   * @param aPeerCerts
   *        The certificate chain the remote peer presented during the handshake (index 0 =
   *        leaf/server certificate). May be <code>null</code>.
   * @param sLocalPrincipal
   *        The name of the local principal that was sent to the remote peer. May be
   *        <code>null</code>.
   * @param aLocalCerts
   *        The local certificate chain that was sent to the remote peer (index 0 = leaf/client
   *        certificate). May be <code>null</code>.
   */
  public AS4TlsConnectionDetails (@Nullable final String sProtocol,
                                  @Nullable final String sCipherSuite,
                                  final int nKeySize,
                                  @Nullable final String sSessionID,
                                  @Nullable final ICommonsList <String> aRequestedServerNames,
                                  @Nullable final String sPeerPrincipal,
                                  @Nullable final ICommonsList <X509Certificate> aPeerCerts,
                                  @Nullable final String sLocalPrincipal,
                                  @Nullable final ICommonsList <X509Certificate> aLocalCerts)
  {
    m_sProtocol = sProtocol;
    m_sCipherSuite = sCipherSuite;
    m_nKeySize = nKeySize < 0 ? KEY_SIZE_UNDEFINED : nKeySize;
    m_sSessionID = sSessionID;
    m_aRequestedServerNames = _nullIfEmpty (aRequestedServerNames);
    m_sPeerPrincipal = sPeerPrincipal;
    m_aPeerCerts = _nullIfEmpty (aPeerCerts);
    m_sLocalPrincipal = sLocalPrincipal;
    m_aLocalCerts = _nullIfEmpty (aLocalCerts);
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
   * @return The server names that were requested via the TLS SNI extension. May be
   *         <code>null</code>. This is only filled for outgoing connections and helps to analyse,
   *         why the other side presented a specific TLS certificate.
   */
  @Nullable
  @ReturnsMutableObject
  public final ICommonsList <String> requestedServerNames ()
  {
    return m_aRequestedServerNames;
  }

  public final boolean hasRequestedServerNames ()
  {
    return m_aRequestedServerNames != null;
  }

  /**
   * @return The name of the principal of the remote peer - usually the subject DN of the TLS
   *         certificate of the other side. May be <code>null</code>. This is the cheap alternative
   *         to evaluating {@link #peerCerts()}.
   */
  @Nullable
  public final String getPeerPrincipal ()
  {
    return m_sPeerPrincipal;
  }

  public final boolean hasPeerPrincipal ()
  {
    return StringHelper.isNotEmpty (m_sPeerPrincipal);
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
   * @return The name of the local principal that was sent to the remote peer - usually the subject
   *         DN of the TLS client certificate. May be <code>null</code> and is only filled, if TLS
   *         client authentication was used.
   */
  @Nullable
  public final String getLocalPrincipal ()
  {
    return m_sLocalPrincipal;
  }

  public final boolean hasLocalPrincipal ()
  {
    return StringHelper.isNotEmpty (m_sLocalPrincipal);
  }

  /**
   * @return The local certificate chain that was sent to the remote peer (index 0 = leaf/client
   *         certificate). May be <code>null</code> and is only filled, if TLS client authentication
   *         was used.
   */
  @Nullable
  @ReturnsMutableObject
  public final ICommonsList <X509Certificate> localCerts ()
  {
    return m_aLocalCerts;
  }

  public final boolean hasLocalCerts ()
  {
    return m_aLocalCerts != null;
  }

  /**
   * @return <code>true</code> if not a single piece of information is contained in this object,
   *         <code>false</code> otherwise.
   */
  public final boolean isEmpty ()
  {
    return !hasProtocol () &&
           !hasCipherSuite () &&
           !hasKeySize () &&
           !hasSessionID () &&
           !hasRequestedServerNames () &&
           !hasPeerPrincipal () &&
           !hasPeerCerts () &&
           !hasLocalPrincipal () &&
           !hasLocalCerts ();
  }

  @NonNull
  private static IJsonArray _getAsJsonCerts (@NonNull final ICommonsList <X509Certificate> aCerts)
  {
    final IJsonArray ret = new JsonArray ();
    for (final X509Certificate aCert : aCerts)
      ret.add (CertificateHelper.getPEMEncodedCertificate (aCert));
    return ret;
  }

  /**
   * @return The TLS connection details as a JSON object, including the certificates. Only elements
   *         that are present, are contained. Never <code>null</code>.
   * @see #getAsJsonObject(boolean)
   */
  @NonNull
  public IJsonObject getAsJsonObject ()
  {
    return getAsJsonObject (true);
  }

  /**
   * @param bIncludeCerts
   *        <code>true</code> to include the PEM encoded peer and local certificates,
   *        <code>false</code> to emit the negotiated connection parameters only. The certificates
   *        are pretty large, so they should be omitted where the size of the result matters.
   * @return The TLS connection details as a JSON object. Only elements that are present, are
   *         contained. Never <code>null</code>.
   */
  @NonNull
  public IJsonObject getAsJsonObject (final boolean bIncludeCerts)
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
    if (hasRequestedServerNames ())
      ret.add (JSON_REQUESTED_SERVER_NAMES, new JsonArray ().addAll (m_aRequestedServerNames));
    if (hasPeerPrincipal ())
      ret.add (JSON_PEER_PRINCIPAL, m_sPeerPrincipal);
    if (bIncludeCerts && hasPeerCerts ())
      ret.add (JSON_PEER_CERTS, _getAsJsonCerts (m_aPeerCerts));
    if (hasLocalPrincipal ())
      ret.add (JSON_LOCAL_PRINCIPAL, m_sLocalPrincipal);
    if (bIncludeCerts && hasLocalCerts ())
      ret.add (JSON_LOCAL_CERTS, _getAsJsonCerts (m_aLocalCerts));
    return ret;
  }

  private static void _addMicroCerts (@NonNull final IMicroElement aParent,
                                      @Nullable final String sNamespaceURI,
                                      @NonNull final ICommonsList <X509Certificate> aCerts)
  {
    for (final X509Certificate aCert : aCerts)
      aParent.addElementNS (sNamespaceURI, "Cert").addText (CertificateHelper.getPEMEncodedCertificate (aCert));
  }

  /**
   * @param sNamespaceURI
   *        The namespace URI to be used. May be <code>null</code>.
   * @param sTagName
   *        The tag name to use for the created element. May neither be <code>null</code> nor empty.
   * @return The TLS connection details as a MicroDOM element, including the certificates. Only
   *         elements that are present, are contained. Never <code>null</code>.
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
   * @param bIncludeCerts
   *        <code>true</code> to include the PEM encoded peer and local certificates,
   *        <code>false</code> to emit the negotiated connection parameters only. The certificates
   *        are pretty large, so they should be omitted where the size of the result matters.
   * @return The TLS connection details as a MicroDOM element. Only elements that are present, are
   *         contained. Never <code>null</code>.
   */
  @NonNull
  public IMicroElement getAsMicroElement (@Nullable final String sNamespaceURI,
                                          @NonNull @Nonempty final String sTagName,
                                          final boolean bIncludeCerts)
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
    if (hasRequestedServerNames ())
    {
      final IMicroElement aNames = ret.addElementNS (sNamespaceURI, "RequestedServerNames");
      for (final String sName : m_aRequestedServerNames)
        aNames.addElementNS (sNamespaceURI, "ServerName").addText (sName);
    }
    if (hasPeerPrincipal ())
      ret.addElementNS (sNamespaceURI, "PeerPrincipal").addText (m_sPeerPrincipal);
    if (bIncludeCerts && hasPeerCerts ())
      _addMicroCerts (ret.addElementNS (sNamespaceURI, "PeerCerts"), sNamespaceURI, m_aPeerCerts);
    if (hasLocalPrincipal ())
      ret.addElementNS (sNamespaceURI, "LocalPrincipal").addText (m_sLocalPrincipal);
    if (bIncludeCerts && hasLocalCerts ())
      _addMicroCerts (ret.addElementNS (sNamespaceURI, "LocalCerts"), sNamespaceURI, m_aLocalCerts);
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Protocol", m_sProtocol)
                                       .append ("CipherSuite", m_sCipherSuite)
                                       .append ("KeySize", m_nKeySize)
                                       .append ("SessionID", m_sSessionID)
                                       .append ("RequestedServerNames", m_aRequestedServerNames)
                                       .append ("PeerPrincipal", m_sPeerPrincipal)
                                       .append ("PeerCerts", m_aPeerCerts)
                                       .append ("LocalPrincipal", m_sLocalPrincipal)
                                       .append ("LocalCerts", m_aLocalCerts)
                                       .getToString ();
  }

  @Nullable
  private static String _getNonPlaceholder (@Nullable final String sValue, @NonNull final String sPlaceholder)
  {
    // The JSSE placeholder session for "no connection" uses dedicated names that carry no
    // information
    return StringHelper.isEmpty (sValue) || sPlaceholder.equals (sValue) ? null : sValue;
  }

  @Nullable
  private static String _getPrincipalName (@Nullable final Principal aPrincipal)
  {
    return aPrincipal == null ? null : aPrincipal.getName ();
  }

  @Nullable
  private static ICommonsList <X509Certificate> _getX509Certs (final Certificate @Nullable [] aCerts)
  {
    if (ArrayHelper.isEmpty (aCerts))
      return null;

    final ICommonsList <X509Certificate> ret = new CommonsArrayList <> (aCerts.length);
    for (final Certificate aCert : aCerts)
      if (aCert instanceof final X509Certificate aX509Cert)
        ret.add (aX509Cert);
    return ret;
  }

  @Nullable
  private static ICommonsList <String> _getRequestedServerNames (@NonNull final SSLSession aSSLSession)
  {
    if (!(aSSLSession instanceof final ExtendedSSLSession aExtSession))
      return null;

    try
    {
      final List <SNIServerName> aNames = aExtSession.getRequestedServerNames ();
      if (aNames == null || aNames.isEmpty ())
        return null;

      final ICommonsList <String> ret = new CommonsArrayList <> (aNames.size ());
      for (final SNIServerName aName : aNames)
        ret.add (aName instanceof final SNIHostName aHostName ? aHostName.getAsciiName () : aName.toString ());
      return ret;
    }
    catch (final UnsupportedOperationException ex)
    {
      // The default implementation of ExtendedSSLSession throws this
      return null;
    }
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

    final ICommonsList <String> aRequestedServerNames = _getRequestedServerNames (aSSLSession);

    String sPeerPrincipal = null;
    ICommonsList <X509Certificate> aPeerCerts = null;
    try
    {
      sPeerPrincipal = _getPrincipalName (aSSLSession.getPeerPrincipal ());
      aPeerCerts = _getX509Certs (aSSLSession.getPeerCertificates ());
    }
    catch (final SSLPeerUnverifiedException ex)
    {
      // Happens e.g. for anonymous cipher suites - not an error
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("The remote TLS peer is not verified, so no peer details are available: " + ex.getMessage ());
    }

    // Only filled if TLS client authentication was used
    final String sLocalPrincipal = _getPrincipalName (aSSLSession.getLocalPrincipal ());
    final ICommonsList <X509Certificate> aLocalCerts = _getX509Certs (aSSLSession.getLocalCertificates ());

    final AS4TlsConnectionDetails ret = new AS4TlsConnectionDetails (sProtocol,
                                                                     sCipherSuite,
                                                                     KEY_SIZE_UNDEFINED,
                                                                     sSessionID,
                                                                     aRequestedServerNames,
                                                                     sPeerPrincipal,
                                                                     aPeerCerts,
                                                                     sLocalPrincipal,
                                                                     aLocalCerts);
    return ret.isEmpty () ? null : ret;
  }
}
