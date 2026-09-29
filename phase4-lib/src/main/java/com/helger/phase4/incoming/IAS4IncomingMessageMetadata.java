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
package com.helger.phase4.incoming;

import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.state.ETriState;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.http.header.HttpHeaderMap;
import com.helger.phase4.messaging.EAS4MessageMode;
import com.helger.phase4.messaging.http.AS4TlsConnectionDetails;

import jakarta.servlet.http.Cookie;

/**
 * This interface lets you access optional metadata for a single incoming message.<br>
 * See {@link AS4IncomingHelper} for a transformation method of this object to a JSON
 * representation.<br>
 * Note: it does not contain the AS4 message ID or similar parameters, because instance of the class
 * must also be present for incoming messages that are invalid AS4 message and hence have no AS4
 * message ID.
 *
 * @author Philip Helger
 * @since 0.9.8
 */
public interface IAS4IncomingMessageMetadata
{
  /**
   * @return A unique ID created just for this message metadata. It can be used to reference to this
   *         message internally. Usually this is a UUID. This is different from the AS4 message ID,
   *         because in case of a corrupted message, the AS4 message ID may be missing or misplaced.
   *         Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getIncomingUniqueID ();

  /**
   * @return The date and time when the request was received. Never <code>null</code>.
   */
  @NonNull
  OffsetDateTime getIncomingDT ();

  /**
   * @return The message mode. May be <code>null</code>.
   */
  @NonNull
  EAS4MessageMode getMode ();

  /**
   * Returns the Internet Protocol (IP) address of the client or last proxy that sent the request.
   *
   * @return a <code>String</code> containing the IP address of the client that sent the request
   */
  @Nullable
  String getRemoteAddr ();

  /**
   * @return <code>true</code> if the remote address is present, <code>false</code> if not.
   * @see #getRemoteAddr()
   */
  default boolean hasRemoteAddr ()
  {
    return StringHelper.isNotEmpty (getRemoteAddr ());
  }

  /**
   * Returns the fully qualified name of the client or the last proxy that sent the request. If the
   * engine cannot or chooses not to resolve the hostname (to improve performance), this method
   * returns the dotted-string form of the IP address.
   *
   * @return a <code>String</code> containing the fully qualified name of the client
   */
  @Nullable
  String getRemoteHost ();

  /**
   * @return <code>true</code> if the remote host is present, <code>false</code> if not.
   * @see #getRemoteHost()
   */
  default boolean hasRemoteHost ()
  {
    return StringHelper.isNotEmpty (getRemoteHost ());
  }

  /**
   * Returns the Internet Protocol (IP) source port of the client or last proxy that sent the
   * request.
   *
   * @return an integer specifying the port number or a negative value if not set
   */
  @CheckForSigned
  int getRemotePort ();

  /**
   * @return <code>true</code> if the remote port is present, <code>false</code> if not.
   * @see #getRemotePort()
   */
  default boolean hasRemotePort ()
  {
    return getRemotePort () > 0;
  }

  /**
   * Returns the login of the user making this request, if the user has been authenticated, or
   * <code>null</code> if the user has not been authenticated. Whether the user name is sent with
   * each subsequent request depends on the browser and type of authentication.
   *
   * @return a <code>String</code> specifying the login of the user making this request, or
   *         <code>null</code> if the user login is not known
   * @since 0.9.10
   */
  @Nullable
  String getRemoteUser ();

  /**
   * @return <code>true</code> if the remote user is present, <code>false</code> if not.
   * @see #getRemoteUser()
   */
  default boolean hasRemoteUser ()
  {
    return StringHelper.isNotEmpty (getRemoteUser ());
  }

  /**
   * Returns the Internet Protocol (IP) address of the interface on which the request was received.
   *
   * @return a <code>String</code> containing the local IP address, or <code>null</code>
   * @since 4.8.0
   */
  @Nullable
  String getLocalAddr ();

  /**
   * @return <code>true</code> if the local address is present, <code>false</code> if not.
   * @see #getLocalAddr()
   * @since 4.8.0
   */
  default boolean hasLocalAddr ()
  {
    return StringHelper.isNotEmpty (getLocalAddr ());
  }

  /**
   * Returns the Internet Protocol (IP) port number of the interface on which the request was
   * received.
   *
   * @return an integer specifying the local port number or a negative value if not set
   * @since 4.8.0
   */
  @CheckForSigned
  int getLocalPort ();

  /**
   * @return <code>true</code> if the local port is present, <code>false</code> if not.
   * @see #getLocalPort()
   * @since 4.8.0
   */
  default boolean hasLocalPort ()
  {
    return getLocalPort () > 0;
  }

  /**
   * Returns the host name of the server to which the request was sent - the value of the part
   * before the ":" in the <code>Host</code> header, or the resolved server name. This matters if
   * one AS4 instance serves more than one endpoint.
   *
   * @return a <code>String</code> containing the server name, or <code>null</code>
   * @since 4.8.0
   */
  @Nullable
  String getServerName ();

  /**
   * @return <code>true</code> if the server name is present, <code>false</code> if not.
   * @see #getServerName()
   * @since 4.8.0
   */
  default boolean hasServerName ()
  {
    return StringHelper.isNotEmpty (getServerName ());
  }

  /**
   * Returns the name and version of the protocol the message was transmitted with - e.g.
   * <code>HTTP/1.1</code> or <code>HTTP/2</code>.
   *
   * @return a <code>String</code> containing the protocol name and version, or <code>null</code>
   * @since 4.8.0
   */
  @Nullable
  String getHttpVersion ();

  /**
   * @return <code>true</code> if the HTTP version is present, <code>false</code> if not.
   * @see #getHttpVersion()
   * @since 4.8.0
   */
  default boolean hasHttpVersion ()
  {
    return StringHelper.isNotEmpty (getHttpVersion ());
  }

  /**
   * Returns whether the message was transmitted using a secure channel like HTTPS.
   * {@link ETriState#UNDEFINED} means, that it could not be determined. Note: if TLS is terminated
   * by a reverse proxy, this refers to the connection between the proxy and this instance.
   *
   * @return The secure state. Never <code>null</code>.
   * @since 4.8.0
   */
  @NonNull
  ETriState getSecure ();

  /**
   * Returns the TLS certificates presented by the remote client to authenticate itself.
   *
   * @return A list containing a chain of X509Certificate objects. Maybe <code>null</code>.
   * @since 2.5.0
   * @deprecated Use {@link #remoteTlsClientCerts()} instead
   */
  @Nullable
  @ReturnsMutableObject
  @Deprecated (forRemoval = true, since = "4.5.1")
  default ICommonsList <X509Certificate> remoteTlsCerts ()
  {
    return remoteTlsClientCerts ();
  }

  /**
   * Returns the TLS certificates presented by the remote client to authenticate itself.
   *
   * @return A list containing a chain of X509Certificate objects. Maybe <code>null</code>.
   * @since 2.5.0
   */
  @Nullable
  @ReturnsMutableObject
  ICommonsList <X509Certificate> remoteTlsClientCerts ();

  /**
   * @return <code>true</code> if the remote TLS certificate chain with at least a single
   *         certificate is present, <code>false</code> if not.
   * @see #remoteTlsClientCerts()
   * @since 2.5.0
   * @deprecated Use {@link #hasRemoteTlsClientCerts()} instead
   */
  @Deprecated (forRemoval = true, since = "4.5.1")
  default boolean hasRemoteTlsCerts ()
  {
    return hasRemoteTlsClientCerts ();
  }

  /**
   * @return <code>true</code> if the remote TLS certificate chain with at least a single
   *         certificate is present, <code>false</code> if not.
   * @see #remoteTlsClientCerts()
   * @since 2.5.0
   */
  default boolean hasRemoteTlsClientCerts ()
  {
    final var aCerts = remoteTlsClientCerts ();
    return aCerts != null && aCerts.isNotEmpty ();
  }

  /**
   * Returns the TLS certificates presented by the remote peer to authenticate itself.
   *
   * @return A list containing a chain of X509Certificate objects. Maybe <code>null</code>.
   * @since 4.5.1
   */
  @Nullable
  @ReturnsMutableObject
  ICommonsList <X509Certificate> remoteTlsPeerCerts ();

  /**
   * @return <code>true</code> if the remote peer TLS certificate chain with at least a single
   *         certificate is present, <code>false</code> if not.
   * @see #remoteTlsPeerCerts()
   * @since 4.5.1
   */
  default boolean hasRemoteTlsPeerCerts ()
  {
    final var aCerts = remoteTlsPeerCerts ();
    return aCerts != null && aCerts.isNotEmpty ();
  }

  /**
   * Returns the parameters of the TLS connection the message was transmitted over - like the TLS
   * protocol version and the negotiated cipher suite.
   *
   * @return The TLS connection details. May be <code>null</code> if the message was not
   *         transmitted over TLS, or if the Servlet container does not provide the respective
   *         request attributes.
   * @since 4.8.0
   */
  @Nullable
  AS4TlsConnectionDetails getTlsConnectionDetails ();

  /**
   * @return <code>true</code> if TLS connection details are present, <code>false</code> if not.
   * @see #getTlsConnectionDetails()
   * @since 4.8.0
   */
  default boolean hasTlsConnectionDetails ()
  {
    return getTlsConnectionDetails () != null;
  }

  /**
   * @return A list of all Cookies contained in the request. Never <code>null</code> but maybe
   *         empty. The returned list is mutable so handle with care.
   * @since 0.9.10
   */
  @NonNull
  @ReturnsMutableObject
  ICommonsList <Cookie> cookies ();

  /**
   * @return A copy of the list of all Cookies contained in the request. Never <code>null</code> but
   *         maybe empty.
   * @since 2.7.3
   */
  @NonNull
  @ReturnsMutableObject
  default ICommonsList <Cookie> getAllCookies ()
  {
    return cookies ().getClone ();
  }

  /**
   * @return A copy of all the HTTP headers from the incoming request. Never <code>null</code> but
   *         maybe empty.
   * @since 2.7.3
   */
  @NonNull
  @ReturnsMutableCopy
  HttpHeaderMap getAllHttpHeaders ();

  /**
   * @return The AS4 message ID of the request message. This field is always <code>null</code> for a
   *         request. This field is always non-<code>null</code> for a response.
   * @see #getMode() to differentiate between request and response
   * @since 1.4.2
   */
  @Nullable
  String getRequestMessageID ();

  /**
   * @return The HTTP status code to be returned. All values &le; 0 means: undefined.
   * @since 4.2.0
   */
  @CheckForSigned
  int getResponseHttpStatusCode ();

  /**
   * @return <code>true</code> if a defined HTTP status code is present, <code>false</code>
   *         otherwise.
   * @since 4.2.0
   */
  boolean hasResponseHttpStatusCode ();
}
