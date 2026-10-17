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
package com.helger.phase4.messaging.http;

import java.io.IOException;

import org.jspecify.annotations.NonNull;

/**
 * Exception that indicates that the peer answered with HTTP 401 (Unauthorized). Phase4 has no
 * support for retrying with credentials, so this is a hard failure. It is derived from
 * {@link IOException} so that it passes through the HTTP response handler, where it explicitly
 * stops all further retries.
 *
 * @author Philip Helger
 * @since 4.8.2
 */
public class AS4HttpUnauthorizedException extends IOException
{
  public AS4HttpUnauthorizedException (@NonNull final String sStatusLine)
  {
    super ("The peer rejected the request with '" + sStatusLine + "' - authentication is not supported");
  }
}
