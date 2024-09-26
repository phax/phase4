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
package com.helger.phase4.sender;

import javax.annotation.Nonnull;

/**
 * Specific callback interface for {@link ValidatingAS4SignalMsgConsumer} to
 * handle the results in a structured way.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public interface IAS4SignalMessageValidationResultHandler
{
  /**
   * Called if no issues were found between the sent and the received receipts.
   * Called 0 to 1 times.
   */
  void onSuccess ();

  /**
   * Called for each error found.
   *
   * @param sErrorMsg
   *        The error text in human readable string what went wrong.
   */
  void onError (@Nonnull String sErrorMsg);

  /**
   * This method is only called if sent and/or received message did not contain
   * DSig references, so there was nothing to compare to.
   */
  void onNotApplicable ();
}
