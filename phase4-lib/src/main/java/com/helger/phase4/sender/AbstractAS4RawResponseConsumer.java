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
package com.helger.phase4.sender;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.traits.IGenericImplTrait;

/**
 * Abstract base implementation of <code>IAS4RawResponseConsumer</code> to
 * provide basic customizability.
 *
 * @author Philip Helger
 * @since 0.13.0
 * @param <IMPLTYPE>
 *        Implementation type of the derived class
 */
@NotThreadSafe
public abstract class AbstractAS4RawResponseConsumer <IMPLTYPE extends AbstractAS4RawResponseConsumer <IMPLTYPE>>
                                                     implements
                                                     IAS4RawResponseConsumer,
                                                     IGenericImplTrait <IMPLTYPE>
{
  public static final boolean DEFAULT_HANDLE_STATUS_LINE = true;
  public static final boolean DEFAULT_HANDLE_HTTP_HEADERS = true;

  private boolean m_bHandleStatusLine = DEFAULT_HANDLE_STATUS_LINE;
  private boolean m_bHandleHttpHeaders = DEFAULT_HANDLE_HTTP_HEADERS;

  protected AbstractAS4RawResponseConsumer ()
  {}

  public final boolean isHandleStatusLine ()
  {
    return m_bHandleStatusLine;
  }

  @Nonnull
  public final IMPLTYPE setHandleStatusLine (final boolean bHandleStatusLine)
  {
    m_bHandleStatusLine = bHandleStatusLine;
    return thisAsT ();
  }

  public final boolean isHandleHttpHeaders ()
  {
    return m_bHandleHttpHeaders;
  }

  @Nonnull
  public final IMPLTYPE setHandleHttpHeaders (final boolean bHandleHttpHeaders)
  {
    m_bHandleHttpHeaders = bHandleHttpHeaders;
    return thisAsT ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("HandleStatusLine", m_bHandleStatusLine)
                                       .append ("HandleHttpHeaders", m_bHandleHttpHeaders)
                                       .getToString ();
  }
}
