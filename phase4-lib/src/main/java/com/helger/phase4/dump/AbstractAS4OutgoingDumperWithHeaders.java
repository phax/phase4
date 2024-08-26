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
package com.helger.phase4.dump;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.messaging.EAS4MessageMode;

/**
 * Abstract implementation of {@link IAS4OutgoingDumper} that always adds the
 * custom headers
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        Implementation type (since v3.0.0)
 * @since 0.9.7
 */
public abstract class AbstractAS4OutgoingDumperWithHeaders <IMPLTYPE extends AbstractAS4OutgoingDumperWithHeaders <IMPLTYPE>>
                                                           implements
                                                           IAS4OutgoingDumper,
                                                           IGenericImplTrait <IMPLTYPE>
{
  public static final boolean DEFAULT_INCLUDE_HEADERS = true;

  private boolean m_bIncludeHeaders = DEFAULT_INCLUDE_HEADERS;

  /**
   * @return <code>true</code> to include the headers in the dump,
   *         <code>false</code> if not. The default is
   *         {@link #DEFAULT_INCLUDE_HEADERS}.
   * @since 2.5.2
   */
  public final boolean isIncludeHeaders ()
  {
    return m_bIncludeHeaders;
  }

  /**
   * Include or exclude the headers from the dump.
   *
   * @param b
   *        <code>true</code> to include the headers in the dump,
   *        <code>false</code> if not.
   * @return this for chaining (since v3)
   * @since 2.5.2
   */
  public final IMPLTYPE setIncludeHeaders (final boolean b)
  {
    m_bIncludeHeaders = b;
    return thisAsT ();
  }

  /**
   * Create the output stream to which the data should be dumped.
   *
   * @param eMsgMode
   *        Are we dumping a request or a response? Never <code>null</code>.
   *        Added in v1.2.0.
   * @param aIncomingMessageMetadata
   *        The incoming message metadata. This is always <code>null</code> for
   *        requests. This is always non-<code>null</code> for responses. Added
   *        in v1.2.0.
   * @param aIncomingState
   *        The incoming message processing state. This is always
   *        <code>null</code> for requests. This is always non-<code>null</code>
   *        for responses. Added in v1.2.0.
   * @param sMessageID
   *        The AS4 message ID of the outgoing message. Neither
   *        <code>null</code> nor empty.
   * @param aCustomHeaders
   *        The HTTP headers of the outgoing message. Never <code>null</code>.
   * @param nTry
   *        The index of the try. The first try has always index 0, the first
   *        retry has index 1, the second retry has index 2 etc. Always &ge; 0.
   * @return The output stream to dump to or <code>null</code> if no dumping
   *         should be performed.
   * @throws IOException
   *         On IO error
   */
  @Nullable
  protected abstract OutputStream openOutputStream (@Nonnull EAS4MessageMode eMsgMode,
                                                    @Nullable IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                                    @Nullable IAS4IncomingMessageState aIncomingState,
                                                    @Nonnull @Nonempty String sMessageID,
                                                    @Nullable HttpHeaderMap aCustomHeaders,
                                                    @Nonnegative int nTry) throws IOException;

  @Nullable
  public OutputStream onBeginRequest (@Nonnull final EAS4MessageMode eMsgMode,
                                      @Nullable final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                      @Nullable final IAS4IncomingMessageState aIncomingState,
                                      @Nonnull @Nonempty final String sMessageID,
                                      @Nullable final HttpHeaderMap aCustomHeaders,
                                      @Nonnegative final int nTry) throws IOException
  {
    final OutputStream ret = openOutputStream (eMsgMode,
                                               aIncomingMessageMetadata,
                                               aIncomingState,
                                               sMessageID,
                                               aCustomHeaders,
                                               nTry);
    if (ret != null && aCustomHeaders != null && aCustomHeaders.isNotEmpty () && m_bIncludeHeaders)
    {
      // At least one custom header is present
      for (final Map.Entry <String, ICommonsList <String>> aEntry : aCustomHeaders)
      {
        final String sHeader = aEntry.getKey ();
        for (final String sValue : aEntry.getValue ())
        {
          // By default quoting is disabled
          final boolean bQuoteIfNecessary = false;
          final String sUnifiedValue = HttpHeaderMap.getUnifiedValue (sValue, bQuoteIfNecessary);
          final String sLine = sHeader + HttpHeaderMap.SEPARATOR_KEY_VALUE + sUnifiedValue + CHttp.EOL;
          ret.write (sLine.getBytes (CHttp.HTTP_CHARSET));
        }
      }
      // Separator only if at least one header is present
      ret.write (CHttp.EOL.getBytes (CHttp.HTTP_CHARSET));
    }
    return ret;
  }

  public void onEndRequest (@Nonnull final EAS4MessageMode eMsgMode,
                            @Nullable final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                            @Nullable final IAS4IncomingMessageState aIncomingState,
                            @Nonnull @Nonempty final String sMessageID,
                            @Nullable final Exception aCaughtException)
  {}
}
