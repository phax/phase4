/*
 * Copyright (C) 2020-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phase4.config.AS4Configuration;

@Immutable
public final class APConfig
{
  private APConfig ()
  {}

  @Nonnull
  private static IConfigWithFallback _getConfig ()
  {
    return AS4Configuration.getConfig ();
  }

  @Nullable
  public static String getMyPeppolSeatID ()
  {
    return _getConfig ().getAsString ("peppol.seatid");
  }

  @Nullable
  public static String getMySmpUrl ()
  {
    return _getConfig ().getAsString ("smp.url");
  }

  @Nullable
  public static String getPhase4ApiRequiredToken ()
  {
    return _getConfig ().getAsString ("phase4.api.requiredtoken");
  }

  @Nullable
  public static String getHttpProxyHost ()
  {
    return _getConfig ().getAsString ("http.proxy.host");
  }

  public static int getHttpProxyPort ()
  {
    return _getConfig ().getAsInt ("http.proxy.port");
  }
}
