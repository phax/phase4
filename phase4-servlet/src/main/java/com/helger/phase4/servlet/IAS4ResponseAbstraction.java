/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import java.nio.charset.Charset;

import javax.annotation.Nonnull;

import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.IHasInputStream;
import com.helger.commons.mime.IMimeType;

/**
 * A synthetic wrapper for an AS4 HTTP response. By default
 * {@link AS4UnifiedResponse} is the logical implementation, but the return
 * types are different.
 *
 * @author Philip Helger
 * @since 0.9.6
 */
public interface IAS4ResponseAbstraction
{
  void addCustomResponseHeaders (@Nonnull HttpHeaderMap aHeaderMap);

  /**
   * Set the response payload as a byte array with a certain character set.
   * 
   * @param aBytes
   *        The bytes to be set. May not be <code>null</code>.
   * @param aCharset
   *        The character set of the byte array. May not be <code>null</code>.
   * @since 0.9.7 this was merged from setContent and setCharset
   */
  void setContent (@Nonnull byte [] aBytes, @Nonnull Charset aCharset);

  void setContent (@Nonnull IHasInputStream aHasIS);

  void setMimeType (@Nonnull IMimeType aMimeType);

  void setStatus (int nStatusCode);

  @Nonnull
  static IAS4ResponseAbstraction wrap (@Nonnull final AS4UnifiedResponse aHttpResponse)
  {
    return new IAS4ResponseAbstraction ()
    {
      public void addCustomResponseHeaders (@Nonnull final HttpHeaderMap aHeaderMap)
      {
        aHttpResponse.addCustomResponseHeaders (aHeaderMap);
      }

      public void setContent (@Nonnull final byte [] aBytes, @Nonnull final Charset aCharset)
      {
        aHttpResponse.setContent (aBytes);
        aHttpResponse.setCharset (aCharset);
      }

      public void setContent (@Nonnull final IHasInputStream aHasIS)
      {
        aHttpResponse.setContent (aHasIS);
      }

      public void setMimeType (@Nonnull final IMimeType aMimeType)
      {
        aHttpResponse.setMimeType (aMimeType);
      }

      public void setStatus (final int nStatusCode)
      {
        aHttpResponse.setStatus (nStatusCode);
      }
    };
  }
}
