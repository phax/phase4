/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import org.apache.wss4j.common.WSS4JConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * Enumeration of key wrap algorithms used in key agreement schemes. The key wrap algorithm wraps
 * the content encryption key using the derived key from key derivation.
 *
 * @author Philip Helger
 * @since 4.4.0
 */
public enum ECryptoKeyWrapAlgorithm implements IHasID <String>
{
  /** AES-128 Key Wrap (eDelivery AS4 2.0) */
  AES_128 (WSS4JConstants.KEYWRAP_AES128),
  /** AES-192 Key Wrap */
  AES_192 (WSS4JConstants.KEYWRAP_AES192),
  /** AES-256 Key Wrap */
  AES_256 (WSS4JConstants.KEYWRAP_AES256),
  /** Triple DES Key Wrap */
  TRIPLE_DES (WSS4JConstants.KEYWRAP_TRIPLEDES);

  private final String m_sID;

  ECryptoKeyWrapAlgorithm (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public static ECryptoKeyWrapAlgorithm getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ECryptoKeyWrapAlgorithm.class, sID);
  }

  @Nullable
  public static ECryptoKeyWrapAlgorithm getFromIDOrDefault (@Nullable final String sID,
                                                            @Nullable final ECryptoKeyWrapAlgorithm eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ECryptoKeyWrapAlgorithm.class, sID, eDefault);
  }
}
