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
package com.helger.phase4.model;

/**
 * Messaging operation to use.
 * 
 * @author Philip Helger
 */
public enum EMessagingOperation
{
  /**
   * This operation transfers enough data from the producer to the Sending MSH
   * to generate an ebMS User Message Unit.
   */
  SUBMIT,
  /**
   * This operation makes data of a previously received (via Receive operation)
   * ebMS User Message Unit available to the Consumer.
   */
  DELIVER,
  /**
   * This operation notifies either a Producer or a Consumer about the status of
   * a previously submitted or received ebMS User Message Unit, or about general
   * MSH status.
   */
  NOTIFY,
  /**
   * This operation initiates the transfer of an ebMS user message from the
   * Sending MSH to the Receiving MSH, after all headers intended for the
   * Receiving MSH have been added (including security and/or reliability, as
   * required).
   */
  SEND,
  /**
   * This operation completes the transfer of an ebMS user message from the
   * Sending MSH to the Receiving MSH. A successful reception means that a
   * contained User Message Unit is now available for further processing by the
   * Receiving MSH.
   */
  RECEIVE;
}
