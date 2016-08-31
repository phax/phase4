package com.helger.as4server.receive;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.namespace.QName;

import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.attr.MapBasedAttributeContainerAny;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.error.IResourceErrorGroup;

/**
 * This class keeps track of the status of an incoming message. It is basically
 * a String to any map.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class AS4MessageState extends MapBasedAttributeContainerAny <String>
{
  private static final String KEY_EBMS3_MESSAGING = "as4.ebms3.messaging";

  private final LocalDateTime m_aReceiptDT;
  private final ESOAPVersion m_eSOAPVersion;

  public AS4MessageState (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_aReceiptDT = PDTFactory.getCurrentLocalDateTime ();
    m_eSOAPVersion = ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
  }

  /**
   * @return Date and time when the receipt started.
   */
  @Nonnull
  public final LocalDateTime getReceiptDT ()
  {
    return m_aReceiptDT;
  }

  /**
   * @return The SOAP version of the current request as specified in the
   *         constructor. Never <code>null</code>.
   */
  @Nonnull
  public final ESOAPVersion getSOAPVersion ()
  {
    return m_eSOAPVersion;
  }

  public void setMessaging (@Nullable final Ebms3Messaging aMessaging)
  {
    setAttribute (KEY_EBMS3_MESSAGING, aMessaging);
  }

  @Nullable
  public Ebms3Messaging getMessaging ()
  {
    return getCastedAttribute (KEY_EBMS3_MESSAGING);
  }

  public void addUnhandledHeader (@Nonnull final QName aQName)
  {
    // TODO
  }

  public void addHandledHeader (@Nonnull final QName aQName)
  {
    // TODO
  }

  public void addProcessingErrors (@Nonnull final QName aQName, @Nonnull final IResourceErrorGroup aAllFailures)
  {
    // TODO
  }

  public void addFailedHeader (@Nonnull final QName aQName)
  {
    // TODO
  }
}
