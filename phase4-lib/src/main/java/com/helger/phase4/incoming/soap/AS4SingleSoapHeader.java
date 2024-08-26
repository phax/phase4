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
package com.helger.phase4.incoming.soap;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.ToStringGenerator;

/**
 * This class represents a single DOM element in a SOAP header with some
 * metadata. It is used to mark headers as processed or not.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class AS4SingleSoapHeader
{
  private final Element m_aNode;
  private final QName m_aQName;
  private final boolean m_bIsMustUnderstand;
  private boolean m_bProcessed = false;

  /**
   * @param aNode
   *        The DOM element. May not be <code>null</code>.
   * @param aQName
   *        The QName of the DOM element. May not be <code>null</code>. Must
   *        match the QName of the provided DOM node.
   * @param bIsMustUnderstand
   *        <code>true</code> if this is a must understand header,
   *        <code>false</code> otherwise.
   */
  public AS4SingleSoapHeader (@Nonnull final Element aNode,
                              @Nonnull final QName aQName,
                              final boolean bIsMustUnderstand)
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

  public boolean isProcessed ()
  {
    return m_bProcessed;
  }

  public void setProcessed (final boolean bProcessed)
  {
    m_bProcessed = bProcessed;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("QName", m_aQName)
                                       .append ("MustUnderstand", m_bIsMustUnderstand)
                                       .append ("Processed", m_bProcessed)
                                       .getToString ();
  }
}
