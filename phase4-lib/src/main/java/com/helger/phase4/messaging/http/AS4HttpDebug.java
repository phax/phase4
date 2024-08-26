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
package com.helger.phase4.messaging.http;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.phase4.marshaller.Ebms3NamespaceHandler;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.IXMLWriterSettings;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * Turn on/off AS4 HTTP debug logging.<br>
 * Note: this class will be replaced with something smarter in the future
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class AS4HttpDebug
{
  public static final boolean DEFAULT_DEBUG = false;

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4HttpDebug.class);
  private static final AtomicBoolean ENABLED = new AtomicBoolean (DEFAULT_DEBUG);
  private static final XMLWriterSettings XWS = new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN)
                                                                       .setNamespaceContext (Ebms3NamespaceHandler.getInstance ());

  private AS4HttpDebug ()
  {}

  /**
   * Enable or disable
   *
   * @param bEnabled
   *        <code>true</code> to enabled, <code>false</code> to disable
   */
  public static void setEnabled (final boolean bEnabled)
  {
    ENABLED.set (bEnabled);
  }

  /**
   * @return <code>true</code> if enabled, <code>false</code> if not.
   */
  public static boolean isEnabled ()
  {
    return ENABLED.get ();
  }

  /**
   * Debug the provided string if {@link #isEnabled()}. Uses the logger to log
   * to the console
   *
   * @param aMsg
   *        The message supplier. May not be <code>null</code>. Invoked only if
   *        {@link #isEnabled()}
   */
  public static void debug (@Nonnull final Supplier <? super String> aMsg)
  {
    if (isEnabled ())
      LOGGER.info ("$$$ AS4 HTTP [" +
                   MetaAS4Manager.getTimestampMgr ().getCurrentDateTime ().toString () +
                   "] " +
                   aMsg.get ());
  }

  /**
   * @return XML writer setting to debug XML documents. It uses formatting and a
   *         predefined namespace context. Never <code>null</code>.
   */
  @Nonnull
  public static IXMLWriterSettings getDebugXMLWriterSettings ()
  {
    return XWS;
  }
}
