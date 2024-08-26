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

import javax.annotation.Nullable;

/**
 * Contains some configuration properties for incoming messages.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public interface IAS4IncomingReceiverConfiguration
{
  /**
   * Get the URL of this receiving AS4 endpoint. This is used e.g. to resolve a
   * PMode and use the correct address.
   *
   * @return The URL of the receiving AS4 endpoint.
   */
  @Nullable
  String getReceiverEndpointAddress ();
}
