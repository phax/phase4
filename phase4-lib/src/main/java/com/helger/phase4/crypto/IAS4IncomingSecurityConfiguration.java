/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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

import java.security.Provider;

import javax.annotation.Nullable;

/**
 * Interface to configure the security configuration for incoming messages.
 *
 * @author Philip Helger
 * @since 2.1.3
 */
public interface IAS4IncomingSecurityConfiguration
{
  /**
   * @return The Java Security provider to be used for incoming messages. May be
   *         <code>null</code> to indicate the usage of the default JDK security
   *         provider.
   */
  @Nullable
  Provider getSecurityProvider ();
}
