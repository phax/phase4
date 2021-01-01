/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.phase4.servlet;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.messaging.EAS4IncomingMessageMode;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.mgr.MetaAS4Manager;

/**
 * This class holds optional metadata for a single incoming request. This is the
 * default implementation of {@link IAS4IncomingMessageMetadata}.
 *
 * @author Philip Helger
 * @since 0.9.8
 */
public class AS4IncomingMessageMetadata implements IAS4IncomingMessageMetadata
{
  private final String m_sIncomingUniqueID;
  private final LocalDateTime m_aIncomingDT;
  private final EAS4IncomingMessageMode m_eMode;
  private String m_sRemoteAddr;
  private String m_sRemoteHost;
  private int m_nRemotePort = -1;
  private String m_sRemoteUser;
  private final ICommonsList <Cookie> m_aCookies = new CommonsArrayList <> ();

  /**
   * Default constructor using a UUID as the incoming unique ID and the current
   * date time.
   *
   * @param eMode
   *        The messaging mode. May not be <code>null</code>.
   */
  public AS4IncomingMessageMetadata (@Nonnull final EAS4IncomingMessageMode eMode)
  {
    this (UUID.randomUUID ().toString (), MetaAS4Manager.getTimestampMgr ().getCurrentDateTime (), eMode);
  }

  /**
   * Constructor in case this every needs to be deserialized or other weird
   * things are necessary.
   *
   * @param sIncomingUniqueID
   *        Incoming unique ID. May neither be <code>null</code> nor empty.
   * @param aIncomingDT
   *        The incoming date time. May not be <code>null</code>.
   * @param eMode
   *        The messaging mode. May not be <code>null</code>.
   */
  protected AS4IncomingMessageMetadata (@Nonnull @Nonempty final String sIncomingUniqueID,
                                        @Nonnull final LocalDateTime aIncomingDT,
                                        @Nonnull final EAS4IncomingMessageMode eMode)
  {
    ValueEnforcer.notEmpty (sIncomingUniqueID, "sIncomingUniqueID");
    ValueEnforcer.notNull (aIncomingDT, "IncomingDT");
    ValueEnforcer.notNull (eMode, "Mode");

    m_sIncomingUniqueID = sIncomingUniqueID;
    m_aIncomingDT = aIncomingDT;
    m_eMode = eMode;
  }

  @Nonnull
  @Nonempty
  public final String getIncomingUniqueID ()
  {
    return m_sIncomingUniqueID;
  }

  @Nonnull
  public final LocalDateTime getIncomingDT ()
  {
    return m_aIncomingDT;
  }

  @Nonnull
  public final EAS4IncomingMessageMode getMode ()
  {
    return m_eMode;
  }

  @Nullable
  public String getRemoteAddr ()
  {
    return m_sRemoteAddr;
  }

  /**
   * Set the remote address to be used.
   *
   * @param sRemoteAddr
   *        The remote address. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public AS4IncomingMessageMetadata setRemoteAddr (@Nullable final String sRemoteAddr)
  {
    m_sRemoteAddr = sRemoteAddr;
    return this;
  }

  @Nullable
  public String getRemoteHost ()
  {
    return m_sRemoteHost;
  }

  /**
   * Set the remote host to be used.
   *
   * @param sRemoteHost
   *        The remote host. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public AS4IncomingMessageMetadata setRemoteHost (@Nullable final String sRemoteHost)
  {
    m_sRemoteHost = sRemoteHost;
    return this;
  }

  @CheckForSigned
  public int getRemotePort ()
  {
    return m_nRemotePort;
  }

  /**
   * Set the remote port to be used.
   *
   * @param nRemotePort
   *        The remote port.
   * @return this for chaining
   */
  @Nonnull
  public AS4IncomingMessageMetadata setRemotePort (final int nRemotePort)
  {
    m_nRemotePort = nRemotePort;
    return this;
  }

  @Nullable
  public String getRemoteUser ()
  {
    return m_sRemoteUser;
  }

  /**
   * Set the remote user to be used.
   *
   * @param sRemoteUser
   *        The remote user. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public AS4IncomingMessageMetadata setRemoteUser (@Nullable final String sRemoteUser)
  {
    m_sRemoteUser = sRemoteUser;
    return this;
  }

  @Nonnull
  @ReturnsMutableObject
  public ICommonsList <Cookie> cookies ()
  {
    return m_aCookies;
  }

  /**
   * Set the cookies to be used.
   *
   * @param aCookies
   *        The cookie array. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public AS4IncomingMessageMetadata setCookies (@Nullable final Cookie [] aCookies)
  {
    m_aCookies.setAll (aCookies);
    return this;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("IncomingUniqueID", m_sIncomingUniqueID)
                                       .append ("IncomingDT", m_aIncomingDT)
                                       .append ("Mode", m_eMode)
                                       .append ("RemoteAddr", m_sRemoteAddr)
                                       .append ("RemoteHost", m_sRemoteHost)
                                       .append ("RemotePort", m_nRemotePort)
                                       .append ("RemoteUser", m_sRemoteUser)
                                       .append ("Cookies", m_aCookies)
                                       .getToString ();
  }
}
