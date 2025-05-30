/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.crypto;

import javax.annotation.Nonnull;

import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;

/**
 * Customize the {@link WSSecSignature} object additional to what is possible
 * via the {@link AS4SigningParams} class.
 *
 * @author Philip Helger
 * @since 2.2.2.
 */
public interface IWSSecSignatureCustomizer
{
  /**
   * Create an overloaded version of WSSecSignature
   *
   * @param aSecHeader
   *        The security header to start with.
   * @return Never <code>null</code>.
   */
  @Nonnull
  default WSSecSignature createWSSecSignature (@Nonnull final WSSecHeader aSecHeader)
  {
    return new WSSecSignature (aSecHeader);
  }

  /**
   * The customization happens AFTER all the default properties are applied. So
   * be sure you know what to do when overwriting stuff.
   *
   * @param aWSSecSignature
   *        The object to modify. May not be <code>null</code>.
   */
  default void customize (@Nonnull final WSSecSignature aWSSecSignature)
  {}
}
