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
package com.helger.phase4.incoming;

import java.nio.charset.Charset;

import javax.annotation.Nonnull;

import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.IHasInputStream;
import com.helger.commons.mime.IMimeType;

/**
 * A synthetic wrapper for an AS4 HTTP response.
 *
 * @author Philip Helger
 * @since 0.9.6
 */
public interface IAS4ResponseAbstraction
{
  /**
   * Set the response payload as a byte array with a certain character set. This
   * is called, if the an XML response is sent back.
   *
   * @param aBytes
   *        The bytes to be set. May not be <code>null</code>.
   * @param aCharset
   *        The character set of the byte array. May not be <code>null</code>.
   * @since 0.9.7 this was merged from setContent and setCharset
   */
  void setContent (@Nonnull byte [] aBytes, @Nonnull Charset aCharset);

  /**
   * Set the content as an input stream provider. This is used if a MIME
   * response is sent back.
   *
   * @param aHeaderMap
   *        Custom HTTP headers to be used. Never <code>null</code> but maybe
   *        empty.
   * @param aHasIS
   *        The input stream provider.
   * @since 0.9.9 this was merged from addCustomResponseHeaders and setContent
   */
  void setContent (@Nonnull HttpHeaderMap aHeaderMap, @Nonnull IHasInputStream aHasIS);

  /**
   * Set the MIME type (Content-Type) of the response.
   *
   * @param aMimeType
   *        Mime type to use. May not be <code>null</code>.
   */
  void setMimeType (@Nonnull IMimeType aMimeType);

  /**
   * Set the HTTP status code to be returned.
   *
   * @param nStatusCode
   *        The HTTP status code.
   */
  void setStatus (int nStatusCode);
}
