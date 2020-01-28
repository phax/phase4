package com.helger.phase4.servlet;

import java.time.LocalDateTime;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.messaging.EAS4IncomingMessageMode;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;

/**
 * This class holds optional metadata for a single incoming request. This is the
 * default implementation of {@link IAS4IncomingMessageMetadata}.
 *
 * @author Philip Helger
 * @since 0.9.8
 */
public class AS4IncomingMessageMetadata implements IAS4IncomingMessageMetadata
{
  private LocalDateTime m_aIncomingDT;
  private EAS4IncomingMessageMode m_eMode;
  private String m_sRemoteAddr;
  private String m_sRemoteHost;
  private int m_nRemotePort = -1;

  public AS4IncomingMessageMetadata ()
  {}

  @Nullable
  public LocalDateTime getIncomingDT ()
  {
    return m_aIncomingDT;
  }

  @Nonnull
  public AS4IncomingMessageMetadata setIncomingDT (@Nullable final LocalDateTime aIncomingDT)
  {
    m_aIncomingDT = aIncomingDT;
    return this;
  }

  @Nonnull
  public AS4IncomingMessageMetadata setIncomingDTNow ()
  {
    return setIncomingDT (PDTFactory.getCurrentLocalDateTime ());
  }

  @Nullable
  public EAS4IncomingMessageMode getMode ()
  {
    return m_eMode;
  }

  @Nonnull
  public AS4IncomingMessageMetadata setMode (@Nullable final EAS4IncomingMessageMode eMode)
  {
    m_eMode = eMode;
    return this;
  }

  @Nullable
  public String getRemoteAddr ()
  {
    return m_sRemoteAddr;
  }

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

  @Nonnull
  public AS4IncomingMessageMetadata setRemotePort (final int nRemotePort)
  {
    m_nRemotePort = nRemotePort;
    return this;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("IncomingDT", m_aIncomingDT)
                                       .append ("Mode", m_eMode)
                                       .append ("RemoteAddr", m_sRemoteAddr)
                                       .append ("RemoteHost", m_sRemoteHost)
                                       .append ("RemotePort", m_nRemotePort)
                                       .getToString ();
  }
}
