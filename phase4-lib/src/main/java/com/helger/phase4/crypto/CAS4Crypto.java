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

import javax.annotation.concurrent.Immutable;

import com.helger.security.keystore.EKeyStoreType;

/**
 * Constant values for the AS4 cryptography.
 *
 * @author Philip Helger
 */
@Immutable
public final class CAS4Crypto
{
  public static final EKeyStoreType DEFAULT_KEY_STORE_TYPE = EKeyStoreType.JKS;
  public static final EKeyStoreType DEFAULT_TRUST_STORE_TYPE = EKeyStoreType.JKS;
  public static final String DEFAULT_CONFIG_PREFIX = "org.apache.wss4j.crypto.merlin.";

  private CAS4Crypto ()
  {}
}
