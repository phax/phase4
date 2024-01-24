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
package com.helger.phase4.crypto;

import javax.annotation.Nonnull;

import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;

/**
 * Callback interface for modifying the decryption {@link RequestData} WSS4J
 * object.
 *
 * @author Philip Helger
 * @since 2.2.0
 */
public interface IAS4DecryptParameterModifier
{
  /**
   * Modify the provided {@link WSSConfig} to add additional handlers. This was
   * created based on issue #150.
   *
   * @param aWSSConfig
   *        The {@link WSSConfig} to be modified.
   */
  default void modifyWSSConfig (@Nonnull final WSSConfig aWSSConfig)
  {}

  /**
   * Modify the provided {@link RequestData} object, e.g. by allowing RSA 1.5
   * algorithms. This method is called after all the default setters, so be
   * careful not to overwrite standard fields, to avoid creating unintended side
   * effects.
   *
   * @param aRequestData
   *        The object to be modified. Never <code>null</code>.
   */
  default void modifyRequestData (@Nonnull final RequestData aRequestData)
  {}
}
