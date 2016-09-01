package com.helger.as4server.servlet;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.helger.commons.ValueEnforcer;

/**
 * This class represents a single DOM element in a SOAP header.
 *
 * @author Philip Helger
 */
@Immutable
public class AS4SOAPHeader
{
  private final Element m_aNode;
  private final QName m_aQName;
  private final boolean m_bIsMustUnderstand;

  public AS4SOAPHeader (@Nonnull final Element aNode, @Nonnull final QName aQName, final boolean bIsMustUnderstand)
  {
    m_aNode = ValueEnforcer.notNull (aNode, "Node");
    m_aQName = ValueEnforcer.notNull (aQName, "QName");
    m_bIsMustUnderstand = bIsMustUnderstand;
  }

  @Nonnull
  public Element getNode ()
  {
    return m_aNode;
  }

  @Nonnull
  public QName getQName ()
  {
    return m_aQName;
  }

  public boolean isMustUnderstand ()
  {
    return m_bIsMustUnderstand;
  }
}
