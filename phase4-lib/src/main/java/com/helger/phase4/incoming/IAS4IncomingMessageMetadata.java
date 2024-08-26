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
package com.helger.phase4.incoming;

import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.messaging.EAS4MessageMode;

import jakarta.servlet.http.Cookie;

/**
 * This interface lets you access optional metadata for a single incoming
 * message.<br>
 * See {@link AS4IncomingHelper} for a transformation method of this object to
 * a JSON representation.<br>
 * Note: it does not contain the AS4 message ID or similar parameters, because
 * instance of the class must also be present for incoming messages that are
 * invalid AS4 message and hence have no AS4 message ID.
 *
 * @author Philip Helger
 * @since 0.9.8
 */
public interface IAS4IncomingMessageMetadata
{
  /**
   * @return A unique ID created just for this message metadata. It can be used
   *         to reference to this message internally. Usually this is a UUID.
   *         This is different from the AS4 message ID, because in case of a
   *         corrupted message, the AS4 message ID may be missing or misplaced.
   *         Never <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  String getIncomingUniqueID ();

  /**
   * @return The date and time when the request was received. Never
   *         <code>null</code>.
   */
  @Nonnull
  OffsetDateTime getIncomingDT ();

  /**
   * @return The message mode. May be <code>null</code>.
   */
  @Nonnull
  EAS4MessageMode getMode ();

  /**
   * Returns the Internet Protocol (IP) address of the client or last proxy that
   * sent the request.
   *
   * @return a <code>String</code> containing the IP address of the client that
   *         sent the request
   */
  @Nullable
  String getRemoteAddr ();

  /**
   * @return <code>true</code> if the remote address is present,
   *         <code>false</code> if not.
   * @see #getRemoteAddr()
   */
  default boolean hasRemoteAddr ()
  {
    return StringHelper.hasText (getRemoteAddr ());
  }

  /**
   * Returns the fully qualified name of the client or the last proxy that sent
   * the request. If the engine cannot or chooses not to resolve the hostname
   * (to improve performance), this method returns the dotted-string form of the
   * IP address.
   *
   * @return a <code>String</code> containing the fully qualified name of the
   *         client
   */
  @Nullable
  String getRemoteHost ();

  /**
   * @return <code>true</code> if the remote host is present, <code>false</code>
   *         if not.
   * @see #getRemoteHost()
   */
  default boolean hasRemoteHost ()
  {
    return StringHelper.hasText (getRemoteHost ());
  }

  /**
   * Returns the Internet Protocol (IP) source port of the client or last proxy
   * that sent the request.
   *
   * @return an integer specifying the port number or a negative value if not
   *         set
   */
  @CheckForSigned
  int getRemotePort ();

  /**
   * @return <code>true</code> if the remote port is present, <code>false</code>
   *         if not.
   * @see #getRemotePort()
   */
  default boolean hasRemotePort ()
  {
    return getRemotePort () > 0;
  }

  /**
   * Returns the login of the user making this request, if the user has been
   * authenticated, or <code>null</code> if the user has not been authenticated.
   * Whether the user name is sent with each subsequent request depends on the
   * browser and type of authentication.
   *
   * @return a <code>String</code> specifying the login of the user making this
   *         request, or <code>null</code> if the user login is not known
   * @since 0.9.10
   */
  @Nullable
  String getRemoteUser ();

  /**
   * @return <code>true</code> if the remote user is present, <code>false</code>
   *         if not.
   * @see #getRemoteUser()
   */
  default boolean hasRemoteUser ()
  {
    return StringHelper.hasText (getRemoteUser ());
  }

  /**
   * Returns the TLS certificates presented by the remote client to authenticate
   * itself.
   *
   * @return A list containing a chain of X509Certificate objects. Maybe
   *         <code>null</code>.
   * @since 2.5.0
   */
  @Nullable
  @ReturnsMutableObject
  ICommonsList <X509Certificate> remoteTlsCerts ();

  /**
   * @return <code>true</code> if the remote TLS certificate chain with at least
   *         a single certificate is present, <code>false</code> if not.
   * @see #remoteTlsCerts()
   * @since 2.5.0
   */
  default boolean hasRemoteTlsCerts ()
  {
    return remoteTlsCerts () != null && remoteTlsCerts ().isNotEmpty ();
  }

  /**
   * @return A list of all Cookies contained in the request. Never
   *         <code>null</code> but maybe empty. The returned list is mutable so
   *         handle with care.
   * @since 0.9.10
   */
  @Nonnull
  @ReturnsMutableObject
  ICommonsList <Cookie> cookies ();

  /**
   * @return A copy of the list of all Cookies contained in the request. Never
   *         <code>null</code> but maybe empty.
   * @since 2.7.3
   */
  @Nonnull
  @ReturnsMutableObject
  default ICommonsList <Cookie> getAllCookies ()
  {
    return cookies ().getClone ();
  }

  /**
   * @return A copy of all the HTTP headers from the incoming request. Never
   *         <code>null</code> but maybe empty.
   * @since 2.7.3
   */
  @Nonnull
  @ReturnsMutableCopy
  HttpHeaderMap getAllHttpHeaders ();

  /**
   * @return The AS4 message ID of the request message. This field is always
   *         <code>null</code> for a request. This field is always
   *         non-<code>null</code> for a response.
   * @see #getMode() to differentiate between request and response
   * @since 1.4.2
   */
  @Nullable
  String getRequestMessageID ();
}
