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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;

/**
 * Abstract version of {@link IAS4IncomingDumper} that emits all headers on the
 * output stream.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        Implementation type (since v3.0.0)
 * @since 0.9.7
 */
public abstract class AbstractAS4IncomingDumperWithHeaders <IMPLTYPE extends AbstractAS4IncomingDumperWithHeaders <IMPLTYPE>>
                                                           implements
                                                           IAS4IncomingDumper,
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
   * @return this for chaining (since v3.0.0)
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
   * @param aMessageMetadata
   *        Request metadata. Never <code>null</code>. Since v0.9.8.
   * @param aHttpHeaderMap
   *        The HTTP headers of the incoming message. Never <code>null</code>.
   * @return The output stream to dump to or <code>null</code> if no dumping
   *         should be performed.
   * @throws IOException
   *         On IO error
   */
  @Nullable
  protected abstract OutputStream openOutputStream (@Nonnull IAS4IncomingMessageMetadata aMessageMetadata,
                                                    @Nonnull HttpHeaderMap aHttpHeaderMap) throws IOException;

  @Nullable
  public OutputStream onNewRequest (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                    @Nonnull final HttpHeaderMap aHttpHeaderMap) throws IOException
  {
    final OutputStream ret = openOutputStream (aMessageMetadata, aHttpHeaderMap);
    if (ret != null && aHttpHeaderMap.isNotEmpty () && m_bIncludeHeaders)
    {
      // At least one header is contained
      for (final Map.Entry <String, ICommonsList <String>> aEntry : aHttpHeaderMap)
      {
        final String sKey = aEntry.getKey ();
        for (final String sValue : aEntry.getValue ())
        {
          final boolean bQuoteIfNecessary = false;
          final String sUnifiedValue = HttpHeaderMap.getUnifiedValue (sValue, bQuoteIfNecessary);
          final String sLine = sKey + HttpHeaderMap.SEPARATOR_KEY_VALUE + sUnifiedValue + CHttp.EOL;
          ret.write (sLine.getBytes (CHttp.HTTP_CHARSET));
        }
      }
      // Separator only if at least one header is present
      ret.write (CHttp.EOL.getBytes (CHttp.HTTP_CHARSET));
    }
    return ret;
  }

  public void onEndRequest (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                            @Nullable final Exception aCaughtException)
  {
    // empty
  }
}
