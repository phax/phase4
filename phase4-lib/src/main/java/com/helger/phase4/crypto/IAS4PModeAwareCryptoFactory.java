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

import org.jspecify.annotations.NonNull;

import com.helger.phase4.model.pmode.IPMode;

/**
 * An extended {@link IAS4CryptoFactory} with the capability to receive a P-Mode that was
 * successfully resolved during reception phase. This information can be used to provide P-Mode
 * specific crypto information for decryption.<br>
 * Source: https://github.com/phax/phase4/pull/121
 *
 * @author Gregor Scholtysik
 * @since 2.1.0
 */
public interface IAS4PModeAwareCryptoFactory extends IAS4CryptoFactory
{
  /**
   * Set the P-Mode in context.<br>
   * This method is only called on reception side after successful resolving of the P-Mode from the
   * incoming ebms information.
   *
   * @param pMode
   *        the P-Mode resolved during reception
   */
  void setContextPMode (@NonNull IPMode pMode);
}
