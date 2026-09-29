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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.cert.X509Certificate;

import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.EndpointDetails;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ProtocolVersion;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.ICommonsList;
import com.helger.httpclient.security.CapturingTlsSocketStrategy;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

/**
 * Immutable domain object with the details of the network connection that was used to transmit a
 * single AS4 message: the negotiated HTTP version, the socket addresses of both ends and - if TLS
 * was used - the {@link AS4TlsConnectionDetails}.
 * <p>
 * This is only created for outgoing connections. For an incoming request the same information is
 * part of <code>IAS4IncomingMessageMetadata</code> itself, because the Servlet request already is
 * a flat set of such properties.
 *
 * @author Philip Helger
 * @since 4.8.0
 */
@Immutable
public class AS4ConnectionDetails
{
  public static final String JSON_HTTP_VERSION = "httpVersion";
  public static final String JSON_REMOTE_ADDRESS = "remoteAddress";
  public static final String JSON_REMOTE_PORT = "remotePort";
  public static final String JSON_LOCAL_ADDRESS = "localAddress";
  public static final String JSON_LOCAL_PORT = "localPort";
  public static final String JSON_TLS = "tls";

  /** Constant indicating, that a port is unknown */
  public static final int PORT_UNDEFINED = -1;

  private final String m_sHttpVersion;
  private final String m_sRemoteAddress;
  private final int m_nRemotePort;
  private final String m_sLocalAddress;
  private final int m_nLocalPort;
  private final AS4TlsConnectionDetails m_aTlsDetails;

  /**
   * Constructor
   *
   * @param sHttpVersion
   *        The HTTP version of the received response - e.g. <code>HTTP/1.1</code>. May be
   *        <code>null</code>.
   * @param sRemoteAddress
   *        The resolved IP address of the other side. May be <code>null</code>.
   * @param nRemotePort
   *        The TCP port of the other side. Use {@link #PORT_UNDEFINED} if unknown.
   * @param sLocalAddress
   *        The local IP address of the connection. May be <code>null</code>.
   * @param nLocalPort
   *        The local TCP port of the connection. Use {@link #PORT_UNDEFINED} if unknown.
   * @param aTlsDetails
   *        The details of the TLS layer. May be <code>null</code> if plain HTTP was used.
   */
  public AS4ConnectionDetails (@Nullable final String sHttpVersion,
                               @Nullable final String sRemoteAddress,
                               final int nRemotePort,
                               @Nullable final String sLocalAddress,
                               final int nLocalPort,
                               @Nullable final AS4TlsConnectionDetails aTlsDetails)
  {
    m_sHttpVersion = sHttpVersion;
    m_sRemoteAddress = sRemoteAddress;
    m_nRemotePort = nRemotePort < 0 ? PORT_UNDEFINED : nRemotePort;
    m_sLocalAddress = sLocalAddress;
    m_nLocalPort = nLocalPort < 0 ? PORT_UNDEFINED : nLocalPort;
    m_aTlsDetails = aTlsDetails;
  }

  /**
   * @return The HTTP version of the received response - e.g. <code>HTTP/1.1</code>. May be
   *         <code>null</code>. Note: the classic Apache HttpClient that is used for outgoing
   *         messages only speaks HTTP/1.1, so anything else can only show up on the receiving
   *         side.
   */
  @Nullable
  public final String getHttpVersion ()
  {
    return m_sHttpVersion;
  }

  public final boolean hasHttpVersion ()
  {
    return StringHelper.isNotEmpty (m_sHttpVersion);
  }

  /**
   * @return The resolved IP address of the other side. May be <code>null</code>. Compared to the
   *         host name of the endpoint URL, this is the machine that was really contacted, which
   *         matters if DNS round robin, a CDN or a load balancer is in play.
   */
  @Nullable
  public final String getRemoteAddress ()
  {
    return m_sRemoteAddress;
  }

  public final boolean hasRemoteAddress ()
  {
    return StringHelper.isNotEmpty (m_sRemoteAddress);
  }

  /**
   * @return The TCP port of the other side, or {@link #PORT_UNDEFINED} if unknown.
   */
  @CheckForSigned
  public final int getRemotePort ()
  {
    return m_nRemotePort;
  }

  public final boolean hasRemotePort ()
  {
    return m_nRemotePort > 0;
  }

  /**
   * @return The local IP address of the connection. May be <code>null</code>.
   */
  @Nullable
  public final String getLocalAddress ()
  {
    return m_sLocalAddress;
  }

  public final boolean hasLocalAddress ()
  {
    return StringHelper.isNotEmpty (m_sLocalAddress);
  }

  /**
   * @return The local TCP port of the connection, or {@link #PORT_UNDEFINED} if unknown.
   */
  @CheckForSigned
  public final int getLocalPort ()
  {
    return m_nLocalPort;
  }

  public final boolean hasLocalPort ()
  {
    return m_nLocalPort > 0;
  }

  /**
   * @return The details of the TLS layer of this connection. May be <code>null</code> if plain HTTP
   *         was used.
   */
  @Nullable
  public final AS4TlsConnectionDetails getTlsDetails ()
  {
    return m_aTlsDetails;
  }

  public final boolean hasTlsDetails ()
  {
    return m_aTlsDetails != null;
  }

  /**
   * @return <code>true</code> if not a single piece of information is contained in this object,
   *         <code>false</code> otherwise.
   */
  public final boolean isEmpty ()
  {
    return !hasHttpVersion () &&
           !hasRemoteAddress () &&
           !hasRemotePort () &&
           !hasLocalAddress () &&
           !hasLocalPort () &&
           !hasTlsDetails ();
  }

  /**
   * @return The connection details as a JSON object, including the TLS certificates. Only elements
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
   *        <code>true</code> to include the PEM encoded TLS certificates, <code>false</code> to
   *        emit the connection parameters only. The certificates are pretty large, so they should
   *        be omitted where the size of the result matters.
   * @return The connection details as a JSON object. Only elements that are present, are contained.
   *         Never <code>null</code>.
   */
  @NonNull
  public IJsonObject getAsJsonObject (final boolean bIncludeCerts)
  {
    final IJsonObject ret = new JsonObject ();
    if (hasHttpVersion ())
      ret.add (JSON_HTTP_VERSION, m_sHttpVersion);
    if (hasRemoteAddress ())
      ret.add (JSON_REMOTE_ADDRESS, m_sRemoteAddress);
    if (hasRemotePort ())
      ret.add (JSON_REMOTE_PORT, m_nRemotePort);
    if (hasLocalAddress ())
      ret.add (JSON_LOCAL_ADDRESS, m_sLocalAddress);
    if (hasLocalPort ())
      ret.add (JSON_LOCAL_PORT, m_nLocalPort);
    if (hasTlsDetails ())
      ret.add (JSON_TLS, m_aTlsDetails.getAsJsonObject (bIncludeCerts));
    return ret;
  }

  /**
   * @param sNamespaceURI
   *        The namespace URI to be used. May be <code>null</code>.
   * @param sTagName
   *        The tag name to use for the created element. May neither be <code>null</code> nor empty.
   * @return The connection details as a MicroDOM element, including the TLS certificates. Only
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
   *        <code>true</code> to include the PEM encoded TLS certificates, <code>false</code> to
   *        emit the connection parameters only. The certificates are pretty large, so they should
   *        be omitted where the size of the result matters.
   * @return The connection details as a MicroDOM element. Only elements that are present, are
   *         contained. Never <code>null</code>.
   */
  @NonNull
  public IMicroElement getAsMicroElement (@Nullable final String sNamespaceURI,
                                          @NonNull @Nonempty final String sTagName,
                                          final boolean bIncludeCerts)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    if (hasHttpVersion ())
      ret.addElementNS (sNamespaceURI, "HttpVersion").addText (m_sHttpVersion);
    if (hasRemoteAddress ())
      ret.addElementNS (sNamespaceURI, "RemoteAddress").addText (m_sRemoteAddress);
    if (hasRemotePort ())
      ret.addElementNS (sNamespaceURI, "RemotePort").addText (m_nRemotePort);
    if (hasLocalAddress ())
      ret.addElementNS (sNamespaceURI, "LocalAddress").addText (m_sLocalAddress);
    if (hasLocalPort ())
      ret.addElementNS (sNamespaceURI, "LocalPort").addText (m_nLocalPort);
    if (hasTlsDetails ())
      ret.addChild (m_aTlsDetails.getAsMicroElement (sNamespaceURI, "Tls", bIncludeCerts));
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("HttpVersion", m_sHttpVersion)
                                       .append ("RemoteAddress", m_sRemoteAddress)
                                       .append ("RemotePort", m_nRemotePort)
                                       .append ("LocalAddress", m_sLocalAddress)
                                       .append ("LocalPort", m_nLocalPort)
                                       .append ("TlsDetails", m_aTlsDetails)
                                       .getToString ();
  }

  @Nullable
  private static String _getIPAddress (@Nullable final SocketAddress aSocketAddress)
  {
    if (aSocketAddress instanceof final InetSocketAddress aISA && aISA.getAddress () != null)
      return aISA.getAddress ().getHostAddress ();
    return null;
  }

  @CheckForSigned
  private static int _getPort (@Nullable final SocketAddress aSocketAddress)
  {
    if (aSocketAddress instanceof final InetSocketAddress aISA)
      return aISA.getPort ();
    return PORT_UNDEFINED;
  }

  /**
   * Create the connection details from the context of a finished Apache HttpClient exchange.
   *
   * @param aHttpClientContext
   *        The HTTP client context that was passed to the execute call. May be <code>null</code>.
   * @return <code>null</code> if no context was provided or if it contains no usable information.
   */
  @Nullable
  public static AS4ConnectionDetails createFromHttpClientContext (@Nullable final HttpClientContext aHttpClientContext)
  {
    if (aHttpClientContext == null)
      return null;

    // HttpCoreContext.getProtocolVersion() falls back to HTTP/1.1 even if nothing was ever set,
    // so only take the version if a response was really received
    ProtocolVersion aProtocolVersion = null;
    final HttpResponse aResponse = aHttpClientContext.getResponse ();
    if (aResponse != null)
    {
      aProtocolVersion = aResponse.getVersion ();
      if (aProtocolVersion == null)
        aProtocolVersion = aHttpClientContext.getProtocolVersion ();
    }

    String sRemoteAddress = null;
    int nRemotePort = PORT_UNDEFINED;
    String sLocalAddress = null;
    int nLocalPort = PORT_UNDEFINED;
    final EndpointDetails aEndpointDetails = aHttpClientContext.getEndpointDetails ();
    if (aEndpointDetails != null)
    {
      sRemoteAddress = _getIPAddress (aEndpointDetails.getRemoteAddress ());
      nRemotePort = _getPort (aEndpointDetails.getRemoteAddress ());
      sLocalAddress = _getIPAddress (aEndpointDetails.getLocalAddress ());
      nLocalPort = _getPort (aEndpointDetails.getLocalAddress ());
    }

    // The SSL session is set by the Apache HttpClient connect executor for every request,
    // including the ones that reuse a pooled connection
    AS4TlsConnectionDetails aTlsDetails = AS4TlsConnectionDetails.createFromSSLSession (aHttpClientContext.getSSLSession ());
    if (aTlsDetails == null)
    {
      // Fallback: the CapturingTlsSocketStrategy is wired in by HttpClientFactory by default and
      // captures the peer certificates of a freshly established TLS connection
      final ICommonsList <X509Certificate> aRemoteTlsCerts = CapturingTlsSocketStrategy.getRemoteTLSCertificates (aHttpClientContext);
      if (aRemoteTlsCerts != null)
        aTlsDetails = new AS4TlsConnectionDetails (null,
                                                   null,
                                                   AS4TlsConnectionDetails.KEY_SIZE_UNDEFINED,
                                                   null,
                                                   null,
                                                   null,
                                                   aRemoteTlsCerts,
                                                   null,
                                                   null);
    }

    final AS4ConnectionDetails ret = new AS4ConnectionDetails (aProtocolVersion == null ? null
                                                                                        : aProtocolVersion.format (),
                                                               sRemoteAddress,
                                                               nRemotePort,
                                                               sLocalAddress,
                                                               nLocalPort,
                                                               aTlsDetails);
    return ret.isEmpty () ? null : ret;
  }
}
