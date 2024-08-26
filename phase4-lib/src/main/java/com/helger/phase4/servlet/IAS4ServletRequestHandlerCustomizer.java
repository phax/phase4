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
package com.helger.phase4.servlet;

import javax.annotation.Nonnull;

import com.helger.phase4.incoming.AS4RequestHandler;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * This is a special callback handler that is meant to be used in combination
 * with the Servlet handler to customize the incoming request handler.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public interface IAS4ServletRequestHandlerCustomizer
{
  /**
   * Called before the message is handled. <br>
   * Note: was called "customize" until v0.9.4
   *
   * @param aRequestScope
   *        Request scope. Never <code>null</code>.
   * @param aUnifiedResponse
   *        The response to be filled. Never <code>null</code>.
   * @param aRequestHandler
   *        The main handler doing the hard work. Never <code>null</code>.
   */
  void customizeBeforeHandling (@Nonnull IRequestWebScopeWithoutResponse aRequestScope,
                                @Nonnull AS4UnifiedResponse aUnifiedResponse,
                                @Nonnull AS4RequestHandler aRequestHandler);

  /**
   * Called after the message was handled, and no exception was thrown.
   *
   * @param aRequestScope
   *        Request scope. Never <code>null</code>.
   * @param aUnifiedResponse
   *        The response to be filled. Never <code>null</code>.
   * @param aRequestHandler
   *        The main handler doing the hard work. Never <code>null</code>.
   * @since 0.9.5
   */
  void customizeAfterHandling (@Nonnull IRequestWebScopeWithoutResponse aRequestScope,
                               @Nonnull AS4UnifiedResponse aUnifiedResponse,
                               @Nonnull AS4RequestHandler aRequestHandler);
}
