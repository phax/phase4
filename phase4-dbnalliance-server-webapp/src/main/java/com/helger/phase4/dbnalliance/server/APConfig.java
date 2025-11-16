/*
 * Copyright (C) 2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.dbnalliance.server;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.dbnalliance.commons.EDBNAllianceStage;
import com.helger.phase4.config.AS4Configuration;

@Immutable
public final class APConfig
{
  private APConfig ()
  {}

  @NonNull
  public static IConfigWithFallback getConfig ()
  {
    return AS4Configuration.getConfig ();
  }

  @NonNull
  public static EDBNAllianceStage getStage ()
  {
    final String sStageID = getConfig ().getAsString ("dbnalliance.stage");
    final EDBNAllianceStage ret = EDBNAllianceStage.getFromIDOrNull (sStageID);
    if (ret == null)
      throw new IllegalStateException ("Failed to determine DBNAlliance stage from value '" + sStageID + "'");
    return ret;
  }

  @Nullable
  public static String getMySeatID ()
  {
    return getConfig ().getAsString ("dbnalliance.seatid");
  }

  @Nullable
  public static String getMySmpUrl ()
  {
    return getConfig ().getAsString ("smp.url");
  }

  @Nullable
  public static String getPhase4ApiRequiredToken ()
  {
    return getConfig ().getAsString ("phase4.api.requiredtoken");
  }

  @Nullable
  public static String getHttpProxyHost ()
  {
    return getConfig ().getAsString ("http.proxy.host");
  }

  public static int getHttpProxyPort ()
  {
    return getConfig ().getAsInt ("http.proxy.port");
  }
}
